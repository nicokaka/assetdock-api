CREATE TABLE categories (
    id UUID PRIMARY KEY,
    organization_id UUID NOT NULL REFERENCES organizations (id),
    name VARCHAR(150) NOT NULL,
    description TEXT NULL,
    active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE UNIQUE INDEX uk_categories_organization_name_lower ON categories (organization_id, LOWER(name));
CREATE INDEX idx_categories_organization_id ON categories (organization_id);
CREATE INDEX idx_categories_active ON categories (active);
