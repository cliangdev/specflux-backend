package com.specflux.github.infrastructure;

import java.time.Instant;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.Data;
import lombok.RequiredArgsConstructor;

/**
 * REST client for GitHub API calls.
 *
 * <p>Handles OAuth token exchange, token refresh, and repository creation.
 */
@Component
@RequiredArgsConstructor
public class GithubApiClient {

  private static final Logger log = LoggerFactory.getLogger(GithubApiClient.class);
  private static final String GITHUB_API_BASE = "https://api.github.com";
  private static final String GITHUB_OAUTH_BASE = "https://github.com/login/oauth";

  private final GithubAppConfig config;
  private final RestTemplate restTemplate = new RestTemplate();

  /**
   * Exchanges an OAuth authorization code for access and refresh tokens.
   *
   * @param code the authorization code from OAuth callback
   * @return the token response containing access token, refresh token, and expiry
   * @throws GithubApiException if the exchange fails
   */
  public TokenResponse exchangeCodeForTokens(String code) {
    String url = GITHUB_OAUTH_BASE + "/access_token";

    HttpHeaders headers = new HttpHeaders();
    headers.setContentType(MediaType.APPLICATION_JSON);
    headers.set("Accept", "application/json");

    Map<String, String> requestBody =
        Map.of(
            "client_id", config.getClientId(),
            "client_secret", config.getClientSecret(),
            "code", code);

    HttpEntity<Map<String, String>> request = new HttpEntity<>(requestBody, headers);

    try {
      String credType = config.isOAuthConfigured() ? "OAuth App" : "GitHub App";
      log.debug("Exchanging OAuth code for tokens (using {})", credType);
      ResponseEntity<TokenResponse> response =
          restTemplate.postForEntity(url, request, TokenResponse.class);

      if (!response.getStatusCode().is2xxSuccessful() || response.getBody() == null) {
        throw new GithubApiException("Failed to exchange code for tokens");
      }

      TokenResponse tokenResponse = response.getBody();
      log.info(
          "OAuth token exchange successful - type: {}, scope: [{}], expires_in: {}s",
          credType,
          tokenResponse.getScope() != null ? tokenResponse.getScope() : "none",
          tokenResponse.getExpiresIn());
      return tokenResponse;
    } catch (HttpClientErrorException e) {
      log.error("GitHub OAuth token exchange failed: {}", e.getMessage());
      throw new GithubApiException("Failed to exchange OAuth code: " + e.getMessage(), e);
    }
  }

  /**
   * Refreshes an expired access token using the refresh token.
   *
   * @param refreshToken the refresh token
   * @return the new token response
   * @throws GithubApiException if the refresh fails
   */
  public TokenResponse refreshAccessToken(String refreshToken) {
    String url = GITHUB_OAUTH_BASE + "/access_token";

    HttpHeaders headers = new HttpHeaders();
    headers.setContentType(MediaType.APPLICATION_JSON);
    headers.set("Accept", "application/json");

    Map<String, String> requestBody =
        Map.of(
            "client_id",
            config.getClientId(),
            "client_secret",
            config.getClientSecret(),
            "grant_type",
            "refresh_token",
            "refresh_token",
            refreshToken);

    HttpEntity<Map<String, String>> request = new HttpEntity<>(requestBody, headers);

    try {
      log.debug("Refreshing GitHub access token");
      ResponseEntity<TokenResponse> response =
          restTemplate.postForEntity(url, request, TokenResponse.class);

      if (!response.getStatusCode().is2xxSuccessful() || response.getBody() == null) {
        throw new GithubApiException("Failed to refresh access token");
      }

      log.info("Successfully refreshed GitHub access token");
      return response.getBody();
    } catch (HttpClientErrorException e) {
      log.error("GitHub token refresh failed: {}", e.getMessage());
      throw new GithubApiException("Failed to refresh access token: " + e.getMessage(), e);
    }
  }

  /**
   * Gets the authenticated user's GitHub profile.
   *
   * @param accessToken the access token
   * @return the user profile
   * @throws GithubApiException if the request fails
   */
  public UserProfile getAuthenticatedUser(String accessToken) {
    String url = GITHUB_API_BASE + "/user";

    HttpHeaders headers = new HttpHeaders();
    headers.set("Authorization", "Bearer " + accessToken);
    headers.set("Accept", "application/vnd.github+json");

    HttpEntity<?> request = new HttpEntity<>(headers);

    try {
      log.debug("Fetching authenticated GitHub user");
      ResponseEntity<UserProfile> response =
          restTemplate.exchange(url, HttpMethod.GET, request, UserProfile.class);

      if (!response.getStatusCode().is2xxSuccessful() || response.getBody() == null) {
        throw new GithubApiException("Failed to get user profile");
      }

      log.debug("Successfully fetched GitHub user: {}", response.getBody().getLogin());
      return response.getBody();
    } catch (HttpClientErrorException e) {
      log.error("GitHub user profile request failed: {}", e.getMessage());
      throw new GithubApiException("Failed to get user profile: " + e.getMessage(), e);
    }
  }

  /**
   * Creates a new repository for the authenticated user.
   *
   * @param accessToken the access token
   * @param name the repository name
   * @param description the repository description (optional)
   * @param isPrivate whether the repository should be private
   * @return the created repository
   * @throws GithubApiException if the creation fails
   */
  public Repository createRepository(
      String accessToken, String name, String description, boolean isPrivate) {
    String url = GITHUB_API_BASE + "/user/repos";

    HttpHeaders headers = new HttpHeaders();
    headers.set("Authorization", "Bearer " + accessToken);
    headers.set("Accept", "application/vnd.github+json");
    headers.setContentType(MediaType.APPLICATION_JSON);

    CreateRepoRequest requestBody = new CreateRepoRequest();
    requestBody.setName(name);
    requestBody.setDescription(description);
    requestBody.setPrivateRepo(isPrivate);
    requestBody.setAutoInit(true);

    HttpEntity<CreateRepoRequest> request = new HttpEntity<>(requestBody, headers);

    try {
      log.debug("Creating GitHub repository: {}", name);
      ResponseEntity<Repository> response =
          restTemplate.postForEntity(url, request, Repository.class);

      if (!response.getStatusCode().is2xxSuccessful() || response.getBody() == null) {
        throw new GithubApiException("Failed to create repository");
      }

      log.info("Successfully created GitHub repository: {}", name);
      return response.getBody();
    } catch (HttpClientErrorException e) {
      if (e.getStatusCode() == HttpStatus.FORBIDDEN) {
        log.error(
            "GitHub repository creation forbidden - credential_type: {}, response: {}",
            config.isOAuthConfigured() ? "OAuth App" : "GitHub App",
            e.getResponseBodyAsString());
        throw new GithubApiException("No permission to create repository", e);
      }
      if (e.getStatusCode() == HttpStatus.UNPROCESSABLE_ENTITY) {
        log.error(
            "GitHub repository creation failed - name: {}, response: {}",
            name,
            e.getResponseBodyAsString());
        throw new GithubApiException("Repository name already exists or is invalid: " + name, e);
      }
      log.error("GitHub repository creation failed: {}", e.getMessage());
      throw new GithubApiException("Failed to create repository: " + e.getMessage(), e);
    }
  }

  /**
   * Lists repositories for the authenticated user.
   *
   * @param accessToken the access token
   * @param page the page number (1-indexed)
   * @param perPage the number of repositories per page (max 100)
   * @return the list of repositories
   * @throws GithubApiException if the request fails
   */
  public RepositoryListResponse listRepositories(String accessToken, int page, int perPage) {
    String url =
        UriComponentsBuilder.fromUriString(GITHUB_API_BASE + "/user/repos")
            .queryParam("visibility", "all")
            .queryParam("affiliation", "owner")
            .queryParam("sort", "updated")
            .queryParam("direction", "desc")
            .queryParam("page", page)
            .queryParam("per_page", perPage)
            .build()
            .toUriString();

    HttpHeaders headers = new HttpHeaders();
    headers.set("Authorization", "Bearer " + accessToken);
    headers.set("Accept", "application/vnd.github+json");

    HttpEntity<?> request = new HttpEntity<>(headers);

    try {
      log.debug("Listing GitHub repositories for user, page={}, perPage={}", page, perPage);
      ResponseEntity<List<Repository>> response =
          restTemplate.exchange(
              url, HttpMethod.GET, request, new ParameterizedTypeReference<List<Repository>>() {});

      if (!response.getStatusCode().is2xxSuccessful() || response.getBody() == null) {
        throw new GithubApiException("Failed to list repositories");
      }

      List<Repository> repos = response.getBody();
      log.debug("Successfully fetched {} repositories", repos.size());

      // Parse Link header for total count if available
      String linkHeader = response.getHeaders().getFirst("Link");
      int totalCount = estimateTotalCount(repos.size(), page, perPage, linkHeader);

      return new RepositoryListResponse(repos, totalCount, page, perPage);
    } catch (HttpClientErrorException e) {
      log.error("GitHub repository list failed: {}", e.getMessage());
      throw new GithubApiException("Failed to list repositories: " + e.getMessage(), e);
    }
  }

  /**
   * Checks if a GitHub repository exists and is accessible.
   *
   * @param accessToken the access token
   * @param owner the repository owner (username or organization)
   * @param repo the repository name
   * @return true if the repository exists and is accessible, false otherwise
   */
  public boolean repositoryExists(String accessToken, String owner, String repo) {
    String url = GITHUB_API_BASE + "/repos/" + owner + "/" + repo;

    HttpHeaders headers = new HttpHeaders();
    headers.set("Authorization", "Bearer " + accessToken);
    headers.set("Accept", "application/vnd.github+json");

    HttpEntity<?> request = new HttpEntity<>(headers);

    try {
      log.debug("Checking if GitHub repository exists: {}/{}", owner, repo);
      ResponseEntity<Void> response =
          restTemplate.exchange(url, HttpMethod.HEAD, request, Void.class);

      return response.getStatusCode().is2xxSuccessful();
    } catch (HttpClientErrorException e) {
      if (e.getStatusCode() == HttpStatus.NOT_FOUND) {
        log.debug("GitHub repository not found: {}/{}", owner, repo);
        return false;
      }
      log.error("GitHub repository check failed: {}", e.getMessage());
      throw new GithubApiException("Failed to check repository: " + e.getMessage(), e);
    }
  }

  /**
   * Deletes a GitHub repository.
   *
   * @param accessToken the access token
   * @param owner the repository owner (username or organization)
   * @param repo the repository name
   * @throws GithubApiException if the deletion fails
   */
  public void deleteRepository(String accessToken, String owner, String repo) {
    String url = GITHUB_API_BASE + "/repos/" + owner + "/" + repo;

    HttpHeaders headers = new HttpHeaders();
    headers.set("Authorization", "Bearer " + accessToken);
    headers.set("Accept", "application/vnd.github+json");

    HttpEntity<?> request = new HttpEntity<>(headers);

    try {
      log.info("Deleting GitHub repository: {}/{}", owner, repo);
      restTemplate.exchange(url, HttpMethod.DELETE, request, Void.class);
      log.info("Successfully deleted GitHub repository: {}/{}", owner, repo);
    } catch (HttpClientErrorException e) {
      log.error("GitHub repository deletion failed: {}", e.getMessage());
      if (e.getStatusCode() == HttpStatus.NOT_FOUND) {
        throw new GithubApiException("Repository not found: " + owner + "/" + repo, e);
      }
      if (e.getStatusCode() == HttpStatus.FORBIDDEN) {
        throw new GithubApiException(
            "No permission to delete repository: " + owner + "/" + repo, e);
      }
      throw new GithubApiException("Failed to delete repository: " + e.getMessage(), e);
    }
  }

  /** Estimates total count based on Link header or current page size. */
  private int estimateTotalCount(int currentSize, int page, int perPage, String linkHeader) {
    // If we got fewer than perPage, this is likely the last page
    if (currentSize < perPage) {
      return (page - 1) * perPage + currentSize;
    }
    // If there's a "last" link, we could parse it, but for simplicity we'll just indicate there's
    // more
    return page * perPage + 1; // At least this many
  }

  /** Response from GitHub OAuth token exchange/refresh. */
  @Data
  @JsonIgnoreProperties(ignoreUnknown = true)
  public static class TokenResponse {
    @JsonProperty("access_token")
    private String accessToken;

    @JsonProperty("expires_in")
    private Long expiresIn;

    @JsonProperty("refresh_token")
    private String refreshToken;

    @JsonProperty("refresh_token_expires_in")
    private Long refreshTokenExpiresIn;

    @JsonProperty("token_type")
    private String tokenType;

    @JsonProperty("scope")
    private String scope;

    /** Calculates the absolute expiry time for the access token. */
    public Instant getAccessTokenExpiresAt() {
      return expiresIn != null
          ? Instant.now().plusSeconds(expiresIn)
          : Instant.now().plusSeconds(28800);
    }

    /** Calculates the absolute expiry time for the refresh token. */
    public Instant getRefreshTokenExpiresAt() {
      return refreshTokenExpiresIn != null
          ? Instant.now().plusSeconds(refreshTokenExpiresIn)
          : Instant.now().plusSeconds(15777000);
    }
  }

  /** GitHub user profile information. */
  @Data
  @JsonIgnoreProperties(ignoreUnknown = true)
  public static class UserProfile {
    private String login;
    private Long id;
    private String email;
    private String name;

    @JsonProperty("avatar_url")
    private String avatarUrl;
  }

  /** Request body for creating a GitHub repository. */
  @Data
  @JsonIgnoreProperties(ignoreUnknown = true)
  public static class CreateRepoRequest {
    private String name;
    private String description;

    @JsonProperty("private")
    private boolean privateRepo;

    @JsonProperty("auto_init")
    private boolean autoInit;
  }

  /** GitHub repository information. */
  @Data
  @JsonIgnoreProperties(ignoreUnknown = true)
  public static class Repository {
    private Long id;
    private String name;

    @JsonProperty("full_name")
    private String fullName;

    private String description;

    @JsonProperty("html_url")
    private String htmlUrl;

    @JsonProperty("clone_url")
    private String cloneUrl;

    @JsonProperty("ssh_url")
    private String sshUrl;

    @JsonProperty("private")
    private boolean privateRepo;
  }

  /** Exception thrown when GitHub API calls fail. */
  public static class GithubApiException extends RuntimeException {
    public GithubApiException(String message) {
      super(message);
    }

    public GithubApiException(String message, Throwable cause) {
      super(message, cause);
    }
  }

  /** Response containing a list of repositories with pagination info. */
  @Data
  public static class RepositoryListResponse {
    private final List<Repository> repos;
    private final int totalCount;
    private final int page;
    private final int perPage;

    public RepositoryListResponse(List<Repository> repos, int totalCount, int page, int perPage) {
      this.repos = repos;
      this.totalCount = totalCount;
      this.page = page;
      this.perPage = perPage;
    }
  }
}
