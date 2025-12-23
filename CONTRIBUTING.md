# Contributing to SpecFlux Backend

Thank you for your interest in contributing! This guide covers the Java/Spring Boot backend.

## API-First Development

This project follows **API-first design**. When adding or modifying endpoints:

1. **Start with the OpenAPI spec** — Define your endpoint in `src/main/resources/openapi/api.yaml`
2. **Generate interfaces** — Run `mvn compile` to generate controller interfaces
3. **Implement the interface** — Create or update the controller to implement the generated interface

Never add endpoints directly in code without updating the OpenAPI spec first.

## Development Setup

### Prerequisites

- **Java 25** (Temurin recommended)
- **Maven 3.9+**
- **Docker** (for PostgreSQL and Firebase emulator)

### Getting Started

```bash
# Clone the repository
git clone https://github.com/specflux/specflux-backend.git
cd specflux-backend

# Run the application (Docker Compose starts PostgreSQL and Firebase automatically)
mvn spring-boot:run
```

Docker Compose is configured to automatically start PostgreSQL and Firebase Auth emulator when you run the application.

### Running Tests

```bash
# Run all tests (uses Testcontainers)
mvn test

# Run specific test class
mvn test -Dtest=UserServiceTest

# Run with coverage
mvn test jacoco:report
```

## Code Style

### Spotless + Google Java Format

This project uses [Spotless](https://github.com/diffplug/spotless) with Google Java Format.

```bash
# Check formatting
mvn spotless:check

# Auto-fix formatting
mvn spotless:apply
```

**Run `mvn spotless:apply` before committing** — CI will fail on formatting issues.

### Code Conventions

- **Package structure:** Follow DDD bounded contexts
- **Naming:** Use descriptive names, avoid abbreviations
- **DTOs:** Suffix with `Request`/`Response` (e.g., `CreateTaskRequest`)
- **Entities:** No suffix, just the domain name (e.g., `Task`)
- **Services:** Application services in `application/service/`, domain services in `domain/service/`

## Architecture

### Domain-Driven Design (DDD)

Each bounded context follows this structure:

```
{context}/
├── domain/                   # Core business logic (no Spring dependencies)
│   ├── model/                # Entities, Value Objects, Aggregates
│   ├── repository/           # Repository interfaces
│   └── service/              # Domain services
├── application/              # Use cases
│   ├── dto/                  # Request/Response DTOs
│   ├── mapper/               # Entity <-> DTO mappers
│   └── service/              # Application services (orchestration)
├── infrastructure/           # Technical implementations
│   ├── persistence/          # JPA repositories
│   └── external/             # External service adapters
└── interfaces/               # Entry points
    └── rest/                 # REST controllers
```

### Key Principles

1. **Domain layer is pure Java** — no Spring annotations
2. **Application layer orchestrates** — calls domain services, handles transactions
3. **Infrastructure implements interfaces** — repositories, external APIs
4. **Controllers are thin** — delegate to application services

## Commit Messages

Use [Conventional Commits](https://www.conventionalcommits.org/):

```
type(scope): short description

- Detailed bullet points
- Explaining the changes
```

### Types

- `feat` — New feature
- `fix` — Bug fix
- `refactor` — Code change without feature/fix
- `test` — Adding or updating tests
- `docs` — Documentation only
- `chore` — Maintenance tasks

### Examples

```
feat(task): add task dependency validation

- Validate dependencies exist before creating task
- Return 400 if circular dependency detected
- Add integration tests for dependency scenarios
```

```
fix(epic): correct progress calculation

- Include blocked tasks in total count
- Fix percentage rounding issue
```

## Pull Request Process

### Before You Start

1. **Check existing issues** — avoid duplicate work
2. **Open an issue first** for large changes
3. **One PR per feature/fix**

### Branch Naming

```
feature/description    # New features
fix/description        # Bug fixes
refactor/description   # Code refactoring
```

### Creating a PR

1. Fork and clone
2. Create branch: `git checkout -b feature/my-feature`
3. Make changes, run tests
4. Format code: `mvn spotless:apply`
5. Push: `git push -u origin feature/my-feature`
6. Open PR against `main`

### PR Checklist

- [ ] Tests pass (`mvn test`)
- [ ] Code formatted (`mvn spotless:check`)
- [ ] No new warnings
- [ ] Commits follow conventional format
- [ ] PR description explains changes

## Testing Guidelines

### Test Structure

```java
@Test
void shouldCreateTask_whenValidRequest() {
    // Arrange
    CreateTaskRequest request = new CreateTaskRequest("Title", "Description");

    // Act
    TaskResponse response = taskService.create(request);

    // Assert
    assertThat(response.title()).isEqualTo("Title");
}
```

### What to Test

- **Unit tests:** Domain logic, mappers, validators
- **Integration tests:** Repositories, API endpoints
- **Use Testcontainers** for database tests

### Test Naming

- `should{ExpectedBehavior}_when{Condition}`
- Example: `shouldThrowException_whenTaskNotFound`

## Questions?

- **General questions:** [GitHub Discussions](https://github.com/specflux/specflux-backend/discussions)
- **Bug reports:** [GitHub Issues](https://github.com/specflux/specflux-backend/issues)

## Code of Conduct

Please read our [Code of Conduct](CODE_OF_CONDUCT.md) before contributing.

---

Thank you for contributing to SpecFlux Backend!
