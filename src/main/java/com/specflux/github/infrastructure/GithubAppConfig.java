package com.specflux.github.infrastructure;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

import jakarta.annotation.PostConstruct;
import lombok.Getter;

/**
 * Configuration for GitHub integration.
 *
 * <p>Supports two types of GitHub credentials:
 *
 * <ul>
 *   <li><b>OAuth App</b> (recommended for repo creation): Traditional OAuth with scopes like
 *       `repo`. Use GITHUB_OAUTH_CLIENT_ID and GITHUB_OAUTH_CLIENT_SECRET.
 *   <li><b>GitHub App</b> (for installations): App-based auth with permissions. Use GITHUB_APP_ID,
 *       GITHUB_CLIENT_ID, and GITHUB_CLIENT_SECRET.
 * </ul>
 *
 * <p>If OAuth App credentials are configured, they take precedence for the OAuth flow since they
 * support the `repo` scope needed for repository creation.
 */
@Configuration
@Getter
public class GithubAppConfig {

  private static final Logger log = LoggerFactory.getLogger(GithubAppConfig.class);

  @Value("${github.app.id:}")
  private String appId;

  @Value("${github.app.client-id:}")
  private String clientId;

  @Value("${github.app.client-secret:}")
  private String clientSecret;

  @Value("${github.app.redirect-uri:}")
  private String redirectUri;

  @Value("${github.oauth.client-id:}")
  private String oauthClientId;

  @Value("${github.oauth.client-secret:}")
  private String oauthClientSecret;

  @PostConstruct
  public void validate() {
    boolean hasOAuthApp = isOAuthAppConfigured();
    boolean hasGitHubApp = isGitHubAppConfigured();

    if (!hasOAuthApp && !hasGitHubApp) {
      log.warn(
          "GitHub not configured. Set GITHUB_OAUTH_CLIENT_ID/SECRET for OAuth App, "
              + "or GITHUB_APP_ID/CLIENT_ID/SECRET for GitHub App integration.");
      return;
    }

    if (redirectUri == null || redirectUri.isBlank()) {
      throw new IllegalStateException(
          "GitHub Redirect URI not configured. Set GITHUB_REDIRECT_URI environment variable.");
    }

    if (hasOAuthApp) {
      log.info(
          "GitHub OAuth App configured (supports repo creation). Redirect URI={}", redirectUri);
    } else {
      log.info("GitHub App configured: App ID={}. Redirect URI={}", appId, redirectUri);
      log.warn(
          "GitHub App tokens cannot create user repositories. "
              + "Configure GITHUB_OAUTH_CLIENT_ID/SECRET to enable repo creation.");
    }
  }

  /** Returns true if OAuth App credentials are configured (preferred for repo creation). */
  public boolean isOAuthAppConfigured() {
    return oauthClientId != null
        && !oauthClientId.isBlank()
        && oauthClientSecret != null
        && !oauthClientSecret.isBlank();
  }

  /** Returns true if GitHub App credentials are configured. */
  public boolean isGitHubAppConfigured() {
    return appId != null
        && !appId.isBlank()
        && clientId != null
        && !clientId.isBlank()
        && clientSecret != null
        && !clientSecret.isBlank();
  }

  /** Returns true if any GitHub integration is properly configured. */
  public boolean isConfigured() {
    return (isOAuthAppConfigured() || isGitHubAppConfigured())
        && redirectUri != null
        && !redirectUri.isBlank();
  }

  /** Returns the client ID to use for OAuth flow (prefers OAuth App if configured). */
  public String getEffectiveClientId() {
    return isOAuthAppConfigured() ? oauthClientId : clientId;
  }

  /** Returns the client secret to use for OAuth flow (prefers OAuth App if configured). */
  public String getEffectiveClientSecret() {
    return isOAuthAppConfigured() ? oauthClientSecret : clientSecret;
  }
}
