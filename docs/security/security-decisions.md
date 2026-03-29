# Security Decisions

This document records the main security-relevant implementation decisions reflected in the current codebase.

## 1. Shared-Schema Multi-Tenancy

Decision:
- keep one schema and enforce tenant isolation with `organization_id`

Why:
- simpler MVP operational model
- easier to exercise end-to-end authorization paths in one application

Trade-off:
- isolation depends on disciplined application and query enforcement, not physical database separation

## 2. Global `SUPER_ADMIN`, Tenant-Scoped Normal Users

Decision:
- `SUPER_ADMIN` is global and may have `organization_id = null`
- tenant-scoped users belong to exactly one organization in the MVP

Why:
- supports explicit platform administration without introducing multi-membership complexity

Related:
- global email uniqueness is documented separately in [ADR-001](../adr/ADR-001-global-unique-email.md)

## 3. Service-Layer Authorization as the Main Enforcement Point

Decision:
- keep authorization centralized in application services and `TenantAccessService`

Why:
- makes tenant and role decisions explicit in business flows
- keeps endpoint access rules close to use-case logic

Trade-off:
- relies on disciplined use of shared access checks rather than annotation-only protection

## 4. JWT Access Tokens with Shared Secret Signing

Decision:
- use stateless JWT access tokens signed with a configured shared secret

Why:
- fits a small backend-focused MVP
- avoids server-side session state

Trade-off:
- no refresh tokens, token revocation, or MFA in the current implementation

## 5. Local In-Memory Throttling

Decision:
- use bounded in-memory throttling for login and CSV import

Why:
- small and coherent for a single-instance portfolio project
- enough to demonstrate abuse controls without introducing infrastructure complexity

Trade-off:
- not distributed across multiple instances
- relies on trusted origin forwarding when `X-Forwarded-For` is used

## 6. Audit Logging for Security-Sensitive Actions

Decision:
- persist audit events for authentication, user management, assignments, import jobs, and lifecycle changes

Why:
- makes administrative and security-relevant changes reviewable
- supports portfolio evidence of accountability and forensic thinking

Trade-off:
- current implementation focuses on persistence and queryability, not external log shipping

## 7. Safe-by-Default Public Surface

Decision:
- disable docs by default, narrow actuator exposure, and suppress stack traces in public errors

Why:
- reduce accidental operational exposure
- keep the public API surface explicit

Trade-off:
- some operational convenience moves to profile/config toggles instead of always-on defaults

## 8. Bounded Imports and Reads

Decision:
- keep CSV import synchronous but bounded, and clamp/validate high-volume reads

Why:
- reduces abuse potential and keeps failure modes predictable

Trade-off:
- large workloads are intentionally out of scope for the current implementation
