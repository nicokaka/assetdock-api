CREATE TABLE web_sessions (
    id UUID PRIMARY KEY,
    user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    csrf_token VARCHAR(128) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL,
    last_active_at TIMESTAMPTZ NOT NULL,
    expires_at TIMESTAMPTZ NOT NULL,
    invalidated_at TIMESTAMPTZ
);

CREATE INDEX idx_web_sessions_user_id ON web_sessions (user_id);
CREATE INDEX idx_web_sessions_last_active_at ON web_sessions (last_active_at);
