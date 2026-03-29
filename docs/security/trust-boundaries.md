# Trust Boundaries

This document describes the main trust boundaries in the current AssetDock implementation and how requests cross them.

## Boundary 1: External Client -> Public HTTP API

Entry points:
- `POST /api/v1/auth/login`
- tenant-scoped business endpoints
- `GET /actuator/health`
- Swagger/OpenAPI only when explicitly enabled

Controls at this boundary:
- stateless Spring Security resource server
- JWT bearer authentication for protected endpoints
- endpoint throttling for login and CSV import
- Problem Details responses with reduced leakage

## Boundary 2: Authenticated Principal -> Authorization Layer

After authentication, access decisions depend on:
- `user_id`
- `organization_id`
- assigned roles

The main enforcement point is the service-layer access control logic in `TenantAccessService`, which separates:
- organization read access
- user read/write access
- catalog read/write access
- asset read/write access
- assignment read/write access
- import read/write access
- audit log read access

This is the main trust boundary for preventing role overreach and cross-tenant access.

## Boundary 3: Tenant Context -> Shared Database Schema

AssetDock uses shared-schema multi-tenancy. Tenant-aware entities carry `organization_id`, and reads/writes are scoped by that identifier in repositories and application services.

Security expectation:
- tenant-scoped actors must not access records from a different `organization_id`
- `SUPER_ADMIN` is the only intentional global exception in the current model

This boundary matters most for:
- users
- catalogs
- assets
- assignments
- import jobs
- audit logs

## Boundary 4: Untrusted CSV Upload -> Import Processing

Uploaded CSV files are treated as untrusted input.

Controls before and during processing:
- empty-file rejection
- file size bound
- data-row bound
- required-header validation
- malformed-row handling
- duplicate `asset_tag` rejection within the same upload

The import flow remains synchronous and tenant-aware.

## Boundary 5: Administrative Mutation -> Audit Trail

Sensitive actions cross into a persistent audit boundary when they are recorded in `audit_logs`.

Current audit coverage includes:
- login success/failure
- automatic user lock
- user creation, role changes, and status changes
- catalog creation and lifecycle mutations
- asset lifecycle changes
- assignment changes
- CSV import lifecycle events

This boundary provides accountability rather than prevention, but it is part of the security posture.

## Boundary 6: Application -> Operational Surface

Operational exposure is intentionally narrow:
- actuator discovery disabled
- only selected management exposure configured
- public docs disabled by default
- error responses sanitized for public clients

This boundary reduces accidental information disclosure from the runtime surface.
