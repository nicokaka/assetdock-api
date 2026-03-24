# AssetDock API

**AssetDock API** is a security-first, multi-tenant asset inventory backend built to model real operational environments.

This project is designed as a professional portfolio piece focused on:

- backend engineering
- secure API design
- multi-tenant architecture
- authentication and authorization
- auditability
- safe data import
- production-minded testing and delivery

It is intentionally built as a **modular monolith** for the MVP, prioritizing correctness, security, and maintainability over unnecessary complexity.

---

## Project Status

> **Current status:** early development / MVP foundation phase

The current focus is to build a strong backend foundation first:

- project structure
- database migrations
- authentication
- authorization
- tenant isolation
- audit logging
- asset flows
- integration testing

Frontend is **not** the priority for the MVP.

---

## Why this project exists

AssetDock is inspired by the operational reality of IT asset management in corporate environments, but it is **not derived from any private internal company system**.

The goal is to build a new, public, clean implementation that demonstrates:

- secure backend design
- real-world domain modeling
- strong engineering fundamentals
- practical AppSec-oriented thinking

This is not intended to be a generic CRUD demo.  
It is intended to look and behave like a backend system designed by someone who understands operations, support, infrastructure, and security concerns in real environments.

---

## Core Positioning

AssetDock API can be summarized as:

> **A multi-tenant asset inventory API built with a security-first mindset, focused on authentication, authorization, organization isolation, audit trails, safe delivery, and real testing.**

---

## MVP Goals

The MVP is meant to prove the following capabilities:

- secure login with JWT
- role-based access control
- tenant isolation by organization
- organization and user management
- asset registration and lifecycle basics
- asset assignment and movement tracking
- audit log persistence for critical actions
- safe CSV import with validation and partial success reporting
- versioned schema migrations
- health checks and API documentation
- unit and integration testing with a real PostgreSQL database

---

## Non-Goals for the MVP

The following are intentionally out of scope for the first version:

- microservices
- full frontend application
- Keycloak
- Prometheus / Grafana
- distributed background processing
- MFA
- refresh token flow
- highly granular permission engine
- advanced observability stack
- event-driven architecture
- over-engineered abstractions

The MVP favors a **terminable, professional, security-conscious implementation** over feature overload.

---

## Architecture Overview

AssetDock is being built as a **modular monolith** with clear domain boundaries.

### Architectural principles

- modular by domain
- explicit security boundaries
- organization-aware data access
- predictable error handling
- forward-only database evolution
- pragmatic design over theoretical purity
- test critical paths first

### Package/module direction

The application is organized around these core modules:

- `auth`
- `organization`
- `user`
- `catalog`
- `asset`
- `assignment`
- `audit`
- `importer`
- `common`
- `config`
- `security`

Each business module follows a structure similar to:

- `api`
- `application`
- `domain`
- `infrastructure`

---

## Multi-Tenancy Model

The MVP uses a **shared-schema multi-tenant model**.

### Tenant rules

- each user belongs to exactly **one organization** in the MVP
- all tenant-relevant entities carry `organization_id`
- reads and writes must always be scoped by organization
- tenant isolation must be verified through automated tests

This model was chosen as the best balance between:

- simplicity
- correctness
- security
- future evolution

---

## Authorization Model

AssetDock uses **role-based access control** with a small, fixed role set for the MVP.

### Initial roles

- `SUPER_ADMIN`
- `ORG_ADMIN`
- `ASSET_MANAGER`
- `AUDITOR`
- `VIEWER`

### Important design rule

Permissions alone are not enough.

Every action must satisfy both:

1. **role permission**
2. **organization scope**

This is a deliberate security design choice.

---

## Security-First Principles

Security is not an afterthought in this project.  
It is one of the main reasons the project exists.

### Security principles in the MVP

- strong password hashing
- signed JWT authentication
- no hardcoded secrets
- secrets provided through environment variables
- input validation
- standardized error handling
- no sensitive stack traces in API responses
- secure logging practices
- tenant isolation by design
- audit logging for critical actions
- pagination and filtering with safe defaults
- integration tests for cross-tenant access denial

### Security mindset

The purpose is not to create a “security-themed CRUD app”.

The purpose is to build a backend where core boundaries are treated seriously:

- authentication
- authorization
- tenancy
- auditability
- controlled mutation
- traceability

---

## Planned Core Domain

The MVP is centered around the following domain areas:

- **Organizations**
- **Users**
- **Roles**
- **Assets**
- **Categories**
- **Manufacturers**
- **Locations**
- **Asset Assignments**
- **Audit Logs**
- **Asset Import Jobs**

### Asset model direction

Assets are modeled with both:

- a **current operational snapshot**
- a **historical movement trail**

For example, an asset may carry current references such as:

- current assigned user
- current location
- current status

While historical changes are preserved separately through assignment/history-related records.

---

## Import Strategy

CSV import is part of the MVP because it reflects real operational needs.

### MVP import rules

- explicit size and row limits
- validation per row
- partial success supported
- detailed failure reporting
- audit trail for critical import events
- import tracked through `asset_import_jobs`

This feature is intentionally designed to be useful, safe, and bounded.

---

## Auditability

Auditability is a first-class concern in the project.

The system is expected to persist audit events for actions such as:

- login success
- login failure
- user creation and updates
- user disabling
- asset creation and updates
- asset archival
- asset assignment / unassignment
- CSV import lifecycle events

Audit logs are not treated as generic application logs.  
They are part of the product behavior and security story.

---

## Technology Stack

Planned MVP stack:

- **Java 21**
- **Spring Boot 3**
- **Gradle**
- **PostgreSQL**
- **Flyway**
- **Spring Security**
- **JWT**
- **OpenAPI / Swagger**
- **Spring Boot Actuator**
- **Docker Compose**
- **GitHub Actions**
- **JUnit 5**
- **Mockito**
- **SpringBootTest**
- **Testcontainers (PostgreSQL)**
- **Problem Details** for standardized API errors

Run the project and tests with a Java 21 JDK.
Set `JAVA_HOME` to a Java 21 installation before invoking Gradle.
The Gradle toolchain is pinned to Java 21, and `gradle.properties` is configured to resolve it from `JAVA_HOME`.

Example setup:

Windows PowerShell:
`$env:JAVA_HOME="C:\Program Files\Java\jdk-21"`
`$env:Path="$env:JAVA_HOME\bin;$env:Path"`
`.\gradlew.bat test`

macOS / Linux:
`export JAVA_HOME=/path/to/jdk-21`
`export PATH="$JAVA_HOME/bin:$PATH"`
`./gradlew test`

If `JAVA_HOME` points to another version, Gradle will fail because the project requires Java 21.

---

## Database and Migration Strategy

Database evolution follows a simple, professional approach:

- versioned migrations
- Flyway-managed schema changes
- forward-only migration mindset
- no rollback automation required for the MVP
- clear constraints and indexing for tenant-aware data access

### Key conventions

- `snake_case` in the database
- UTC timestamps
- UUIDs for main entities
- organization-scoped uniqueness where appropriate

Examples:

- unique asset tag per organization
- organization-scoped user identity
- indexed tenant-aware search fields

---

## Testing Strategy

Testing is a core part of the project, not an afterthought.

### Planned testing layers

- unit tests for focused business logic
- integration tests for security and persistence flows
- Testcontainers with real PostgreSQL
- critical-path tests for:
  - login
  - role enforcement
  - tenant isolation
  - asset flows
  - import flows

### What matters most

The project should prove, through tests, that:

- one tenant cannot access another tenant’s data
- authorization rules are enforced
- imports behave predictably
- critical mutations are auditable

---

## API Direction

The MVP is expected to expose endpoints across areas such as:

- authentication
- organizations
- users
- assets
- assignments
- catalogs
- imports
- audit logs

The exact contract will evolve during implementation, but the system is intentionally designed as a backend-first API.

---

## Development Priorities

Implementation order is expected to follow this rough sequence:

1. project scaffold
2. environment and base configuration
3. migrations and persistence foundation
4. authentication
5. authorization and tenant isolation
6. organization and user management
7. catalog entities
8. assets
9. assignments
10. audit logging
11. CSV import
12. hardening and test coverage

---

## What this project is meant to demonstrate

AssetDock is intended to demonstrate the ability to design and implement:

- a backend system with real domain relevance
- strong separation of concerns
- secure-by-default decisions
- production-minded foundations
- multi-tenant access control
- AppSec-aware engineering trade-offs

It is especially aligned with roles related to:

- backend engineering
- security-minded backend development
- application security
- product security
- security engineering adjacent to platform/backend work

---

## Local Development

Local development setup will be documented as the scaffold evolves.

Planned local workflow includes:

- PostgreSQL via Docker Compose
- configuration through environment variables
- Flyway-managed schema startup
- OpenAPI documentation
- Actuator health endpoints
- automated tests through Gradle

---

## Roadmap

### MVP
- authentication
- role-based authorization
- tenant isolation
- organizations and users
- asset CRUD and search
- assignment flow
- audit log persistence
- CSV import with import jobs
- integration tests
- CI pipeline

### Phase 2
Potential future work may include:

- refresh tokens
- MFA
- external identity provider integration
- richer audit log querying
- improved asset history modeling
- frontend
- rate limiting
- deeper observability
- stronger database-level tenant hardening

---

## Guiding Philosophy

This project intentionally avoids complexity that looks impressive but slows delivery without improving the MVP.

The guiding principle is:

> **Build a backend that is small enough to finish, strong enough to defend, and real enough to matter.**

---

## License

TBD

---

## Notes

This repository is under active construction.  
The architecture, scope, and implementation order are being intentionally shaped to prioritize:

- correctness
- security
- clarity
- execution discipline
