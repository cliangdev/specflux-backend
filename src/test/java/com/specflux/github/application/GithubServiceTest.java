package com.specflux.github.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.util.Collections;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.transaction.annotation.Transactional;

import com.specflux.common.AbstractIntegrationTest;
import com.specflux.github.domain.GithubInstallation;
import com.specflux.github.domain.GithubInstallationRepository;
import com.specflux.github.infrastructure.GithubApiClient;
import com.specflux.github.infrastructure.GithubApiClient.TokenResponse;
import com.specflux.github.infrastructure.GithubApiClient.UserProfile;
import com.specflux.shared.infrastructure.security.FirebasePrincipal;
import com.specflux.user.domain.User;
import com.specflux.user.domain.UserRepository;

import jakarta.persistence.EntityNotFoundException;

/**
 * Integration tests for GithubService.
 *
 * <p>Tests OAuth token exchange, installation management, and token refresh.
 */
@Transactional
class GithubServiceTest extends AbstractIntegrationTest {

  @DynamicPropertySource
  static void configureSchema(DynamicPropertyRegistry registry) {
    configureSchemaForClass(registry, GithubServiceTest.class);
  }

  @MockitoBean private GithubApiClient githubApiClient;

  @Autowired private GithubService githubService;
  @Autowired private GithubInstallationRepository installationRepository;
  @Autowired private UserRepository userRepository;

  private User testUser;

  @BeforeEach
  void setUp() {
    testUser = new User("usr_test123456789", "firebase_uid_123", "test@example.com", "Test User");
    userRepository.save(testUser);
    setSecurityContext(testUser);
  }

  @AfterEach
  void clearSecurityContext() {
    SecurityContextHolder.clearContext();
  }

  @Test
  void exchangeCodeForTokens_shouldCreateNewInstallation() {
    TokenResponse tokenResponse = createTokenResponse();
    UserProfile userProfile = createUserProfile(12345L, "testuser");

    when(githubApiClient.exchangeCodeForTokens("test-code")).thenReturn(tokenResponse);
    when(githubApiClient.getAuthenticatedUser(tokenResponse.getAccessToken()))
        .thenReturn(userProfile);

    GithubInstallation installation = githubService.exchangeCodeForTokens("test-code");

    assertThat(installation).isNotNull();
    assertThat(installation.getGithubUsername()).isEqualTo("testuser");
    assertThat(installation.getUserId()).isEqualTo(testUser.getId());
    assertThat(installation.getAccessToken()).isEqualTo(tokenResponse.getAccessToken());
    assertThat(installationRepository.count()).isEqualTo(1);
  }

  @Test
  void exchangeCodeForTokens_shouldUpdateExistingInstallation() {
    GithubInstallation existing =
        new GithubInstallation(
            "ghi_existing12345",
            testUser.getId(),
            12345L,
            "old-token",
            Instant.now().plusSeconds(3600),
            "old-refresh",
            Instant.now().plusSeconds(86400),
            "olduser");
    installationRepository.save(existing);

    TokenResponse tokenResponse = createTokenResponse();
    UserProfile userProfile = createUserProfile(12345L, "newuser");

    when(githubApiClient.exchangeCodeForTokens("test-code")).thenReturn(tokenResponse);
    when(githubApiClient.getAuthenticatedUser(tokenResponse.getAccessToken()))
        .thenReturn(userProfile);

    GithubInstallation installation = githubService.exchangeCodeForTokens("test-code");

    assertThat(installation.getPublicId()).isEqualTo("ghi_existing12345");
    assertThat(installation.getGithubUsername()).isEqualTo("newuser");
    assertThat(installation.getAccessToken()).isEqualTo(tokenResponse.getAccessToken());
    assertThat(installationRepository.count()).isEqualTo(1);
  }

  @Test
  void hasInstallation_shouldReturnTrueWhenInstallationExists() {
    GithubInstallation installation = createTestInstallation();
    installationRepository.save(installation);

    assertThat(githubService.hasInstallation()).isTrue();
  }

  @Test
  void hasInstallation_shouldReturnFalseWhenNoInstallation() {
    assertThat(githubService.hasInstallation()).isFalse();
  }

  @Test
  void getInstallation_shouldReturnInstallation() {
    GithubInstallation installation = createTestInstallation();
    installationRepository.save(installation);

    GithubInstallation result = githubService.getInstallation();

    assertThat(result.getPublicId()).isEqualTo(installation.getPublicId());
    assertThat(result.getGithubUsername()).isEqualTo("testuser");
  }

  @Test
  void getInstallation_shouldThrowWhenNoInstallation() {
    assertThatThrownBy(() -> githubService.getInstallation())
        .isInstanceOf(EntityNotFoundException.class)
        .hasMessageContaining("No GitHub installation found");
  }

  @Test
  void disconnect_shouldDeleteInstallation() {
    GithubInstallation installation = createTestInstallation();
    installationRepository.save(installation);
    assertThat(installationRepository.count()).isEqualTo(1);

    githubService.disconnect();

    assertThat(installationRepository.count()).isEqualTo(0);
  }

  @Test
  void refreshAccessToken_shouldRefreshWhenExpired() {
    GithubInstallation installation =
        new GithubInstallation(
            "ghi_expired123456",
            testUser.getId(),
            12345L,
            "expired-token",
            Instant.now().minusSeconds(3600),
            "refresh-token",
            Instant.now().plusSeconds(86400),
            "testuser");
    installationRepository.save(installation);

    TokenResponse newTokenResponse = createTokenResponse();
    when(githubApiClient.refreshAccessToken("refresh-token")).thenReturn(newTokenResponse);

    GithubInstallation refreshed = githubService.refreshAccessToken(installation);

    assertThat(refreshed.getAccessToken()).isEqualTo(newTokenResponse.getAccessToken());
    verify(githubApiClient).refreshAccessToken("refresh-token");
  }

  @Test
  void refreshAccessToken_shouldNotRefreshWhenStillValid() {
    GithubInstallation installation = createTestInstallation();
    installationRepository.save(installation);

    GithubInstallation result = githubService.refreshAccessToken(installation);

    assertThat(result.getAccessToken()).isEqualTo(installation.getAccessToken());
    verify(githubApiClient, never()).refreshAccessToken(anyString());
  }

  private TokenResponse createTokenResponse() {
    TokenResponse response = new TokenResponse();
    response.setAccessToken("new-access-token");
    response.setExpiresIn(3600L);
    response.setRefreshToken("new-refresh-token");
    response.setRefreshTokenExpiresIn(86400L);
    return response;
  }

  private UserProfile createUserProfile(Long id, String login) {
    UserProfile profile = new UserProfile();
    profile.setId(id);
    profile.setLogin(login);
    return profile;
  }

  private GithubInstallation createTestInstallation() {
    return new GithubInstallation(
        "ghi_test123456789",
        testUser.getId(),
        12345L,
        "access-token",
        Instant.now().plusSeconds(3600),
        "refresh-token",
        Instant.now().plusSeconds(86400),
        "testuser");
  }

  private void setSecurityContext(User user) {
    FirebasePrincipal principal =
        new FirebasePrincipal(
            user.getFirebaseUid(), user.getEmail(), user.getDisplayName(), user.getAvatarUrl());
    var auth = new UsernamePasswordAuthenticationToken(principal, null, Collections.emptyList());
    SecurityContextHolder.getContext().setAuthentication(auth);
  }
}
