package com.specflux.epic.domain;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.transaction.annotation.Transactional;

import com.specflux.common.AbstractIntegrationTest;
import com.specflux.project.domain.Project;
import com.specflux.project.domain.ProjectRepository;
import com.specflux.user.domain.User;
import com.specflux.user.domain.UserRepository;

/**
 * Integration tests for EpicRepository.
 *
 * <p>Uses schema isolation (epic_repository_test) for parallel test execution.
 */
@Transactional
class EpicRepositoryTest extends AbstractIntegrationTest {

  @DynamicPropertySource
  static void configureSchema(DynamicPropertyRegistry registry) {
    configureSchemaForClass(registry, EpicRepositoryTest.class);
  }

  @Autowired private EpicRepository epicRepository;
  @Autowired private ProjectRepository projectRepository;
  @Autowired private UserRepository userRepository;

  private User testUser;
  private Project testProject;

  @BeforeEach
  void setUp() {
    testUser =
        userRepository.save(
            new User("user_epic_owner", "firebase_epic", "epic@example.com", "Epic Owner"));
    testProject =
        projectRepository.save(new Project("proj_epic_test", "EPIC", "Epic Project", testUser));
  }

  @Test
  void shouldSaveAndRetrieveEpic() {
    Epic epic = new Epic("epic_abc123", testProject, 1, "EPIC-1", "First Epic", testUser);
    epic.setDescription("Epic description");
    epic.setTargetDate(LocalDate.of(2025, 6, 30));

    Epic saved = epicRepository.save(epic);

    assertThat(saved.getId()).isNotNull();
    assertThat(saved.getPublicId()).isEqualTo("epic_abc123");
    assertThat(saved.getProject().getId()).isEqualTo(testProject.getId());
    assertThat(saved.getSequenceNumber()).isEqualTo(1);
    assertThat(saved.getDisplayKey()).isEqualTo("EPIC-1");
    assertThat(saved.getTitle()).isEqualTo("First Epic");
    assertThat(saved.getDescription()).isEqualTo("Epic description");
    assertThat(saved.getStatus()).isEqualTo(EpicStatus.PLANNING);
    assertThat(saved.getTargetDate()).isEqualTo(LocalDate.of(2025, 6, 30));
    assertThat(saved.getCreatedBy().getId()).isEqualTo(testUser.getId());
    assertThat(saved.getCreatedAt()).isNotNull();
    assertThat(saved.getUpdatedAt()).isNotNull();
  }

  @Test
  void shouldFindByPublicId() {
    Epic epic = new Epic("epic_def456", testProject, 2, "EPIC-2", "Second Epic", testUser);
    epicRepository.save(epic);

    Optional<Epic> found = epicRepository.findByPublicId("epic_def456");

    assertThat(found).isPresent();
    assertThat(found.get().getTitle()).isEqualTo("Second Epic");
  }

  @Test
  void shouldFindByProjectIdAndDisplayKey() {
    Epic epic = new Epic("epic_ghi789", testProject, 3, "EPIC-3", "Third Epic", testUser);
    epicRepository.save(epic);

    Optional<Epic> found =
        epicRepository.findByProjectIdAndDisplayKey(testProject.getId(), "EPIC-3");

    assertThat(found).isPresent();
    assertThat(found.get().getPublicId()).isEqualTo("epic_ghi789");
  }

  @Test
  void shouldFindByProjectId() {
    Epic epic1 = new Epic("epic_001", testProject, 1, "EPIC-1", "Epic One", testUser);
    Epic epic2 = new Epic("epic_002", testProject, 2, "EPIC-2", "Epic Two", testUser);
    epicRepository.save(epic1);
    epicRepository.save(epic2);

    List<Epic> epics = epicRepository.findByProjectId(testProject.getId());

    assertThat(epics).hasSize(2);
    assertThat(epics).extracting(Epic::getDisplayKey).containsExactlyInAnyOrder("EPIC-1", "EPIC-2");
  }

  @Test
  void shouldFindByProjectIdAndStatus() {
    Epic epic1 = new Epic("epic_s1", testProject, 1, "EPIC-S1", "Planning Epic", testUser);
    Epic epic2 = new Epic("epic_s2", testProject, 2, "EPIC-S2", "In Progress Epic", testUser);
    epic2.setStatus(EpicStatus.IN_PROGRESS);
    epicRepository.save(epic1);
    epicRepository.save(epic2);

    List<Epic> planningEpics =
        epicRepository.findByProjectIdAndStatus(testProject.getId(), EpicStatus.PLANNING);

    assertThat(planningEpics).hasSize(1);
    assertThat(planningEpics.get(0).getDisplayKey()).isEqualTo("EPIC-S1");
  }

  @Test
  void shouldUpdateEpicStatus() {
    Epic epic = new Epic("epic_upd", testProject, 1, "EPIC-UPD", "Update Epic", testUser);
    Epic saved = epicRepository.save(epic);

    saved.setStatus(EpicStatus.IN_PROGRESS);
    epicRepository.save(saved);

    Optional<Epic> found = epicRepository.findById(saved.getId());
    assertThat(found).isPresent();
    assertThat(found.get().getStatus()).isEqualTo(EpicStatus.IN_PROGRESS);
  }
}
