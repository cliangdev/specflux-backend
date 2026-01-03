-- GitHub installations table for OAuth tokens
-- Stores GitHub App installation data per user
CREATE TABLE github_installations (
    id BIGSERIAL PRIMARY KEY,
    public_id VARCHAR(24) NOT NULL UNIQUE,
    user_id BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    installation_id BIGINT NOT NULL,
    access_token VARCHAR(500) NOT NULL,
    access_token_expires_at TIMESTAMP WITH TIME ZONE NOT NULL,
    refresh_token VARCHAR(500) NOT NULL,
    refresh_token_expires_at TIMESTAMP WITH TIME ZONE NOT NULL,
    github_username VARCHAR(255) NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- Each user can only have one GitHub installation
CREATE UNIQUE INDEX uq_github_installations_user_id ON github_installations(user_id);

-- Index for installation ID lookup
CREATE INDEX idx_github_installations_installation_id ON github_installations(installation_id);

-- Index for token expiry checks
CREATE INDEX idx_github_installations_access_token_expires ON github_installations(access_token_expires_at);

COMMENT ON TABLE github_installations IS 'Stores GitHub App installation OAuth tokens per user';
COMMENT ON COLUMN github_installations.installation_id IS 'GitHub installation ID from OAuth flow';
COMMENT ON COLUMN github_installations.access_token IS 'OAuth access token (encrypted at rest in production)';
COMMENT ON COLUMN github_installations.refresh_token IS 'OAuth refresh token (encrypted at rest in production)';
COMMENT ON COLUMN github_installations.github_username IS 'GitHub username of the authenticated user';
