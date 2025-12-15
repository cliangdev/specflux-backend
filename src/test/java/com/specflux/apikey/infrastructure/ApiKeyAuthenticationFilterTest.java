package com.specflux.apikey.infrastructure;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import com.specflux.apikey.application.ApiKeyService;
import com.specflux.apikey.domain.ApiKeyRepository;
import com.specflux.common.AbstractIntegrationTest;
import com.specflux.user.domain.User;
import com.specflux.user.domain.UserRepository;

/**
 * Integration tests for API key authentication filter chain.
 *
 * <p>These tests verify that the security filter chain correctly processes API key tokens. Unlike
 * controller tests that use {@code .with(user())}, these tests send actual Bearer tokens to verify
 * the filter chain order and token handling.
 */
@AutoConfigureMockMvc
@Transactional
class ApiKeyAuthenticationFilterTest extends AbstractIntegrationTest {

  @DynamicPropertySource
  static void configureSchema(DynamicPropertyRegistry registry) {
    configureSchemaForClass(registry, ApiKeyAuthenticationFilterTest.class);
  }

  @Autowired private MockMvc mockMvc;
  @Autowired private ApiKeyService apiKeyService;
  @Autowired private ApiKeyRepository apiKeyRepository;
  @Autowired private UserRepository userRepository;

  private User testUser;
  private String validApiKey;

  @BeforeEach
  void setUp() {
    testUser =
        userRepository.save(
            new User("user_filtertest", "fb_filtertest", "filtertest@test.com", "Filter Test"));

    var createResult = apiKeyService.createApiKey(testUser.getId(), "Test Key", null);
    validApiKey = createResult.fullKey();
  }

  @Test
  void authenticatedRequest_withValidApiKey_shouldSucceed() throws Exception {
    mockMvc
        .perform(get("/api/projects").header("Authorization", "Bearer " + validApiKey))
        .andExpect(status().isOk());
  }

  @Test
  void authenticatedRequest_withInvalidApiKey_shouldReturn401() throws Exception {
    mockMvc
        .perform(get("/api/projects").header("Authorization", "Bearer sfx_invalid_key_12345"))
        .andExpect(status().isUnauthorized());
  }

  @Test
  void authenticatedRequest_withRevokedApiKey_shouldReturn401() throws Exception {
    var apiKey = apiKeyRepository.findByUserId(testUser.getId()).get(0);
    apiKeyService.revokeKey(apiKey.getPublicId(), testUser.getId());

    mockMvc
        .perform(get("/api/projects").header("Authorization", "Bearer " + validApiKey))
        .andExpect(status().isUnauthorized());
  }

  @Test
  void authenticatedRequest_withExpiredApiKey_shouldReturn401() throws Exception {
    var apiKey = apiKeyRepository.findByUserId(testUser.getId()).get(0);
    apiKeyService.revokeKey(apiKey.getPublicId(), testUser.getId());

    var expiredResult =
        apiKeyService.createApiKey(
            testUser.getId(), "Expired Key", java.time.Instant.now().minusSeconds(3600));

    mockMvc
        .perform(get("/api/projects").header("Authorization", "Bearer " + expiredResult.fullKey()))
        .andExpect(status().isUnauthorized());
  }

  @Test
  void authenticatedRequest_withNoAuth_shouldReturn403() throws Exception {
    mockMvc.perform(get("/api/projects")).andExpect(status().isForbidden());
  }

  @Test
  void authenticatedRequest_withMalformedToken_shouldReturn403() throws Exception {
    mockMvc
        .perform(get("/api/projects").header("Authorization", "Bearer not_a_valid_token"))
        .andExpect(status().isForbidden());
  }

  @Test
  void publicEndpoint_actuator_shouldNotRequireAuth() throws Exception {
    mockMvc.perform(get("/actuator/health")).andExpect(status().isOk());
  }

  @Test
  void publicEndpoint_apiDocs_shouldNotRequireAuth() throws Exception {
    mockMvc.perform(get("/api-docs")).andExpect(status().isOk());
  }
}
