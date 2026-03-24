# AssetDock API

AssetDock API is a security-first, multi-tenant asset inventory backend built as a modular monolith.

## MVP Status

The backend MVP is implemented and currently focused on final hardening.

Implemented modules:

- `auth`
- `organization`
- `user`
- `catalog`
- `asset`
- `assignment`
- `importer`
- `audit`

Shared support modules:

- `security`
- `common`
- `config`

## Architecture

- Modular monolith with packages by domain: `api`, `application`, `domain`, `infrastructure`
- Shared-schema multi-tenancy with `organization_id` isolation
- Forward-only Flyway migrations
- JWT authentication + role-based authorization

Roles in MVP:

- `SUPER_ADMIN`
- `ORG_ADMIN`
- `ASSET_MANAGER`
- `AUDITOR`
- `VIEWER`

## Implemented Features

- JWT login (`POST /api/v1/auth/login`)
- User management with global unique email
- Tenant-aware catalog management (`categories`, `manufacturers`, `locations`)
- Tenant-aware asset management
- Asset assignment flow (`assign`, `unassign`, history by asset)
- Asset CSV import with persisted jobs, row validation, size/line limits and partial success
- Persisted audit logs for critical actions
- Audit log read endpoint with tenant-aware access, pagination and simple filters

## API Overview

Main endpoint groups:

- `/api/v1/auth/*`
- `/organizations/*`
- `/users/*`
- `/categories`, `/manufacturers`, `/locations`
- `/assets/*`
- `/assets/{assetId}/assignments`, `/assets/{assetId}/unassign`
- `/imports/assets/*`
- `/audit-logs` and `/api/v1/audit-logs`

Notes:

- Authentication route is versioned.
- Business routes are stable and tenant-aware.
- Audit log read is available in both unversioned and versioned forms for compatibility.

## Audit Log Read Endpoint

`GET /audit-logs` (or `GET /api/v1/audit-logs`)

Supported query params:

- `page` (default `0`)
- `size` (default `20`, max `100`)
- `eventType` (optional)
- `from` (optional, ISO-8601 datetime)
- `to` (optional, ISO-8601 datetime)
- `organizationId` (optional, intended for `SUPER_ADMIN` scope selection)

Permissions:

- `ORG_ADMIN`: allowed in own tenant
- `AUDITOR`: allowed in own tenant
- `SUPER_ADMIN`: allowed globally
- `VIEWER`: denied

## Security and Multi-Tenancy

- Tenant isolation is enforced in reads and writes for tenant-scoped entities.
- Cross-tenant access is denied.
- Errors follow standardized Problem Details responses.
- Critical actions are audit logged.

## Technology Stack

- Java 21
- Spring Boot 3
- Spring Security
- Spring JDBC
- PostgreSQL
- Flyway
- Apache Commons CSV
- OpenAPI / Swagger
- Spring Boot Actuator
- JUnit 5
- Testcontainers (PostgreSQL)

## Local Development

### Prerequisites

- Java 21 JDK
- Docker + Docker Compose

### Java 21 requirement

The project toolchain is pinned to Java 21.

- `build.gradle` sets `JavaLanguageVersion.of(21)`
- `gradle.properties` resolves toolchains from `JAVA_HOME`
- Auto-download is disabled

Set `JAVA_HOME` to a Java 21 JDK before running Gradle.

Windows PowerShell:

```powershell
$env:JAVA_HOME="C:\Program Files\Java\jdk-21"
$env:Path="$env:JAVA_HOME\bin;$env:Path"
```

macOS / Linux:

```bash
export JAVA_HOME=/path/to/jdk-21
export PATH="$JAVA_HOME/bin:$PATH"
```

### Run locally

```bash
docker compose up -d
./gradlew test
./gradlew bootRun
```

Windows equivalents:

- `.\gradlew.bat test`
- `.\gradlew.bat bootRun`

## Testing

Current test suite includes unit and integration tests for:

- authentication
- authorization
- tenant isolation
- users
- catalogs
- assets
- assignments
- CSV import jobs
- audit logging

## Out of Scope for MVP

- Frontend application
- Refresh token flow
- MFA
- Microservices
- Queue/background processing for import jobs
- Generic import engine across domains

