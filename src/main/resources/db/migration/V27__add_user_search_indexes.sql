CREATE INDEX idx_users_full_name_trgm ON users USING gin(full_name gin_trgm_ops);
CREATE INDEX idx_users_email_trgm ON users USING gin(email gin_trgm_ops);
