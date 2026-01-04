package com.specflux.github.interfaces.rest;

import java.net.URI;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Base64;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.specflux.api.generated.GitHubApi;
import com.specflux.api.generated.model.GithubInstallationStatusDto;
import com.specflux.github.application.GithubService;
import com.specflux.github.domain.GithubInstallation;
import com.specflux.github.infrastructure.GithubAppConfig;

import lombok.RequiredArgsConstructor;

/**
 * REST controller for GitHub App integration endpoints.
 *
 * <p>Handles OAuth installation flow and repository creation for project cloud sync. Implements the
 * generated GitHubApi interface from OpenAPI specification.
 */
@RestController
@RequiredArgsConstructor
public class GithubController implements GitHubApi {

  private static final Logger log = LoggerFactory.getLogger(GithubController.class);
  private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

  private final GithubService githubService;
  private final GithubAppConfig githubAppConfig;

  @Value("${specflux.frontend.url:http://localhost:5173}")
  private String frontendUrl;

  /**
   * {@inheritDoc}
   *
   * <p>Initiates GitHub App installation by redirecting to GitHub OAuth. For desktop apps, accepts
   * a redirect_uri to redirect back to a local server after OAuth completes.
   */
  @Override
  public ResponseEntity<Void> initiateGithubInstall(URI redirectUri) {
    if (!githubAppConfig.isConfigured()) {
      log.error("GitHub App not configured");
      return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
    }

    // Build state parameter with redirect_uri for desktop app callback
    String state = buildOAuthState(redirectUri);

    String authUrl =
        String.format(
            "https://github.com/login/oauth/authorize?client_id=%s&redirect_uri=%s&scope=repo&state=%s",
            githubAppConfig.getClientId(),
            githubAppConfig.getRedirectUri(),
            URLEncoder.encode(state, StandardCharsets.UTF_8));

    log.info("Redirecting to GitHub OAuth (desktop redirect: {})", redirectUri != null);
    return ResponseEntity.status(HttpStatus.FOUND).location(URI.create(authUrl)).build();
  }

  /**
   * Builds OAuth state parameter containing the client's redirect URI.
   *
   * <p>For desktop apps, this preserves the local server URL to redirect to after OAuth.
   */
  private String buildOAuthState(URI redirectUri) {
    try {
      if (redirectUri != null) {
        String json = OBJECT_MAPPER.writeValueAsString(new OAuthState(redirectUri.toString()));
        return Base64.getUrlEncoder()
            .withoutPadding()
            .encodeToString(json.getBytes(StandardCharsets.UTF_8));
      }
    } catch (Exception e) {
      log.warn("Failed to encode OAuth state", e);
    }
    return "";
  }

  /**
   * Extracts redirect URI from OAuth state parameter.
   *
   * @return the redirect URI or null if not present/invalid
   */
  private String extractRedirectUri(String state) {
    if (state == null || state.isEmpty()) {
      return null;
    }
    try {
      String json = new String(Base64.getUrlDecoder().decode(state), StandardCharsets.UTF_8);
      OAuthState oauthState = OBJECT_MAPPER.readValue(json, OAuthState.class);
      return oauthState.redirectUri();
    } catch (Exception e) {
      log.warn("Failed to decode OAuth state", e);
      return null;
    }
  }

  /** OAuth state record for JSON serialization. */
  private record OAuthState(String redirectUri) {}

  /**
   * {@inheritDoc}
   *
   * <p>Exchanges the authorization code for tokens and saves the installation. For desktop apps,
   * redirects back to the local server URL from the state parameter with token info.
   */
  @Override
  public ResponseEntity<Void> handleGithubCallback(
      String code, Integer installationId, String setupAction, String state) {

    // Extract desktop app's redirect URI from state (if present)
    String clientRedirectUri = extractRedirectUri(state);

    try {
      log.info(
          "Handling GitHub OAuth callback: installationId={}, setupAction={}, hasClientRedirect={}",
          installationId,
          setupAction,
          clientRedirectUri != null);

      GithubInstallation installation = githubService.exchangeCodeForTokens(code);
      log.info("GitHub installation successful for user: {}", installation.getGithubUsername());

      String redirectUrl = buildCallbackRedirectUrl(clientRedirectUri, installation, true);
      return ResponseEntity.status(HttpStatus.FOUND).location(URI.create(redirectUrl)).build();

    } catch (Exception e) {
      log.error("GitHub OAuth callback failed", e);
      String redirectUrl = buildCallbackRedirectUrl(clientRedirectUri, null, false);
      return ResponseEntity.status(HttpStatus.FOUND).location(URI.create(redirectUrl)).build();
    }
  }

  /**
   * Builds the callback redirect URL for success or error cases.
   *
   * <p>For desktop apps (clientRedirectUri provided): redirects to local server with token info.
   * For web apps: redirects to configured frontend URL with status query param.
   */
  private String buildCallbackRedirectUrl(
      String clientRedirectUri, GithubInstallation installation, boolean success) {

    if (clientRedirectUri != null) {
      // Desktop app: redirect to local server with token/error info
      StringBuilder url = new StringBuilder(clientRedirectUri);
      url.append(clientRedirectUri.contains("?") ? "&" : "?");

      if (success && installation != null) {
        url.append("github=success");
        url.append("&username=")
            .append(
                URLEncoder.encode(
                    installation.getGithubUsername() != null
                        ? installation.getGithubUsername()
                        : "",
                    StandardCharsets.UTF_8));
      } else {
        url.append("github=error");
      }
      return url.toString();
    }

    // Web app: redirect to frontend settings page
    return frontendUrl + "/settings/integrations?github=" + (success ? "success" : "error");
  }

  /**
   * {@inheritDoc}
   *
   * <p>Gets the GitHub installation status for the current user.
   */
  @Override
  public ResponseEntity<GithubInstallationStatusDto> getGithubInstallationStatus() {
    boolean hasInstallation = githubService.hasInstallation();

    GithubInstallationStatusDto status = new GithubInstallationStatusDto(hasInstallation);

    if (hasInstallation) {
      try {
        GithubInstallation installation = githubService.getInstallation();
        status.setGithubUsername(installation.getGithubUsername());
        if (installation.getCreatedAt() != null) {
          status.setConnectedAt(
              OffsetDateTime.ofInstant(installation.getCreatedAt(), ZoneOffset.UTC));
        }
      } catch (Exception e) {
        log.warn("Failed to fetch installation details", e);
      }
    }

    return ResponseEntity.ok(status);
  }

  /**
   * {@inheritDoc}
   *
   * <p>Disconnects the GitHub installation for the current user.
   */
  @Override
  public ResponseEntity<Void> disconnectGithubInstallation() {
    githubService.disconnect();
    log.info("GitHub installation disconnected");
    return ResponseEntity.noContent().build();
  }
}
