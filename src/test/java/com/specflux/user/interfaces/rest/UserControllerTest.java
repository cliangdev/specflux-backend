package com.specflux.user.interfaces.rest;

import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

import com.specflux.api.generated.model.UpdateUserRequestDto;
import com.specflux.common.AbstractControllerIntegrationTest;
import com.specflux.user.domain.User;
import com.specflux.user.domain.UserRepository;

/**
 * Integration tests for UserController.
 *
 * <p>Uses schema isolation for parallel test execution.
 */
class UserControllerTest extends AbstractControllerIntegrationTest {

  private static final String SCHEMA_NAME = "user_controller_test";

  @DynamicPropertySource
  static void configureSchema(DynamicPropertyRegistry registry) {
    AbstractControllerIntegrationTest.configureSchema(registry, SCHEMA_NAME);
  }

  @Autowired private UserRepository localUserRepository;

  @BeforeEach
  void setUpGetOrCreateMock() {
    // Also mock getOrCreateCurrentUser for User API endpoints
    when(currentUserService.getOrCreateCurrentUser()).thenReturn(testUser);
  }

  @Test
  void getCurrentUser_shouldReturnCurrentUserProfile() throws Exception {
    mockMvc
        .perform(get("/users/me").with(user("user")))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.publicId").value(testUser.getPublicId()))
        .andExpect(jsonPath("$.email").value(testUser.getEmail()))
        .andExpect(jsonPath("$.displayName").value(testUser.getDisplayName()))
        .andExpect(jsonPath("$.createdAt").exists());
  }

  @Test
  void getCurrentUser_withoutAuth_shouldReturn403() throws Exception {
    mockMvc.perform(get("/users/me")).andExpect(status().isForbidden());
  }

  @Test
  void updateCurrentUser_displayName_shouldUpdateAndReturn() throws Exception {
    UpdateUserRequestDto request = new UpdateUserRequestDto();
    request.setDisplayName("New Display Name");

    mockMvc
        .perform(
            put("/users/me")
                .with(user("user"))
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.publicId").value(testUser.getPublicId()))
        .andExpect(jsonPath("$.displayName").value("New Display Name"));

    // Verify persisted
    User updated = localUserRepository.findByPublicId(testUser.getPublicId()).orElseThrow();
    assert updated.getDisplayName().equals("New Display Name");
  }

  @Test
  void updateCurrentUser_avatarUrl_shouldUpdateAndReturn() throws Exception {
    UpdateUserRequestDto request = new UpdateUserRequestDto();
    request.setAvatarUrl("https://example.com/avatar.png");

    mockMvc
        .perform(
            put("/users/me")
                .with(user("user"))
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.avatarUrl").value("https://example.com/avatar.png"));
  }

  @Test
  void updateCurrentUser_withoutAuth_shouldReturn403() throws Exception {
    UpdateUserRequestDto request = new UpdateUserRequestDto();
    request.setDisplayName("New Name");

    mockMvc
        .perform(
            put("/users/me")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
        .andExpect(status().isForbidden());
  }

  @Test
  void getUser_byPublicId_shouldReturnUser() throws Exception {
    // Create another user to lookup
    User otherUser =
        localUserRepository.save(
            new User("usr_other123", "fb_other", "other@test.com", "Other User"));

    mockMvc
        .perform(get("/users/{ref}", "usr_other123").with(user("user")))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.publicId").value("usr_other123"))
        .andExpect(jsonPath("$.email").value("other@test.com"))
        .andExpect(jsonPath("$.displayName").value("Other User"));
  }

  @Test
  void getUser_notFound_shouldReturn404() throws Exception {
    mockMvc
        .perform(get("/users/{ref}", "usr_nonexistent").with(user("user")))
        .andExpect(status().isNotFound())
        .andExpect(jsonPath("$.code").value("NOT_FOUND"));
  }

  @Test
  void getUser_withoutAuth_shouldReturn403() throws Exception {
    mockMvc.perform(get("/users/{ref}", "usr_any")).andExpect(status().isForbidden());
  }
}
