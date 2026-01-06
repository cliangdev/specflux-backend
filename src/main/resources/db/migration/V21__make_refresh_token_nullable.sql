-- Make refresh_token columns nullable for OAuth App support
-- OAuth Apps don't provide refresh tokens (only GitHub Apps do)

ALTER TABLE github_installations
    ALTER COLUMN refresh_token DROP NOT NULL,
    ALTER COLUMN refresh_token_expires_at DROP NOT NULL;

COMMENT ON COLUMN github_installations.refresh_token IS 'OAuth refresh token (null for OAuth App, present for GitHub App)';
