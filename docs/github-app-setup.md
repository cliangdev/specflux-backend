# GitHub App Setup for SpecFlux

This document provides instructions for registering and configuring the GitHub App required for SpecFlux's Project Cloud Sync feature.

## Overview

SpecFlux uses a GitHub App to create and manage specification repositories on behalf of users. The app requires minimal permissions and uses OAuth for user authorization.

## Prerequisites

- GitHub organization or personal account with permission to register apps
- Access to SpecFlux backend configuration

## Step 1: Register GitHub App

1. Navigate to GitHub App registration:
   - **Organization:** `https://github.com/organizations/{YOUR_ORG}/settings/apps/new`
   - **Personal:** `https://github.com/settings/apps/new`

2. Fill in the following details:

### Basic Information

| Field | Value |
|-------|-------|
| **GitHub App name** | `SpecFlux Sync` |
| **Homepage URL** | `https://specflux.ai` (or your deployment URL) |
| **Description** | `Automated specification repository management for SpecFlux projects` |

### Identifying and authorizing users

| Field | Value |
|-------|-------|
| **Callback URL** | `https://api.specflux.ai/api/github/callback` (replace with your backend URL) |
| **Setup URL (optional)** | Leave blank |
| **Request user authorization (OAuth) during installation** | ✅ Checked |
| **Enable Device Flow** | ❌ Unchecked |
| **Expire user authorization tokens** | ✅ Checked |
| **Refresh user authorization tokens** | ✅ Checked |

### Post installation

| Field | Value |
|-------|-------|
| **Setup URL (optional)** | Leave blank |

### Webhook

| Field | Value |
|-------|-------|
| **Active** | ❌ Unchecked (webhooks not needed for MVP) |

### Permissions

Set the following **Repository permissions**:

| Permission | Access Level | Reason |
|------------|-------------|---------|
| **Contents** | Read and write | Create repos and manage spec files |

**Note:** All other permissions should remain at "No access"

### Where can this GitHub App be installed?

- ✅ Select: **Any account** (allows users to install on personal or organization accounts)

3. Click **Create GitHub App**

## Step 2: Generate Client Secret

1. After creating the app, scroll to **Client secrets**
2. Click **Generate a new client secret**
3. **Copy the secret immediately** - it will only be shown once
4. Save it securely for the next step

## Step 3: Note App Credentials

After registration, you'll see the app details page. Note the following:

| Credential | Location | Used For |
|------------|----------|----------|
| **App ID** | Top of the page | Identifying the app |
| **Client ID** | Under "About" section | OAuth flow |
| **Client Secret** | Generated in previous step | OAuth flow |

## Step 4: Configure SpecFlux Backend

Add the following environment variables to your backend configuration:

### Development (.env file)

```bash
# GitHub App Configuration
GITHUB_APP_ID=123456
GITHUB_CLIENT_ID=Iv1.abc123def456
GITHUB_CLIENT_SECRET=your_client_secret_here
GITHUB_REDIRECT_URI=http://localhost:8090/api/github/callback
```

### Production (Kubernetes/Cloud)

```yaml
# Add to your deployment configuration
env:
  - name: GITHUB_APP_ID
    value: "123456"
  - name: GITHUB_CLIENT_ID
    value: "Iv1.abc123def456"
  - name: GITHUB_CLIENT_SECRET
    valueFrom:
      secretKeyRef:
        name: github-app-secrets
        key: client-secret
  - name: GITHUB_REDIRECT_URI
    value: "https://api.specflux.ai/api/github/callback"
```

### Environment Variables Reference

| Variable | Description | Example |
|----------|-------------|---------|
| `GITHUB_APP_ID` | Numeric ID of the GitHub App | `123456` |
| `GITHUB_CLIENT_ID` | OAuth client ID starting with `Iv1.` | `Iv1.abc123def456` |
| `GITHUB_CLIENT_SECRET` | OAuth client secret (keep secure!) | `ghp_abc123...` |
| `GITHUB_REDIRECT_URI` | OAuth callback URL (must match registration) | `http://localhost:8090/api/github/callback` |

## Step 5: Update .env.example

Update the `.env.example` file in the repository to include placeholders:

```bash
# =============================================================================
# GitHub App Integration
# =============================================================================

# GitHub App ID (numeric)
GITHUB_APP_ID=123456

# GitHub OAuth Client ID (starts with Iv1.)
GITHUB_CLIENT_ID=Iv1.abc123def456

# GitHub OAuth Client Secret (keep secure!)
GITHUB_CLIENT_SECRET=your_client_secret_here

# OAuth callback URL (must match GitHub App registration)
GITHUB_REDIRECT_URI=http://localhost:8090/api/github/callback
```

## Verification

After configuration, verify the setup:

1. Start the SpecFlux backend
2. Check logs for GitHub configuration loading:
   ```
   INFO  c.s.github.GithubConfig - GitHub App configured: App ID=123456
   ```
3. Test the installation flow:
   ```bash
   # Should redirect to GitHub
   curl -I http://localhost:8090/api/github/install
   ```

## Security Notes

- **Never commit** `GITHUB_CLIENT_SECRET` to version control
- Use environment variables or secret management systems
- Rotate client secrets periodically
- Monitor GitHub App installation events

## Troubleshooting

### OAuth callback URL mismatch
- Error: `redirect_uri_mismatch`
- Solution: Ensure `GITHUB_REDIRECT_URI` exactly matches the Callback URL in GitHub App settings

### Invalid client credentials
- Error: `bad_verification_code` or `invalid_client`
- Solution: Verify `GITHUB_CLIENT_ID` and `GITHUB_CLIENT_SECRET` are correct

### App not found
- Error: `404 Not Found` when accessing app
- Solution: Verify `GITHUB_APP_ID` is correct and app is active

## References

- [GitHub Apps Documentation](https://docs.github.com/en/apps)
- [OAuth Apps vs GitHub Apps](https://docs.github.com/en/apps/oauth-apps/building-oauth-apps/differences-between-github-apps-and-oauth-apps)
- [GitHub API - Create a repository](https://docs.github.com/en/rest/repos/repos#create-a-repository-for-the-authenticated-user)
