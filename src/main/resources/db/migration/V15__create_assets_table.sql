CREATE TABLE assets (
    id UUID PRIMARY KEY,
    organization_id UUID NOT NULL REFERENCES organizations (id),
    asset_tag VARCHAR(120) NOT NULL,
    serial_number VARCHAR(150) NULL,
    hostname VARCHAR(150) NULL,
    display_name VARCHAR(200) NOT NULL,
    description TEXT NULL,
    category_id UUID NULL REFERENCES categories (id),
    manufacturer_id UUID NULL REFERENCES manufacturers (id),
    current_location_id UUID NULL REFERENCES locations (id),
    current_assigned_user_id UUID NULL REFERENCES users (id),
    status asset_status NOT NULL DEFAULT 'IN_STOCK',
    purchase_date DATE NULL,
    warranty_expiry_date DATE NULL,
    archived_at TIMESTAMPTZ NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE UNIQUE INDEX uk_assets_organization_asset_tag_lower ON assets (organization_id, LOWER(asset_tag));
CREATE INDEX idx_assets_organization_id ON assets (organization_id);
CREATE INDEX idx_assets_serial_number ON assets (serial_number);
CREATE INDEX idx_assets_category_id ON assets (category_id);
CREATE INDEX idx_assets_manufacturer_id ON assets (manufacturer_id);
CREATE INDEX idx_assets_current_location_id ON assets (current_location_id);
CREATE INDEX idx_assets_current_assigned_user_id ON assets (current_assigned_user_id);
CREATE INDEX idx_assets_status ON assets (status);
CREATE INDEX idx_assets_archived_at ON assets (archived_at);
