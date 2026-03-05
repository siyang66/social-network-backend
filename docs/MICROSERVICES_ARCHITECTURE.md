# Meet5 Microservices Architecture Proposal

## Business Context

Meet5 is Germany's leading social dating app for 40+ age group:
- **3M+ users**, 500k DAU (10% daily active)
- **50M requests/day** (~580 QPS avg, 1,700-2,900 QPS peak)
- **Core features**: Events (social meetups), Members (profiles/matching), Chat (messaging), Fraud Detection, Crawler Detection

---

## Traffic Analysis

### Traffic Distribution (50M req/day)
| Feature | % | Requests/Day | Peak QPS |
|---------|---|--------------|----------|
| Members (profiles, search) | 40% | 20M | 700 |
| Events (browse, join) | 30% | 15M | 525 |
| Chat (messages) | 20% | 10M | 350 |
| Fraud/Crawler checks | 10% | 5M | 180 |

### Data Volume (Annual)
- **messages**: 1B rows/year (needs partitioning)
- **profile_visits**: 3.6B rows/year (hot write path)
- **user_likes**: 1.8B rows/year
- **event_participations**: 50M rows/year
- **fraud_records**: 100k rows/year

### Bottlenecks
1. Chat messages (1B rows/year) → time-based partitioning needed
2. Profile visits/likes (5.4B rows/year) → async processing needed
3. Fraud detection (runs on every interaction) → sync bottleneck at scale

---

## Why Microservices

### Monolith Pain Points
- **Tight coupling**: fraud detection blocks write path
- **Cannot scale independently**: chat needs 3x instances of events
- **Deployment risk**: one bad deploy affects everything
- **Team bottleneck**: multiple teams on same codebase

### Benefits
- Independent scaling (chat 3x, events 2x, members 1x)
- Team autonomy (deploy independently)
- Technology flexibility (WebSocket for chat, Elasticsearch for events)
- Fault isolation (chat downtime doesn't affect events)

---

## Service Architecture

### Service Split (5 Core Services)

**Business Services** (domain-driven):
1. **Events Service**: event CRUD, search (Elasticsearch), join/leave, recommendations
2. **Members Service**: profile CRUD, matching, discovery, likes, visits
3. **Chat Service**: real-time messaging (WebSocket), message history

**Platform Services** (cross-cutting):
4. **Fraud Service**: rule engine + ML scoring, consumes events via Kafka, blocks users
5. **Crawler Detection Service**: rate limiting, bot detection, CAPTCHA

### Why This Split?
- **Events + Members + Chat**: different scaling needs, different tech stacks, different teams
- **Fraud + Crawler**: shared by all services, compute-intensive, scale independently

---

## Data Flow & Event Streaming

### Request Flow
```
Users → Load Balancer → API Gateway (Apigee)
  ├─> Events Service
  ├─> Members Service
  └─> Chat Service
       ↓
  Check Fraud/Crawler (sync)
       ↓
  Write to Cloud SQL + Publish Event to Kafka
       ↓
  Fraud Service consumes events (async)
```

### Kafka Events
- `PROFILE_VISITED` → Fraud Service counts visits
- `PROFILE_LIKED` → Fraud Service counts likes
- `EVENT_JOINED` → Fraud Service detects rapid joins
- `MESSAGE_SENT` → Fraud Service detects spam
- `USER_FRAUD_MARKED` → All services update Redis flags

### Fraud Detection Flow
1. **Sync check** (before write): Redis lookup for fraud flag → immediate blocking
2. **Async detection** (after write): Kafka event → Fraud Service aggregates → marks user → publishes `USER_FRAUD_MARKED`
3. **State propagation**: Services consume `USER_FRAUD_MARKED` → update Redis

**Rules**:
- 100+ visits AND 100+ likes within 10 minutes
- 10+ event joins within 1 minute
- 50+ messages within 5 minutes

---

## GCP Deployment

### Tech Stack
| Component | GCP Service | Purpose |
|-----------|-------------|---------|
| Compute | Cloud Run | Auto-scaling, WebSocket support |
| Database | Cloud SQL (PostgreSQL) | 1 master + 2 read replicas |
| Cache | Memorystore (Redis) | Fraud flags, session, rate counters |
| Events | Cloud Pub/Sub | Event streaming |
| Search | Elasticsearch (GKE) | Event search with geo-location |
| Gateway | Apigee | Routing, auth, rate limiting |
| Monitoring | Cloud Monitoring + Logging | Metrics, logs, traces |

### Cost Estimate
- **Current scale** (50M req/day): $3,000-5,000/month
- **10x scale** (500M req/day): $5,000-8,000/month

---

## Migration Strategy (14 Weeks)

### Strangler Fig Pattern

**Phase 1-2: Preparation** (Week 1-2)
- Add outbox table to monolith
- Set up GCP infrastructure (Cloud SQL, Redis, Pub/Sub)
- Deploy monitoring

**Phase 3-4: Extract Fraud Service** (Week 3-4)
- Deploy in shadow mode (consumes events, doesn't block)
- Validate accuracy
- Canary release: 5% → 100%

**Phase 5-6: Extract Members Service** (Week 5-6)
- Deploy Members Service
- Dual-write (monolith + microservice)
- Canary release: 5% → 100%

**Phase 7-9: Extract Events Service** (Week 7-9)
- Deploy with Elasticsearch
- Dual-write
- Canary release: 5% → 100%

**Phase 10-12: Extract Chat Service** (Week 10-12)
- Deploy with WebSocket support
- Migrate active conversations
- Canary release: 5% → 100%

**Phase 13-14: Cleanup** (Week 13-14)
- Stop dual-write
- Archive monolith data
- Keep rollback switch for 1 month

### Parallel Feature Development
- **New features**: build in microservices directly
- **Legacy features**: migrate incrementally
- **Control**: API Gateway routing + feature flags

---

## Key Metrics

### Service Health
- Latency: P50, P95, P99 per endpoint
- Throughput: requests/sec per service
- Error Rate: 4xx, 5xx per endpoint
- Availability: 99.9% uptime target

### Business Metrics
- Event joins/day
- Messages sent/day
- Fraud blocks/day
- Crawler blocks/day

### Infrastructure
- DB connection pool usage
- Redis hit rate
- Pub/Sub lag
- Cloud Run instances

---

## Summary

**Service Split**: Events, Members, Chat (business) + Fraud, Crawler Detection (platform)

**Event-Driven**: Kafka/Pub/Sub for async communication, Redis for sync fraud checks

**Migration**: 14-week Strangler Fig pattern, shadow mode → canary → full migration

**GCP Stack**: Cloud Run + Cloud SQL + Memorystore + Pub/Sub + Elasticsearch

**Cost**: $3,000-5,000/month for 50M req/day


---

## Migration Strategy: Strangler Fig Pattern

### Overview
Gradually replace monolith functionality with microservices while keeping both running in parallel. No big-bang cutover, zero downtime, instant rollback capability.

### 6-Phase Migration Plan

**Phase 1: Preparation (Week 1-2)**
- Add outbox table to monolith for event publishing
- Introduce event contracts (`VISIT_RECORDED`, `LIKE_RECORDED`, `USER_FRAUD_MARKED`)
- Set up GCP infrastructure (Cloud SQL, Memorystore, Pub/Sub)
- Deploy monitoring (Cloud Monitoring, Cloud Logging, Cloud Trace)

**Phase 2: Extract Fraud Service (Week 3-4)**
- Deploy Fraud Service on Cloud Run (shadow mode)
- Monolith publishes events to Pub/Sub, Fraud Service consumes
- Fraud Service writes to separate DB, no production impact
- Compare fraud detection results (monolith vs microservice) for validation

**Phase 3: Canary Fraud Service (Week 5-6)**
- Route 5% of fraud checks to Fraud Service via API Gateway
- Monitor error rate, latency, accuracy
- Gradually increase to 10% → 25% → 50% → 100%
- Monolith remains as fallback (circuit breaker)

**Phase 4: Extract Interaction Service (Week 7-10)**
- Deploy Interaction Service on Cloud Run
- Implement dual-write: write to both monolith and Interaction Service
- Run reconciliation job to compare data consistency
- Route 5% read traffic to Interaction Service, gradually increase

**Phase 5: Switch Primary Traffic (Week 11-12)**
- Once metrics stable (error rate < 0.1%, P99 < 200ms), switch primary writes to Interaction Service
- Monolith becomes read-only backup
- Keep dual-write for 2 weeks for safety

**Phase 6: Decommission Monolith Modules (Week 13-14)**
- Stop dual-write, Interaction Service is single source of truth
- Archive monolith interaction tables
- Keep rollback switch for 1 month

### Parallel Feature Development Strategy

**Strangler Fig Approach**:
1. **New features**: build directly in microservices (no monolith code)
2. **Legacy features**: migrate incrementally using phases above
3. **Shared features**: use API Gateway routing + feature flags

**Example: Adding "Super Like" Feature**
- Build in Interaction Service (new endpoint `/api/v1/interactions/super-like`)
- API Gateway routes `/super-like` to microservice only
- No monolith changes needed
- Deploy independently, rollback independently

**Traffic Control Mechanisms**:
- **API Gateway Routing**: URL-based routing (`/api/v1/*` → monolith, `/api/v2/*` → microservices)
- **Feature Flags**: enable/disable features per user cohort (use Cloud Firestore or Redis)
- **Weighted Routing**: percentage-based traffic split (Cloud Endpoints supports natively)

---

## Failure Modes and Degradation

### Pub/Sub Unavailable
- **Impact**: events not published, downstream consumers lag
- **Mitigation**: outbox pattern (write events to local DB, relay via separate process)
- **Fallback**: writes succeed, events replayed when Pub/Sub recovers

### Fraud Service Unavailable
- **Impact**: cannot check fraud status
- **Mitigation**: circuit breaker opens after 50% failure rate
- **Fallback**: use last-known Redis value → if Redis empty, fail-open with warning log

### Redis Unavailable
- **Impact**: fraud checks slow, cache misses
- **Mitigation**: fallback to Fraud Service HTTP call (sync)
- **Fallback**: if Fraud Service also down, fail-open (allow writes, log alert)

### Cloud SQL Unavailable
- **Impact**: writes fail, reads fail
- **Mitigation**: read replicas for reads, circuit breaker for writes
- **Fallback**: return 503, retry with exponential backoff

---

## State Propagation and Consistency

### Fraud State Propagation
**Event**: `USER_FRAUD_MARKED`
```json
{
  "eventType": "USER_FRAUD_MARKED",
  "userId": 12345,
  "timestamp": "2026-03-05T10:00:00Z",
  "reason": "100+ visits and likes within 10 minutes",
  "metadata": { "visitCount": 150, "likeCount": 120 }
}
```

**Consumers**:
- Interaction Service → update Redis flag (hard gate for future writes)
- Query Service → invalidate cache
- Notification Service → send alert

**Consistency Model**:
- **Strong consistency**: sync Redis check before each write (immediate blocking)
- **Eventual consistency**: Kafka consumers update local state (seconds-level lag acceptable)
- **Monitoring**: alert if consumer lag > 5 seconds

### Data Consistency Guarantees
1. **Within Service**: ACID transactions via Cloud SQL
2. **Across Services**: eventual consistency via events + idempotent consumers
3. **Critical State**: Redis as shared state store (fraud flags, rate limit counters)
4. **Reconciliation**: daily batch job compares monolith vs microservice data

---

## Tech Stack Summary

| Component | GCP Service | Purpose |
|-----------|-------------|---------|
| API Gateway | Cloud Endpoints / Apigee | routing, auth, rate limiting |
| Compute | Cloud Run / App Engine | stateless services, auto-scaling |
| Database | Cloud SQL (PostgreSQL) | transactional data, read replicas |
| Cache | Memorystore (Redis) | fraud flags, hot data |
| Message Broker | Cloud Pub/Sub | event streaming |
| Monitoring | Cloud Monitoring | metrics, alerts |
| Logging | Cloud Logging | centralized logs |
| Tracing | Cloud Trace | distributed tracing |
| Secret Management | Secret Manager | API keys, DB credentials |

---

## Deployment and Release Strategy

### Blue-Green Deployment
1. Deploy new version to "green" environment
2. Run smoke tests (health checks, integration tests)
3. Switch Cloud Load Balancer to green
4. Monitor for 30 minutes (error rate, latency, throughput)
5. If issues detected → instant rollback to blue
6. If stable → decommission blue after 24 hours

### Canary Release (for migration)
1. Route 5% traffic to new service via API Gateway
2. Monitor metrics (error rate < 0.1%, P99 < 200ms)
3. If stable → increase to 10% → 25% → 50% → 100%
4. Each step runs for 24-48 hours
5. Rollback at any step if metrics degrade

### Feature Flags
- Use Cloud Firestore or Redis for flag storage
- Enable features per user cohort (e.g., 10% beta users)
- Kill switch for instant disable without deployment

---

## Observability and Monitoring

### Key Metrics
- **Latency**: P50, P95, P99 per endpoint
- **Throughput**: requests/sec per service
- **Error Rate**: 4xx, 5xx per endpoint
- **Consumer Lag**: Pub/Sub subscription delay
- **Circuit Breaker State**: open/closed/half-open
- **Cache Hit Rate**: Redis hit/miss ratio

### Alerts
- P99 latency > 200ms for 5 minutes → page on-call
- Error rate > 1% for 5 minutes → page on-call
- Consumer lag > 10 seconds → warning
- Circuit breaker open > 1 minute → warning
- Cloud SQL connection pool > 80% → warning

### Distributed Tracing
- Use Cloud Trace to track requests across services
- Trace ID propagated via HTTP headers (`X-Cloud-Trace-Context`)
- Identify bottlenecks (slow DB queries, high latency services)

---

## Cost Estimation (Rough)

**Assumptions**: 50M requests/day, 500k DAU

| Service | GCP Component | Monthly Cost (USD) |
|---------|---------------|-------------------|
| Compute | Cloud Run (3 services) | $300-500 |
| Database | Cloud SQL (1 master + 2 replicas) | $500-800 |
| Cache | Memorystore (Redis 5GB) | $150-200 |
| Message Broker | Cloud Pub/Sub | $100-150 |
| Load Balancer | Cloud Load Balancer | $50-100 |
| Monitoring | Cloud Monitoring + Logging | $100-200 |
| **Total** | | **$1,200-2,000/month** |

**Scaling to 500M requests/day (10x)**: ~$5,000-8,000/month (mostly DB + compute)

---

## Summary

**Migration Timeline**: 14 weeks (3.5 months) for core split

**Key Benefits**:
- Independent scaling (Interaction Service can scale without User Service)
- Async fraud detection (removes bottleneck from write path)
- Zero downtime migration (Strangler Fig + canary release)
- Parallel feature development (new features in microservices, legacy in monolith)

**Risk Mitigation**:
- Dual-write + reconciliation (data consistency)
- Circuit breakers + fallbacks (graceful degradation)
- Canary release (gradual rollout, instant rollback)
- Monitoring + alerts (early detection of issues)

**Next Steps**:
1. Set up GCP project + infrastructure
2. Implement outbox pattern in monolith
3. Deploy Fraud Service in shadow mode
4. Run load tests to validate performance assumptions
