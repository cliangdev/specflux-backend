package com.specflux.github.interfaces.rest;

import java.net.URI;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

import com.specflux.api.generated.GitHubApi;
import com.specflux.api.generated.model.GithubInstallationStatusDto;
import com.specflux.github.application.GithubService;
import com.specflux.github.domain.GithubInstallation;
import com.specflux.github.infrastructure.GithubAppConfig;

import lombok.RequiredArgsConstructor;

/**
 * REST controller for GitHub App integration endpoints.
 *
 * <p>Handles OAuth installation flow and repository creation for project cloud sync.
 * Implements the generated GitHubApi interface from OpenAPI specification.
 */
@RestController
@RequiredArgsConstructor
public class GithubController implements GitHubApi {

  private static final Logger log = LoggerFactory.getLogger(GithubController.class);

  private final GithubService githubService;
  private final GithubAppConfig githubAppConfig;

  @Value("${specflux.frontend.url:http://localhost:5173}")
  private String frontendUrl;

  /**
   * {@inheritDoc}
   *
   * <p>Initiates GitHub App installation by redirecting to GitHub OAuth.
   */
  @Override
  public ResponseEntity<Void> initiateGithubInstall() {
    if (!githubAppConfig.isConfigured()) {
      log.error("GitHub App not configured");
      return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
    }

    String authUrl =
        String.format(
            "https://github.com/login/oauth/authorize?client_id=%s&redirect_uri=%s&scope=repo",
            githubAppConfig.getClientId(), githubAppConfig.getRedirectUri());

    log.info("Redirecting to GitHub OAuth: {}", authUrl);
    return ResponseEntity.status(HttpStatus.FOUND).location(URI.create(authUrl)).build();
  }

  /**
   * {@inheritDoc}
   *
   * <p>Exchanges the authorization code for tokens and saves the installation.
   */
  @Override
  public ResponseEntity<Void> handleGithubCallback(
      String code, Integer installationId, String setupAction, String state) {

    try {
      log.info(
          "Handling GitHub OAuth callback: installationId={}, setupAction={}",
          installationId,
          setupAction);

      // Exchange code for tokens and create/update installation
      GithubInstallation installation = githubService.exchangeCodeForTokens(code);

      log.info("GitHub installation successful for user: {}", installation.getGithubUsername());

      // Redirect to frontend with success
      String redirectUrl = frontendUrl + "/settings/integrations?github=success";
      return ResponseEntity.status(HttpStatus.FOUND).location(URI.create(redirectUrl)).build();

    } catch (Exception e) {
      log.error("GitHub OAuth callback failed", e);

      // Redirect to frontend with error
      String redirectUrl = frontendUrl + "/settings/integrations?github=error";
      return ResponseEntity.status(HttpStatus.FOUND).location(URI.create(redirectUrl)).build();
    }
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
