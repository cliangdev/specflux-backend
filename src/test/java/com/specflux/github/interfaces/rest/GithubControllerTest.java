package com.specflux.github.interfaces.rest;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Base64;

import org.junit.jupiter.api.Test;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import com.specflux.common.AbstractControllerIntegrationTest;
import com.specflux.github.application.GithubService;
import com.specflux.github.domain.GithubInstallation;
import com.specflux.github.infrastructure.GithubAppConfig;

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
  void initiateGithubInstall_whenConfigured_shouldRedirectToGithub() throws Exception {
    when(githubAppConfig.isConfigured()).thenReturn(true);
    when(githubAppConfig.getClientId()).thenReturn("test-client-id");
    when(githubAppConfig.getRedirectUri()).thenReturn("http://localhost:8090/api/github/callback");

    mockMvc
        .perform(get("/api/github/install").with(user("user")))
        .andExpect(status().isFound())
        .andExpect(
            header()
                .string(
                    "Location",
                    org.hamcrest.Matchers.containsString(
                        "https://github.com/login/oauth/authorize")))
        .andExpect(
            header()
                .string(
                    "Location", org.hamcrest.Matchers.containsString("client_id=test-client-id")));
  }

  @Test
  void initiateGithubInstall_whenNotConfigured_shouldReturn500() throws Exception {
    when(githubAppConfig.isConfigured()).thenReturn(false);

    mockMvc
        .perform(get("/api/github/install").with(user("user")))
        .andExpect(status().isInternalServerError());
  }

  @Test
  void initiateGithubInstall_withRedirectUri_shouldIncludeInState() throws Exception {
    when(githubAppConfig.isConfigured()).thenReturn(true);
    when(githubAppConfig.getClientId()).thenReturn("test-client-id");
    when(githubAppConfig.getRedirectUri()).thenReturn("http://localhost:8090/api/github/callback");

    mockMvc
        .perform(
            get("/api/github/install")
                .param("redirect_uri", "http://localhost:8765")
                .with(user("user")))
        .andExpect(status().isFound())
        .andExpect(header().string("Location", org.hamcrest.Matchers.containsString("state=")));
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

  // ==================== Helper Methods ====================

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
