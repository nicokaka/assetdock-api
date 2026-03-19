CREATE TABLE manufacturers (
    id UUID PRIMARY KEY,
    organization_id UUID NOT NULL REFERENCES organizations (id),
    name VARCHAR(150) NOT NULL,
    description TEXT NULL,
    website VARCHAR(255) NULL,
    active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE UNIQUE INDEX uk_manufacturers_organization_name_lower ON manufacturers (organization_id, LOWER(name));
CREATE INDEX idx_manufacturers_organization_id ON manufacturers (organization_id);
CREATE INDEX idx_manufacturers_active ON manufacturers (active);
