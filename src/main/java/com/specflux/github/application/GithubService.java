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
import com.specflux.github.infrastructure.GithubApiClient.RepositoryListResponse;
import com.specflux.github.infrastructure.GithubApiClient.TokenResponse;
import com.specflux.github.infrastructure.GithubApiClient.UserProfile;
import com.specflux.shared.application.CurrentUserService;
import com.specflux.user.domain.User;
import com.specflux.user.domain.UserRepository;

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
  private final UserRepository userRepository;
  private final CurrentUserService currentUserService;
  private final TransactionTemplate transactionTemplate;

  /**
   * Exchanges an OAuth authorization code for tokens and creates/updates the GitHub installation.
   *
   * <p>This overload is used when calling from an authenticated context.
   *
   * @param code the authorization code from OAuth callback
   * @return the created or updated GitHub installation
   * @throws GithubApiException if token exchange or user profile fetch fails
   */
  public GithubInstallation exchangeCodeForTokens(String code) {
    User currentUser = currentUserService.getOrCreateCurrentUser();
    return exchangeCodeForTokens(code, currentUser.getPublicId());
  }

  /**
   * Exchanges an OAuth authorization code for tokens and creates/updates the GitHub installation.
   *
   * <p>This overload is used when calling from an unauthenticated context (OAuth callback) where
   * the user public ID is passed via the OAuth state parameter.
   *
   * @param code the authorization code from OAuth callback
   * @param userPublicId the public ID of the user to associate the installation with
   * @return the created or updated GitHub installation
   * @throws GithubApiException if token exchange or user profile fetch fails
   * @throws EntityNotFoundException if no user exists with the given public ID
   */
  public GithubInstallation exchangeCodeForTokens(String code, String userPublicId) {
    User user =
        userRepository
            .findByPublicId(userPublicId)
            .orElseThrow(
                () -> new EntityNotFoundException("User not found for public ID: " + userPublicId));
    Long userId = user.getId();

    // External API calls must be outside transaction to avoid connection pool exhaustion
    TokenResponse tokenResponse = githubApiClient.exchangeCodeForTokens(code);
    UserProfile userProfile = githubApiClient.getAuthenticatedUser(tokenResponse.getAccessToken());

    return transactionTemplate.execute(
        status -> {
          Optional<GithubInstallation> existingInstallation =
              installationRepository.findByUserId(userId);

          GithubInstallation installation;
          if (existingInstallation.isPresent()) {
            installation = existingInstallation.get();
            installation.updateTokens(
                tokenResponse.getAccessToken(),
                tokenResponse.getAccessTokenExpiresAt(),
                tokenResponse.getRefreshToken(),
                tokenResponse.getRefreshTokenExpiresAt());
            installation.setGithubUsername(userProfile.getLogin());
            log.info("Updated GitHub installation for user {}", userId);
          } else {
            String publicId = generatePublicId("ghi");
            installation =
                new GithubInstallation(
                    publicId,
                    userId,
                    userProfile.getId(),
                    tokenResponse.getAccessToken(),
                    tokenResponse.getAccessTokenExpiresAt(),
                    tokenResponse.getRefreshToken(),
                    tokenResponse.getRefreshTokenExpiresAt(),
                    userProfile.getLogin());
            log.info("Created new GitHub installation for user {}", userId);
          }

          return installationRepository.save(installation);
        });
  }

  /**
   * Refreshes the access token if it's expired or about to expire.
   *
   * <p>OAuth App tokens don't have refresh tokens, so we skip refresh for those.
   *
   * @param installation the GitHub installation
   * @return the installation with refreshed tokens (if refresh was needed)
   * @throws GithubApiException if token refresh fails
   */
  public GithubInstallation refreshAccessToken(GithubInstallation installation) {
    // OAuth Apps don't have refresh tokens - their access tokens are long-lived
    if (installation.getRefreshToken() == null) {
      log.debug(
          "Installation {} has no refresh token (OAuth App), skipping refresh",
          installation.getPublicId());
      return installation;
    }

    if (!installation.needsTokenRefresh(TOKEN_REFRESH_THRESHOLD_MINUTES)) {
      log.debug(
          "Access token for installation {} is still valid, skipping refresh",
          installation.getPublicId());
      return installation;
    }

    log.info("Refreshing access token for installation {}", installation.getPublicId());
    TokenResponse tokenResponse =
        githubApiClient.refreshAccessToken(installation.getRefreshToken());

    return transactionTemplate.execute(
        status -> {
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
    GithubInstallation freshInstallation = refreshAccessToken(installation);

    log.info(
        "Creating GitHub repository '{}' for user {}",
        repoName,
        freshInstallation.getGithubUsername());

    return githubApiClient.createRepository(
        freshInstallation.getAccessToken(), repoName, description, isPrivate);
  }

  /**
   * Lists GitHub repositories for the current user.
   *
   * @param page the page number (1-indexed)
   * @param perPage the number of repositories per page (max 100)
   * @return the list of repositories with pagination info
   * @throws GithubApiException if listing fails
   */
  public RepositoryListResponse listRepositories(int page, int perPage) {
    GithubInstallation installation = getInstallation();
    GithubInstallation freshInstallation = refreshAccessToken(installation);

    log.info(
        "Listing GitHub repositories for user {}, page={}, perPage={}",
        freshInstallation.getGithubUsername(),
        page,
        perPage);

    return githubApiClient.listRepositories(freshInstallation.getAccessToken(), page, perPage);
  }

  /**
   * Creates a new GitHub repository for the current user.
   *
   * @param repoName the repository name
   * @param description the repository description (optional)
   * @param isPrivate whether the repository should be private
   * @return the created repository
   * @throws GithubApiException if repository creation fails
   */
  public Repository createRepository(String repoName, String description, boolean isPrivate) {
    GithubInstallation installation = getInstallation();
    return createRepository(installation, repoName, description, isPrivate);
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
