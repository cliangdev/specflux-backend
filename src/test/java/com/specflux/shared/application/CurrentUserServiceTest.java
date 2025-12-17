package com.specflux.shared.application;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Collections;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.transaction.annotation.Transactional;

import com.specflux.common.AbstractIntegrationTest;
import com.specflux.shared.infrastructure.security.FirebasePrincipal;
import com.specflux.user.domain.User;
import com.specflux.user.domain.UserRepository;

/**
 * Integration tests for CurrentUserService.
 *
 * <p>Tests account linking scenarios where a user signs up with email, then signs in with GitHub
 * using the same email address.
 */
@Transactional
class CurrentUserServiceTest extends AbstractIntegrationTest {

  @DynamicPropertySource
  static void configureSchema(DynamicPropertyRegistry registry) {
    configureSchemaForClass(registry, CurrentUserServiceTest.class);
  }

  @Autowired private CurrentUserService currentUserService;

  @Autowired private UserRepository userRepository;

  @AfterEach
  void clearSecurityContext() {
    SecurityContextHolder.clearContext();
  }

  @Test
  void shouldCreateNewUserWhenNoneExists() {
    // Given: A new user with email and GitHub UID
    FirebasePrincipal principal =
        new FirebasePrincipal(
            "github_uid_123", "newuser@example.com", "New User", "https://avatar.com/new.png");
    setSecurityContext(principal);

    // When: getOrCreateCurrentUser is called
    User user = currentUserService.getOrCreateCurrentUser();

    // Then: A new user is created
    assertThat(user).isNotNull();
    assertThat(user.getFirebaseUid()).isEqualTo("github_uid_123");
    assertThat(user.getEmail()).isEqualTo("newuser@example.com");
    assertThat(user.getDisplayName()).isEqualTo("New User");
    assertThat(user.getAvatarUrl()).isEqualTo("https://avatar.com/new.png");
  }

  @Test
  void shouldReturnExistingUserByFirebaseUid() {
    // Given: An existing user
    User existingUser =
        new User("usr_existing123456", "email_uid_456", "existing@example.com", "Existing User");
    userRepository.save(existingUser);

    // When: Same user signs in with same Firebase UID
    FirebasePrincipal principal =
        new FirebasePrincipal("email_uid_456", "existing@example.com", "Existing User", null);
    setSecurityContext(principal);
    User user = currentUserService.getOrCreateCurrentUser();

    // Then: The existing user is returned (no duplicate created)
    assertThat(user.getPublicId()).isEqualTo("usr_existing123456");
    assertThat(userRepository.count()).isEqualTo(1);
  }

  @Test
  void shouldLinkAccountsWhenSameEmailDifferentFirebaseUid() {
    // Given: User signed up with email (has one Firebase UID)
    User emailUser =
        new User("usr_emailuser12345", "email_uid_789", "same@example.com", "Email User");
    userRepository.save(emailUser);

    // When: Same user signs in with GitHub (different Firebase UID, same email)
    FirebasePrincipal githubPrincipal =
        new FirebasePrincipal(
            "github_uid_different",
            "same@example.com",
            "GitHub User",
            "https://avatar.com/github.png");
    setSecurityContext(githubPrincipal);
    User user = currentUserService.getOrCreateCurrentUser();

    // Then: The existing user is returned with updated Firebase UID (account linking)
    assertThat(user.getPublicId()).isEqualTo("usr_emailuser12345");
    assertThat(user.getFirebaseUid()).isEqualTo("github_uid_different");
    assertThat(user.getDisplayName()).isEqualTo("GitHub User");
    assertThat(user.getAvatarUrl()).isEqualTo("https://avatar.com/github.png");

    // And: Only one user exists in the database
    assertThat(userRepository.count()).isEqualTo(1);
  }

  @Test
  void shouldUpdateDisplayNameAndAvatarOnAccountLink() {
    // Given: User signed up with email (no avatar)
    User emailUser =
        new User("usr_nopicture1234", "email_uid_nopic", "user@example.com", "Plain User");
    userRepository.save(emailUser);

    // When: Same user signs in with GitHub (has avatar)
    FirebasePrincipal githubPrincipal =
        new FirebasePrincipal(
            "github_uid_withpic",
            "user@example.com",
            "GitHub Display Name",
            "https://github.com/avatar.png");
    setSecurityContext(githubPrincipal);
    User user = currentUserService.getOrCreateCurrentUser();

    // Then: Display name and avatar are updated
    assertThat(user.getDisplayName()).isEqualTo("GitHub Display Name");
    assertThat(user.getAvatarUrl()).isEqualTo("https://github.com/avatar.png");
  }

  @Test
  void shouldNotUpdateDisplayNameIfNullInNewPrincipal() {
    // Given: User with display name
    User existingUser =
        new User("usr_keepname12345", "old_uid_123", "keep@example.com", "Original Name");
    userRepository.save(existingUser);

    // When: New principal has null display name
    FirebasePrincipal newPrincipal =
        new FirebasePrincipal("new_uid_456", "keep@example.com", null, null);
    setSecurityContext(newPrincipal);
    User user = currentUserService.getOrCreateCurrentUser();

    // Then: Original display name is preserved
    assertThat(user.getDisplayName()).isEqualTo("Original Name");
  }

  private void setSecurityContext(FirebasePrincipal principal) {
    var auth = new UsernamePasswordAuthenticationToken(principal, null, Collections.emptyList());
    SecurityContextHolder.getContext().setAuthentication(auth);
  }
}
