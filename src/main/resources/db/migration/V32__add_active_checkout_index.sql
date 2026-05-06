CREATE INDEX IF NOT EXISTS idx_asset_checkouts_active 
ON asset_checkouts (asset_id) 
WHERE checked_in_at IS NULL;
