package com.specflux.github.infrastructure;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

import jakarta.annotation.PostConstruct;
import lombok.Getter;

/**
 * Configuration for GitHub integration supporting both OAuth App and GitHub App credentials.
 *
 * <h2>Credential Types</h2>
 *
 * <ul>
 *   <li><b>OAuth App</b> (for repo creation): Traditional OAuth with scopes like {@code repo}.
 *       Required for creating user repositories via {@code POST /user/repos}.
 *   <li><b>GitHub App</b> (for installations): Fine-grained permissions. Cannot create user repos.
 * </ul>
 *
 * <h2>Environment Variables</h2>
 *
 * <pre>
 * # OAuth App (recommended - enables repo creation)
 * GITHUB_OAUTH_CLIENT_ID=Ov23li...
 * GITHUB_OAUTH_CLIENT_SECRET=...
 *
 * # GitHub App (alternative - cannot create user repos)
 * GITHUB_APP_ID=123456
 * GITHUB_APP_CLIENT_ID=Iv1.abc...
 * GITHUB_APP_CLIENT_SECRET=...
 *
 * # Shared
 * GITHUB_REDIRECT_URI=https://api.specflux.dev/api/github/callback
 * </pre>
 *
 * <p>If OAuth App credentials are configured, they are used for the OAuth flow. This enables
 * repository creation which GitHub App tokens cannot do.
 */
@Configuration
@Getter
public class GithubAppConfig {

  private static final Logger log = LoggerFactory.getLogger(GithubAppConfig.class);

  // ==================== OAuth App Credentials ====================
  // Use these for repo creation (POST /user/repos requires `repo` scope)

  @Value("${github.oauth.client-id:}")
  private String oauthClientId;

  @Value("${github.oauth.client-secret:}")
  private String oauthClientSecret;

  // ==================== GitHub App Credentials ====================
  // Use these if OAuth App is not configured (cannot create user repos)

  @Value("${github.app.id:}")
  private String appId;

  @Value("${github.app.client-id:}")
  private String appClientId;

  @Value("${github.app.client-secret:}")
  private String appClientSecret;

  // ==================== Shared Configuration ====================

  @Value("${github.redirect-uri:}")
  private String redirectUri;

  @PostConstruct
  public void validate() {
    boolean hasOAuth = isOAuthConfigured();
    boolean hasApp = isAppConfigured();

    if (!hasOAuth && !hasApp) {
      log.warn(
          "GitHub not configured. Set GITHUB_OAUTH_CLIENT_ID/SECRET (recommended) "
              + "or GITHUB_APP_* variables.");
      return;
    }

    if (redirectUri == null || redirectUri.isBlank()) {
      throw new IllegalStateException(
          "GitHub Redirect URI not configured. Set GITHUB_REDIRECT_URI environment variable.");
    }

    if (hasOAuth) {
      log.info(
          "GitHub OAuth App configured (supports repo creation). Redirect URI={}", redirectUri);
    } else {
      log.info("GitHub App configured (App ID={}). Redirect URI={}", appId, redirectUri);
      log.warn(
          "GitHub App tokens cannot create user repositories. "
              + "Configure GITHUB_OAUTH_CLIENT_ID/SECRET to enable 'Create New' repository.");
    }
  }

  /** Returns true if OAuth App credentials are configured (preferred for repo operations). */
  public boolean isOAuthConfigured() {
    return oauthClientId != null
        && !oauthClientId.isBlank()
        && oauthClientSecret != null
        && !oauthClientSecret.isBlank();
  }

  /** Returns true if GitHub App credentials are configured. */
  public boolean isAppConfigured() {
    return appId != null
        && !appId.isBlank()
        && appClientId != null
        && !appClientId.isBlank()
        && appClientSecret != null
        && !appClientSecret.isBlank();
  }

  /** Returns true if any GitHub credentials are configured. */
  public boolean isConfigured() {
    return (isOAuthConfigured() || isAppConfigured())
        && redirectUri != null
        && !redirectUri.isBlank();
  }

  /**
   * Returns the client ID for OAuth flow. Prefers OAuth App if configured.
   *
   * @return OAuth App client ID if configured, otherwise GitHub App client ID
   */
  public String getClientId() {
    return isOAuthConfigured() ? oauthClientId : appClientId;
  }

  /**
   * Returns the client secret for OAuth flow. Prefers OAuth App if configured.
   *
   * @return OAuth App client secret if configured, otherwise GitHub App client secret
   */
  public String getClientSecret() {
    return isOAuthConfigured() ? oauthClientSecret : appClientSecret;
  }
}
