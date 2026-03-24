CREATE TABLE asset_import_jobs (
    id UUID PRIMARY KEY,
    organization_id UUID NOT NULL REFERENCES organizations (id),
    uploaded_by_user_id UUID NOT NULL REFERENCES users (id),
    status asset_import_job_status NOT NULL,
    file_name VARCHAR(255) NOT NULL,
    total_rows INTEGER NOT NULL DEFAULT 0,
    processed_rows INTEGER NOT NULL DEFAULT 0,
    success_count INTEGER NOT NULL DEFAULT 0,
    error_count INTEGER NOT NULL DEFAULT 0,
    result_summary_json JSONB NOT NULL DEFAULT '{}'::jsonb,
    started_at TIMESTAMPTZ NOT NULL,
    finished_at TIMESTAMPTZ NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_asset_import_jobs_organization_id ON asset_import_jobs (organization_id);
CREATE INDEX idx_asset_import_jobs_uploaded_by_user_id ON asset_import_jobs (uploaded_by_user_id);
CREATE INDEX idx_asset_import_jobs_status ON asset_import_jobs (status);
CREATE INDEX idx_asset_import_jobs_created_at ON asset_import_jobs (created_at DESC);
