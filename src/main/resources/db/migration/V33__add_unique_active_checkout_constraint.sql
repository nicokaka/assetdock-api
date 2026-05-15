-- L-6: Enforce uniqueness at the database level: only one active (not yet checked-in)
-- checkout is permitted per asset. This prevents double-checkout and double-checkin
-- even if the application-layer guard is bypassed or a race condition occurs.
CREATE UNIQUE INDEX uq_asset_checkouts_active
    ON asset_checkouts (asset_id)
    WHERE checked_in_at IS NULL;
