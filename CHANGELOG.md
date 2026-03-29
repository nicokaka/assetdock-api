# Changelog

All notable changes to this project are documented here.

The format is intentionally lightweight and release-oriented for this portfolio project.

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
