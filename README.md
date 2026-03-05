# social-network-backend

Backend service for a social network app. Core features: profile visits, likes, and time-window based fraud detection.

---

## Requirements Analysis

### Functional Requirements
- Record visit and like interactions
- Retrieve visitor lists (sorted by time)
- Time-window based fraud detection (100+ visits AND 100+ likes within 10 minutes)

### Non-Functional Requirements
- **Performance**: indexing, bulk writes, read/write splitting
- **Reliability**: rate limiting (20 req/s), circuit breaking
- **Security**: JWT-based trusted identity injection
- **Maintainability**: clear layered architecture, unified exception handling
- **Scalability**: service boundaries ready for microservice extraction

### Key Characteristics
- Hot write path requiring protection
- Real-time fraud control (immediate blocking)
- Microservice-ready design

---

## System Architecture

### Layered Design
```
Controller  → HTTP protocol, request/response handling
Service     → Business logic, transactions, fraud checks
Mapper      → SQL access via MyBatis
Model       → Data structures and validation
```

### Cross-Cutting Concerns
- **Security**: JWT filter injects trusted userId (prevents client-side forgery)
- **DataSource Routing**: AOP-based read/write splitting (@Order(1) before transaction)
- **Cache**: Redis eviction on writes, ready for @Cacheable on reads
- **Exception Handling**: GlobalExceptionHandler normalizes all errors

### Protection Strategy
1. **Rate Limiting**: 20 req/s cap → 429 on exceed
2. **Circuit Breaking**: fail-fast on downstream issues → 503 on open circuit
3. **Fraud Gate**: check fraud status first (fail-fast), then write
4. **Transaction Boundary**: fraud detection + state update in single transaction

### Caching Strategy (Two-Level)
1. **Infrastructure Level**: master/slave split offloads read traffic from write DB
2. **Application Level**: Redis cache eviction on writes keeps data consistent

---

## Tech Stack

**Core**: Java 17 + Spring Boot 3.2, MyBatis (no JPA), PostgreSQL master/slave, Redis, Resilience4j

**Why These Choices**:
- MyBatis: assignment requirement + full SQL control for complex queries
- Relational DB: transactional consistency + relationship modeling
- Redis: hot data caching + fraud flag lookup
- Resilience4j: write-path protection under high concurrency

**Trade-offs**:
- No JPA: better performance tuning, explicit SQL
- No NoSQL: need ACID transactions
- No ML fraud: rule-based engine sufficient, lower ops overhead
- Sync fraud detection: immediate blocking, can evolve to async (Kafka) later

---

## Data Model

### Schema Design
```
users            → profile master data
profile_visits   → visit interactions
user_likes       → like interactions
fraud_records    → audit evidence
```

**Separation Benefits**: independent optimization, easier service extraction

### Index Strategy (Aligned with Hot Queries)
- Visitor lists: `(visited_id, visited_at DESC)`
- Fraud aggregation: `(visitor_id, visited_at)`, `(liker_id, liked_at)`
- Like deduplication: `UNIQUE(liker_id, liked_id)`

### Efficiency Techniques
- Hand-optimized SQL for key queries
- MyBatis `<foreach>` for bulk inserts
- AOP-based read/write splitting
- Pagination aligned with indexes

### Scaling Strategy
- **Phase 1**: time-based partitioning (month/day) on interaction tables
- **Phase 2**: user_id hash sharding
- **Combined**: time partition + user-hash sub-sharding

---

## API Design

### Design Principles
1. **Trusted Identity**: JWT filter injects userId, client cannot forge
2. **Clear Error Semantics**: 403 fraud, 404 not found, 429 rate limit, 503 circuit open
3. **Protected Writes**: all write endpoints have rate limit + circuit breaker + fraud gate

### Key Endpoints
| Method | Path | Auth | Purpose |
|--------|------|------|---------|
| POST | /api/v1/auth/login | no | returns JWT token |
| POST | /api/v1/user/visit | JWT | record visit, returns visit + visited profile |
| POST | /api/v1/user/like | JWT | toggle like/unlike |
| GET | /api/v1/user/visitors | JWT | returns visitors sorted by time |
| POST | /api/v1/admin/bulk-insert-events | JWT | bulk insert via MyBatis foreach |

### Implementation Flow
```
Request → JwtFilter (inject trusted userId)
        → Controller (validate input)
        → Service (check fraud status → write → update fraud state)
        → Mapper (SQL execution)
        → GlobalExceptionHandler (normalize errors)
```

**Key Implementation Points**:
- Fraud check runs first (fail-fast)
- Transaction covers fraud detection + state update (atomicity)
- Redis eviction on writes (cache consistency)
- Service interfaces ready for Feign/gRPC extraction

---

## Structure

```
src/main/java/com/meet5/social/
├── controller/     HTTP entry points: UserController, AuthController, AdminController
├── service/        Business interfaces: UserService, VisitService, LikeService, FraudDetectionService
├── service/impl/   Implementations, each annotated for future microservice extraction
├── mapper/         MyBatis mapper interfaces
├── model/          Data models: ProfileVisit, UserLike, UserProfile, FraudRecord, VisitResult, Event
├── domain/         User entity + validation
├── repository/     Bulk insert operations
├── common/         Result<T>, GlobalExceptionHandler, FraudUserException, UserNotFoundException
└── config/         Redis, datasource routing, AOP, JwtUtil, JwtFilter, SecurityConfig
```

---

## Running Locally

Requires PostgreSQL (master on 5432, slave on 5433) and Redis on 6379.

```bash
./mvnw clean package -DskipTests
java -jar target/social-network-backend-1.0.0.jar
```

---

## Design Decisions

### Why JWT?
The naive approach is to pass `userId` directly as a request parameter — any client can forge any userId and act on behalf of another user. The standard fix is a gateway that validates the token and injects a trusted `X-User-Id` header downstream. This project skips the gateway to keep things simple, so JWT fills that role: the token is signed server-side, verified on every request by a filter, and the userId is injected into the SecurityContext via `@AuthenticationPrincipal`. The client never supplies the userId directly.

### Why Redis?
Write endpoints (visit, like) evict Redis cache keys on each write so reads don't serve stale data. The eviction infrastructure is already in place — once read endpoints are added, `@Cacheable` can be layered on top to reduce DB load on hot profiles without further changes to the write path.

### Fraud Detection Strategy
The rule — 100+ visits AND 100+ likes within 10 minutes — targets automated bots. Both conditions must be met to reduce false positives. Detection runs synchronously on each write so a fraud user is blocked in the same request that triggers the threshold, with no window for further writes.

The threshold is configurable via `app.fraud.threshold`.

**Future direction:** fraud detection is currently coupled to the write path. The natural next step is publishing `VisitRecorded` / `LikeRecorded` events to Kafka and having a separate Fraud Service consume them asynchronously — removing detection latency from the write path and decoupling the services. The hard gate (blocking already-flagged users) would still run synchronously via a Redis lookup.

### Data Access
Raw JDBC + MyBatis. Bulk inserts use MyBatis `<foreach>` to batch rows into a single statement. Read/write splitting routes `@Transactional(readOnly=true)` to the slave via an AOP aspect at `@Order(1)` — this ensures the datasource is set before the transaction opens.

---

## Error Handling

All exceptions are handled centrally in `GlobalExceptionHandler`:

- `FraudUserException` → 403, warn log
- `UserNotFoundException` → 404, warn log
- Unhandled `Exception` → 500, error log

---

## Testing

```bash
./mvnw test
```

Unit tests cover: controller layer (MockMvc, mocked services), visit/like service logic (fraud gate, user not found, fraud detection flag), and fraud detection thresholds (below/above threshold, configurable threshold).

---

## Evolution Roadmap

### Production Readiness Priorities
- **P0**: Security hardening (OAuth2, RBAC, secret management, audit logging)
- **P1**: Concurrency idempotency (distributed lock or optimistic locking for concurrent likes)
- **P2**: Performance validation (load testing, tuning rate limits/connection pools/cache TTL)
- **P3**: Async decoupling (Kafka + streaming fraud detection)

### Scaling for High Traffic (e.g., 50M requests/day)
1. Optimize single-instance performance (indexes, connection pools, cache hit rate)
2. Horizontal scaling at app tier (auto-scaling)
3. Database read replicas + read/write split
4. Redis cache for hot data
5. Time-based partitioning + user-hash sharding for interaction tables
