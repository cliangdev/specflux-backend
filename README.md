# SpecFlux Backend

Spring Boot backend for SpecFlux - AI-Powered Multi-Repo Development Orchestrator.

## Prerequisites

- **Java 25**
- **Maven 3.9+**
- **PostgreSQL 15+** (for development/production)

## Tech Stack

- Spring Boot 4.0.0
- Maven
- PostgreSQL
- Lombok
- Springdoc OpenAPI
- Spotless (code formatting)

## Project Structure

This project follows **Domain-Driven Design (DDD)** with package-by-feature organization:

```
src/main/java/com/specflux/
├── SpecFluxApplication.java      # Main entry point
├── shared/                       # Shared kernel (base classes)
│   ├── domain/                   # Base domain classes
│   └── infrastructure/           # Cross-cutting concerns
├── user/                         # User bounded context
├── project/                      # Project bounded context
├── epic/                         # Epic bounded context
├── task/                         # Task bounded context
└── release/                      # Release bounded context

Each bounded context contains:
├── domain/                       # Core business logic
│   ├── model/                    # Entities, Value Objects, Aggregates
│   ├── repository/               # Repository interfaces
│   └── service/                  # Domain services
├── application/                  # Use cases
│   ├── dto/                      # Request/Response DTOs
│   ├── mapper/                   # Entity <-> DTO mappers
│   └── service/                  # Application services
├── infrastructure/               # Technical implementations
│   ├── persistence/              # JPA repositories
│   └── external/                 # External service adapters
└── interfaces/                   # Entry points
    └── rest/                     # REST controllers
```

## Getting Started

### Build

```bash
# Compile
mvn clean compile

# Run tests
mvn test

# Package
mvn package
```

### Run

```bash
# Development mode (default profile: dev)
mvn spring-boot:run

# With specific profile
mvn spring-boot:run -Dspring-boot.run.profiles=dev
```

### Code Formatting

```bash
# Check formatting
mvn spotless:check

# Apply formatting
mvn spotless:apply
```

## Profiles

| Profile | Description | Usage |
|---------|-------------|-------|
| `dev` | Local development | Default profile, debug logging |
| `test` | Testing | Used by test classes, Testcontainers |
| `prod` | Production | Minimal logging, swagger disabled |

## API Documentation

When running in `dev` profile:
- Swagger UI: http://localhost:8080/swagger-ui.html
- OpenAPI JSON: http://localhost:8080/api-docs

## Environment Variables

| Variable | Description | Default |
|----------|-------------|---------|
| `SERVER_PORT` | Server port | 8080 |
| `SPRING_PROFILES_ACTIVE` | Active profile | dev |
| `DATABASE_URL` | PostgreSQL URL (prod) | - |
| `DATABASE_USERNAME` | Database username (prod) | - |
| `DATABASE_PASSWORD` | Database password (prod) | - |
