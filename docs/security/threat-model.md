# Threat Model

AssetDock is a security-first, multi-tenant backend for asset inventory and assignment workflows. The relevant threat model for the current implementation is centered on API abuse, tenant breakout, privilege misuse, and audit integrity.

## Protected Assets

- Access tokens and user credentials
- Tenant-scoped data for users, catalogs, assets, assignments, imports, and audit logs
- Security-sensitive state such as user roles, user status, and assignment history
- Persistent audit records for authentication and administrative actions

## Primary Trust Assumptions

- TLS termination is handled by the deployment environment.
- PostgreSQL and application secrets are controlled outside the app.
- `X-Forwarded-For` is only trustworthy behind a trusted proxy.
- Local in-memory throttling is acceptable for the current single-instance-oriented scope.

## Main Threats Considered

## 1. Authentication Abuse

- Repeated password guessing against `POST /api/v1/auth/login`
- Enumeration pressure through account state differences

Current response:
- generic auth failures
- failed-login tracking
- automatic account lock after a configurable threshold
- lightweight endpoint throttling on login
- audit events for login success, failure, and automatic locking

## 2. Authorization and Tenant Breakout

- Cross-tenant reads or writes by tenant-scoped users
- Privilege escalation through role or status changes
- Over-broad access to sensitive endpoints

Current response:
- tenant-aware authorization checks in service-layer access control
- explicit role boundaries for `SUPER_ADMIN`, `ORG_ADMIN`, `ASSET_MANAGER`, `AUDITOR`, and `VIEWER`
- guardrails against removing the last effective admin or assigning invalid global roles

## 3. Import Abuse

- Oversized CSV uploads
- Malformed CSV structure
- Duplicate `asset_tag` values inside one upload
- Repeated import attempts

Current response:
- file size and row-count limits
- header validation and malformed-row handling
- duplicate asset-tag rejection inside the same upload
- import throttling and audit coverage for invalid attempts

## 4. Data Integrity Abuse

- Assigning archived assets
- Assigning inactive or locked users
- Using inactive locations in new assignments
- Mutating archived assets after archive

Current response:
- lifecycle checks in asset, catalog, and assignment services
- bounded query defaults for large reads
- RFC 9457-style problem responses

## 5. Operational Surface Exposure

- Unnecessary exposure of actuator or API docs
- Verbose public error responses

Current response:
- `health` exposed, broader actuator discovery disabled
- Swagger/OpenAPI disabled by default and only enabled explicitly
- public errors avoid stack traces and internal exception leakage

## Out of Scope for the Current Implementation

- MFA or phishing-resistant authentication
- Distributed/shared rate limiting across instances
- Malware scanning for uploads
- Secret rotation infrastructure
- Host, network, or cloud control hardening outside the application
