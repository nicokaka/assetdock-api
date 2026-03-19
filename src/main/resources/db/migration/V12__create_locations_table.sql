CREATE TABLE locations (
    id UUID PRIMARY KEY,
    organization_id UUID NOT NULL REFERENCES organizations (id),
    name VARCHAR(150) NOT NULL,
    description TEXT NULL,
    active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE UNIQUE INDEX uk_locations_organization_name_lower ON locations (organization_id, LOWER(name));
CREATE INDEX idx_locations_organization_id ON locations (organization_id);
CREATE INDEX idx_locations_active ON locations (active);
