CREATE TABLE audit_logs (
    id UUID PRIMARY KEY,
    organization_id UUID NULL REFERENCES organizations (id),
    actor_user_id UUID NULL REFERENCES users (id),
    event_type audit_event_type NOT NULL,
    resource_type VARCHAR(100) NOT NULL,
    resource_id UUID NULL,
    outcome VARCHAR(40) NOT NULL,
    ip_address VARCHAR(64) NULL,
    user_agent TEXT NULL,
    request_id VARCHAR(100) NULL,
    details_json JSONB NOT NULL DEFAULT '{}'::jsonb,
    occurred_at TIMESTAMPTZ NOT NULL
);

CREATE INDEX idx_audit_logs_occurred_at ON audit_logs (occurred_at DESC);
CREATE INDEX idx_audit_logs_organization_id ON audit_logs (organization_id);
CREATE INDEX idx_audit_logs_actor_user_id ON audit_logs (actor_user_id);
CREATE INDEX idx_audit_logs_event_type ON audit_logs (event_type);
CREATE INDEX idx_audit_logs_resource ON audit_logs (resource_type, resource_id);
CREATE INDEX idx_audit_logs_request_id ON audit_logs (request_id);
