# TripOps | Booking & Inventory Management Service

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

TripOps is a Spring Boot backend for booking workflows, package inventory management, authentication, and operational API flows. The codebase is intentionally structured as a modular monolith: domain boundaries are explicit, cross-cutting concerns are centralized, and the deployment model remains simple while the architecture stays ready for future service extraction.

This repository is backend-first. It emphasizes transactional correctness, access control, cache-aware read paths, and maintainable module ownership over tutorial-style scaffolding or premature distributed systems complexity.

## Overview

TripOps currently runs as a single deployable application backed by PostgreSQL and Redis. Business capabilities are separated into domain modules with their own controllers, services, repositories, DTOs, and mapping logic. Shared concerns such as security, exception handling, OpenAPI configuration, and health exposure live in a dedicated common layer.

The system models inventory through travel packages and availability metadata, then composes booking and payment flows on top of those records. This keeps the operational surface coherent today while preserving a clean extraction path for `auth`, `packages`, `booking`, and `payment` if the deployment topology needs to evolve later.

## Architecture

### Architectural direction

- Modular monolith, single runtime, explicit domain boundaries
- Layered module design: controller -> service -> repository -> persistence
- Stateless REST APIs secured with JWT and role-based authorization
- Shared infrastructure for security, exception handling, caching, and API documentation
- Future extraction path designed around domain ownership, not around current distributed deployment

### Why modular monolith here

TripOps keeps the operational simplicity of one deployable unit while enforcing boundaries that matter in production:

- low coordination overhead across domains
- straightforward local development and test execution
- transactional consistency inside a single relational datastore
- clear internal seams for later service extraction if scaling or team topology requires it

This project does not claim to be a distributed system today. The extraction strategy is architectural preparation, not an overstated microservices claim.

### High-level flow

```text
Clients
   |
   v
Spring Security Filter Chain
   |
   v
REST Controllers
   |
   v
Domain Services
   | \
   |  \--> Redis-backed cached read paths for package inventory
   |
   v
Spring Data JPA Repositories
   |
   v
PostgreSQL
```

- **Cache type:** Redis via `CacheManager`
- **Key prefix:** `travel::` for operational visibility
- **Serialization:** JSON via `GenericJackson2JsonRedisSerializer`
- **Null handling:** Null values not cached (prevents pollution)

| Module | Responsibility |
| --- | --- |
| `auth` | Login flow, JWT issuance, authenticated principal resolution, request filtering |
| `user` | User registration, admin creation, role assignment, user listing |
| `packages` | Package catalog and inventory-facing operations, package lifecycle, cached reads |
| `booking` | Booking creation, booking retrieval, ownership-scoped booking workflows |
| `payment` | Payment record creation and booking confirmation workflow |
| `common` | Security configuration, OpenAPI, cache config, health endpoints, API envelope, exception handling |

## Feature Highlights

- Stateless authentication with JWT bearer tokens
- RBAC enforcement for `USER` and `ADMIN` access paths
- Package catalog management with status, pricing, dates, and capacity metadata
- Booking workflow with authenticated user ownership enforcement
- Payment workflow that advances booking state after confirmation
- Redis-backed caching for high-read package endpoints
- Centralized error handling with structured JSON error responses
- OpenAPI/Swagger exposure for endpoint discovery and backend integration testing
- Health endpoints for runtime verification
- CI validation through Maven build and automated test execution

## Technology Stack

| Category | Technologies |
| --- | --- |
| Language | Java 21 |
| Framework | Spring Boot 3.1.4 |
| Web | Spring Web |
| Security | Spring Security, JWT, BCrypt, RBAC |
| Persistence | Spring Data JPA, Hibernate |
| Database | PostgreSQL |
| Cache | Redis, Spring Cache |
| Build | Maven |
| API Docs | springdoc OpenAPI, Swagger UI |
| Testing | JUnit 5, Mockito, MockMvc, Spring Security Test |
| Containers | Docker, Docker Compose |
| CI | GitHub Actions |

## Project Structure

```text
TripOps-Booking-Inventory-Management-Service/
|-- .github/workflows/backend-ci.yml
|-- Dockerfile
|-- docker-compose.yml
|-- pom.xml
|-- src
|   |-- main
|   |   |-- java/com/aj/travel
|   |   |   |-- auth
|   |   |   |-- booking
|   |   |   |-- common
|   |   |   |-- packages
|   |   |   |-- payment
|   |   |   `-- user
|   |   `-- resources
|   |       |-- application.yml
|   |       |-- application-dev.yml
|   |       `-- application-prod.yml
|   `-- test
|       |-- java/com/aj/travel
|       |   |-- auth
|       |   |-- booking
|       |   |-- common
|       |   |-- packages
|       |   |-- payment
|       |   |-- security
|       |   `-- user
|       `-- resources/application-test.yml
`-- README.md
```

### Module shape

Each business module follows the same internal layout:

- `controller` for HTTP boundary handling
- `service` for orchestration and business rules
- `repository` for persistence access
- `domain` for entities and enums
- `dto` and `mapper` for API contracts and translation

That consistency keeps the monolith navigable and lowers the cost of future extraction by making ownership boundaries obvious in the codebase.

## Security Implementation

Security is implemented as a stateless request pipeline using Spring Security and JWT.

- `JwtAuthenticationFilter` resolves bearer tokens and hydrates the security context per request
- sessions are disabled with `SessionCreationPolicy.STATELESS`
- passwords are stored using `BCryptPasswordEncoder`
- authorization is enforced through request matcher rules and method-level annotations
- unauthorized and forbidden responses are normalized into JSON API errors

### Access model

| Area | Access |
| --- | --- |
| `/`, `/api/system/health` | Public |
| `/auth/register`, `/auth/login` | Public |
| `/swagger-ui/**`, `/v3/api-docs/**` | Public |
| `GET /packages`, `GET /packages/{id}` | `USER`, `ADMIN` |
| `POST/PUT/DELETE /packages/**` | `ADMIN` |
| `POST /bookings`, `GET /bookings/my` | `USER` |
| `GET /bookings/all` | `ADMIN` |
| `POST /payments`, `POST /payments/confirm/{bookingId}` | `USER` |
| `POST /admin/register`, `GET /users` | `ADMIN` |

## Redis Caching

Redis is used as the backing store for Spring Cache on package read paths.

- cache type: Redis
- key prefix: `travel::`
- value serialization: JSON via `GenericJackson2JsonRedisSerializer`
- null values are not cached
- `packages` cache TTL: 10 minutes
- `packageById` cache TTL: 30 minutes

Package write operations evict or refresh the relevant cache entries, so catalog reads stay fast without leaving stale inventory records behind after create, update, or delete flows.

## Database and Transactional Consistency

PostgreSQL is the system of record. Persistence is managed through Spring Data JPA with Hibernate, and database access is backed by HikariCP.

Production-oriented choices already present in the codebase:

- service-layer transaction boundaries via `@Transactional`
- `readOnly = true` query methods for non-mutating paths
- `spring.jpa.open-in-view=false` to keep persistence behavior explicit
- cache invalidation coupled to package mutations
- centralized exception translation for not found, duplicate, validation, auth, and authorization failures

Current consistency model is intentionally simple: the application relies on single-database transactions for module interactions inside one deployable process. That is appropriate for the current architecture and avoids introducing distributed coordination patterns before they are needed.

## API Surface

Representative endpoint groups:

- `POST /auth/register`
- `POST /auth/login`
- `GET /packages`
- `GET /packages/{id}`
- `POST /packages`
- `POST /bookings`
- `GET /bookings/my`
- `GET /bookings/all`
- `POST /payments`
- `POST /payments/confirm/{bookingId}`
- `GET /users`
- `GET /api/system/health`

Responses are wrapped in a consistent API envelope for success cases, while failures are returned as structured error payloads with status, error type, message, timestamp, and request path.

## API Documentation

Swagger UI:

```text
http://localhost:8080/swagger-ui/index.html
```

OpenAPI docs:

```text
http://localhost:8080/v3/api-docs
```

The OpenAPI configuration includes bearer authentication metadata so secured endpoints can be exercised directly from Swagger.

## Docker

The repository includes a multi-stage Docker build and a `docker-compose.yml` stack for the backend, PostgreSQL, and Redis.

### 1. Prepare environment

Update `.env` before starting the stack. `JWT_SECRET` must be at least 32 characters or the application will fail startup validation.

Example:

```env
POSTGRES_DB=travel_db
POSTGRES_USER=postgres
POSTGRES_PASSWORD=postgres
JWT_SECRET=change-this-development-secret-key-123456
```

### 2. Start the stack

```bash
docker compose up --build
```

The compose stack:

- starts PostgreSQL 16 and Redis 7
- waits for both dependencies to become healthy
- builds the backend image from the repository Dockerfile
- injects runtime configuration through environment variables

### 3. Access the service

```text
API:       http://localhost:8080
Swagger:   http://localhost:8080/swagger-ui/index.html
Health:    http://localhost:8080/api/system/health
```

### 4. Stop the stack

```bash
docker compose down
```

## Local Development

### Prerequisites

- Java 21
- Maven wrapper support
- PostgreSQL
- Redis

### Run dependencies locally

You can either:

- run PostgreSQL and Redis directly on your machine, or
- start only the infrastructure services with Docker and run the Spring Boot app from the host

Infrastructure only:

```bash
docker compose up postgres redis
```

### Build and verify

Linux/macOS:

```bash
./mvnw clean verify
```

Windows PowerShell:

```powershell
.\mvnw.cmd clean verify
```

### Start the application

Linux/macOS:

```bash
./mvnw spring-boot:run
```

Windows PowerShell:

```powershell
.\mvnw.cmd spring-boot:run
```

By default, the application runs with the `dev` profile and expects PostgreSQL on `localhost:5432` and Redis on `localhost:6379` unless you override the environment variables below.

## Environment Configuration

The application uses Spring profiles plus environment-driven runtime configuration.

### Core application settings

| Variable | Purpose | Dev default / example |
| --- | --- | --- |
| `SPRING_PROFILES_ACTIVE` | Active Spring profile | `dev` |
| `SERVER_PORT` | HTTP port | `8080` |
| `DB_URL` | JDBC connection URL | `jdbc:postgresql://localhost:5432/travel_db` |
| `DB_USERNAME` | Database username | `postgres` |
| `DB_PASSWORD` | Database password | `123456` |
| `REDIS_HOST` | Redis host | `localhost` |
| `REDIS_PORT` | Redis port | `6379` |
| `REDIS_TIMEOUT` | Redis timeout | `2s` |
| `JWT_SECRET` | JWT signing secret, minimum 32 chars | `change-this-development-secret-key-123456` |
| `JPA_DDL_AUTO` | Hibernate schema mode | `update` in dev |
| `JPA_SHOW_SQL` | SQL logging toggle | `true` in dev |
| `HIBERNATE_FORMAT_SQL` | SQL formatting toggle | `true` in dev |

### Connection pool settings

| Variable | Purpose | Dev default |
| --- | --- | --- |
| `DB_POOL_SIZE` | Maximum pool size | `10` |
| `DB_MIN_IDLE` | Minimum idle connections | `2` |
| `DB_IDLE_TIMEOUT` | Idle timeout in ms | `300000` |
| `DB_MAX_LIFETIME` | Max connection lifetime in ms | `1800000` |
| `DB_CONNECTION_TIMEOUT` | Connection acquisition timeout in ms | `30000` |

### Optional integration settings

| Variable | Purpose |
| --- | --- |
| `RAZORPAY_KEY` | Payment provider configuration placeholder |
| `RAZORPAY_SECRET` | Payment provider configuration placeholder |
| `BOOTSTRAP_ADMIN_NAME` | Reserved runtime configuration for admin bootstrap workflows |
| `BOOTSTRAP_ADMIN_EMAIL` | Reserved runtime configuration for admin bootstrap workflows |
| `BOOTSTRAP_ADMIN_PASSWORD` | Reserved runtime configuration for admin bootstrap workflows |

For the `prod` profile, infrastructure and secret values must be supplied explicitly.

## Testing and CI

The repository includes unit, controller, and integration coverage for core backend behavior, including:

- authentication and login flow
- RBAC enforcement
- package, booking, and payment controller paths
- service-level business logic
- health endpoint exposure

Test profile behavior:

- in-memory H2 configured in PostgreSQL compatibility mode
- Redis auto-configuration disabled
- cache layer disabled for deterministic tests

CI is defined in `.github/workflows/backend-ci.yml` and runs Maven build plus test verification on pushes to `dev` and `architecture-rebuild`, and on pull requests targeting `dev`.

## Scalability Roadmap

The current architecture is intentionally conservative. The next maturity steps are clear, but not falsely claimed as already complete:

- introduce schema migration tooling such as Flyway or Liquibase
- add observability primitives such as metrics, tracing, and structured request correlation
- formalize package inventory concurrency rules for high-contention update paths
- harden admin bootstrap and secrets handling for deployment environments
- introduce async workflow boundaries where payment or notification side effects justify them
- extract domain services only when operational scale or team boundaries make independent deployment worthwhile

## License

This project is licensed under the [MIT License](LICENSE).
