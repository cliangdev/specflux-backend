package com.specflux.github.interfaces.rest;

import java.net.URI;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Base64;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.specflux.api.generated.GitHubApi;
import com.specflux.api.generated.model.CreateGithubRepoRequestDto;
import com.specflux.api.generated.model.GithubInstallResponseDto;
import com.specflux.api.generated.model.GithubInstallationStatusDto;
import com.specflux.api.generated.model.GithubRepoDto;
import com.specflux.api.generated.model.GithubRepoExistsResponseDto;
import com.specflux.api.generated.model.GithubRepoListResponseDto;
import com.specflux.github.application.GithubService;
import com.specflux.github.domain.GithubInstallation;
import com.specflux.github.infrastructure.GithubApiClient.Repository;
import com.specflux.github.infrastructure.GithubApiClient.RepositoryListResponse;
import com.specflux.github.infrastructure.GithubAppConfig;
import com.specflux.shared.application.CurrentUserService;
import com.specflux.user.domain.User;

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
  private final CurrentUserService currentUserService;

  @Value("${specflux.frontend.url:http://localhost:5173}")
  private String frontendUrl;

  /**
   * {@inheritDoc}
   *
   * <p>Returns the GitHub OAuth URL for the client to open in a browser. For desktop apps, accepts
   * a redirect_uri to redirect back to a local server after OAuth completes.
   */
  @Override
  public ResponseEntity<GithubInstallResponseDto> initiateGithubInstall(URI redirectUri) {
    if (!githubAppConfig.isConfigured()) {
      log.error("GitHub App not configured");
      return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
    }

    User currentUser = currentUserService.getOrCreateCurrentUser();
    String userPublicId = currentUser.getPublicId();
    String state = buildOAuthState(redirectUri, userPublicId);

    String authUrl =
        String.format(
            "https://github.com/login/oauth/authorize?client_id=%s&redirect_uri=%s&scope=repo&state=%s",
            githubAppConfig.getClientId(),
            githubAppConfig.getRedirectUri(),
            URLEncoder.encode(state, StandardCharsets.UTF_8));

    log.info("Returning GitHub OAuth URL (desktop redirect: {})", redirectUri != null);
    return ResponseEntity.ok(new GithubInstallResponseDto(URI.create(authUrl)));
  }

  /**
   * Builds OAuth state parameter containing user public ID and optional redirect URI.
   *
   * <p>User public ID is required to associate the GitHub installation with the correct user since
   * the callback endpoint is unauthenticated. For desktop apps, also includes the local server URL.
   */
  private String buildOAuthState(URI redirectUri, String userPublicId) {
    try {
      String redirectUriStr = redirectUri != null ? redirectUri.toString() : null;
      String json = OBJECT_MAPPER.writeValueAsString(new OAuthState(redirectUriStr, userPublicId));
      return Base64.getUrlEncoder()
          .withoutPadding()
          .encodeToString(json.getBytes(StandardCharsets.UTF_8));
    } catch (Exception e) {
      log.warn("Failed to encode OAuth state", e);
      return "";
    }
  }

  /**
   * Parses the OAuth state parameter.
   *
   * @return the parsed state or null if invalid
   */
  private OAuthState parseOAuthState(String state) {
    if (state == null || state.isEmpty()) {
      return null;
    }
    try {
      String json = new String(Base64.getUrlDecoder().decode(state), StandardCharsets.UTF_8);
      return OBJECT_MAPPER.readValue(json, OAuthState.class);
    } catch (Exception e) {
      log.warn("Failed to decode OAuth state", e);
      return null;
    }
  }

  /** OAuth state record for JSON serialization. */
  private record OAuthState(String redirectUri, String userPublicId) {}

  /**
   * {@inheritDoc}
   *
   * <p>Exchanges the authorization code for tokens and saves the installation. For desktop apps,
   * redirects back to the local server URL from the state parameter with token info.
   */
  @Override
  public ResponseEntity<Void> handleGithubCallback(
      String code, Integer installationId, String setupAction, String state) {

    // Parse OAuth state to get user public ID and redirect URI
    OAuthState oauthState = parseOAuthState(state);
    String clientRedirectUri = oauthState != null ? oauthState.redirectUri() : null;
    String userPublicId = oauthState != null ? oauthState.userPublicId() : null;

    if (userPublicId == null || userPublicId.isBlank()) {
      log.error("GitHub OAuth callback missing user public ID in state");
      String redirectUrl = buildCallbackRedirectUrl(clientRedirectUri, null, false);
      return ResponseEntity.status(HttpStatus.FOUND).location(URI.create(redirectUrl)).build();
    }

    try {
      log.info(
          "Handling GitHub OAuth callback: userPublicId={}, installationId={}, setupAction={}, hasClientRedirect={}",
          userPublicId,
          installationId,
          setupAction,
          clientRedirectUri != null);

      GithubInstallation installation = githubService.exchangeCodeForTokens(code, userPublicId);
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

  /**
   * {@inheritDoc}
   *
   * <p>Lists GitHub repositories for the current user.
   */
  @Override
  public ResponseEntity<GithubRepoListResponseDto> listGithubRepos(Integer page, Integer perPage) {
    int pageNum = page != null ? page : 1;
    int perPageNum = perPage != null ? perPage : 30;

    RepositoryListResponse response = githubService.listRepositories(pageNum, perPageNum);

    List<GithubRepoDto> repoDtos = response.getRepos().stream().map(this::toGithubRepoDto).toList();

    GithubRepoListResponseDto dto =
        new GithubRepoListResponseDto(repoDtos, response.getTotalCount(), pageNum, perPageNum);

    return ResponseEntity.ok(dto);
  }

  /**
   * {@inheritDoc}
   *
   * <p>Creates a new GitHub repository for the current user.
   */
  @Override
  public ResponseEntity<GithubRepoDto> createGithubRepo(CreateGithubRepoRequestDto createRequest) {
    String name = createRequest.getName();
    String description = createRequest.getDescription();
    Boolean isPrivate = createRequest.getPrivate();

    Repository repo =
        githubService.createRepository(name, description, isPrivate != null ? isPrivate : true);

    log.info("Created GitHub repository: {}", repo.getFullName());
    return ResponseEntity.status(HttpStatus.CREATED).body(toGithubRepoDto(repo));
  }

  /**
   * {@inheritDoc}
   *
   * <p>Checks if a GitHub repository exists and is accessible.
   */
  @Override
  public ResponseEntity<GithubRepoExistsResponseDto> checkGithubRepoExists(
      String owner, String repo) {
    boolean exists = githubService.repositoryExists(owner, repo);
    log.debug("Repository {}/{} exists: {}", owner, repo, exists);
    return ResponseEntity.ok(new GithubRepoExistsResponseDto(exists));
  }

  /** Converts a GitHub repository to a DTO. */
  private GithubRepoDto toGithubRepoDto(Repository repo) {
    GithubRepoDto dto = new GithubRepoDto();
    dto.setId(repo.getId());
    dto.setName(repo.getName());
    dto.setFullName(repo.getFullName());
    dto.setDescription(repo.getDescription());
    dto.setPrivate(repo.isPrivateRepo());
    dto.setHtmlUrl(URI.create(repo.getHtmlUrl()));
    dto.setCloneUrl(URI.create(repo.getCloneUrl()));
    return dto;
  }
}
