CREATE EXTENSION IF NOT EXISTS pg_trgm;

CREATE INDEX idx_assets_org_status ON assets(organization_id, status) WHERE archived_at IS NULL;
CREATE INDEX idx_assets_org_tag ON assets(organization_id, asset_tag);
CREATE INDEX idx_assets_display_name_trgm ON assets USING gin(display_name gin_trgm_ops);
CREATE INDEX idx_assets_hostname_trgm ON assets USING gin(hostname gin_trgm_ops);
CREATE INDEX idx_assets_serial_number_trgm ON assets USING gin(serial_number gin_trgm_ops);
