# SpecFlux Backend - Spring Boot Cloud Service

This file provides guidance to Claude Code when working in the specflux-backend repository.

## Overview

SpecFlux Backend is the cloud backend service for SpecFlux, providing:
- REST API for project, epic, task, and release management
- Firebase Authentication for user management
- PostgreSQL database with Flyway migrations

## Tech Stack

- **Framework:** Spring Boot 4.0
- **Language:** Java 25
- **Database:** PostgreSQL with Flyway migrations
- **Authentication:** Firebase Admin SDK (JWT validation)
- **Build:** Maven
- **Architecture:** Domain-Driven Design (DDD)
- **Code Gen:** OpenAPI Generator (generates interfaces and DTOs)
- **Code Style:** Spotless (Google Java Format)
- **Testing:** JUnit 5, Testcontainers, MockMvc

## Development Workflow

**CRITICAL: Always follow this order when implementing API features.**

Use the `springboot-patterns` skill for detailed patterns and examples.

### 1. OpenAPI First
```bash
# 1. Update the API spec
vim src/main/resources/openapi/api.yaml

# 2. Generate interfaces and DTOs
./mvnw compile
```

### 2. Write Controller Test First
```java
// Create test in src/test/java/.../interfaces/rest/{Domain}ControllerTest.java
// Extend AbstractControllerIntegrationTest
// Test: happy path, auth required, not found, validation errors
```

### 3. Implement Controller
```java
// Implement generated {Domain}Api interface
// Use @Override on all methods
// Delegate all logic to application service
```

### 4. Implement Application Service
```java
// Use TransactionTemplate for transactions
// Handle validation, business logic, DTO conversion
```

### 5. Verify
```bash
./mvnw test                    # All tests must pass
./mvnw spotless:check          # Code formatting
```

### 6. Commit
```bash
git add .
git commit -m "feat: add endpoint description"
```

## Project Structure (DDD)

```
src/main/java/com/specflux/
├── {domain}/                    # One package per domain
│   ├── application/             # Application services
│   ├── domain/                  # Domain models & repository interfaces
│   ├── infrastructure/          # JPA repository implementations
│   └── interfaces/rest/         # Controllers & mappers
└── shared/                      # Cross-cutting concerns
    ├── domain/                  # Base classes (Entity, AggregateRoot)
    ├── application/             # Shared services (CurrentUserService)
    └── infrastructure/          # Security, config, web setup
```

## ID Strategy

| ID Type | Purpose | Format | Example |
|---------|---------|--------|---------|
| Internal ID | Database PK | Auto-increment bigint | `847291` |
| Public ID | API responses | Prefixed nanoid | `task_V1StGXR8_Z5jdHi` |
| Display Key | Human reference | Project key + sequence | `SPEC-42` |
| Firebase UID | Auth provider ID | Firebase format | `abc123XYZ...` |

**Entity Prefixes:** `user_`, `proj_`, `epic_`, `task_`, `rel_`

**SECURITY: Never expose Firebase UID externally.** Firebase UID is an internal authentication identifier and should never appear in:
- API responses
- URLs or query parameters
- OAuth state parameters
- Logs visible to users

Always use `publicId` (e.g., `user_xxx`) for external references to users.

## API Design

**Base URL:** `/api`
**Auth:** Firebase JWT in `Authorization: Bearer <token>` header

**Key Endpoints:**
- `GET/POST /api/projects` - Project list/create
- `GET/PUT/DELETE /api/projects/{ref}` - Project by key or public ID
- `GET/POST /api/projects/{ref}/epics` - Epic operations
- `GET/POST /api/projects/{ref}/tasks` - Task operations
- `GET/POST /api/projects/{ref}/releases` - Release operations

## Commands

```bash
# Build & Run
./mvnw clean compile                    # Compile (generates OpenAPI code)
./mvnw spring-boot:run                  # Run application (port 8090)
./mvnw test                             # Run all tests
./mvnw test -Dtest=TaskControllerTest   # Run specific test

# Code Quality
./mvnw spotless:check                   # Check formatting
./mvnw spotless:apply                   # Fix formatting

# Database
./mvnw flyway:migrate                   # Run migrations
./mvnw flyway:info                      # Show migration status
```

## Key Patterns

### TransactionTemplate (not @Transactional)
```java
return transactionTemplate.execute(status -> {
    // Database operations only
    return result;
});
```

**CRITICAL: Never include external API/HTTP calls inside transactions.** External calls hold the database connection while waiting for the response, causing connection pool exhaustion and performance issues.

```java
// WRONG - external call inside transaction
return transactionTemplate.execute(status -> {
    TokenResponse tokens = externalApi.getTokens(code);  // BAD!
    entity.setToken(tokens.getAccessToken());
    return repository.save(entity);
});

// CORRECT - external call outside transaction
TokenResponse tokens = externalApi.getTokens(code);  // External call first
return transactionTemplate.execute(status -> {
    entity.setToken(tokens.getAccessToken());
    return repository.save(entity);  // Only DB operations in transaction
});
```

### Mapper (static utility class)
```java
@UtilityClass
public class TaskMapper {
    public TaskDto toDto(Task domain) { /* ... */ }
}
```

### Controller (implements generated interface)
```java
@RestController
@RequiredArgsConstructor
public class TaskController implements TasksApi {
    @Override
    public ResponseEntity<TaskDto> getTask(...) { /* ... */ }
}
```

## Code Style

**Avoid unnecessary inline comments.** Comments should explain "why", not "what". Self-explanatory code doesn't need comments:

```java
// BAD - states the obvious
// Look up user by public ID
User user = userRepository.findByPublicId(publicId);

// Create new installation
installation = new GithubInstallation(...);

// GOOD - no comment needed, code is self-evident
User user = userRepository.findByPublicId(publicId);
installation = new GithubInstallation(...);

// GOOD - explains why (architectural decision)
// External API calls must be outside transaction to avoid connection pool exhaustion
TokenResponse tokens = externalApi.getTokens(code);
```

## Database Migrations

Location: `src/main/resources/db/migration/`
Naming: `V{version}__{description}.sql`

```sql
-- V17__add_task_notes.sql
ALTER TABLE tasks ADD COLUMN notes TEXT;
```

## Environment Variables

| Variable | Description | Required |
|----------|-------------|----------|
| `DATABASE_URL` | PostgreSQL connection string | Yes |
| `FIREBASE_CREDENTIALS` | Path to Firebase service account JSON | Yes |
| `SERVER_PORT` | HTTP port (default: 8090) | No |

## Git Workflow

- Branch from `main` for features: `feature/description`
- Use conventional commits: `feat:`, `fix:`, `refactor:`, etc.
- Create PRs for review before merging

## Related Repositories

- **specflux** - Frontend Tauri desktop app (connects to this backend)
