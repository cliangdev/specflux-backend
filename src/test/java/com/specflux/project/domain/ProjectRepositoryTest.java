package com.specflux.project.domain;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.transaction.annotation.Transactional;

import com.specflux.common.AbstractIntegrationTest;
import com.specflux.user.domain.User;
import com.specflux.user.domain.UserRepository;

/**
 * Integration tests for ProjectRepository.
 *
 * <p>Uses schema isolation (project_repository_test) for parallel test execution.
 */
@Transactional
class ProjectRepositoryTest extends AbstractIntegrationTest {

  private static final String SCHEMA_NAME = "project_repository_test";

  @DynamicPropertySource
  static void configureSchema(DynamicPropertyRegistry registry) {
    AbstractIntegrationTest.configureSchema(registry, SCHEMA_NAME);
  }

  @Autowired private ProjectRepository projectRepository;
  @Autowired private UserRepository userRepository;

  private User testUser;

  @BeforeEach
  void setUp() {
    testUser =
        userRepository.save(
            new User("user_owner123", "firebase_owner", "owner@example.com", "Project Owner"));
  }

  @Test
  void shouldSaveAndRetrieveProject() {
    Project project = new Project("proj_abc123", "SPEC", "SpecFlux", testUser);
    project.setDescription("A project management tool");

    Project saved = projectRepository.save(project);

    assertThat(saved.getId()).isNotNull();
    assertThat(saved.getPublicId()).isEqualTo("proj_abc123");
    assertThat(saved.getProjectKey()).isEqualTo("SPEC");
    assertThat(saved.getName()).isEqualTo("SpecFlux");
    assertThat(saved.getDescription()).isEqualTo("A project management tool");
    assertThat(saved.getOwner().getId()).isEqualTo(testUser.getId());
    assertThat(saved.getEpicSequence()).isEqualTo(0);
    assertThat(saved.getTaskSequence()).isEqualTo(0);
    assertThat(saved.getReleaseSequence()).isEqualTo(0);
    assertThat(saved.getCreatedAt()).isNotNull();
    assertThat(saved.getUpdatedAt()).isNotNull();
  }

  @Test
  void shouldFindByPublicId() {
    Project project = new Project("proj_def456", "TEST", "Test Project", testUser);
    projectRepository.save(project);

    Optional<Project> found = projectRepository.findByPublicId("proj_def456");

    assertThat(found).isPresent();
    assertThat(found.get().getName()).isEqualTo("Test Project");
  }

  @Test
  void shouldFindByProjectKey() {
    Project project = new Project("proj_ghi789", "DEMO", "Demo Project", testUser);
    projectRepository.save(project);

    Optional<Project> found = projectRepository.findByProjectKey("DEMO");

    assertThat(found).isPresent();
    assertThat(found.get().getPublicId()).isEqualTo("proj_ghi789");
  }

  @Test
  void shouldFindByOwnerId() {
    User anotherUser =
        userRepository.save(
            new User("user_other456", "firebase_other", "other@example.com", "Other User"));

    Project project1 = new Project("proj_001", "PRJ1", "Project One", testUser);
    Project project2 = new Project("proj_002", "PRJ2", "Project Two", testUser);
    Project project3 = new Project("proj_003", "PRJ3", "Project Three", anotherUser);

    projectRepository.save(project1);
    projectRepository.save(project2);
    projectRepository.save(project3);

    List<Project> ownerProjects = projectRepository.findByOwnerId(testUser.getId());

    assertThat(ownerProjects).hasSize(2);
    assertThat(ownerProjects)
        .extracting(Project::getProjectKey)
        .containsExactlyInAnyOrder("PRJ1", "PRJ2");
  }

  @Test
  void shouldCheckExistsByProjectKey() {
    Project project = new Project("proj_jkl012", "EXIST", "Existing Project", testUser);
    projectRepository.save(project);

    assertThat(projectRepository.existsByProjectKey("EXIST")).isTrue();
    assertThat(projectRepository.existsByProjectKey("NOPE")).isFalse();
  }

  @Test
  void shouldUpdateProjectName() {
    Project project = new Project("proj_mno345", "UPD", "Original Name", testUser);
    Project saved = projectRepository.save(project);

    saved.setName("Updated Name");
    projectRepository.save(saved);

    Optional<Project> found = projectRepository.findById(saved.getId());
    assertThat(found).isPresent();
    assertThat(found.get().getName()).isEqualTo("Updated Name");
  }

  @Test
  void shouldIncrementSequences() {
    Project project = new Project("proj_pqr678", "SEQ", "Sequence Project", testUser);
    Project saved = projectRepository.save(project);

    assertThat(saved.nextEpicSequence()).isEqualTo(1);
    assertThat(saved.nextEpicSequence()).isEqualTo(2);
    assertThat(saved.nextTaskSequence()).isEqualTo(1);
    assertThat(saved.nextReleaseSequence()).isEqualTo(1);

    projectRepository.save(saved);

    Optional<Project> found = projectRepository.findById(saved.getId());
    assertThat(found).isPresent();
    assertThat(found.get().getEpicSequence()).isEqualTo(2);
    assertThat(found.get().getTaskSequence()).isEqualTo(1);
    assertThat(found.get().getReleaseSequence()).isEqualTo(1);
  }
}
