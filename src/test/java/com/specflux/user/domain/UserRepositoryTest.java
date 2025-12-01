package com.specflux.user.domain;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.transaction.annotation.Transactional;

import com.specflux.common.AbstractIntegrationTest;

/**
 * Integration tests for UserRepository.
 *
 * <p>Uses schema isolation (user_repository_test) for parallel test execution.
 */
@Transactional
class UserRepositoryTest extends AbstractIntegrationTest {

  private static final String SCHEMA_NAME = "user_repository_test";

  @DynamicPropertySource
  static void configureSchema(DynamicPropertyRegistry registry) {
    AbstractIntegrationTest.configureSchema(registry, SCHEMA_NAME);
  }

  @Autowired private UserRepository userRepository;

  @Test
  void shouldSaveAndRetrieveUser() {
    User user = new User("user_abc123", "firebase_uid_123", "test@example.com", "Test User");

    User saved = userRepository.save(user);

    assertThat(saved.getId()).isNotNull();
    assertThat(saved.getPublicId()).isEqualTo("user_abc123");
    assertThat(saved.getFirebaseUid()).isEqualTo("firebase_uid_123");
    assertThat(saved.getEmail()).isEqualTo("test@example.com");
    assertThat(saved.getDisplayName()).isEqualTo("Test User");
    assertThat(saved.getCreatedAt()).isNotNull();
    assertThat(saved.getUpdatedAt()).isNotNull();
  }

  @Test
  void shouldFindByFirebaseUid() {
    User user = new User("user_def456", "firebase_uid_456", "user2@example.com", "User Two");
    userRepository.save(user);

    Optional<User> found = userRepository.findByFirebaseUid("firebase_uid_456");

    assertThat(found).isPresent();
    assertThat(found.get().getEmail()).isEqualTo("user2@example.com");
  }

  @Test
  void shouldFindByPublicId() {
    User user = new User("user_ghi789", "firebase_uid_789", "user3@example.com", "User Three");
    userRepository.save(user);

    Optional<User> found = userRepository.findByPublicId("user_ghi789");

    assertThat(found).isPresent();
    assertThat(found.get().getFirebaseUid()).isEqualTo("firebase_uid_789");
  }

  @Test
  void shouldFindByEmail() {
    User user = new User("user_jkl012", "firebase_uid_012", "unique@example.com", "Unique User");
    userRepository.save(user);

    Optional<User> found = userRepository.findByEmail("unique@example.com");

    assertThat(found).isPresent();
    assertThat(found.get().getPublicId()).isEqualTo("user_jkl012");
  }

  @Test
  void shouldCheckExistsByFirebaseUid() {
    User user = new User("user_mno345", "firebase_uid_345", "exists@example.com", "Exists User");
    userRepository.save(user);

    assertThat(userRepository.existsByFirebaseUid("firebase_uid_345")).isTrue();
    assertThat(userRepository.existsByFirebaseUid("nonexistent")).isFalse();
  }

  @Test
  void shouldCheckExistsByEmail() {
    User user = new User("user_pqr678", "firebase_uid_678", "check@example.com", "Check User");
    userRepository.save(user);

    assertThat(userRepository.existsByEmail("check@example.com")).isTrue();
    assertThat(userRepository.existsByEmail("notfound@example.com")).isFalse();
  }

  @Test
  void shouldUpdateUserDisplayName() {
    User user = new User("user_stu901", "firebase_uid_901", "update@example.com", "Original Name");
    User saved = userRepository.save(user);

    saved.setDisplayName("Updated Name");
    userRepository.save(saved);

    Optional<User> found = userRepository.findById(saved.getId());
    assertThat(found).isPresent();
    assertThat(found.get().getDisplayName()).isEqualTo("Updated Name");
  }
}
