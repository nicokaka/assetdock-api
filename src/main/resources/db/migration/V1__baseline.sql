-- Baseline migration for the initial scaffold.
-- Future tenant-aware tables will share a schema and carry organization_id.
CREATE EXTENSION IF NOT EXISTS pgcrypto;
