# TripOps | Booking & Inventory Service

![Java](https://img.shields.io/badge/Java-21-orange)
![Spring Boot](https://img.shields.io/badge/Spring_Boot-3.1.4-6DB33F)
![Architecture](https://img.shields.io/badge/Architecture-Modular_Monolith-1f6feb)
![Security](https://img.shields.io/badge/Security-JWT%20%2B%20RBAC-0f766e)
![Database](https://img.shields.io/badge/Database-PostgreSQL-4169E1)
![Cache](https://img.shields.io/badge/Cache-Redis-DC382D)
![Build](https://img.shields.io/badge/Build-Maven-C71A36)
![API](https://img.shields.io/badge/API-OpenAPI%20%2F%20Swagger-85EA2D)
![Container](https://img.shields.io/badge/Container-Docker-2496ED)
![License](https://img.shields.io/badge/License-MIT-black)

**TripOps** is a production-grade Spring Boot backend for booking workflows, package inventory management, and operational API flows. Built as a modular monolith with role-based access control, Redis-backed caching, and transactional consistency across multiple business domains.

This repository emphasizes production patterns: transactional correctness, explicit authorization boundaries, cache-aware read paths, and maintainable module ownership. Architecture and operational decisions are intentional and documented, suitable for team handoff and future scaling.

## Overview

TripOps runs as a single deployable Spring Boot application backed by PostgreSQL (system of record) and Redis (acceleration layer). Business capabilities are separated into domain modules with their own controllers, services, repositories, and entities.

The system models inventory through travel packages and availability metadata, then composes booking and payment flows on top of those records. This keeps the operational surface coherent and enables independent scaling of specific domains if operational requirements justify service extraction.

## Architecture

### Architectural Direction

- **Modular monolith:** Single runtime, single deployment process, explicit domain boundaries
- **Layered module design:** Controller → Service → Repository → Persistence
- **Stateless REST APIs** secured with JWT bearer tokens and role-based authorization
- **Shared infrastructure:** Security configuration, exception handling, caching, and API documentation shared across all modules
- **Future extraction path:** Module boundaries and REST API contracts designed for service extraction if scaling or team topology requires independent deployment

### Why Modular Monolith

TripOps keeps the operational simplicity of one deployable unit while enforcing architectural boundaries that matter in production:

- **Low coordination overhead:** All services coordinate within a single process; no network latency, timeout handling, or service discovery required
- **Straightforward local development:** Clone, build, run—no orchestration complexity during feature development
- **Transactional consistency:** Single relational database and one connection per request eliminates distributed transaction coordination
- **Clear module seams:** If a specific domain needs independent scaling, extraction is configured, not rearchitected

This project does not claim to be a distributed system today. The modular structure is architectural preparation for growth, not premature microservices complexity.

### Module Responsibilities

| Module | Responsibility |
| --- | --- |
| **auth** | User login flow, JWT issuance, authenticated principal resolution, request filtering |
| **user** | User registration, admin creation, role assignment, user listing with role scoping |
| **packages** | Package catalog and inventory operations, package lifecycle, Redis-backed cached reads |
| **booking** | Booking creation, booking retrieval, ownership-scoped booking workflows, state management |
| **payment** | Payment record creation, payment confirmation flow, booking state advancement |
| **common** | Security configuration, OpenAPI setup, cache configuration, health endpoints, centralized exception handling, API response envelope |

### High-Level Request Flow

```
Clients
  ↓
Spring Security Filter Chain
  ├─ JWT Validation
  ├─ Principal Resolution
  └─ Authorization Checks
  ↓
REST Controllers (domain-specific)
  ↓
Domain Services (orchestration, business rules)
  ├─ Cache-aware read paths
  └─ Transaction boundaries
  ↓
Spring Data JPA Repositories
  ├─ Database write operations
  ├─ Cache invalidation on mutations
  └─ HikariCP connection pooling
  ↓
PostgreSQL (system of record)

[Redis sits alongside for read caching only]
```

## Security Implementation

Security is implemented as a stateless request pipeline using Spring Security and JWT:

### Authentication Flow

- `JwtAuthenticationFilter` extracts bearer tokens from request headers
- Token signature and expiration are validated
- User principal is hydrated into the Spring Security context per request
- Sessions are disabled with `SessionCreationPolicy.STATELESS`
- Passwords are stored using `BCryptPasswordEncoder` with sufficient work factor

### Authorization Model

| Area | Access Level | Enforcement |
| --- | --- | --- |
| `/`, `/api/system/health` | Public | No auth required |
| `/auth/register`, `/auth/login` | Public | No auth required |
| `/swagger-ui/**`, `/v3/api-docs/**` | Public | No auth required |
| `GET /packages`, `GET /packages/{id}` | `USER`, `ADMIN` | Method-level + endpoint security |
| `POST/PUT/DELETE /packages/**` | `ADMIN` | Method-level `@PreAuthorize` |
| `POST /bookings`, `GET /bookings/my` | `USER` | Ownership + role check |
| `GET /bookings/all` | `ADMIN` | Role-based access |
| `POST /payments`, confirm flows | `USER` | Ownership + transaction coupling |
| `POST /admin/register`, `GET /users` | `ADMIN` | Role-based access |

### Key Security Decisions

- **No sessions:** Stateless JWT eliminates session storage, enabling horizontal scaling
- **Ownership enforcement:** Service layer explicitly checks authenticated user against resource owner
- **Centralized error handling:** Unauthorized and forbidden responses are normalized into JSON API error format
- **OpenAPI integration:** Bearer authentication metadata allows secured endpoints to be tested from Swagger UI

## Feature Highlights

- **Stateless authentication** with JWT bearer tokens, enabling horizontal scaling of API instances
- **RBAC enforcement** for `USER` and `ADMIN` access paths with method-level authorization checks
- **Package catalog management** with status, pricing, dates, and capacity metadata
- **Booking workflow** with authenticated user ownership enforcement and state tracking
- **Payment workflow** that advances booking state after confirmation
- **Redis-backed caching** for high-read package endpoints with explicit TTL policies and mutation-coupled invalidation
- **Centralized error handling** with structured JSON error responses including timestamp, path, and error classification
- **OpenAPI/Swagger** exposure for endpoint discovery and backend integration testing
- **Health endpoints** for runtime verification and operational monitoring
- **Multi-environment configuration** with dev/test/prod profiles and externalized secrets
- **Comprehensive test coverage** including unit, integration, and controller-level tests
- **Containerized deployment** with Docker Compose stack including PostgreSQL and Redis with health checks

## Technology Stack

| Category | Technologies |
| --- | --- |
| **Language** | Java 21 |
| **Framework** | Spring Boot 3.1.4 |
| **Web** | Spring Web, Spring MVC |
| **Security** | Spring Security, JWT, BCrypt, RBAC |
| **Persistence** | Spring Data JPA, Hibernate, HikariCP |
| **Database** | PostgreSQL 16 |
| **Caching** | Redis 7, Spring Cache, GenericJackson2JsonRedisSerializer |
| **Build** | Maven Wrapper |
| **API Documentation** | springdoc OpenAPI, Swagger UI |
| **Testing** | JUnit 5, Mockito, MockMvc, Spring Security Test, H2 (test profile) |
| **Containers** | Docker, Docker Compose |
| **CI/CD** | GitHub Actions |

## Project Structure

```text
.
├── .github/workflows/
│   └── backend-ci.yml              # GitHub Actions CI pipeline
├── src/
│   ├── main/
│   │   ├── java/com/aj/travel/
│   │   │   ├── auth/               # Login, JWT issuance, authentication
│   │   │   │   ├── controller/
│   │   │   │   ├── service/
│   │   │   │   ├── domain/
│   │   │   │   └── dto/
│   │   │   ├── booking/            # Booking creation, retrieval, workflow
│   │   │   │   ├── controller/
│   │   │   │   ├── service/
│   │   │   │   ├── repository/
│   │   │   │   ├── domain/
│   │   │   │   └── dto/
│   │   │   ├── packages/           # Package catalog, inventory, caching
│   │   │   │   ├── controller/
│   │   │   │   ├── service/
│   │   │   │   ├── repository/
│   │   │   │   ├── domain/
│   │   │   │   └── dto/
│   │   │   ├── payment/            # Payment records, confirmation flow
│   │   │   │   ├── controller/
│   │   │   │   ├── service/
│   │   │   │   ├── repository/
│   │   │   │   ├── domain/
│   │   │   │   └── dto/
│   │   │   ├── user/               # User registration, admin creation, roles
│   │   │   │   ├── controller/
│   │   │   │   ├── service/
│   │   │   │   ├── repository/
│   │   │   │   ├── domain/
│   │   │   │   └── dto/
│   │   │   ├── common/             # Shared infrastructure
│   │   │   │   ├── config/
│   │   │   │   ├── exception/
│   │   │   │   ├── response/
│   │   │   │   ├── security/
│   │   │   │   ├── cache/
│   │   │   │   └── util/
│   │   │   └── TripOpsApplication.java
│   │   └── resources/
│   │       ├── application.yml
│   │       ├── application-dev.yml
│   │       ├── application-test.yml
│   │       └── application-prod.yml
│   └── test/
│       ├── java/com/aj/travel/
│       │   ├── auth/
│       │   ├── booking/
│       │   ├── packages/
│       │   ├── payment/
│       │   ├── security/
│       │   └── user/
│       └── resources/
│           └── application-test.yml
├── Dockerfile
├── docker-compose.yml
├── pom.xml
├── .env.example
└── README.md
```

### Module Consistency

Each domain module follows the same internal structure:

- `controller/` — HTTP request mapping and response serialization
- `service/` — Business logic orchestration and transaction boundaries
- `repository/` — Spring Data JPA interfaces and custom queries
- `domain/` — JPA entities and domain enums
- `dto/` and `mapper/` — API contracts and entity-to-DTO translation

This consistency keeps the monolith navigable and lowers the cost of future service extraction by making ownership boundaries obvious in the codebase.

## Database and Transactional Consistency

PostgreSQL is the system of record. Persistence is managed through Spring Data JPA with Hibernate, and connection pooling is handled by HikariCP.

### Production-Oriented Patterns

- **Service-layer transaction boundaries:** `@Transactional` annotations on service methods define atomic operation boundaries
- **Read-only query optimization:** Query methods marked with `readOnly = true` allow Hibernate query optimization for non-mutating paths
- **Explicit view handling:** `spring.jpa.open-in-view=false` prevents lazy loading outside transaction scope, making persistence behavior explicit
- **Cache invalidation coupled to mutations:** Write operations evict or refresh cache entries to prevent stale data
- **Connection pool tuning:** HikariCP pool size, idle timeout, and max lifetime are configured per environment

### Transactional Model

The current consistency model is intentionally simple and appropriate for scale:

- Application and database state live in one consistent database
- Module interactions coordinate within a single relational transaction
- No distributed saga pattern or eventual consistency; writes are synchronous and immediately visible
- Unique constraints at database layer prevent data anomalies even if application logic is bypassed

## Redis Caching Strategy

Redis is integrated as the backing store for Spring Cache on package read paths.

### Cache Configuration

- **Cache type:** Redis via `CacheManager`
- **Key prefix:** `travel::` for operational visibility
- **Value serialization:** JSON via `GenericJackson2JsonRedisSerializer`
- **Null value handling:** Null values are not cached (prevents cache pollution)
- **Serialization:** Jackson JSON with polymorphic type handling for complex objects

### TTL Strategy

| Cache | TTL | Reasoning |
| --- | --- | --- |
| `packages` | 10 minutes | High-traffic catalog reads; acceptable staleness for package list |
| `packageById` | 30 minutes | Detail reads less frequent; longer TTL acceptable |

### Cache Invalidation

Write operations evict or refresh relevant cache entries, keeping catalog reads fast without stale inventory records:

- `POST /packages` → Evict `packages` cache entry
- `PUT /packages/{id}` → Evict `packages` and `packageById` entries
- `DELETE /packages/{id}` → Evict `packages` and `packageById` entries

### Redis Failure Handling

Redis is a performance acceleration layer, not a correctness dependency. If Redis becomes unavailable:

- Read operations fall through to PostgreSQL (Cache Miss)
- Write operations proceed normally (not blocked by cache layer)
- Application continues functioning with degraded read performance
- No data loss or consistency issues

## API Surface

### Primary Endpoints

| Area | Method | Path | Access |
| --- | --- | --- | --- |
| **Auth** | `POST` | `/auth/register` | Public |
| **Auth** | `POST` | `/auth/login` | Public |
| **Packages** | `GET` | `/packages` | USER, ADMIN |
| **Packages** | `GET` | `/packages/{id}` | USER, ADMIN |
| **Packages** | `POST` | `/packages` | ADMIN |
| **Packages** | `PUT` | `/packages/{id}` | ADMIN |
| **Packages** | `DELETE` | `/packages/{id}` | ADMIN |
| **Bookings** | `POST` | `/bookings` | USER |
| **Bookings** | `GET` | `/bookings/my` | USER |
| **Bookings** | `GET` | `/bookings/all` | ADMIN |
| **Payments** | `POST` | `/payments` | USER |
| **Payments** | `POST` | `/payments/confirm/{bookingId}` | USER |
| **Users** | `POST` | `/admin/register` | ADMIN |
| **Users** | `GET` | `/users` | ADMIN |
| **System** | `GET` | `/api/system/health` | Public |

### Response Format

All responses follow a consistent JSON envelope:

**Success:**
```json
{
  "status": 200,
  "message": "Operation successful",
  "data": { /* payload */ }
}
```

**Error:**
```json
{
  "status": 400,
  "error": "VALIDATION_ERROR",
  "message": "Validation failed",
  "timestamp": "2025-05-23T10:30:00Z",
  "path": "/bookings"
}
```

## API Documentation

### Swagger UI

Interactive API explorer with request/response examples and authentication support:

```
http://localhost:8080/swagger-ui/index.html
```

### OpenAPI Specification

Machine-readable API contract for client code generation:

```
http://localhost:8080/v3/api-docs
```

The OpenAPI configuration includes bearer authentication metadata, allowing secured endpoints to be exercised directly from Swagger.

## Docker Setup

### Configuration

Create a `.env` file in the project root (see `.env.example`):

```env
POSTGRES_DB=travel_db
POSTGRES_USER=postgres
POSTGRES_PASSWORD=postgres
REDIS_PASSWORD=redis_password
JWT_SECRET=change-this-development-secret-key-123456789012345
```

**Requirements:**
- `JWT_SECRET` must be at least 32 characters; application validates on startup
- All secrets should be externalized in production (not in `.env`)

### Stack Management

#### Start Everything

```bash
docker compose up --build
```

The compose stack:
- Starts PostgreSQL 16 with volume persistence
- Starts Redis 7 with volume persistence
- Waits for both dependencies to become healthy (health probes)
- Builds the backend image from the Dockerfile
- Injects configuration through `.env` file

#### Start Only Infrastructure

For local development with host-based Spring Boot:

```bash
docker compose up postgres redis
```

Then run the application locally:

```bash
./mvnw spring-boot:run
```

#### Stop and Cleanup

```bash
docker compose down
```

To remove volumes as well (lose data):

```bash
docker compose down -v
```

### Service Access

| Service | URL/Port |
| --- | --- |
| API | `http://localhost:8080` |
| Swagger UI | `http://localhost:8080/swagger-ui/index.html` |
| Health Check | `http://localhost:8080/api/system/health` |
| PostgreSQL | `localhost:5432` |
| Redis | `localhost:6379` |

## Local Development

### Prerequisites

- Java 21
- Maven wrapper support (included in repository)
- PostgreSQL 16 (or use Docker)
- Redis 7 (or use Docker)

### Build and Verify

```bash
./mvnw clean verify
```

**Windows PowerShell:**

```powershell
.\mvnw.cmd clean verify
```

### Run the Application

```bash
./mvnw spring-boot:run
```

**Windows PowerShell:**

```powershell
.\mvnw.cmd spring-boot:run
```

Default profile is `dev`; application expects PostgreSQL on `localhost:5432` and Redis on `localhost:6379` unless overridden via environment variables.

### Run Tests

```bash
./mvnw test
```

Tests use in-memory H2 database (PostgreSQL compatibility mode) and disable Redis to ensure test isolation and speed.

## Environment Configuration

### Spring Profiles

- `dev` — Local development with verbose logging and convenient defaults
- `test` — Automated test execution with H2 in-memory database
- `prod` — Production deployment with strict validation and external configuration

### Application Settings

| Variable | Purpose | Dev Default | Example |
| --- | --- | --- | --- |
| `SPRING_PROFILES_ACTIVE` | Active profile | `dev` | `prod` |
| `SERVER_PORT` | HTTP server port | `8080` | `8080` |
| `DB_URL` | PostgreSQL JDBC URL | `jdbc:postgresql://localhost:5432/travel_db` | `jdbc:postgresql://db.example.com:5432/travel_prod` |
| `DB_USERNAME` | Database user | `postgres` | `travel_app` |
| `DB_PASSWORD` | Database password | `postgres` | (from secret manager) |
| `REDIS_HOST` | Redis hostname | `localhost` | `redis.example.com` |
| `REDIS_PORT` | Redis port | `6379` | `6379` |
| `REDIS_TIMEOUT` | Connection timeout | `2s` | `5s` |
| `JWT_SECRET` | JWT signing key (min 32 chars) | `dev-key-change-in-production-123456` | (from secret manager) |
| `JPA_DDL_AUTO` | Hibernate schema mode | `update` | `validate` |
| `JPA_SHOW_SQL` | SQL query logging | `true` | `false` |
| `HIBERNATE_FORMAT_SQL` | Pretty-print SQL | `true` | `false` |

### Connection Pool Settings

| Variable | Purpose | Dev Default |
| --- | --- | --- |
| `DB_POOL_SIZE` | Maximum pool connections | `10` |
| `DB_MIN_IDLE` | Minimum idle connections | `2` |
| `DB_IDLE_TIMEOUT` | Idle timeout (ms) | `300000` |
| `DB_MAX_LIFETIME` | Max connection lifetime (ms) | `1800000` |
| `DB_CONNECTION_TIMEOUT` | Acquisition timeout (ms) | `30000` |

### Optional Integration Settings

| Variable | Purpose |
| --- | --- |
| `RAZORPAY_KEY` | Payment provider API key (see Scalability Roadmap) |
| `RAZORPAY_SECRET` | Payment provider secret (see Scalability Roadmap) |
| `BOOTSTRAP_ADMIN_NAME` | Admin user name for environment-driven provisioning |
| `BOOTSTRAP_ADMIN_EMAIL` | Admin user email for environment-driven provisioning |
| `BOOTSTRAP_ADMIN_PASSWORD` | Admin user password for environment-driven provisioning |

**For production:**

- All secrets should be externalized to environment variables or secret management services (AWS Secrets Manager, HashiCorp Vault, etc.)
- Never commit credentials to the repository
- Use short-lived, rotatable credentials where possible
- Implement least-privilege access for all service accounts

## Testing and CI

### Test Coverage

The repository includes unit, integration, and controller-level tests for:

- Authentication and login flow
- RBAC enforcement
- Package operations (list, detail, create, update, delete)
- Booking workflow with ownership enforcement
- Payment workflow with state advancement
- Health endpoint exposure

### Test Profile Behavior

- In-memory H2 database configured in PostgreSQL compatibility mode
- Redis auto-configuration disabled
- Cache layer disabled for deterministic test execution
- Faster test execution compared to integration tests

### Run Tests

```bash
./mvnw test
```

### CI Pipeline

Defined in `.github/workflows/backend-ci.yml`:

- Runs on pushes to `dev` and `architecture-rebuild` branches
- Runs on pull requests targeting `dev`
- Maven build with dependency resolution
- Automated test execution
- Reports test results and coverage

**Recommended for production:**

- Make test execution mandatory (currently included in CI)
- Add code coverage gates (e.g., minimum 70%)
- Add security scanning (dependency vulnerabilities, SAST)
- Add performance regression testing for critical paths

## Scalability Roadmap

The current architecture is intentionally conservative and appropriate for current scale. These roadmap items are prioritized by operational impact:

### High Priority

- **Schema migrations:** Replace `JPA_DDL_AUTO=update` with Flyway for safe, reproducible schema evolution across environments
- **Observability layer:** Structured logging, distributed tracing (OpenTelemetry), and application metrics for multi-service deployments
- **Async workflows:** For payment or notification side effects where latency justifies asynchronous processing

### Medium Priority

- **Payment provider integration:** Wire Razorpay API with idempotent confirmation flow and retry strategies
- **Inventory concurrency:** Implement pessimistic locking or optimistic retry strategy for high-contention package availability updates
- **Admin bootstrap improvements:** Enhance admin provisioning from environment variables to secure seed file or dedicated provisioning service

### Lower Priority (Driven by Scale)

- **Extract services:** If specific domains (packages, bookings, payments) require independent scaling, extract as separate Spring Boot applications with async inter-service communication
- **Event sourcing:** If audit trail or temporal queries become business requirements
- **Read replicas:** PostgreSQL read replicas for scaling read-heavy analytics queries

## Production Readiness Checklist

Before deploying to production users:

- [ ] Replace `JPA_DDL_AUTO=update` with Flyway schema migrations
- [ ] Externalize all secrets (JWT, database credentials, payment keys)
- [ ] Enable HTTPS and secure communication channels
- [ ] Implement rate limiting and DDoS protection
- [ ] Set up centralized logging (ELK, Datadog, CloudWatch)
- [ ] Enable distributed tracing (OpenTelemetry, Jaeger)
- [ ] Define backup and disaster recovery procedures
- [ ] Conduct security audit and penetration testing
- [ ] Document runbooks for common operational scenarios
- [ ] Set up alerting for application and infrastructure metrics
- [ ] Validate caching TTL and invalidation under production load
- [ ] Load test to identify bottlenecks and scaling limits
- [ ] Test failover and recovery procedures
- [ ] Plan capacity and cost management strategy

## License

This project is licensed under the [MIT License](LICENSE).
