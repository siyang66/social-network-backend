# social-network-backend

Backend service for a social network app. Core features: profile visits, likes, and time-window based fraud detection.

**Tech stack:** Java 17 + Spring Boot 3.2, raw JDBC + MyBatis (no JPA), PostgreSQL with master/slave read-write splitting, Redis, Resilience4j, Spring Security + JWT.

**Architecture approach:**

- Service interfaces cleanly separate contract from implementation, making each service independently replaceable and ready for extraction into a dedicated microservice with minimal changes.
- Write endpoints are protected by rate limiting and circuit breaking via Resilience4j to handle high-concurrency scenarios — rate limiting caps request throughput, circuit breaking prevents cascading failures when downstream dependencies are slow or unavailable.
- A two-level caching strategy is in place: master/slave splitting offloads read traffic from the write DB at the infrastructure level, and Redis cache eviction on writes keeps read data consistent at the application level. Once read endpoints are added, `@Cacheable` can be layered on top to further reduce DB pressure on hot profiles.
- Fraud detection runs synchronously on each write for immediate blocking, with a clear path to async decoupling via Kafka when the service is split out.

## Structure

```
src/main/java/com/meet5/social/
-- controller/     HTTP entry points: UserController, AuthController, AdminController
-- service/        Business interfaces: UserService, VisitService, LikeService, FraudDetectionService
-- service/impl/   Implementations, each annotated for future microservice extraction
-- mapper/         MyBatis mapper interfaces
-- model/          Data models: ProfileVisit, UserLike, UserProfile, FraudRecord, VisitResult, Event
-- domain/         User entity + validation
-- repository/     Bulk insert operations
-- common/         Result<T>, GlobalExceptionHandler, FraudUserException, UserNotFoundException
-- config/         Redis, datasource routing, AOP, JwtUtil, JwtFilter, SecurityConfig
```


## Running locally

Requires PostgreSQL (master on 5432, slave on 5433) and Redis on 6379.

```bash
./mvnw clean package -DskipTests
java -jar target/social-network-backend-1.0.0.jar
```

## API

| Method | Path | Auth | Notes |
|--------|------|------|-------|
| POST | /api/v1/auth/login | no | returns JWT token |
| POST | /api/v1/user/visit | JWT | record visit, returns visit + visited user's profile |
| POST | /api/v1/user/like | JWT | like or unlike a profile |
| POST | /api/v1/admin/bulk-insert-events | JWT | bulk insert events via MyBatis foreach |

Write endpoints are protected by a circuit breaker and rate limiter (20 req/s). Exceeding the limit returns 429; an open circuit returns 503.

## Design decisions

### Why JWT?

The naive approach is to pass `userId` directly as a request parameter — any client can forge any userId and act on behalf of another user. The standard fix is a gateway that validates the token and injects a trusted `X-User-Id` header downstream. This project skips the gateway to keep things simple, so JWT fills that role: the token is signed server-side, verified on every request by a filter, and the userId is injected into the SecurityContext via `@AuthenticationPrincipal`. The client never supplies the userId directly.

### Why Redis?

Write endpoints (visit, like) evict Redis cache keys on each write so reads don't serve stale data. The eviction infrastructure is already in place — once read endpoints are added, `@Cacheable` can be layered on top to reduce DB load on hot profiles without further changes to the write path.

### Fraud detection strategy

The rule — 100+ visits AND 100+ likes within 10 minutes — targets automated bots. Both conditions must be met to reduce false positives. Detection runs synchronously on each write so a fraud user is blocked in the same request that triggers the threshold, with no window for further writes.

The threshold is configurable via `app.fraud.threshold`.

**Future direction:** fraud detection is currently coupled to the write path. The natural next step is publishing `VisitRecorded` / `LikeRecorded` events to Kafka and having a separate Fraud Service consume them asynchronously — removing detection latency from the write path and decoupling the services. The hard gate (blocking already-flagged users) would still run synchronously via a Redis lookup.

### Data access

Raw JDBC + MyBatis. Bulk inserts use MyBatis `<foreach>` to batch rows into a single statement. Read/write splitting routes `@Transactional(readOnly=true)` to the slave via an AOP aspect at `@Order(1)` — this ensures the datasource is set before the transaction opens.



## Error handling

All exceptions are handled centrally in `GlobalExceptionHandler`:

- `FraudUserException` → 403, warn log
- `UserNotFoundException` → 404, warn log
- Unhandled `Exception` → 500, error log

## Running tests

```bash
./mvnw test
```

Unit tests cover: controller layer (MockMvc, mocked services), visit/like service logic (fraud gate, user not found, fraud detection flag), and fraud detection thresholds (below/above threshold, configurable threshold).

## Microservices proposal

See `docs/MICROSERVICES_ARCHITECTURE.md`.
