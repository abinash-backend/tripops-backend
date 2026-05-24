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

**TripOps** is a production-ready Spring Boot backend for travel booking and inventory management. Demonstrates explicit RBAC, Redis-backed caching with mutation-coupled invalidation, transactional consistency, and stateless JWT authentication.

## Architecture at a Glance

**Modular Monolith:** Single deployable unit with explicit domain boundaries and clear module seams.

- **Stateless REST APIs** secured with JWT bearer tokens and role-based authorization
- **PostgreSQL** as system of record with HikariCP connection pooling
- **Redis** acceleration layer on package read paths with explicit TTL policies
- **Spring Data JPA** for transactional consistency and cache invalidation coupled to mutations
- **Centralized security** via Spring Security and method-level authorization checks

| Module | Responsibility |
| --- | --- |
| **auth** | Login flow, JWT issuance, request filtering |
| **user** | Registration, admin creation, role assignment |
| **packages** | Catalog management, inventory, Redis-backed reads |
| **booking** | Booking creation, ownership-scoped workflows, state tracking |
| **payment** | Payment records, confirmation flow, booking advancement |
| **common** | Security, OpenAPI, caching, health endpoints, exception handling |

## Security

### Authentication & Authorization

- **JWT + Spring Security:** Stateless bearer token validation per request; no sessions
- **RBAC enforcement:** Method-level `@PreAuthorize` for role-based access (`USER`, `ADMIN`)
- **Ownership checks:** Service layer explicitly validates authenticated user against resource owner
- **BCrypt passwords:** Sufficient work factor for credential storage

### Access Control

| Path | Access Level |
| --- | --- |
| `GET /packages`, `GET /packages/{id}` | `USER`, `ADMIN` |
| `POST/PUT/DELETE /packages/**` | `ADMIN` only |
| `POST /bookings`, `GET /bookings/my` | `USER` (ownership enforced) |
| `GET /bookings/all` | `ADMIN` only |
| `POST /payments`, confirm flows | `USER` (ownership enforced) |

## Redis Caching Strategy

Redis is the acceleration layer for high-traffic package catalog reads. Cache invalidation is coupled to all mutations.

### Configuration

- **Cache type:** Redis via `CacheManager`
- **Key prefix:** `travel::` for operational visibility
- **Serialization:** JSON via `GenericJackson2JsonRedisSerializer`
- **Null handling:** Null values not cached (prevents pollution)

### TTL Policy

| Cache | TTL | Reasoning |
| --- | --- | --- |
| `packages` | 10 minutes | High-traffic list endpoint; acceptable staleness |
| `packageById` | 30 minutes | Detail reads less frequent |

### Mutation-Coupled Invalidation

Cache entries are evicted on all writes to prevent stale reads:

- `POST /packages` → Evict `packages` entry
- `PUT /packages/{id}` → Evict `packages` and `packageById` entries
- `DELETE /packages/{id}` → Evict `packages` and `packageById` entries

### Redis Failure Handling

Redis is a performance layer, not a correctness dependency:

- **Cache miss** → Fall through to PostgreSQL (transparent to client)
- **Redis unavailable** → Write operations continue normally; read performance degrades
- **No data loss** or consistency impact

## Database & Transactional Consistency

PostgreSQL is the system of record. Persistence via Spring Data JPA with Hibernate and HikariCP pooling.

### Production Patterns

- **Service-layer boundaries:** `@Transactional` annotations define atomic operation scope
- **Read-only optimization:** Query methods marked `readOnly = true` for Hibernate optimization
- **Explicit view handling:** `spring.jpa.open-in-view=false` prevents lazy loading outside transaction
- **Cache invalidation:** Write operations evict cache entries immediately
- **Unique constraints:** Database-level validation prevents anomalies even if logic is bypassed

### Consistency Model

Application and database state live in one consistent database with synchronous writes. Module interactions coordinate within a single relational transaction. No distributed saga or eventual consistency patterns; writes are immediately visible.

## API Surface

| Area | Method | Path | Access |
| --- | --- | --- | --- |
| **Auth** | `POST` | `/auth/register` | Public |
| **Auth** | `POST` | `/auth/login` | Public |
| **Packages** | `GET` | `/packages` | USER, ADMIN |
| **Packages** | `GET` | `/packages/{id}` | USER, ADMIN |
| **Packages** | `POST/PUT/DELETE` | `/packages/**` | ADMIN |
| **Bookings** | `POST` | `/bookings` | USER |
| **Bookings** | `GET` | `/bookings/my` | USER |
| **Bookings** | `GET` | `/bookings/all` | ADMIN |
| **Payments** | `POST` | `/payments` | USER |
| **Payments** | `POST` | `/payments/confirm/{bookingId}` | USER |
| **Users** | `POST` | `/admin/register` | ADMIN |
| **Users** | `GET` | `/users` | ADMIN |
| **System** | `GET` | `/api/system/health` | Public |

### Response Envelope

All responses follow a consistent JSON structure:

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

- **Swagger UI:** `http://localhost:8080/swagger-ui/index.html`
- **OpenAPI Spec:** `http://localhost:8080/v3/api-docs`

Bearer authentication metadata is included in OpenAPI configuration, enabling secured endpoints to be tested directly from Swagger.

## Project Structure

```text
.
├── .github/workflows/
│   └── backend-ci.yml              # GitHub Actions CI
├── src/
│   ├── main/java/com/aj/travel/
│   │   ├── auth/                   # Login, JWT, authentication
│   │   ├── booking/                # Booking workflows
│   │   ├── packages/               # Catalog, inventory, caching
│   │   ├── payment/                # Payment records, confirmation
│   │   ├── user/                   # Registration, admin, roles
│   │   ├── common/                 # Security, OpenAPI, caching, exceptions
│   │   └── TripOpsApplication.java
│   ├── resources/
│   │   ├── application.yml
│   │   ├── application-dev.yml
│   │   ├── application-test.yml
│   │   └── application-prod.yml
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

Each module follows: `controller/` → `service/` → `repository/` → `domain/` → `dto/`. This consistency maintains clear module separation and maintainable domain boundaries across the codebase.

## Tech Stack

| Category | Technologies |
| --- | --- |
| **Language** | Java 21 |
| **Framework** | Spring Boot 3.1.4 |
| **Security** | Spring Security, JWT, BCrypt, RBAC |
| **Persistence** | Spring Data JPA, Hibernate, HikariCP |
| **Database** | PostgreSQL 16 |
| **Caching** | Redis 7, Spring Cache |
| **API** | Spring Web, OpenAPI / Swagger |
| **Build** | Maven Wrapper |
| **Testing** | JUnit 5, Mockito, MockMvc, H2 (test profile) |
| **Containers** | Docker, Docker Compose |
| **CI/CD** | GitHub Actions |

## Docker Setup

### Configuration

Create `.env` in project root (see `.env.example`):

```env
POSTGRES_DB=travel_db
POSTGRES_USER=postgres
POSTGRES_PASSWORD=postgres
REDIS_PASSWORD=redis_password
JWT_SECRET=change-this-development-secret-key-123456789012345
```

**Requirements:**
- `JWT_SECRET` must be at least 32 characters (validated on startup)
- Secrets should be externalized in production (not committed)

### Stack Management

**Start everything:**
```bash
docker compose up --build
```

**Start only infrastructure (PostgreSQL + Redis) for local development:**
```bash
docker compose up postgres redis
./mvnw spring-boot:run
```

**Stop and cleanup:**
```bash
docker compose down          # Keep volumes
docker compose down -v       # Remove volumes (lose data)
```

### Service Access

| Service | URL/Port |
| --- | --- |
| API | `http://localhost:8080` |
| Swagger | `http://localhost:8080/swagger-ui/index.html` |
| Health | `http://localhost:8080/api/system/health` |
| PostgreSQL | `localhost:5432` |
| Redis | `localhost:6379` |

## Deployment

- **Docker Compose stack** with PostgreSQL and Redis for containerized local and staging environments
- **Multi-environment configuration** via Spring profiles (`dev`, `test`, `prod`)
- **GitHub Actions CI** workflow for build and test automation
- **Production deployment** ready for AWS ECS, Kubernetes, or traditional VM hosting
- All secrets externalized to environment variables or secret managers (AWS Secrets Manager, HashiCorp Vault)

## Local Development

### Prerequisites

- Java 21
- Maven wrapper (included)
- PostgreSQL 16 (or Docker)
- Redis 7 (or Docker)

### Build

```bash
./mvnw clean verify
```

### Run

```bash
./mvnw spring-boot:run
```

Default profile is `dev`; expects PostgreSQL on `localhost:5432` and Redis on `localhost:6379`.

### Test

```bash
./mvnw test
```

Tests use in-memory H2 database (PostgreSQL mode) with Redis disabled for isolation and speed.

## Environment Configuration

### Spring Profiles

- `dev` — Local development with verbose logging
- `test` — Test execution with H2 in-memory database
- `prod` — Production deployment with strict validation

### Key Settings

| Variable | Purpose | Dev Default |
| --- | --- | --- |
| `SPRING_PROFILES_ACTIVE` | Active profile | `dev` |
| `SERVER_PORT` | HTTP server port | `8080` |
| `DB_URL` | PostgreSQL JDBC URL | `jdbc:postgresql://localhost:5432/travel_db` |
| `DB_USERNAME` | Database user | `postgres` |
| `DB_PASSWORD` | Database password | `postgres` |
| `REDIS_HOST` | Redis hostname | `localhost` |
| `REDIS_PORT` | Redis port | `6379` |
| `REDIS_TIMEOUT` | Connection timeout | `2s` |
| `JWT_SECRET` | JWT signing key (min 32 chars) | `dev-key-change-in-production-123456` |
| `JPA_DDL_AUTO` | Hibernate schema mode | `update` |
| `JPA_SHOW_SQL` | SQL query logging | `true` |

### Connection Pool Settings

| Variable | Purpose | Dev Default |
| --- | --- | --- |
| `DB_POOL_SIZE` | Max pool connections | `10` |
| `DB_MIN_IDLE` | Min idle connections | `2` |
| `DB_IDLE_TIMEOUT` | Idle timeout (ms) | `300000` |
| `DB_MAX_LIFETIME` | Max lifetime (ms) | `1800000` |
| `DB_CONNECTION_TIMEOUT` | Acquisition timeout (ms) | `30000` |

## Testing & CI

### Test Coverage

Unit, integration, and controller-level tests for:
- Authentication and login flow
- RBAC enforcement
- Package operations (list, detail, create, update, delete)
- Booking workflow with ownership enforcement
- Payment workflow with state advancement

### CI Pipeline

Defined in `.github/workflows/backend-ci.yml`:
- Runs on pushes to `dev` and pull requests targeting `dev`
- Maven build with dependency resolution
- Automated test execution

## Next Steps for Production

- **Schema migrations:** Replace `JPA_DDL_AUTO=update` with Flyway
- **Observability:** Structured logging and application metrics
- **Async workflows:** Message queue for payment/notification side effects
- **Payment provider:** Wire Razorpay API with idempotent retry strategy
- **Inventory concurrency:** Implement locking strategy for high-contention updates

## License

This project is licensed under the [MIT License](LICENSE).
