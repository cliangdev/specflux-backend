package com.specflux.apikey.interfaces.rest;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

import com.specflux.apikey.application.ApiKeyService;
import com.specflux.apikey.domain.ApiKey;
import com.specflux.apikey.domain.ApiKeyRepository;
import com.specflux.common.AbstractControllerIntegrationTest;
import com.specflux.user.domain.User;

/**
 * Integration tests for ApiKeyController.
 *
 * <p>Tests API key creation, listing, and revocation endpoints.
 */
class ApiKeyControllerTest extends AbstractControllerIntegrationTest {

  @DynamicPropertySource
  static void configureSchema(DynamicPropertyRegistry registry) {
    configureSchemaForClass(registry, ApiKeyControllerTest.class);
  }

  @Autowired private ApiKeyRepository apiKeyRepository;
  @Autowired private ApiKeyService apiKeyService;

  @BeforeEach
  void cleanUpApiKeys() {
    apiKeyRepository.findByUserId(testUser.getId()).forEach(apiKeyRepository::delete);
  }

  @Test
  void createApiKey_shouldReturnCreatedKey() throws Exception {
    String requestBody =
        """
        {"name": "Test Key"}
        """;

    mockMvc
        .perform(
            post("/api/users/me/api-keys")
                .with(user("user"))
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestBody))
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.id").exists())
        .andExpect(jsonPath("$.name").value("Test Key"))
        .andExpect(jsonPath("$.key").exists())
        .andExpect(jsonPath("$.keyPrefix").exists())
        .andExpect(jsonPath("$.createdAt").exists());
  }

  @Test
  void createApiKey_withExpiration_shouldSetExpiresAt() throws Exception {
    String requestBody =
        """
        {"name": "Expiring Key", "expiresAt": "2025-12-31T23:59:59Z"}
        """;

    mockMvc
        .perform(
            post("/api/users/me/api-keys")
                .with(user("user"))
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestBody))
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.expiresAt").exists());
  }

  @Test
  void createApiKey_withoutBody_shouldSucceed() throws Exception {
    mockMvc
        .perform(
            post("/api/users/me/api-keys")
                .with(user("user"))
                .contentType(MediaType.APPLICATION_JSON)
                .content("{}"))
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.key").exists());
  }

  @Test
  void createApiKey_whenAlreadyHasKey_shouldReturn409() throws Exception {
    apiKeyService.createApiKey(testUser.getId(), "First Key", null);

    String requestBody =
        """
        {"name": "Second Key"}
        """;

    mockMvc
        .perform(
            post("/api/users/me/api-keys")
                .with(user("user"))
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestBody))
        .andExpect(status().isConflict());
  }

  @Test
  void createApiKey_withoutAuth_shouldReturn403() throws Exception {
    mockMvc
        .perform(
            post("/api/users/me/api-keys").contentType(MediaType.APPLICATION_JSON).content("{}"))
        .andExpect(status().isForbidden());
  }

  @Test
  void listApiKeys_shouldReturnEmptyList() throws Exception {
    mockMvc
        .perform(get("/api/users/me/api-keys").with(user("user")))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$").isArray())
        .andExpect(jsonPath("$.length()").value(0));
  }

  @Test
  void listApiKeys_shouldReturnKeys() throws Exception {
    apiKeyService.createApiKey(testUser.getId(), "My Key", null);

    mockMvc
        .perform(get("/api/users/me/api-keys").with(user("user")))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$").isArray())
        .andExpect(jsonPath("$.length()").value(1))
        .andExpect(jsonPath("$[0].name").value("My Key"))
        .andExpect(jsonPath("$[0].keyPrefix").exists())
        .andExpect(jsonPath("$[0].createdAt").exists())
        .andExpect(jsonPath("$[0].key").doesNotExist());
  }

  @Test
  void listApiKeys_withoutAuth_shouldReturn403() throws Exception {
    mockMvc.perform(get("/api/users/me/api-keys")).andExpect(status().isForbidden());
  }

  @Test
  void revokeApiKey_shouldReturn204() throws Exception {
    var result = apiKeyService.createApiKey(testUser.getId(), "To Revoke", null);
    String keyId = result.apiKey().getPublicId();

    mockMvc
        .perform(delete("/api/users/me/api-keys/{id}", keyId).with(user("user")))
        .andExpect(status().isNoContent());

    ApiKey revokedKey = apiKeyRepository.findByPublicId(keyId).orElseThrow();
    assert revokedKey.isRevoked();
  }

  @Test
  void revokeApiKey_notFound_shouldReturn404() throws Exception {
    mockMvc
        .perform(delete("/api/users/me/api-keys/{id}", "key_nonexistent").with(user("user")))
        .andExpect(status().isNotFound());
  }

  @Test
  void revokeApiKey_belongsToOtherUser_shouldReturn403() throws Exception {
    User otherUser =
        userRepository.save(
            new User("usr_other_apikey", "fb_other_apikey", "other@test.com", "Other User"));
    var result = apiKeyService.createApiKey(otherUser.getId(), "Other's Key", null);
    String keyId = result.apiKey().getPublicId();

    mockMvc
        .perform(delete("/api/users/me/api-keys/{id}", keyId).with(user("user")))
        .andExpect(status().isForbidden());
  }

  @Test
  void revokeApiKey_withoutAuth_shouldReturn403() throws Exception {
    mockMvc
        .perform(delete("/api/users/me/api-keys/{id}", "key_any"))
        .andExpect(status().isForbidden());
  }

  @Test
  void revokeApiKey_afterRevoke_canCreateNewKey() throws Exception {
    String createBody =
        """
        {"name": "First Key"}
        """;

    var createResult =
        mockMvc
            .perform(
                post("/api/users/me/api-keys")
                    .with(user("user"))
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(createBody))
            .andExpect(status().isCreated())
            .andReturn();

    String keyId =
        objectMapper.readTree(createResult.getResponse().getContentAsString()).get("id").asText();

    mockMvc
        .perform(delete("/api/users/me/api-keys/{id}", keyId).with(user("user")))
        .andExpect(status().isNoContent());

    String newKeyBody =
        """
        {"name": "New Key"}
        """;

    mockMvc
        .perform(
            post("/api/users/me/api-keys")
                .with(user("user"))
                .contentType(MediaType.APPLICATION_JSON)
                .content(newKeyBody))
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.name").value("New Key"));
  }

  @Test
  void apiKeyAuth_validKey_shouldAuthenticate() throws Exception {
    var result = apiKeyService.createApiKey(testUser.getId(), "Auth Test Key", null);
    String fullKey = result.fullKey();

    mockMvc
        .perform(get("/api/users/me/api-keys").header("Authorization", "Bearer " + fullKey))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$").isArray())
        .andExpect(jsonPath("$.length()").value(1))
        .andExpect(jsonPath("$[0].name").value("Auth Test Key"));
  }

  @Test
  void apiKeyAuth_invalidKey_shouldReturn401() throws Exception {
    mockMvc
        .perform(
            get("/api/users/me/api-keys")
                .header("Authorization", "Bearer sfx_invalidkeyvalue123456789012345678901234"))
        .andExpect(status().isUnauthorized());
  }

  @Test
  void apiKeyAuth_revokedKey_shouldReturn401() throws Exception {
    var result = apiKeyService.createApiKey(testUser.getId(), "Revoked Key", null);
    String fullKey = result.fullKey();
    apiKeyService.revokeKey(result.apiKey().getPublicId(), testUser.getId());

    mockMvc
        .perform(get("/api/users/me/api-keys").header("Authorization", "Bearer " + fullKey))
        .andExpect(status().isUnauthorized());
  }

  @Test
  void apiKeyAuth_expiredKey_shouldReturn401() throws Exception {
    var result =
        apiKeyService.createApiKey(
            testUser.getId(), "Expired Key", java.time.Instant.now().minusSeconds(3600));
    String fullKey = result.fullKey();

    mockMvc
        .perform(get("/api/users/me/api-keys").header("Authorization", "Bearer " + fullKey))
        .andExpect(status().isUnauthorized());
  }

  @Test
  void apiKeyAuth_wrongPrefix_shouldReturn403() throws Exception {
    mockMvc
        .perform(
            get("/api/users/me/api-keys").header("Authorization", "Bearer wrong_prefix_key_here"))
        .andExpect(status().isForbidden());
  }

  @Test
  void apiKeyAuth_canPerformActions_asAuthenticatedUser() throws Exception {
    var result = apiKeyService.createApiKey(testUser.getId(), "Action Test Key", null);
    String fullKey = result.fullKey();

    mockMvc
        .perform(
            delete("/api/users/me/api-keys/{id}", result.apiKey().getPublicId())
                .header("Authorization", "Bearer " + fullKey))
        .andExpect(status().isNoContent());

    ApiKey revokedKey =
        apiKeyRepository.findByPublicId(result.apiKey().getPublicId()).orElseThrow();
    assert revokedKey.isRevoked();
  }
}
