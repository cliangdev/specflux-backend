package com.specflux.task.domain;

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
import com.specflux.epic.domain.Epic;
import com.specflux.epic.domain.EpicRepository;
import com.specflux.project.domain.Project;
import com.specflux.project.domain.ProjectRepository;
import com.specflux.user.domain.User;
import com.specflux.user.domain.UserRepository;

/**
 * Integration tests for TaskRepository.
 *
 * <p>Uses schema isolation (task_repository_test) for parallel test execution.
 */
@Transactional
class TaskRepositoryTest extends AbstractIntegrationTest {

  private static final String SCHEMA_NAME = "task_repository_test";

  @DynamicPropertySource
  static void configureSchema(DynamicPropertyRegistry registry) {
    AbstractIntegrationTest.configureSchema(registry, SCHEMA_NAME);
  }

  @Autowired private TaskRepository taskRepository;
  @Autowired private EpicRepository epicRepository;
  @Autowired private ProjectRepository projectRepository;
  @Autowired private UserRepository userRepository;

  private User testUser;
  private User assignee;
  private Project testProject;
  private Epic testEpic;

  @BeforeEach
  void setUp() {
    testUser =
        userRepository.save(
            new User("user_task_owner", "firebase_task", "task@example.com", "Task Owner"));
    assignee =
        userRepository.save(
            new User("user_assignee", "firebase_assignee", "assignee@example.com", "Assignee"));
    testProject =
        projectRepository.save(new Project("proj_task_test", "TASK", "Task Project", testUser));
    testEpic =
        epicRepository.save(
            new Epic("epic_task_test", testProject, 1, "TASK-E1", "Test Epic", testUser));
  }

  @Test
  void shouldSaveAndRetrieveTask() {
    Task task = new Task("task_abc123", testProject, 1, "TASK-1", "First Task", testUser);
    task.setDescription("Task description");
    task.setEpic(testEpic);
    task.setPriority(TaskPriority.HIGH);
    task.setEstimatedDuration(120);

    Task saved = taskRepository.save(task);

    assertThat(saved.getId()).isNotNull();
    assertThat(saved.getPublicId()).isEqualTo("task_abc123");
    assertThat(saved.getProject().getId()).isEqualTo(testProject.getId());
    assertThat(saved.getEpic().getId()).isEqualTo(testEpic.getId());
    assertThat(saved.getSequenceNumber()).isEqualTo(1);
    assertThat(saved.getDisplayKey()).isEqualTo("TASK-1");
    assertThat(saved.getTitle()).isEqualTo("First Task");
    assertThat(saved.getDescription()).isEqualTo("Task description");
    assertThat(saved.getStatus()).isEqualTo(TaskStatus.BACKLOG);
    assertThat(saved.getPriority()).isEqualTo(TaskPriority.HIGH);
    assertThat(saved.getRequiresApproval()).isTrue();
    assertThat(saved.getEstimatedDuration()).isEqualTo(120);
    assertThat(saved.getCreatedBy().getId()).isEqualTo(testUser.getId());
    assertThat(saved.getCreatedAt()).isNotNull();
  }

  @Test
  void shouldFindByPublicId() {
    Task task = new Task("task_def456", testProject, 2, "TASK-2", "Second Task", testUser);
    taskRepository.save(task);

    Optional<Task> found = taskRepository.findByPublicId("task_def456");

    assertThat(found).isPresent();
    assertThat(found.get().getTitle()).isEqualTo("Second Task");
  }

  @Test
  void shouldFindByProjectIdAndDisplayKey() {
    Task task = new Task("task_ghi789", testProject, 3, "TASK-3", "Third Task", testUser);
    taskRepository.save(task);

    Optional<Task> found =
        taskRepository.findByProjectIdAndDisplayKey(testProject.getId(), "TASK-3");

    assertThat(found).isPresent();
    assertThat(found.get().getPublicId()).isEqualTo("task_ghi789");
  }

  @Test
  void shouldFindByProjectId() {
    Task task1 = new Task("task_001", testProject, 1, "TASK-1", "Task One", testUser);
    Task task2 = new Task("task_002", testProject, 2, "TASK-2", "Task Two", testUser);
    taskRepository.save(task1);
    taskRepository.save(task2);

    List<Task> tasks = taskRepository.findByProjectId(testProject.getId());

    assertThat(tasks).hasSize(2);
    assertThat(tasks).extracting(Task::getDisplayKey).containsExactlyInAnyOrder("TASK-1", "TASK-2");
  }

  @Test
  void shouldFindByProjectIdAndStatus() {
    Task task1 = new Task("task_s1", testProject, 1, "TASK-S1", "Backlog Task", testUser);
    Task task2 = new Task("task_s2", testProject, 2, "TASK-S2", "In Progress Task", testUser);
    task2.setStatus(TaskStatus.IN_PROGRESS);
    taskRepository.save(task1);
    taskRepository.save(task2);

    List<Task> backlogTasks =
        taskRepository.findByProjectIdAndStatus(testProject.getId(), TaskStatus.BACKLOG);

    assertThat(backlogTasks).hasSize(1);
    assertThat(backlogTasks.get(0).getDisplayKey()).isEqualTo("TASK-S1");
  }

  @Test
  void shouldFindByEpicId() {
    Task task1 = new Task("task_e1", testProject, 1, "TASK-E1", "Epic Task 1", testUser);
    Task task2 = new Task("task_e2", testProject, 2, "TASK-E2", "Epic Task 2", testUser);
    Task task3 = new Task("task_e3", testProject, 3, "TASK-E3", "No Epic Task", testUser);
    task1.setEpic(testEpic);
    task2.setEpic(testEpic);
    taskRepository.save(task1);
    taskRepository.save(task2);
    taskRepository.save(task3);

    List<Task> epicTasks = taskRepository.findByEpicId(testEpic.getId());

    assertThat(epicTasks).hasSize(2);
    assertThat(epicTasks)
        .extracting(Task::getDisplayKey)
        .containsExactlyInAnyOrder("TASK-E1", "TASK-E2");
  }

  @Test
  void shouldFindByAssignedToId() {
    Task task1 = new Task("task_a1", testProject, 1, "TASK-A1", "Assigned Task", testUser);
    Task task2 = new Task("task_a2", testProject, 2, "TASK-A2", "Unassigned Task", testUser);
    task1.setAssignedTo(assignee);
    taskRepository.save(task1);
    taskRepository.save(task2);

    List<Task> assignedTasks = taskRepository.findByAssignedToId(assignee.getId());

    assertThat(assignedTasks).hasSize(1);
    assertThat(assignedTasks.get(0).getDisplayKey()).isEqualTo("TASK-A1");
  }

  @Test
  void shouldUpdateTaskStatus() {
    Task task = new Task("task_upd", testProject, 1, "TASK-UPD", "Update Task", testUser);
    Task saved = taskRepository.save(task);

    saved.setStatus(TaskStatus.IN_PROGRESS);
    saved.setGithubPrUrl("https://github.com/org/repo/pull/123");
    taskRepository.save(saved);

    Optional<Task> found = taskRepository.findById(saved.getId());
    assertThat(found).isPresent();
    assertThat(found.get().getStatus()).isEqualTo(TaskStatus.IN_PROGRESS);
    assertThat(found.get().getGithubPrUrl()).isEqualTo("https://github.com/org/repo/pull/123");
  }
}
