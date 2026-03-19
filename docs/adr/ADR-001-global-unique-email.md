# ADR-001: Global Unique Email for Users

## Status

Accepted

## Context

In the MVP, `users.email` is globally unique across the whole system, not only inside an organization.

The project also has a global `SUPER_ADMIN` role that exists outside any tenant and may have `organization_id = null`.

## Decision

We keep `users.email` globally unique in the MVP.

This is a pragmatic decision to:

- simplify authentication and login resolution
- avoid ambiguous identity lookups across tenants
- support the global `SUPER_ADMIN` model without special-case login rules

## Consequences

- login can resolve a user by email alone
- local bootstrap and admin setup remain simple
- tenant-specific duplicate emails are intentionally not supported in the MVP

## Future Evolution

If the product later needs the same person attached to multiple organizations, the preferred evolution path is:

1. keep a globally unique identity
2. introduce memberships or multi-organization associations
3. avoid duplicating the same email per organization

The expected future model is therefore `identity + memberships`, not `tenant-scoped duplicate emails`.
