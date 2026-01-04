package com.specflux.github.infrastructure;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

import jakarta.annotation.PostConstruct;
import lombok.Getter;

/**
 * Configuration for GitHub App integration.
 *
 * <p>Loads GitHub App credentials from environment variables for OAuth flow and API access.
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

  /**
   * Validates the GitHub App configuration on startup.
   *
   * @throws IllegalStateException if required configuration is missing
   */
  @PostConstruct
  public void validate() {
    if (appId == null || appId.isBlank()) {
      log.warn(
          "GitHub App ID not configured. Set GITHUB_APP_ID environment variable to enable GitHub"
              + " integration.");
      return;
    }

    if (clientId == null || clientId.isBlank()) {
      throw new IllegalStateException(
          "GitHub Client ID not configured. Set GITHUB_CLIENT_ID environment variable.");
    }

    if (clientSecret == null || clientSecret.isBlank()) {
      throw new IllegalStateException(
          "GitHub Client Secret not configured. Set GITHUB_CLIENT_SECRET environment variable.");
    }

    if (redirectUri == null || redirectUri.isBlank()) {
      throw new IllegalStateException(
          "GitHub Redirect URI not configured. Set GITHUB_REDIRECT_URI environment variable.");
    }

    log.info("GitHub App configured: App ID={}, Redirect URI={}", appId, redirectUri);
  }

  /** Returns true if GitHub integration is properly configured. */
  public boolean isConfigured() {
    return appId != null
        && !appId.isBlank()
        && clientId != null
        && !clientId.isBlank()
        && clientSecret != null
        && !clientSecret.isBlank()
        && redirectUri != null
        && !redirectUri.isBlank();
  }
}
