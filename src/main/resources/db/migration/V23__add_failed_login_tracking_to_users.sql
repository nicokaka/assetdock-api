ALTER TABLE users
    ADD COLUMN failed_login_attempts INTEGER NOT NULL DEFAULT 0;

CREATE INDEX idx_users_failed_login_attempts ON users (failed_login_attempts);
