# Microservices Architecture Proposal

## Traffic and data analysis

Baseline assumptions: 5M registered users, active daily users ~10% (500k DAU).

Estimated write traffic:
- Each active user averages 20 visits + 10 likes per day
- Visits: 500k × 20 = 10M writes/day → ~115 writes/sec average, peak ~3–5x = 350–575 writes/sec
- Likes: 500k × 10 = 5M writes/day → ~58 writes/sec average, peak ~175 writes/sec
- Total write peak: ~750 writes/sec

Estimated read traffic (read:write ratio typically 10:1 for social apps):
- Profile reads, visitor lists, like lists: ~7,500 reads/sec at peak

Data volume:
- profile_visits: 10M rows/day → ~3.6B rows/year (requires partitioning by time)
- user_likes: 5M rows/day → ~1.8B rows/year
- fraud_records: low volume, negligible

Implications for service split:
- The visit/like write path is the hottest — Interaction Service needs horizontal scaling independent of User Service
- Read traffic on visitor/like lists justifies a dedicated Query Service with Redis caching to avoid hammering the DB
- Fraud detection runs an aggregation query on every write — at 750 writes/sec this becomes a bottleneck; async decoupling via Kafka is necessary at this scale
- profile_visits will need time-based table partitioning (e.g. monthly) to keep query performance acceptable as rows accumulate

---

## Assumptions, Non-goals, and Trade-offs

### Assumptions

- Seconds-level fraud propagation lag is acceptable; real-time hard blocking is only required on the write path.
- Each service owns its own database schema; cross-service reads go through APIs or events, not shared tables.
- The monolith's existing PostgreSQL schema is the starting point — no data migration strategy is covered here.

### Non-goals

- No data migration plan from monolith to microservices.
- No multi-region or active-active setup.
- Query Service, Notification Service, and Audit Service are listed as future candidates only.

### Trade-offs

**Why 3 core services (User, Interaction, Fraud), not more?**
Splitting further adds operational overhead before there is evidence of a bottleneck. Start with the minimal split that removes the main pain points, then extract further based on actual load.

**Why hard gate (sync) + event (async) dual-layer for fraud?**
The sync check (Redis lookup before each write) gives immediate blocking. The async Kafka event handles downstream side effects without blocking the write path. Async-only would leave a window where a fraud user can still write between detection and propagation.

**Why Redis for fraud status?**
A DB call on every write adds latency and load. Redis gives sub-millisecond reads. The trade-off is eventual consistency — acceptable given the sync detection already runs on the write path.

---

## Failure modes and degradation

### Kafka unavailable

- Writes still succeed; downstream consumers drift until Kafka recovers and replays.
- Mitigation: outbox pattern — write events to a local `outbox` table in the same transaction, relay to Kafka via a separate process.

### Fraud Service unavailable

- Fallback: use last-known Redis value. If Redis also has no entry, fail open and log a warning.
- Circuit breaker opens after repeated failures; fallback activates automatically.

### Redis unavailable

- Fallback: Interaction Service calls Fraud Service directly (sync HTTP). If that also fails, fail open with a warning log.
- Read endpoints fall back to DB reads — higher latency but correct.

---

## Current state

Everything lives in one service: user management, visits, likes, fraud detection, bulk ops. Main pain points at scale:
- fraud detection runs synchronously on every write, slowing down the API
- can't scale the read-heavy query layer without scaling everything else
- one bad deploy can take down the whole thing

---

## Proposed split

```
API Gateway (GCP Cloud Endpoints / Apigee)
-- User Service          CRUD users, profiles
-- Interaction Service   visits, likes
-- Fraud Service         detection, scoring, blocking
-- Query Service         read-optimized views, Redis cache (future)
-- Notification Service  alerts, emails (future)
-- Audit Service         logging, compliance (future)
```

All services publish/consume events via Kafka. The gateway is the only public entry point.

---

## Service definitions

### User Service

Owns the `users` table.

```
POST   /api/v1/users
GET    /api/v1/users/{id}/profile
PUT    /api/v1/users/{id}
DELETE /api/v1/users/{id}
POST   /api/v1/users/bulk
```

### Interaction Service

Owns `profile_visits` and `user_likes`. Checks fraud status before each write, then publishes an event.

```
POST   /api/v1/interactions/visit
POST   /api/v1/interactions/like
GET    /api/v1/interactions/visitors/{userId}
GET    /api/v1/interactions/likes/{userId}
```

### Fraud Service

Runs detection logic, marks users, publishes `USER_FRAUD_MARKED`.

```
GET    /api/v1/fraud/status/{userId}
POST   /api/v1/fraud/check
GET    /api/v1/fraud/history/{userId}
```

Detection rule: 100+ visits AND 100+ likes within 10 minutes. Threshold configurable.

---

## API versioning

URL-based: `/api/v1/`, `/api/v2/`. Breaking changes get a new major version, supported in parallel for 12 months.

---

## Circuit breaking

Using Resilience4j on all inter-service calls.

```
# Example values for demo — tune via load test against actual SLA targets
failureRateThreshold: 50%
waitDurationInOpenState: 30s
permittedNumberOfCallsInHalfOpenState: 3
slidingWindowSize: 10
```

Thresholds depend on p99 latency of downstream services and acceptable error budget. Set after profiling, not upfront.

---

## Fraud state propagation

When Fraud Service marks a user, it publishes `USER_FRAUD_MARKED` to Kafka:

```json
{
  "eventType": "USER_FRAUD_MARKED",
  "userId": 12345,
  "timestamp": "2026-02-23T10:00:00Z",
  "reason": "100+ visits and likes within 10 minutes",
  "metadata": { "visitCount": 150, "likeCount": 120, "timeWindowMinutes": 10 }
}
```

Consumers:
- Interaction Service — sets blocked flag in Redis
- Query Service — invalidates cache
- Notification Service — sends alert

Consistency: Interaction Service checks Redis before each write (hard gate, sync). Kafka consumers update local state async. Event lag depends on consumer throughput and partition count; seconds-level lag is assumed acceptable. In production, monitor consumer lag and alert if it exceeds the agreed SLA threshold.

---

## Visit action flow

1. API Gateway — auth + rate limit
2. Interaction Service checks fraud status from Redis (fallback: Fraud Service HTTP)
3. Records the visit
4. Publishes `VisitRecorded` to Kafka
5. Query Service and Audit Service consume independently

---

## Fraud detection flow

1. `VisitRecorded` / `LikeRecorded` event triggers Fraud Service
2. Queries visit + like counts in the time window
3. If threshold exceeded — marks user, publishes `USER_FRAUD_MARKED`
4. Interaction Service sets blocked flag in Redis
5. Notification Service sends alert

---

## Canary release and traffic splitting

Gradual traffic splitting at the gateway level allows migrating from monolith to microservices without a hard cutover. A small percentage of traffic is routed to the new service first; if metrics are stable, the percentage increases incrementally until the new service handles 100% of traffic. At any point, routing can be shifted back to the monolith instantly without data migration.

GCP Cloud Endpoints / Apigee supports weighted routing natively; Istio can handle splitting at the service mesh level without application changes.

---

## Tech stack

- API Gateway: GCP Cloud Endpoints / Apigee
- Service mesh: Kubernetes + Istio
- Message broker: Apache Kafka
- Database: PostgreSQL (with time-based partitioning for high-volume tables)
- Cache: Redis
- Circuit breaker: Resilience4j
- Service discovery: GCP Cloud DNS / Istio service registry
- Logging: GCP Cloud Logging + ELK
- Monitoring: Prometheus + Grafana / GCP Cloud Monitoring

---

## Deployment

Blue-green: deploy to green, run smoke tests, switch traffic, monitor, rollback if needed. Combined with canary release for gradual migration from monolith.
