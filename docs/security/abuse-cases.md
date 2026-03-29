# Abuse Cases

This document captures the main abuse cases the current implementation is designed to handle, plus the controls that exist today.

## 1. Brute-force Login Attempts

Abuse case:
- an attacker repeatedly submits credentials to the login endpoint

Current controls:
- failed login counter on the user record
- automatic lock after configurable consecutive failures
- lightweight endpoint throttling on `POST /api/v1/auth/login`
- generic failure responses to reduce state leakage
- audit entries for success, failure, and automatic locking

## 2. Privilege Escalation by Tenant Admins

Abuse case:
- a tenant admin attempts to assign `SUPER_ADMIN` or remove the last effective admin

Current controls:
- only `SUPER_ADMIN` can assign `SUPER_ADMIN`
- tenant-scoped users cannot receive `SUPER_ADMIN`
- last effective `ORG_ADMIN` and last effective `SUPER_ADMIN` protections
- role and status changes are audited

## 3. Cross-Tenant Data Access

Abuse case:
- a tenant-scoped actor tries to read or mutate another tenant's users, assets, imports, or audit logs

Current controls:
- service-layer tenant checks
- repository queries scoped by `organization_id`
- deny-by-default behavior on sensitive endpoints
- integration coverage for cross-tenant denial paths

## 4. Import Abuse and Malformed CSV Input

Abuse case:
- repeated uploads, oversized files, malformed structure, or duplicate identifiers in one upload

Current controls:
- import throttling
- file size and row-count limits
- required-header validation
- safe handling of malformed rows
- duplicate `asset_tag` rejection inside the same upload
- audit coverage for invalid import attempts

## 5. Unsafe Asset and Assignment State Transitions

Abuse case:
- assigning archived assets
- assigning locked/inactive users
- using inactive locations
- mutating archived assets after archive

Current controls:
- explicit lifecycle checks in asset and assignment services
- archived asset protections
- active-user and active-location requirements for new assignments

## 6. Oversized Query Abuse

Abuse case:
- clients attempt unbounded reads or excessive page sizes

Current controls:
- explicit page and size validation on paginated audit log queries
- bounded list limits for non-paginated reads
- safe error responses for invalid query input

## 7. Information Leakage Through Operational Endpoints

Abuse case:
- clients probe docs, actuator endpoints, or error responses for internal details

Current controls:
- docs disabled by default
- constrained actuator exposure
- no stack traces or internal exception details in public responses

## Intentionally Out of Scope

The current project does not claim:
- distributed throttling
- adaptive risk scoring
- malware scanning of uploads
- MFA
- SIEM integration or alert pipelines

Those may be valid future directions, but they are not represented as implemented controls today.
