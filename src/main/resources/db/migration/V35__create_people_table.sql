-- 1. Create the people table
CREATE TABLE people (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    organization_id UUID NOT NULL REFERENCES organizations(id) ON DELETE CASCADE,
    full_name VARCHAR(150) NOT NULL,
    email VARCHAR(320) NULL,
    department VARCHAR(100) NULL,
    active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_people_organization_id ON people (organization_id);
CREATE INDEX idx_people_active ON people (active);

-- 2. Seed/copy existing users to the people table so that their IDs match exactly!
-- This preserves references in assets, assignments, and checkouts!
INSERT INTO people (id, organization_id, full_name, email, active, created_at, updated_at)
SELECT id, organization_id, full_name, email, (status = 'ACTIVE'), created_at, updated_at FROM users;

-- 3. Alter assets table constraints and columns
ALTER TABLE assets DROP CONSTRAINT IF EXISTS assets_current_assigned_user_id_fkey;
ALTER TABLE assets RENAME COLUMN current_assigned_user_id TO current_assigned_person_id;
ALTER TABLE assets ADD CONSTRAINT fk_assets_current_assigned_person FOREIGN KEY (current_assigned_person_id) REFERENCES people(id) ON DELETE SET NULL;
DROP INDEX IF EXISTS idx_assets_current_assigned_user_id;
CREATE INDEX idx_assets_current_assigned_person_id ON assets (current_assigned_person_id);

-- 4. Alter asset_assignments table constraints and columns
ALTER TABLE asset_assignments DROP CONSTRAINT IF EXISTS asset_assignments_user_id_fkey;
ALTER TABLE asset_assignments RENAME COLUMN user_id TO person_id;
ALTER TABLE asset_assignments ADD CONSTRAINT fk_asset_assignments_person FOREIGN KEY (person_id) REFERENCES people(id) ON DELETE CASCADE;
DROP INDEX IF EXISTS idx_asset_assignments_user_id;
CREATE INDEX idx_asset_assignments_person_id ON asset_assignments (person_id);

-- 5. Alter asset_checkouts table constraints and columns
ALTER TABLE asset_checkouts DROP CONSTRAINT IF EXISTS asset_checkouts_user_id_fkey;
ALTER TABLE asset_checkouts RENAME COLUMN user_id TO person_id;
ALTER TABLE asset_checkouts ADD CONSTRAINT fk_asset_checkouts_person FOREIGN KEY (person_id) REFERENCES people(id) ON DELETE CASCADE;
DROP INDEX IF EXISTS idx_asset_checkouts_user_id;
CREATE INDEX idx_asset_checkouts_person_id ON asset_checkouts (person_id);
