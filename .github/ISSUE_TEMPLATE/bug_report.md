---
name: Bug Report
about: Report a bug in the SpecFlux Backend API
title: '[Bug] '
labels: bug
assignees: ''
---

## Description

A clear description of the bug.

## API Endpoint (if applicable)

- **Method:** [e.g., GET, POST, PUT, DELETE]
- **Endpoint:** [e.g., /api/projects/{id}/tasks]

## Steps to Reproduce

1. Make request to '...'
2. With payload '...'
3. See error

## Expected Behavior

What you expected to happen.

## Actual Behavior

What actually happened, including response status code and body.

## Environment

- **Java Version:** [e.g., 25.0.1]
- **Spring Boot Version:** [e.g., 4.0.0]
- **PostgreSQL Version:** [e.g., 16]
- **OS:** [e.g., macOS 14.0, Ubuntu 22.04]
- **Running via:** [e.g., Maven, Docker, JAR]

## Request/Response

<details>
<summary>Request details</summary>

```bash
curl -X POST http://localhost:8090/api/... \
  -H "Authorization: Bearer ..." \
  -H "Content-Type: application/json" \
  -d '{"key": "value"}'
```

</details>

<details>
<summary>Response (if any)</summary>

```json
{
  "error": "...",
  "code": "..."
}
```

</details>

## Logs

<details>
<summary>Application logs</summary>

```
Paste logs here
```

</details>

## Database State (if relevant)

Describe relevant database state or include sanitized query results.

## Additional Context

Any other context about the problem.
