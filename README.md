<div align="center">

🌐 **English** · [Português](README.pt-BR.md)

# 🔒 AssetDock API

**Security-First, Multi-Tenant Asset Inventory Platform**

[![CI](https://github.com/nicokaka/assetdock-api/actions/workflows/ci.yml/badge.svg)](https://github.com/nicokaka/assetdock-api/actions/workflows/ci.yml)
![Java 21](https://img.shields.io/badge/Java-21-ED8B00?logo=openjdk&logoColor=white)
![Spring Boot 3](https://img.shields.io/badge/Spring%20Boot-3.5-6DB33F?logo=springboot&logoColor=white)
![PostgreSQL 17](https://img.shields.io/badge/PostgreSQL-17-4169E1?logo=postgresql&logoColor=white)
![License](https://img.shields.io/badge/License-MIT-green)

A production-grade backend API for managing hardware and software assets across organizations.  
Built with **tenant isolation**, **role-based access control**, **immutable audit trails**, and a focus on **data integrity** — the foundations that matter in any security-conscious environment.

[Architecture](#architecture) · [Security Model](#security--multi-tenancy) · [Security Assurance Docs](#security-assurance-docs) · [API Reference](#api-reference) · [Getting Started](#getting-started)

</div>

---

## Why AssetDock?

Organizations need a reliable, auditable system to track **who has what, when it was assigned, and what changed**. AssetDock solves this with:

- 🏢 **Multi-tenant isolation** — each organization's data is logically separated at the query level; cross-tenant access is denied by design.
- 🛡️ **Role-based authorization** — five distinct roles enforce least-privilege access across every endpoint.
- 📋 **Immutable audit logging** — critical auth, user management, assignment, import, and lifecycle actions are recorded with actor, timestamp, and tenant context.
- 🚦 **Abuse controls** — failed-login lockout, endpoint throttling, bounded queries, and validated CSV imports harden the public API surface.
- 📦 **Bulk CSV import** — upload asset inventories with per-row validation, size/line limits, and partial-success semantics.

---

## Architecture

```
┌─────────────────────────────────────────────────────────┐
│                     AssetDock API                       │
│                  Modular Monolith                       │
├──────────┬──────────┬──────────┬────────────┬───────────┤
│   Auth   │  Asset   │ Catalog  │ Assignment │  Importer │
│  Module  │  Module  │  Module  │   Module   │   Module  │
├──────────┼──────────┼──────────┼────────────┼───────────┤
│   User   │   Org    │  Audit   │  Security  │  Common   │
│  Module  │  Module  │  Module  │  (shared)  │  (shared) │
├──────────┴──────────┴──────────┴────────────┴───────────┤
│          Spring Security + JWT (HMAC / HS256)           │
├─────────────────────────────────────────────────────────┤
│           PostgreSQL 17 + Flyway Migrations             │
└─────────────────────────────────────────────────────────┘
```

| Principle | Implementation |
|---|---|
| **Modular Monolith** | Each domain module follows `api → application → domain → infrastructure` layering |
| **Shared-Schema Multi-Tenancy** | Tenant isolation via `organization_id` on all scoped entities |
| **Forward-Only Migrations** | Flyway-managed DDL — no manual SQL, no rollback scripts |
| **Domain-Driven Packaging** | 8 bounded contexts: `auth`, `user`, `organization`, `catalog`, `asset`, `assignment`, `importer`, `audit` |
| **Architecture Decision Records** | Documented trade-offs in `docs/adr/` (e.g. global email uniqueness strategy) |

---

## Security & Multi-Tenancy

> Security is not a feature — it's the architecture.

### Role-Based Access Control (RBAC)

| Role | Scope | Capabilities |
|---|---|---|
| `SUPER_ADMIN` | Global | Full system access, cross-tenant operations |
| `ORG_ADMIN` | Tenant | Manage users, assets, catalogs within their org |
| `ASSET_MANAGER` | Tenant | CRUD on assets, assignments, imports |
| `AUDITOR` | Tenant | Read-only access to audit logs |
| `VIEWER` | Tenant | Read-only access to assets and catalogs |

### Tenant Isolation Guarantees

- ✅ Every read and write operation is scoped to the authenticated tenant
- ✅ Cross-tenant data access is systematically denied
- ✅ Audit logs capture `organizationId`, `actorId`, `eventType`, and `timestamp`
- ✅ Standardized error responses follow **RFC 9457 (Problem Details)** format

### Authentication Flow

```
Client ──► POST /api/v1/auth/login (credentials)
       ◄── JWT access token (signed, expiring)
Client ──► GET /assets (Authorization: Bearer <token>)
       ◄── Tenant-scoped response
```

## Security Assurance Docs

- [Threat Model](docs/security/threat-model.md)
- [Trust Boundaries](docs/security/trust-boundaries.md)
- [Abuse Cases](docs/security/abuse-cases.md)
- [Security Decisions](docs/security/security-decisions.md)

---

## API Reference

All business endpoints are tenant-aware. The authenticated user's `organization_id` is automatically applied.

| Group | Endpoints | Description |
|---|---|---|
| **Auth** | `POST /api/v1/auth/login` | JWT authentication |
| **Organizations** | `/organizations/*` | Tenant-scoped organization read |
| **Users** | `/users/*` | User lifecycle, status, and role management |
| **Catalog** | `/categories`, `/manufacturers`, `/locations` | Asset metadata management |
| **Assets** | `/assets/*` | Asset lifecycle management |
| **Assignments** | `/assets/{id}/assignments`, `/assets/{id}/unassign` | Assign/unassign assets with history tracking |
| **Import** | `/imports/assets/*` | CSV bulk import with job tracking |
| **Audit Logs** | `/audit-logs` | Paginated, filterable audit trail |

> 📖 **Interactive docs** are enabled in `local` and `test` profiles, or when `PUBLIC_DOCS_ENABLED=true`.

---

## Testing Strategy

The project includes unit and integration coverage powered by **JUnit 5** and **Testcontainers** (real PostgreSQL for integration flows).

| Test Layer | What's Covered |
|---|---|
| **Integration Tests** | Auth login, asset CRUD, assignments, catalog management, CSV imports, audit log read/write, user management |
| **Unit Tests** | Authentication service logic, catalog services, user management, error handling (ProblemDetail factory), dev seed runner |

```bash
# Run the full test suite against a real PostgreSQL container
./gradlew clean check
```

CI runs automatically on every push and pull request via **GitHub Actions**.

---

## Tech Stack

| Layer | Technology |
|---|---|
| **Language** | Java 21 (LTS) |
| **Framework** | Spring Boot 3.5, Spring Security, Spring JDBC |
| **Auth** | JWT with `spring-security-oauth2-jose` |
| **Database** | PostgreSQL 17 (Alpine) |
| **Migrations** | Flyway |
| **API Docs** | SpringDoc OpenAPI (Swagger UI) |
| **Monitoring** | Spring Boot Actuator |
| **Bulk Import** | Apache Commons CSV |
| **Testing** | JUnit 5, Testcontainers |
| **CI/CD** | GitHub Actions |
| **Containerisation** | Docker Compose |

---

## Getting Started

### Prerequisites

- **Java 21 JDK** (Temurin recommended)
- **Docker** + **Docker Compose**

### 1. Clone & configure

```bash
git clone https://github.com/nicokaka/assetdock-api.git
cd assetdock-api
cp .env.example .env   # configure database settings and JWT_SECRET
```

### 2. Start the database

```bash
docker compose up -d
```

### 3. Run tests

```bash
./gradlew clean check
```

### 4. Start the server

```bash
./gradlew bootRun
```

The API will be available at `http://localhost:8080`.  
Swagger UI at `http://localhost:8080/swagger-ui.html`.

---

## Project Roadmap

### ✅ MVP — Completed

- Multi-tenant asset inventory with full CRUD
- JWT authentication and RBAC (5 roles)
- Asset assignment flow with history
- CSV bulk import with validation and partial success
- Immutable audit logging with read endpoints
- CI pipeline with Testcontainers

### 🔮 Future Enhancements

- Refresh token rotation and token revocation
- Multi-factor authentication (MFA / TOTP)
- Background job processing for large imports
- Generic import engine across domains
- Frontend application (React / Next.js)

---

## Project Structure

```
assetdock-api/
├── src/main/java/com/assetdock/api/
│   ├── auth/           # Authentication & JWT
│   ├── user/           # User lifecycle management
│   ├── organization/   # Tenant management
│   ├── catalog/        # Categories, manufacturers, locations
│   ├── asset/          # Core asset CRUD
│   ├── assignment/     # Asset assignment & history
│   ├── importer/       # CSV bulk import engine
│   ├── audit/          # Immutable audit trail
│   ├── security/       # Shared security filters & config
│   ├── common/         # Shared utilities & error handling
│   └── config/         # Application configuration
├── src/test/           # Unit and integration tests
├── docs/adr/           # Architecture Decision Records
├── .github/workflows/  # CI and security workflows
├── docker-compose.yml  # Local PostgreSQL
└── build.gradle        # Gradle build config
```

---

<div align="center">

**Built with 🔒 security at the core.**

*Designed as a portfolio-grade backend demonstrating enterprise patterns:  
multi-tenancy, RBAC, audit compliance, domain-driven design, and automated testing.*

</div>
