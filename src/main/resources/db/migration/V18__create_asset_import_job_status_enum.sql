CREATE TYPE asset_import_job_status AS ENUM (
    'PROCESSING',
    'COMPLETED',
    'COMPLETED_WITH_ERRORS',
    'FAILED'
);
