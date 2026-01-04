package com.specflux.github.application;

import java.util.Optional;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;

import com.specflux.github.domain.GithubInstallation;
import com.specflux.github.domain.GithubInstallationRepository;
import com.specflux.github.infrastructure.GithubApiClient;
import com.specflux.github.infrastructure.GithubApiClient.GithubApiException;
import com.specflux.github.infrastructure.GithubApiClient.Repository;
import com.specflux.github.infrastructure.GithubApiClient.TokenResponse;
import com.specflux.github.infrastructure.GithubApiClient.UserProfile;
import com.specflux.shared.application.CurrentUserService;
import com.specflux.user.domain.User;

import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;

/**
 * Application service for GitHub integration operations.
 *
 * <p>Handles OAuth token exchange, token refresh, and repository creation.
 */
@Service
@RequiredArgsConstructor
public class GithubService {

  private static final Logger log = LoggerFactory.getLogger(GithubService.class);
  private static final int TOKEN_REFRESH_THRESHOLD_MINUTES = 5;

  private final GithubApiClient githubApiClient;
  private final GithubInstallationRepository installationRepository;
  private final CurrentUserService currentUserService;
  private final TransactionTemplate transactionTemplate;

  /**
   * Exchanges an OAuth authorization code for tokens and creates/updates the GitHub installation.
   *
   * @param code the authorization code from OAuth callback
   * @return the created or updated GitHub installation
   * @throws GithubApiException if token exchange or user profile fetch fails
   */
  public GithubInstallation exchangeCodeForTokens(String code) {
    return transactionTemplate.execute(
        status -> {
          User currentUser = currentUserService.getOrCreateCurrentUser();

          // Exchange code for tokens
          TokenResponse tokenResponse = githubApiClient.exchangeCodeForTokens(code);

          // Get user profile to extract GitHub username and installation ID
          UserProfile userProfile =
              githubApiClient.getAuthenticatedUser(tokenResponse.getAccessToken());

          // Check if installation already exists for this user
          Optional<GithubInstallation> existingInstallation =
              installationRepository.findByUserId(currentUser.getId());

          GithubInstallation installation;
          if (existingInstallation.isPresent()) {
            // Update existing installation
            installation = existingInstallation.get();
            installation.updateTokens(
                tokenResponse.getAccessToken(),
                tokenResponse.getAccessTokenExpiresAt(),
                tokenResponse.getRefreshToken(),
                tokenResponse.getRefreshTokenExpiresAt());
            installation.setGithubUsername(userProfile.getLogin());
            log.info("Updated GitHub installation for user {}", currentUser.getId());
          } else {
            // Create new installation
            String publicId = generatePublicId("ghi");
            installation =
                new GithubInstallation(
                    publicId,
                    currentUser.getId(),
                    userProfile.getId(),
                    tokenResponse.getAccessToken(),
                    tokenResponse.getAccessTokenExpiresAt(),
                    tokenResponse.getRefreshToken(),
                    tokenResponse.getRefreshTokenExpiresAt(),
                    userProfile.getLogin());
            log.info("Created new GitHub installation for user {}", currentUser.getId());
          }

          return installationRepository.save(installation);
        });
  }

  /**
   * Refreshes the access token if it's expired or about to expire.
   *
   * @param installation the GitHub installation
   * @return the installation with refreshed tokens (if refresh was needed)
   * @throws GithubApiException if token refresh fails
   */
  public GithubInstallation refreshAccessToken(GithubInstallation installation) {
    if (!installation.needsTokenRefresh(TOKEN_REFRESH_THRESHOLD_MINUTES)) {
      log.debug(
          "Access token for installation {} is still valid, skipping refresh",
          installation.getPublicId());
      return installation;
    }

    return transactionTemplate.execute(
        status -> {
          log.info("Refreshing access token for installation {}", installation.getPublicId());
          TokenResponse tokenResponse =
              githubApiClient.refreshAccessToken(installation.getRefreshToken());

          installation.updateTokens(
              tokenResponse.getAccessToken(),
              tokenResponse.getAccessTokenExpiresAt(),
              tokenResponse.getRefreshToken(),
              tokenResponse.getRefreshTokenExpiresAt());

          return installationRepository.save(installation);
        });
  }

  /**
   * Creates a new GitHub repository for the authenticated user.
   *
   * @param installation the GitHub installation (must have valid tokens)
   * @param repoName the repository name
   * @param description the repository description (optional)
   * @param isPrivate whether the repository should be private
   * @return the created repository
   * @throws GithubApiException if repository creation fails
   */
  public Repository createRepository(
      GithubInstallation installation, String repoName, String description, boolean isPrivate) {
    // Refresh token if needed
    GithubInstallation freshInstallation = refreshAccessToken(installation);

    log.info(
        "Creating GitHub repository '{}' for user {}",
        repoName,
        freshInstallation.getGithubUsername());

    return githubApiClient.createRepository(
        freshInstallation.getAccessToken(), repoName, description, isPrivate);
  }

  /**
   * Gets the GitHub installation for the current user.
   *
   * @return the installation
   * @throws EntityNotFoundException if no installation exists for the current user
   */
  public GithubInstallation getInstallation() {
    User currentUser = currentUserService.getCurrentUser();
    return installationRepository
        .findByUserId(currentUser.getId())
        .orElseThrow(
            () ->
                new EntityNotFoundException(
                    "No GitHub installation found for user " + currentUser.getId()));
  }

  /**
   * Checks if the current user has a GitHub installation.
   *
   * @return true if the user has a GitHub installation
   */
  public boolean hasInstallation() {
    User currentUser = currentUserService.getCurrentUser();
    return installationRepository.existsByUserId(currentUser.getId());
  }

  /**
   * Disconnects the GitHub installation for the current user.
   *
   * <p>This deletes the installation record, removing all stored tokens.
   */
  public void disconnect() {
    transactionTemplate.executeWithoutResult(
        status -> {
          User currentUser = currentUserService.getCurrentUser();
          installationRepository.deleteByUserId(currentUser.getId());
          log.info("Disconnected GitHub installation for user {}", currentUser.getId());
        });
  }

  private String generatePublicId(String prefix) {
    return prefix + "_" + UUID.randomUUID().toString().replace("-", "").substring(0, 16);
  }
}
