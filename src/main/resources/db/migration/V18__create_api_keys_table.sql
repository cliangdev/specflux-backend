-- API keys table for secure programmatic access
-- Keys are stored as SHA-256 hashes for security
CREATE TABLE api_keys (
    id BIGSERIAL PRIMARY KEY,
    public_id VARCHAR(24) NOT NULL UNIQUE,
    user_id BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    key_prefix VARCHAR(16) NOT NULL UNIQUE,
    key_hash VARCHAR(64) NOT NULL,
    name VARCHAR(100),
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    expires_at TIMESTAMP WITH TIME ZONE,
    last_used_at TIMESTAMP WITH TIME ZONE,
    revoked_at TIMESTAMP WITH TIME ZONE
);

-- MVP constraint: only one ACTIVE (non-revoked) key per user
-- This allows multiple revoked keys in history but prevents creating new keys while one is active
CREATE UNIQUE INDEX uq_api_keys_user_active ON api_keys(user_id) WHERE revoked_at IS NULL;

-- Index for fast prefix-based lookup during authentication
CREATE INDEX idx_api_keys_prefix ON api_keys(key_prefix);

-- Index for user lookups
CREATE INDEX idx_api_keys_user_id ON api_keys(user_id);

COMMENT ON TABLE api_keys IS 'Stores API keys for programmatic access. Keys are SHA-256 hashed.';
COMMENT ON COLUMN api_keys.key_prefix IS 'First 12 chars after sfx_ prefix for fast lookup';
COMMENT ON COLUMN api_keys.key_hash IS 'SHA-256 hash of the full API key';
COMMENT ON COLUMN api_keys.expires_at IS 'NULL means never expires';
COMMENT ON COLUMN api_keys.revoked_at IS 'Non-NULL means key has been revoked';
