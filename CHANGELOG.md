# Changelog

All notable changes to this project are documented here.

The format is intentionally lightweight and release-oriented for this portfolio project.

## [Unreleased]

### Changed
- Audit v2: status transition guard, actor filter, session query performance, archive message, read-only transaction annotations
- Checkout RBAC hardened: race condition guard, setup endpoint security, dashboard null-org edge case

### Added
- Integration tests for the Checkout module (`CheckoutIntegrationTest`)

---

## [1.1.0] - 2026-05-07

### Added
- Asset Checkout and Timeline system (Phase 1): checkout, checkin, history endpoints with pessimistic locking
- Setup wizard module: self-hosted first-run provisioning with PostgreSQL advisory lock
- Active Checkouts KPI on dashboard
- Category, location, status, and role filters for Assets and Users lists
- Admin password reset and emergency CLI tool
- Production hardening (pessimistic locking, secure cookies enforcement, debouncing)
- Route Error Boundaries and robust session handoff on frontend
- Self-Hosted Deployment configuration (`docker-compose.selfhosted.yml` and `SELFHOSTED.md`)

### Fixed
- Stabilized API contracts and generic error handling bypasses.
- Resolved all critical production audit findings (connection errors, routing, cache invalidation, UI bugs).

## [1.0.0] - 2026-03-29

Initial public portfolio release of AssetDock API.

### Included

- modular monolith backend for multi-tenant asset inventory
- JWT authentication and tenant-aware RBAC
- organization, user, catalog, asset, assignment, importer, and audit modules
- persistent audit logging for security-sensitive and administrative actions
- abuse protections for login and CSV import flows
- lifecycle hardening for users, catalogs, assets, and assignments
- bounded query and pagination behavior on exposed read paths
- security documentation covering threat model, trust boundaries, abuse cases, and security decisions
- repository security/governance files and automation:
  - `LICENSE`
  - `SECURITY.md`
  - Dependabot
  - CodeQL
