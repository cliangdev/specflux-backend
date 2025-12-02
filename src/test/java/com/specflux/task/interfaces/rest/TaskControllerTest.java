package com.specflux.task.interfaces.rest;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.specflux.api.generated.model.CreateTaskRequest;
import com.specflux.api.generated.model.UpdateTaskRequest;
import com.specflux.common.AbstractIntegrationTest;
import com.specflux.epic.domain.Epic;
import com.specflux.epic.domain.EpicRepository;
import com.specflux.project.domain.Project;
import com.specflux.project.domain.ProjectRepository;
import com.specflux.shared.application.CurrentUserService;
import com.specflux.task.domain.Task;
import com.specflux.task.domain.TaskPriority;
import com.specflux.task.domain.TaskRepository;
import com.specflux.task.domain.TaskStatus;
import com.specflux.user.domain.User;
import com.specflux.user.domain.UserRepository;

/**
 * Integration tests for TaskController.
 *
 * <p>Uses schema isolation for parallel test execution.
 */
@AutoConfigureMockMvc
@Transactional
class TaskControllerTest extends AbstractIntegrationTest {

  private static final String SCHEMA_NAME = "task_controller_test";

  @DynamicPropertySource
  static void configureSchema(DynamicPropertyRegistry registry) {
    AbstractIntegrationTest.configureSchema(registry, SCHEMA_NAME);
  }

  @Autowired private MockMvc mockMvc;
  @Autowired private UserRepository userRepository;
  @Autowired private ProjectRepository projectRepository;
  @Autowired private EpicRepository epicRepository;
  @Autowired private TaskRepository taskRepository;
  @MockitoBean private CurrentUserService currentUserService;

  private final ObjectMapper objectMapper = new ObjectMapper();

  private User testUser;
  private Project testProject;
  private Epic testEpic;

  @BeforeEach
  void setUp() {
    // Create test user
    testUser =
        userRepository.save(
            new User("user_task_test", "firebase_task", "task@example.com", "Task Test User"));
    // Create test project
    testProject =
        projectRepository.save(
            new Project("proj_task_test", "TASK", "Task Test Project", testUser));
    // Create test epic
    testEpic =
        epicRepository.save(
            new Epic("epic_task_test", testProject, 1, "TASK-E1", "Test Epic", testUser));
    // Mock current user service to return the test user
    when(currentUserService.getCurrentUser()).thenReturn(testUser);
  }

  @Test
  @WithMockUser(username = "user")
  void createTask_shouldReturnCreatedTask() throws Exception {
    CreateTaskRequest request = new CreateTaskRequest();
    request.setTitle("Implement authentication");
    request.setDescription("Add OAuth2 support");
    request.setPriority(com.specflux.api.generated.model.TaskPriority.HIGH);
    request.setRequiresApproval(true);

    mockMvc
        .perform(
            post("/projects/{projectRef}/tasks", testProject.getPublicId())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.publicId").exists())
        .andExpect(jsonPath("$.displayKey").value("TASK-1"))
        .andExpect(jsonPath("$.projectId").value(testProject.getPublicId()))
        .andExpect(jsonPath("$.title").value("Implement authentication"))
        .andExpect(jsonPath("$.description").value("Add OAuth2 support"))
        .andExpect(jsonPath("$.status").value("BACKLOG"))
        .andExpect(jsonPath("$.priority").value("HIGH"))
        .andExpect(jsonPath("$.requiresApproval").value(true))
        .andExpect(jsonPath("$.createdById").value(testUser.getPublicId()))
        .andExpect(jsonPath("$.createdAt").exists())
        .andExpect(jsonPath("$.updatedAt").exists());
  }

  @Test
  @WithMockUser(username = "user")
  void createTask_withEpic_shouldLinkToEpic() throws Exception {
    CreateTaskRequest request = new CreateTaskRequest();
    request.setTitle("Task with Epic");
    request.setEpicRef(testEpic.getPublicId());

    mockMvc
        .perform(
            post("/projects/{projectRef}/tasks", testProject.getPublicId())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.epicId").value(testEpic.getPublicId()))
        .andExpect(jsonPath("$.epicDisplayKey").value("TASK-E1"));
  }

  @Test
  @WithMockUser(username = "user")
  void createTask_projectNotFound_shouldReturn404() throws Exception {
    CreateTaskRequest request = new CreateTaskRequest();
    request.setTitle("Test Task");

    mockMvc
        .perform(
            post("/projects/{projectRef}/tasks", "nonexistent")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
        .andExpect(status().isNotFound())
        .andExpect(jsonPath("$.code").value("NOT_FOUND"));
  }

  @Test
  @WithMockUser(username = "user")
  void getTask_byPublicId_shouldReturnTask() throws Exception {
    Task task =
        taskRepository.save(
            new Task("task_test123", testProject, 1, "TASK-1", "Test Task", testUser));

    mockMvc
        .perform(
            get(
                "/projects/{projectRef}/tasks/{taskRef}",
                testProject.getPublicId(),
                "task_test123"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.publicId").value("task_test123"))
        .andExpect(jsonPath("$.displayKey").value("TASK-1"))
        .andExpect(jsonPath("$.title").value("Test Task"));
  }

  @Test
  @WithMockUser(username = "user")
  void getTask_byDisplayKey_shouldReturnTask() throws Exception {
    Task task =
        taskRepository.save(
            new Task("task_bykey", testProject, 42, "TASK-42", "Task By Key", testUser));

    mockMvc
        .perform(
            get("/projects/{projectRef}/tasks/{taskRef}", testProject.getProjectKey(), "TASK-42"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.publicId").value("task_bykey"))
        .andExpect(jsonPath("$.displayKey").value("TASK-42"));
  }

  @Test
  @WithMockUser(username = "user")
  void getTask_notFound_shouldReturn404() throws Exception {
    mockMvc
        .perform(
            get("/projects/{projectRef}/tasks/{taskRef}", testProject.getPublicId(), "nonexistent"))
        .andExpect(status().isNotFound())
        .andExpect(jsonPath("$.code").value("NOT_FOUND"));
  }

  @Test
  @WithMockUser(username = "user")
  void updateTask_shouldReturnUpdatedTask() throws Exception {
    Task task =
        taskRepository.save(
            new Task("task_update", testProject, 1, "TASK-1", "Original Title", testUser));

    UpdateTaskRequest request = new UpdateTaskRequest();
    request.setTitle("Updated Title");
    request.setDescription("New description");
    request.setStatus(com.specflux.api.generated.model.TaskStatus.IN_PROGRESS);
    request.setPriority(com.specflux.api.generated.model.TaskPriority.CRITICAL);

    mockMvc
        .perform(
            patch(
                    "/projects/{projectRef}/tasks/{taskRef}",
                    testProject.getPublicId(),
                    "task_update")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.title").value("Updated Title"))
        .andExpect(jsonPath("$.description").value("New description"))
        .andExpect(jsonPath("$.status").value("IN_PROGRESS"))
        .andExpect(jsonPath("$.priority").value("CRITICAL"));
  }

  @Test
  @WithMockUser(username = "user")
  void updateTask_partialUpdate_shouldOnlyUpdateProvidedFields() throws Exception {
    Task task = new Task("task_partial", testProject, 1, "TASK-1", "Original Title", testUser);
    task.setDescription("Original description");
    task.setPriority(TaskPriority.LOW);
    taskRepository.save(task);

    UpdateTaskRequest request = new UpdateTaskRequest();
    request.setTitle("New Title");
    // other fields not set

    mockMvc
        .perform(
            patch(
                    "/projects/{projectRef}/tasks/{taskRef}",
                    testProject.getPublicId(),
                    "task_partial")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.title").value("New Title"))
        .andExpect(jsonPath("$.description").value("Original description"))
        .andExpect(jsonPath("$.status").value("BACKLOG"))
        .andExpect(jsonPath("$.priority").value("LOW"));
  }

  @Test
  @WithMockUser(username = "user")
  void deleteTask_shouldReturn204() throws Exception {
    Task task =
        taskRepository.save(
            new Task("task_delete", testProject, 1, "TASK-1", "To Delete", testUser));

    mockMvc
        .perform(
            delete(
                "/projects/{projectRef}/tasks/{taskRef}", testProject.getPublicId(), "task_delete"))
        .andExpect(status().isNoContent());

    // Verify deletion
    mockMvc
        .perform(
            get("/projects/{projectRef}/tasks/{taskRef}", testProject.getPublicId(), "task_delete"))
        .andExpect(status().isNotFound());
  }

  @Test
  @WithMockUser(username = "user")
  void listTasks_shouldReturnPaginatedList() throws Exception {
    // Create test tasks
    taskRepository.save(new Task("task_list1", testProject, 1, "TASK-1", "Task 1", testUser));
    taskRepository.save(new Task("task_list2", testProject, 2, "TASK-2", "Task 2", testUser));
    taskRepository.save(new Task("task_list3", testProject, 3, "TASK-3", "Task 3", testUser));

    mockMvc
        .perform(get("/projects/{projectRef}/tasks", testProject.getPublicId()).param("limit", "2"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data").isArray())
        .andExpect(jsonPath("$.data.length()").value(2))
        .andExpect(jsonPath("$.pagination.total").value(3))
        .andExpect(jsonPath("$.pagination.hasMore").value(true))
        .andExpect(jsonPath("$.pagination.nextCursor").exists());
  }

  @Test
  @WithMockUser(username = "user")
  void listTasks_withStatusFilter_shouldReturnFilteredList() throws Exception {
    Task task1 = new Task("task_backlog", testProject, 1, "TASK-1", "Backlog Task", testUser);
    task1.setStatus(TaskStatus.BACKLOG);
    taskRepository.save(task1);

    Task task2 = new Task("task_prog", testProject, 2, "TASK-2", "In Progress Task", testUser);
    task2.setStatus(TaskStatus.IN_PROGRESS);
    taskRepository.save(task2);

    mockMvc
        .perform(
            get("/projects/{projectRef}/tasks", testProject.getPublicId())
                .param("status", "IN_PROGRESS"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data.length()").value(1))
        .andExpect(jsonPath("$.data[0].status").value("IN_PROGRESS"));
  }

  @Test
  @WithMockUser(username = "user")
  void listTasks_withPriorityFilter_shouldReturnFilteredList() throws Exception {
    Task task1 = new Task("task_low", testProject, 1, "TASK-1", "Low Priority", testUser);
    task1.setPriority(TaskPriority.LOW);
    taskRepository.save(task1);

    Task task2 = new Task("task_high", testProject, 2, "TASK-2", "High Priority", testUser);
    task2.setPriority(TaskPriority.HIGH);
    taskRepository.save(task2);

    mockMvc
        .perform(
            get("/projects/{projectRef}/tasks", testProject.getPublicId())
                .param("priority", "HIGH"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data.length()").value(1))
        .andExpect(jsonPath("$.data[0].priority").value("HIGH"));
  }

  @Test
  @WithMockUser(username = "user")
  void listTasks_withEpicFilter_shouldReturnFilteredList() throws Exception {
    Task task1 = new Task("task_epic", testProject, 1, "TASK-1", "Task with Epic", testUser);
    task1.setEpic(testEpic);
    taskRepository.save(task1);

    Task task2 = new Task("task_noepic", testProject, 2, "TASK-2", "Task without Epic", testUser);
    taskRepository.save(task2);

    mockMvc
        .perform(
            get("/projects/{projectRef}/tasks", testProject.getPublicId())
                .param("epicRef", testEpic.getPublicId()))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data.length()").value(1))
        .andExpect(jsonPath("$.data[0].epicId").value(testEpic.getPublicId()));
  }

  @Test
  @WithMockUser(username = "user")
  void listTasks_withSearch_shouldReturnMatchingTasks() throws Exception {
    taskRepository.save(
        new Task("task_auth", testProject, 1, "TASK-1", "Implement authentication", testUser));
    taskRepository.save(
        new Task("task_ui", testProject, 2, "TASK-2", "Build UI components", testUser));

    mockMvc
        .perform(
            get("/projects/{projectRef}/tasks", testProject.getPublicId()).param("search", "auth"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data.length()").value(1))
        .andExpect(jsonPath("$.data[0].title").value("Implement authentication"));
  }

  @Test
  @WithMockUser(username = "user")
  void listTasks_withSort_shouldReturnSortedList() throws Exception {
    taskRepository.save(new Task("task_z", testProject, 1, "TASK-1", "Zeta Task", testUser));
    taskRepository.save(new Task("task_a", testProject, 2, "TASK-2", "Alpha Task", testUser));

    mockMvc
        .perform(
            get("/projects/{projectRef}/tasks", testProject.getPublicId())
                .param("sort", "title")
                .param("order", "asc"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data[0].title").value("Alpha Task"))
        .andExpect(jsonPath("$.data[1].title").value("Zeta Task"));
  }

  @Test
  @WithMockUser(username = "user")
  void listTasks_emptyProject_shouldReturnEmptyList() throws Exception {
    mockMvc
        .perform(get("/projects/{projectRef}/tasks", testProject.getPublicId()))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data").isArray())
        .andExpect(jsonPath("$.data.length()").value(0))
        .andExpect(jsonPath("$.pagination.total").value(0))
        .andExpect(jsonPath("$.pagination.hasMore").value(false));
  }

  @Test
  @WithMockUser(username = "user")
  void createTask_sequentialNumbers_shouldIncrementCorrectly() throws Exception {
    // Create first task
    CreateTaskRequest request1 = new CreateTaskRequest();
    request1.setTitle("First Task");
    mockMvc
        .perform(
            post("/projects/{projectRef}/tasks", testProject.getPublicId())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request1)))
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.displayKey").value("TASK-1"));

    // Create second task
    CreateTaskRequest request2 = new CreateTaskRequest();
    request2.setTitle("Second Task");
    mockMvc
        .perform(
            post("/projects/{projectRef}/tasks", testProject.getPublicId())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request2)))
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.displayKey").value("TASK-2"));
  }
}
