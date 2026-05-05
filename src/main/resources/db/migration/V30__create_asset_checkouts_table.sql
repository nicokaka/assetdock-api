CREATE TABLE asset_checkouts (
    id UUID PRIMARY KEY,
    organization_id UUID NOT NULL REFERENCES organizations(id),
    asset_id UUID NOT NULL REFERENCES assets(id),
    user_id UUID NOT NULL REFERENCES users(id),
    checked_out_at TIMESTAMP WITH TIME ZONE NOT NULL,
    expected_return_date TIMESTAMP WITH TIME ZONE,
    checked_in_at TIMESTAMP WITH TIME ZONE,
    checked_out_by UUID NOT NULL REFERENCES users(id),
    checked_in_by UUID REFERENCES users(id),
    notes TEXT,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_asset_checkouts_org_id ON asset_checkouts(organization_id);
CREATE INDEX idx_asset_checkouts_asset_id ON asset_checkouts(asset_id);
CREATE INDEX idx_asset_checkouts_user_id ON asset_checkouts(user_id);
