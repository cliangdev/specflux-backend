# Security Policy

## Supported Versions

| Version | Supported          |
| ------- | ------------------ |
| 1.x.x   | :white_check_mark: |
| < 1.0   | :x:                |

We only provide security updates for the latest stable release.

## Reporting a Vulnerability

**Please do not report security vulnerabilities through public GitHub issues.**

### How to Report

Use one of these methods:

1. **GitHub Security Advisories** (preferred)
   [Report a vulnerability](https://github.com/specflux/specflux-backend/security/advisories/new) through GitHub's private reporting feature.

2. **Email**
   Send details to **security@specflux.dev**

### What to Include

- Description of the vulnerability
- Steps to reproduce
- Potential impact
- Java/Spring Boot version (if relevant)
- Database configuration details (sanitized)
- Any suggested fixes (optional)
- Your contact information for follow-up

### What to Expect

| Timeline | Action |
|----------|--------|
| 24-48 hours | Acknowledgment of your report |
| 7 days | Initial assessment and severity determination |
| 30 days | Fix developed and tested |
| 90 days | Public disclosure (coordinated with reporter) |

Timelines may vary based on complexity. We'll keep you informed throughout the process.

## Backend-Specific Security

### API Authentication

- All endpoints require Firebase Authentication tokens
- API keys are validated server-side
- Rate limiting is applied to prevent abuse

### Database Security

- PostgreSQL connections use SSL in production
- Credentials are never logged
- Prepared statements prevent SQL injection

### Firebase/GCP Security

- Service account credentials are stored securely
- Minimal IAM permissions are granted
- Auth tokens are validated on every request

## Security Update Process

1. **Patch Development** — Fix developed in private fork
2. **Testing** — Patch tested against affected versions
3. **Release** — Security update released with advisory
4. **Notification** — Users notified via GitHub Security Advisory

## Scope

This security policy covers:

- **SpecFlux Backend API** — Spring Boot service
- **Database Layer** — PostgreSQL schema and queries
- **Authentication** — Firebase integration
- **Official Distributions** — Releases from our GitHub repository

### Out of Scope

- Third-party integrations not maintained by SpecFlux
- Self-hosted modifications
- Vulnerabilities in dependencies (report to upstream maintainers)
- Frontend/Desktop app (see [specflux/specflux](https://github.com/specflux/specflux) repo)

## Recognition

We appreciate security researchers who help keep SpecFlux safe. With your permission, we'll acknowledge your contribution in:

- Security advisory credits
- SECURITY.md acknowledgments section

## Safe Harbor

We consider security research conducted in accordance with this policy to be:

- Authorized and welcome
- Exempt from legal action from us
- Helpful to our community

Please act in good faith and avoid:

- Accessing data beyond what's necessary to demonstrate the vulnerability
- Destroying or modifying data
- Disrupting services

---

Thank you for helping keep SpecFlux and our users safe.
