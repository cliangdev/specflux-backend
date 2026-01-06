package com.specflux.github.interfaces.rest;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Base64;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import com.specflux.common.AbstractControllerIntegrationTest;
import com.specflux.github.application.GithubService;
import com.specflux.github.domain.GithubInstallation;
import com.specflux.github.infrastructure.GithubApiClient.Repository;
import com.specflux.github.infrastructure.GithubApiClient.RepositoryListResponse;
import com.specflux.github.infrastructure.GithubAppConfig;

import jakarta.persistence.EntityNotFoundException;

/**
 * Integration tests for GithubController.
 *
 * <p>Uses schema isolation for parallel test execution.
 */
class GithubControllerTest extends AbstractControllerIntegrationTest {

  @DynamicPropertySource
  static void configureSchema(DynamicPropertyRegistry registry) {
    configureSchemaForClass(registry, GithubControllerTest.class);
  }

  @MockitoBean private GithubService githubService;
  @MockitoBean private GithubAppConfig githubAppConfig;

  // ==================== GET /api/github/status ====================

  @Test
  void getGithubStatus_whenConnected_shouldReturnInstallationDetails() throws Exception {
    when(githubService.hasInstallation()).thenReturn(true);

    GithubInstallation installation = createTestInstallation();
    when(githubService.getInstallation()).thenReturn(installation);

    mockMvc
        .perform(get("/api/github/status").with(user("user")))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.installed").value(true))
        .andExpect(jsonPath("$.githubUsername").value("testuser"))
        .andExpect(jsonPath("$.connectedAt").exists());
  }

  @Test
  void getGithubStatus_whenNotConnected_shouldReturnNotInstalled() throws Exception {
    when(githubService.hasInstallation()).thenReturn(false);

    mockMvc
        .perform(get("/api/github/status").with(user("user")))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.installed").value(false))
        .andExpect(jsonPath("$.githubUsername").doesNotExist());
  }

  @Test
  void getGithubStatus_withoutAuth_shouldReturn403() throws Exception {
    mockMvc.perform(get("/api/github/status")).andExpect(status().isForbidden());
  }

  // ==================== DELETE /api/github/disconnect ====================

  @Test
  void disconnectGithub_shouldCallServiceAndReturn204() throws Exception {
    mockMvc
        .perform(delete("/api/github/disconnect").with(user("user")))
        .andExpect(status().isNoContent());

    verify(githubService).disconnect();
  }

  @Test
  void disconnectGithub_withoutAuth_shouldReturn403() throws Exception {
    mockMvc.perform(delete("/api/github/disconnect")).andExpect(status().isForbidden());
  }

  // ==================== GET /api/github/install ====================

  @Test
  void initiateGithubInstall_whenConfigured_shouldReturnAuthUrl() throws Exception {
    when(githubAppConfig.isConfigured()).thenReturn(true);
    when(githubAppConfig.getClientId()).thenReturn("test-client-id");
    when(githubAppConfig.getRedirectUri()).thenReturn("http://localhost:8090/api/github/callback");

    mockMvc
        .perform(get("/api/github/install").with(user("user")))
        .andExpect(status().isOk())
        .andExpect(
            jsonPath("$.authUrl")
                .value(
                    org.hamcrest.Matchers.containsString(
                        "https://github.com/login/oauth/authorize")))
        .andExpect(
            jsonPath("$.authUrl")
                .value(org.hamcrest.Matchers.containsString("client_id=test-client-id")));
  }

  @Test
  void initiateGithubInstall_whenNotConfigured_shouldReturn500() throws Exception {
    when(githubAppConfig.isConfigured()).thenReturn(false);

    mockMvc
        .perform(get("/api/github/install").with(user("user")))
        .andExpect(status().isInternalServerError());
  }

  @Test
  void initiateGithubInstall_withRedirectUri_shouldIncludeStateInAuthUrl() throws Exception {
    when(githubAppConfig.isConfigured()).thenReturn(true);
    when(githubAppConfig.getClientId()).thenReturn("test-client-id");
    when(githubAppConfig.getRedirectUri()).thenReturn("http://localhost:8090/api/github/callback");

    mockMvc
        .perform(
            get("/api/github/install")
                .param("redirect_uri", "http://localhost:8765")
                .with(user("user")))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.authUrl").value(org.hamcrest.Matchers.containsString("state=")));
  }

  @Test
  void initiateGithubInstall_withoutAuth_shouldReturn403() throws Exception {
    mockMvc.perform(get("/api/github/install")).andExpect(status().isForbidden());
  }

  // ==================== GET /api/github/callback ====================

  @Test
  void handleGithubCallback_success_shouldRedirectToFrontendWithSuccess() throws Exception {
    GithubInstallation installation = createTestInstallation();
    when(githubService.exchangeCodeForTokens("test-code", "user_github")).thenReturn(installation);

    // State with userPublicId but no redirect_uri (web flow)
    String state = buildOAuthState(null, "user_github");

    mockMvc
        .perform(
            get("/api/github/callback")
                .param("code", "test-code")
                .param("installation_id", "12345")
                .param("state", state))
        .andExpect(status().isFound())
        .andExpect(
            header().string("Location", org.hamcrest.Matchers.containsString("github=success")));
  }

  @Test
  void handleGithubCallback_failure_shouldRedirectToFrontendWithError() throws Exception {
    when(githubService.exchangeCodeForTokens("invalid-code", "user_github"))
        .thenThrow(new RuntimeException("Token exchange failed"));

    String state = buildOAuthState(null, "user_github");

    mockMvc
        .perform(
            get("/api/github/callback")
                .param("code", "invalid-code")
                .param("installation_id", "12345")
                .param("state", state))
        .andExpect(status().isFound())
        .andExpect(
            header().string("Location", org.hamcrest.Matchers.containsString("github=error")));
  }

  @Test
  void handleGithubCallback_withoutState_shouldRedirectWithError() throws Exception {
    // Without state (missing userPublicId), should redirect with error
    mockMvc
        .perform(
            get("/api/github/callback")
                .param("code", "test-code")
                .param("installation_id", "12345"))
        .andExpect(status().isFound())
        .andExpect(
            header().string("Location", org.hamcrest.Matchers.containsString("github=error")));
  }

  @Test
  void handleGithubCallback_withState_shouldRedirectToClientUri() throws Exception {
    GithubInstallation installation = createTestInstallation();
    // Mock service with userPublicId parameter (callback is unauthenticated, uses UID from state)
    when(githubService.exchangeCodeForTokens("test-code", "user_github")).thenReturn(installation);

    // Build state with redirect_uri and userPublicId (same format as controller)
    String state = buildOAuthState("http://localhost:8765", "user_github");

    mockMvc
        .perform(
            get("/api/github/callback")
                .param("code", "test-code")
                .param("installation_id", "12345")
                .param("state", state))
        .andExpect(status().isFound())
        .andExpect(
            header().string("Location", org.hamcrest.Matchers.startsWith("http://localhost:8765")))
        .andExpect(
            header().string("Location", org.hamcrest.Matchers.containsString("github=success")))
        .andExpect(
            header().string("Location", org.hamcrest.Matchers.containsString("username=testuser")));
  }

  @Test
  void handleGithubCallback_withState_failure_shouldRedirectToClientUriWithError()
      throws Exception {
    when(githubService.exchangeCodeForTokens("invalid-code", "user_github"))
        .thenThrow(new RuntimeException("Token exchange failed"));

    String state = buildOAuthState("http://localhost:8765", "user_github");

    mockMvc
        .perform(
            get("/api/github/callback")
                .param("code", "invalid-code")
                .param("installation_id", "12345")
                .param("state", state))
        .andExpect(status().isFound())
        .andExpect(
            header().string("Location", org.hamcrest.Matchers.startsWith("http://localhost:8765")))
        .andExpect(
            header().string("Location", org.hamcrest.Matchers.containsString("github=error")));
  }

  // ==================== GET /api/github/repos ====================

  @Test
  void listGithubRepos_shouldReturnRepoList() throws Exception {
    Repository repo1 = createTestRepository(1L, "repo-one", "user/repo-one", false);
    Repository repo2 = createTestRepository(2L, "repo-two", "user/repo-two", true);
    RepositoryListResponse response = new RepositoryListResponse(List.of(repo1, repo2), 2, 1, 30);

    when(githubService.listRepositories(1, 30)).thenReturn(response);

    mockMvc
        .perform(get("/api/github/repos").with(user("user")))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.repos").isArray())
        .andExpect(jsonPath("$.repos.length()").value(2))
        .andExpect(jsonPath("$.repos[0].name").value("repo-one"))
        .andExpect(jsonPath("$.repos[0].fullName").value("user/repo-one"))
        .andExpect(jsonPath("$.repos[0].private").value(false))
        .andExpect(jsonPath("$.repos[1].name").value("repo-two"))
        .andExpect(jsonPath("$.repos[1].private").value(true))
        .andExpect(jsonPath("$.totalCount").value(2))
        .andExpect(jsonPath("$.page").value(1))
        .andExpect(jsonPath("$.perPage").value(30));
  }

  @Test
  void listGithubRepos_withPagination_shouldPassParameters() throws Exception {
    RepositoryListResponse response = new RepositoryListResponse(List.of(), 0, 2, 50);
    when(githubService.listRepositories(2, 50)).thenReturn(response);

    mockMvc
        .perform(
            get("/api/github/repos").param("page", "2").param("per_page", "50").with(user("user")))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.page").value(2))
        .andExpect(jsonPath("$.perPage").value(50));

    verify(githubService).listRepositories(2, 50);
  }

  @Test
  void listGithubRepos_whenNotConnected_shouldReturn404() throws Exception {
    when(githubService.listRepositories(anyInt(), anyInt()))
        .thenThrow(new EntityNotFoundException("No GitHub installation found"));

    mockMvc.perform(get("/api/github/repos").with(user("user"))).andExpect(status().isNotFound());
  }

  @Test
  void listGithubRepos_withoutAuth_shouldReturn403() throws Exception {
    mockMvc.perform(get("/api/github/repos")).andExpect(status().isForbidden());
  }

  // ==================== POST /api/github/repos ====================

  @Test
  void createGithubRepo_shouldCreateAndReturnRepo() throws Exception {
    Repository createdRepo = createTestRepository(123L, "new-project", "user/new-project", true);
    createdRepo.setDescription("A new project");

    when(githubService.createRepository("new-project", "A new project", true))
        .thenReturn(createdRepo);

    mockMvc
        .perform(
            post("/api/github/repos")
                .with(user("user"))
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    """
                    {
                      "name": "new-project",
                      "description": "A new project",
                      "private": true
                    }
                    """))
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.id").value(123))
        .andExpect(jsonPath("$.name").value("new-project"))
        .andExpect(jsonPath("$.fullName").value("user/new-project"))
        .andExpect(jsonPath("$.description").value("A new project"))
        .andExpect(jsonPath("$.private").value(true))
        .andExpect(jsonPath("$.htmlUrl").value("https://github.com/user/new-project"))
        .andExpect(jsonPath("$.cloneUrl").value("https://github.com/user/new-project.git"));
  }

  @Test
  void createGithubRepo_withMinimalPayload_shouldUseDefaults() throws Exception {
    Repository createdRepo = createTestRepository(456L, "minimal-repo", "user/minimal-repo", true);

    when(githubService.createRepository(eq("minimal-repo"), isNull(), eq(true)))
        .thenReturn(createdRepo);

    mockMvc
        .perform(
            post("/api/github/repos")
                .with(user("user"))
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    """
                    {
                      "name": "minimal-repo"
                    }
                    """))
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.name").value("minimal-repo"));
  }

  @Test
  void createGithubRepo_withPublicVisibility_shouldCreatePublicRepo() throws Exception {
    Repository createdRepo = createTestRepository(789L, "public-repo", "user/public-repo", false);

    when(githubService.createRepository("public-repo", null, false)).thenReturn(createdRepo);

    mockMvc
        .perform(
            post("/api/github/repos")
                .with(user("user"))
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    """
                    {
                      "name": "public-repo",
                      "private": false
                    }
                    """))
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.private").value(false));
  }

  @Test
  void createGithubRepo_whenNotConnected_shouldReturn404() throws Exception {
    when(githubService.createRepository(anyString(), any(), anyBoolean()))
        .thenThrow(new EntityNotFoundException("No GitHub installation found"));

    mockMvc
        .perform(
            post("/api/github/repos")
                .with(user("user"))
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    """
                    {
                      "name": "test-repo"
                    }
                    """))
        .andExpect(status().isNotFound());
  }

  @Test
  void createGithubRepo_withoutAuth_shouldReturn403() throws Exception {
    mockMvc
        .perform(
            post("/api/github/repos")
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    """
                    {
                      "name": "test-repo"
                    }
                    """))
        .andExpect(status().isForbidden());
  }

  @Test
  void createGithubRepo_withoutName_shouldReturn400() throws Exception {
    mockMvc
        .perform(
            post("/api/github/repos")
                .with(user("user"))
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    """
                    {
                      "description": "No name provided"
                    }
                    """))
        .andExpect(status().isBadRequest());
  }

  // ==================== GET /api/github/repos/{owner}/{repo}/exists ====================

  @Test
  void checkGithubRepoExists_whenExists_shouldReturnTrue() throws Exception {
    when(githubService.repositoryExists("octocat", "hello-world")).thenReturn(true);

    mockMvc
        .perform(get("/api/github/repos/octocat/hello-world/exists").with(user("user")))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.exists").value(true));
  }

  @Test
  void checkGithubRepoExists_whenNotExists_shouldReturnFalse() throws Exception {
    when(githubService.repositoryExists("octocat", "deleted-repo")).thenReturn(false);

    mockMvc
        .perform(get("/api/github/repos/octocat/deleted-repo/exists").with(user("user")))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.exists").value(false));
  }

  @Test
  void checkGithubRepoExists_whenNotConnected_shouldReturn404() throws Exception {
    when(githubService.repositoryExists(anyString(), anyString()))
        .thenThrow(new EntityNotFoundException("No GitHub installation found"));

    mockMvc
        .perform(get("/api/github/repos/octocat/hello-world/exists").with(user("user")))
        .andExpect(status().isNotFound());
  }

  @Test
  void checkGithubRepoExists_withoutAuth_shouldReturn403() throws Exception {
    mockMvc
        .perform(get("/api/github/repos/octocat/hello-world/exists"))
        .andExpect(status().isForbidden());
  }

  // ==================== DELETE /api/github/repos/{owner}/{repo} ====================

  @Test
  void deleteGithubRepo_shouldDeleteAndReturn204() throws Exception {
    mockMvc
        .perform(delete("/api/github/repos/octocat/hello-world").with(user("user")))
        .andExpect(status().isNoContent());

    verify(githubService).deleteRepository("octocat", "hello-world");
  }

  @Test
  void deleteGithubRepo_whenNotConnected_shouldReturn404() throws Exception {
    org.mockito.Mockito.doThrow(new EntityNotFoundException("No GitHub installation found"))
        .when(githubService)
        .deleteRepository(anyString(), anyString());

    mockMvc
        .perform(delete("/api/github/repos/octocat/hello-world").with(user("user")))
        .andExpect(status().isNotFound());
  }

  @Test
  void deleteGithubRepo_withoutAuth_shouldReturn403() throws Exception {
    mockMvc
        .perform(delete("/api/github/repos/octocat/hello-world"))
        .andExpect(status().isForbidden());
  }

  // ==================== Helper Methods ====================

  private Repository createTestRepository(
      Long id, String name, String fullName, boolean isPrivate) {
    Repository repo = new Repository();
    repo.setId(id);
    repo.setName(name);
    repo.setFullName(fullName);
    repo.setPrivateRepo(isPrivate);
    repo.setHtmlUrl("https://github.com/" + fullName);
    repo.setCloneUrl("https://github.com/" + fullName + ".git");
    return repo;
  }

  private String buildOAuthState(String redirectUri, String userPublicId) {
    String redirectUriJson = redirectUri != null ? "\"" + redirectUri + "\"" : "null";
    String json =
        "{\"redirectUri\":" + redirectUriJson + ",\"userPublicId\":\"" + userPublicId + "\"}";
    return Base64.getUrlEncoder()
        .withoutPadding()
        .encodeToString(json.getBytes(StandardCharsets.UTF_8));
  }

  private GithubInstallation createTestInstallation() {
    return new GithubInstallation(
        "ghi_test123",
        testUser.getId(),
        12345L,
        "access-token",
        Instant.now().plusSeconds(3600),
        "refresh-token",
        Instant.now().plusSeconds(86400),
        "testuser");
  }
}
