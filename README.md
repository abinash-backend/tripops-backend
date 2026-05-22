# TripOps-Booking-Inventory-Management-Service

![Java](https://img.shields.io/badge/Java-21-orange?style=for-the-badge&logo=openjdk)
![Spring Boot](https://img.shields.io/badge/Spring_Boot-3-brightgreen?style=for-the-badge&logo=springboot)
![Spring Security](https://img.shields.io/badge/Spring_Security-Secured-6DB33F?style=for-the-badge&logo=springsecurity)
![JWT](https://img.shields.io/badge/Auth-JWT-blue?style=for-the-badge&logo=jsonwebtokens)
![RBAC](https://img.shields.io/badge/Authorization-RBAC-darkgreen?style=for-the-badge)
![Database](https://img.shields.io/badge/Database-PostgreSQL-4169E1?style=for-the-badge&logo=postgresql)
![Cache](https://img.shields.io/badge/Cache-Redis-DC382D?style=for-the-badge&logo=redis)
![Build](https://img.shields.io/badge/Build-Maven-C71A36?style=for-the-badge&logo=apachemaven)
![API Docs](https://img.shields.io/badge/API-Swagger%20%2F%20OpenAPI-85EA2D?style=for-the-badge&logo=swagger)
![Testing](https://img.shields.io/badge/Tests-JUnit5%20%7C%20Mockito%20%7C%20MockMvc-25A162?style=for-the-badge)
![CI](https://img.shields.io/badge/CI-GitHub_Actions-2088FF?style=for-the-badge&logo=githubactions)
![License](https://img.shields.io/badge/License-MIT-black?style=for-the-badge)

---

## About

**TripOps-Booking-Inventory-Management-Service** is a modern Spring Boot backend built for travel agencies and booking platforms. It provides APIs for travel package management, booking workflows, secure authentication, authorization, and payment handling.

The project originally began as a traditional **Spring MVC + Thymeleaf** application and has since been rebuilt into a **REST-first backend** using a **modular monolith architecture**. The current design emphasizes maintainability, clear module boundaries, API-driven development, and production-oriented backend practices.

---

## Architecture Overview

The application is structured as a **modular monolith**, where business capabilities are separated into well-defined modules while remaining part of a single deployable Spring Boot application.

### Core Architectural Principles

- Clear separation of business domains
- Layered architecture across all modules
- Shared common infrastructure for security, configuration, and exception handling
- REST-based communication model
- Centralized authentication and role-based authorization

### Layered Structure

`Controller -> Service -> Repository -> Database`

- `Controller` handles HTTP requests and response mapping
- `Service` contains business logic and workflow orchestration
- `Repository` manages persistence via Spring Data JPA
- `Database` stores domain data in PostgreSQL

---

## Domain Modules

| Module | Responsibility |
| --- | --- |
| `auth` | Authentication, JWT issuance, request security, and login-related workflows |
| `user` | User registration, profile management, and identity-related operations |
| `packages` | Travel package creation, updates, retrieval, and package catalog management |
| `booking` | Booking lifecycle management, booking creation, status tracking, and related workflows |
| `payment` | Payment processing flow, payment status handling, and payment-related integration points |
| `common` | Shared infrastructure including configuration, exception handling, API helpers, and reusable utilities |

---

## System Architecture Diagram

```text
                          +------------------------------+
                          |         Client Apps          |
                          |  Web / Mobile / Postman      |
                          +--------------+---------------+
                                         |
                                         v
                          +------------------------------+
                          |      REST API Controllers    |
                          +--------------+---------------+
                                         |
                                         v
        +-------------------------------------------------------------------+
        |                         Service Layer                             |
        | auth | user | packages | booking | payment | common |
        +-------------------------+-------------+-------------+--------------+
                                  |
                                  v
                        +-------------------------+
                        |    Repository Layer     |
                        |   Spring Data JPA       |
                        +------------+------------+
                                     |
                                     v
                        +-------------------------+
                        |   PostgreSQL Database   |
                        +-------------------------+
```

---

## Request Flow Example

### Booking Request Flow

```text
Client
  |
  v
JWT Authenticated Request
  |
  v
Booking Controller
  |
  v
Booking Service
  |
  +--> Validate User
  |
  +--> Validate Travel Package
  |
  +--> Create Booking
  |
  +--> Trigger Payment Workflow
  |
  v
Booking Repository
  |
  v
PostgreSQL Database
  |
  v
API Response
```

### Authentication Flow

```text
User Login -> Credentials Validation -> JWT Generation -> Secured API Access -> RBAC Enforcement
```

---

## Tech Stack

| Category | Technologies |
| --- | --- |
| Language | Java 21 |
| Framework | Spring Boot 3.1.4 |
| Security | Spring Security, JWT Authentication, RBAC Authorization |
| Persistence | Spring Data JPA, Hibernate |
| Database | PostgreSQL |
| Caching | Spring Cache, Redis |
| Build Tool | Maven |
| API Documentation | Swagger / OpenAPI |
| Testing | JUnit 5, Mockito, MockMvc |
| CI/CD | GitHub Actions CI |

---

## Features

### User Features

- User registration and login
- JWT-based authentication
- Access to protected backend APIs
- Browse and view travel packages
- Create and manage bookings
- Payment workflow support

### Admin Features

- Role-based access control with `ADMIN` and `USER`
- Travel package management
- Booking management
- Payment monitoring and workflow control
- Administrative access to secured management APIs
- API documentation access for integration and testing

---

## API Documentation

Swagger UI is available at:

`http://localhost:8080/swagger-ui/index.html`

OpenAPI documentation helps developers explore and test the available endpoints interactively.

---

## Project Structure

```text
TripOps-Booking-Inventory-Management-Service/
|-- .github/
|   |-- workflows/
|   |   `-- backend-ci.yml
|-- .mvn/
|-- src/
|   |-- main/
|   |   |-- java/
|   |   |   `-- com/
|   |   |       `-- aj/
|   |   |           `-- travel/
|   |   |               |-- auth/
|   |   |               |-- user/
|   |   |               |-- packages/
|   |   |               |-- booking/
|   |   |               |-- payment/
|   |   |               `-- common/
|   |   `-- resources/
|   |       |-- application.yml
|   |       |-- application-dev.yml
|   |       `-- application-prod.yml
|   `-- test/
|-- .dockerignore
|-- .env
|-- Dockerfile
|-- docker-compose.yml
|-- pom.xml
|-- mvnw
|-- mvnw.cmd
`-- README.md
```

---

## Getting Started

### Prerequisites

- Java 21
- PostgreSQL
- Redis
- Git
- Docker Desktop or Docker Engine

### Clone Repository

```bash
git clone <repository-url>
cd TripOps-Booking-Inventory-Management-Service
```

### Build the Project

```bash
./mvnw clean verify
```

On Windows PowerShell:

```powershell
.\mvnw.cmd clean verify
```

### Run the Application

```bash
./mvnw spring-boot:run
```

On Windows PowerShell:

```powershell
.\mvnw.cmd spring-boot:run
```

The default `dev` profile expects local PostgreSQL and Redis instances unless you override the environment variables below.

After startup, the backend will be available locally and the Swagger UI can be accessed from the documentation URL above.

---

## Configuration

The application uses Spring profiles with environment variables for runtime configuration. By default, it starts with `SPRING_PROFILES_ACTIVE=dev`. The defaults below are the values defined in `application-dev.yml`.

| Variable | Default |
| --- | --- |
| `SPRING_PROFILES_ACTIVE` | `dev` |
| `SERVER_PORT` | `8080` |
| `DB_URL` | `jdbc:postgresql://localhost:5432/travel_db` |
| `DB_USERNAME` | `postgres` |
| `DB_PASSWORD` | `123456` |
| `DB_POOL_SIZE` | `10` |
| `DB_MIN_IDLE` | `2` |
| `DB_IDLE_TIMEOUT` | `300000` |
| `DB_MAX_LIFETIME` | `1800000` |
| `DB_CONNECTION_TIMEOUT` | `30000` |
| `JPA_DDL_AUTO` | `update` |
| `JPA_SHOW_SQL` | `true` |
| `HIBERNATE_FORMAT_SQL` | `true` |
| `REDIS_HOST` | `localhost` |
| `REDIS_PORT` | `6379` |
| `REDIS_TIMEOUT` | `2s` |
| `JWT_SECRET` | `local-dev-jwt-secret-change-me` |
| `RAZORPAY_KEY` | empty |
| `RAZORPAY_SECRET` | empty |
| `BOOTSTRAP_ADMIN_NAME` | `Default Admin` |
| `BOOTSTRAP_ADMIN_EMAIL` | `admin@travel.local` |
| `BOOTSTRAP_ADMIN_PASSWORD` | `admin123` |

The `prod` profile does not provide fallbacks for sensitive or infrastructure-specific settings, so those values must be supplied explicitly.

---

## Docker

Build and run the backend with PostgreSQL and Redis:

```bash
docker compose up --build
```

Access the API:

`http://localhost:8080`

Swagger UI:

`http://localhost:8080/swagger-ui/index.html`

Stop containers:

```bash
docker compose down
```

The compose stack injects database and application settings through environment variables so the same image can be promoted across environments.

---

## Branch Strategy

The project follows a clean branch strategy for stability and ongoing development.

| Branch | Purpose |
| --- | --- |
| `main` | Production-ready code |
| `dev` | Integration branch for active development |
| `feature/*` | Feature-specific branches for new work |
| `architecture-rebuild` | Frozen branch representing the architecture migration from MVC to REST |

---

## Project Evolution

This project has gone through a significant architectural transition:

```text
Spring MVC + Thymeleaf
          |
          v
Layered Backend Refactoring
          |
          v
REST API Rebuild
          |
          v
JWT Authentication + RBAC
          |
          v
Modular Monolith Spring Boot Backend
```

### Evolution Summary

- Started as a server-rendered MVC application using Thymeleaf
- Reworked into a REST-oriented backend for API-driven clients
- Introduced JWT-based security for stateless authentication
- Added role-based authorization for administrative and user access paths
- Reorganized the codebase into domain-focused modules

---

## CI Pipeline

GitHub Actions is used to automate backend validation on every relevant change.

### CI Responsibilities

- Build verification with the Maven wrapper
- Automated test execution
- Early feedback for integration issues
- Consistent validation across branches

The current workflow runs on pushes to `dev` and `architecture-rebuild`, and on pull requests targeting `dev`.

---

## Future Improvements

- Redis caching for high-read endpoints
- Database indexing and query optimization
- Performance and stress testing
- Load balancing for horizontal scalability
- Environment-specific deployment profiles

---

## Author

**Abinash Nayak**  
Java Backend Developer  
GitHub: [Aj-world](https://github.com/Aj-world)

---

## License

This project is licensed under the **MIT License**. See the [LICENSE](LICENSE) file for details.
