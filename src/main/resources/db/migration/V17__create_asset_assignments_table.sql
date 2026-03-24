CREATE TABLE asset_assignments (
    id UUID PRIMARY KEY,
    organization_id UUID NOT NULL REFERENCES organizations (id),
    asset_id UUID NOT NULL REFERENCES assets (id),
    user_id UUID NOT NULL REFERENCES users (id),
    location_id UUID NULL REFERENCES locations (id),
    assigned_at TIMESTAMPTZ NOT NULL,
    unassigned_at TIMESTAMPTZ NULL,
    assigned_by UUID NOT NULL REFERENCES users (id),
    notes TEXT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT chk_asset_assignments_unassigned_after_assigned
        CHECK (unassigned_at IS NULL OR unassigned_at >= assigned_at)
);

CREATE UNIQUE INDEX uk_asset_assignments_active_asset
    ON asset_assignments (organization_id, asset_id)
    WHERE unassigned_at IS NULL;

CREATE INDEX idx_asset_assignments_organization_id ON asset_assignments (organization_id);
CREATE INDEX idx_asset_assignments_asset_id ON asset_assignments (asset_id);
CREATE INDEX idx_asset_assignments_user_id ON asset_assignments (user_id);
CREATE INDEX idx_asset_assignments_location_id ON asset_assignments (location_id);
CREATE INDEX idx_asset_assignments_assigned_by ON asset_assignments (assigned_by);
CREATE INDEX idx_asset_assignments_assigned_at ON asset_assignments (assigned_at DESC);
CREATE INDEX idx_asset_assignments_unassigned_at ON asset_assignments (unassigned_at);
