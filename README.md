# SpecFlux Backend

API server for [SpecFlux](https://github.com/specflux/specflux) - AI-Powered Multi-Repo Development Orchestrator.

[![License: Elastic License 2.0](https://img.shields.io/badge/License-Elastic%202.0-blue.svg)](LICENSE)

## API-First Development

This project follows **API-first design**. All endpoints are defined in the OpenAPI spec before implementation:

1. Define endpoints in `src/main/resources/openapi/api.yaml`
2. Run `mvn compile` to generate interfaces
3. Implement the generated interfaces in controllers

See [API Documentation](#api-documentation) for the live spec.

## Prerequisites

- **Java 25** (Temurin recommended)
- **Maven 3.9+**
- **Docker** (for PostgreSQL and Firebase emulator)

### Java Installation (asdf)

```bash
asdf install java temurin-25.0.1+8.0.LTS
asdf set java temurin-25.0.1+8.0.LTS
```

## Quick Start

```bash
mvn spring-boot:run
```

Docker Compose automatically starts PostgreSQL and Firebase Auth emulator.

The API will be available at **http://localhost:8090**

## Tech Stack

- Spring Boot 4.0.0
- Java 25
- PostgreSQL
- Firebase Authentication
- Maven

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

## Development

### Build

```bash
# Compile
mvn clean compile

# Run tests (uses Testcontainers)
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

This project uses Spotless with Google Java Format.

```bash
# Check formatting
mvn spotless:check

# Apply formatting
mvn spotless:apply
```

## API Documentation

When running in `dev` profile:
- **Swagger UI:** http://localhost:8090/swagger-ui.html
- **OpenAPI JSON:** http://localhost:8090/api-docs

## Configuration

### Profiles

| Profile | Description | Usage |
|---------|-------------|-------|
| `dev` | Local development | Default profile, debug logging |
| `test` | Testing | Used by test classes, Testcontainers |
| `prod` | Production | Minimal logging, Swagger disabled |

### Environment Variables

| Variable | Description | Default |
|----------|-------------|---------|
| `SERVER_PORT` | Server port | 8090 |
| `SPRING_PROFILES_ACTIVE` | Active profile | dev |
| `DATABASE_URL` | PostgreSQL URL | - |
| `DATABASE_USERNAME` | Database username | - |
| `DATABASE_PASSWORD` | Database password | - |
| `FIREBASE_PROJECT_ID` | Firebase project ID | - |
| `GOOGLE_APPLICATION_CREDENTIALS` | Path to Firebase service account JSON | - |

## Troubleshooting

### Port already in use

```bash
# Find process using port 8090
lsof -i :8090

# Kill it
kill -9 <PID>
```

### Database connection issues

1. Ensure PostgreSQL is running: `docker ps | grep postgres`
2. Check connection: `psql -h localhost -U specflux -d specflux`
3. Verify `application-dev.yml` has correct credentials

### Firebase emulator not connecting

1. Ensure emulator is running: `firebase emulators:start --only auth`
2. Check emulator UI at http://localhost:4000
3. Verify `FIREBASE_AUTH_EMULATOR_HOST=localhost:9099` is set

### Tests failing with Testcontainers

1. Ensure Docker is running
2. Check Docker has sufficient resources
3. Try `docker system prune` to clean up old containers

## Documentation

- [CONTRIBUTING.md](CONTRIBUTING.md) — How to contribute
- [CODE_OF_CONDUCT.md](CODE_OF_CONDUCT.md) — Community guidelines
- [SECURITY.md](SECURITY.md) — Report vulnerabilities

## License

[Elastic License 2.0](LICENSE) — Free for personal use. Commercial use restricted.
