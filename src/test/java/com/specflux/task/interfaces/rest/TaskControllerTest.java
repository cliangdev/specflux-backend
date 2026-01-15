package com.specflux.task.interfaces.rest;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

import com.specflux.api.generated.model.AddTaskDependencyRequestDto;
import com.specflux.api.generated.model.CreateAcceptanceCriteriaRequestDto;
import com.specflux.api.generated.model.CreateTaskRequestDto;
import com.specflux.api.generated.model.TaskPriorityDto;
import com.specflux.api.generated.model.TaskStatusDto;
import com.specflux.api.generated.model.UpdateAcceptanceCriteriaRequestDto;
import com.specflux.api.generated.model.UpdateTaskRequestDto;
import com.specflux.common.AbstractControllerIntegrationTest;
import com.specflux.epic.domain.Epic;
import com.specflux.epic.domain.EpicRepository;
import com.specflux.prd.domain.Prd;
import com.specflux.prd.domain.PrdRepository;
import com.specflux.project.domain.Project;
import com.specflux.project.domain.ProjectRepository;
import com.specflux.task.domain.Task;
import com.specflux.task.domain.TaskPriority;
import com.specflux.task.domain.TaskRepository;
import com.specflux.task.domain.TaskStatus;

/**
 * Integration tests for TaskController.
 *
 * <p>Uses schema isolation for parallel test execution.
 */
class TaskControllerTest extends AbstractControllerIntegrationTest {

  @DynamicPropertySource
  static void configureSchema(DynamicPropertyRegistry registry) {
    configureSchemaForClass(registry, TaskControllerTest.class);
  }

  @Autowired private ProjectRepository projectRepository;
  @Autowired private EpicRepository epicRepository;
  @Autowired private PrdRepository prdRepository;
  @Autowired private TaskRepository taskRepository;

  private Project testProject;
  private Epic testEpic;

  @BeforeEach
  void setUpProjectAndEpic() {
    testProject =
        projectRepository.save(
            new Project("proj_task_test", "TASK", "Task Test Project", testUser));
    testEpic =
        epicRepository.save(
            new Epic("epic_task_test", testProject, 1, "TASK-E1", "Test Epic", testUser));
  }

  @Test
  void createTask_shouldReturnCreatedTask() throws Exception {
    CreateTaskRequestDto request = new CreateTaskRequestDto();
    request.setTitle("Implement authentication");
    request.setDescription("Add OAuth2 support");
    request.setPriority(TaskPriorityDto.HIGH);
    request.setRequiresApproval(true);

    mockMvc
        .perform(
            post("/api/projects/{projectRef}/tasks", testProject.getPublicId())
                .with(user("user"))
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.id").exists())
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
  void createTask_withEpic_shouldLinkToEpic() throws Exception {
    CreateTaskRequestDto request = new CreateTaskRequestDto();
    request.setTitle("Task with Epic");
    request.setEpicRef(testEpic.getPublicId());

    mockMvc
        .perform(
            post("/api/projects/{projectRef}/tasks", testProject.getPublicId())
                .with(user("user"))
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.epicId").value(testEpic.getPublicId()))
        .andExpect(jsonPath("$.epicDisplayKey").value("TASK-E1"));
  }

  @Test
  void createTask_projectNotFound_shouldReturn404() throws Exception {
    CreateTaskRequestDto request = new CreateTaskRequestDto();
    request.setTitle("Test Task");

    mockMvc
        .perform(
            post("/api/projects/{projectRef}/tasks", "nonexistent")
                .with(user("user"))
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
        .andExpect(status().isNotFound())
        .andExpect(jsonPath("$.code").value("NOT_FOUND"));
  }

  @Test
  void getTask_byPublicId_shouldReturnTask() throws Exception {
    Task task =
        taskRepository.save(
            new Task("task_test123", testProject, 1, "TASK-1", "Test Task", testUser));

    mockMvc
        .perform(
            get(
                    "/api/projects/{projectRef}/tasks/{taskRef}",
                    testProject.getPublicId(),
                    "task_test123")
                .with(user("user")))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.id").value("task_test123"))
        .andExpect(jsonPath("$.displayKey").value("TASK-1"))
        .andExpect(jsonPath("$.title").value("Test Task"));
  }

  @Test
  void getTask_byDisplayKey_shouldReturnTask() throws Exception {
    Task task =
        taskRepository.save(
            new Task("task_bykey", testProject, 42, "TASK-42", "Task By Key", testUser));

    mockMvc
        .perform(
            get(
                    "/api/projects/{projectRef}/tasks/{taskRef}",
                    testProject.getProjectKey(),
                    "TASK-42")
                .with(user("user")))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.id").value("task_bykey"))
        .andExpect(jsonPath("$.displayKey").value("TASK-42"));
  }

  @Test
  void getTask_notFound_shouldReturn404() throws Exception {
    mockMvc
        .perform(
            get(
                    "/api/projects/{projectRef}/tasks/{taskRef}",
                    testProject.getPublicId(),
                    "nonexistent")
                .with(user("user")))
        .andExpect(status().isNotFound())
        .andExpect(jsonPath("$.code").value("NOT_FOUND"));
  }

  @Test
  void updateTask_shouldReturnUpdatedTask() throws Exception {
    Task task =
        taskRepository.save(
            new Task("task_update", testProject, 1, "TASK-1", "Original Title", testUser));

    UpdateTaskRequestDto request = new UpdateTaskRequestDto();
    request.setTitle("Updated Title");
    request.setDescription("New description");
    request.setStatus(TaskStatusDto.IN_PROGRESS);
    request.setPriority(TaskPriorityDto.CRITICAL);

    mockMvc
        .perform(
            patch(
                    "/api/projects/{projectRef}/tasks/{taskRef}",
                    testProject.getPublicId(),
                    "task_update")
                .with(user("user"))
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.title").value("Updated Title"))
        .andExpect(jsonPath("$.description").value("New description"))
        .andExpect(jsonPath("$.status").value("IN_PROGRESS"))
        .andExpect(jsonPath("$.priority").value("CRITICAL"));
  }

  @Test
  void updateTask_partialUpdate_shouldOnlyUpdateProvidedFields() throws Exception {
    Task task = new Task("task_partial", testProject, 1, "TASK-1", "Original Title", testUser);
    task.setDescription("Original description");
    task.setPriority(TaskPriority.LOW);
    taskRepository.save(task);

    UpdateTaskRequestDto request = new UpdateTaskRequestDto();
    request.setTitle("New Title");
    // other fields not set

    mockMvc
        .perform(
            patch(
                    "/api/projects/{projectRef}/tasks/{taskRef}",
                    testProject.getPublicId(),
                    "task_partial")
                .with(user("user"))
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.title").value("New Title"))
        .andExpect(jsonPath("$.description").value("Original description"))
        .andExpect(jsonPath("$.status").value("BACKLOG"))
        .andExpect(jsonPath("$.priority").value("LOW"));
  }

  @Test
  void deleteTask_shouldReturn204() throws Exception {
    Task task =
        taskRepository.save(
            new Task("task_delete", testProject, 1, "TASK-1", "To Delete", testUser));

    mockMvc
        .perform(
            delete(
                    "/api/projects/{projectRef}/tasks/{taskRef}",
                    testProject.getPublicId(),
                    "task_delete")
                .with(user("user")))
        .andExpect(status().isNoContent());

    // Verify deletion
    mockMvc
        .perform(
            get(
                    "/api/projects/{projectRef}/tasks/{taskRef}",
                    testProject.getPublicId(),
                    "task_delete")
                .with(user("user")))
        .andExpect(status().isNotFound());
  }

  @Test
  void listTasks_shouldReturnPaginatedList() throws Exception {
    // Create test tasks
    taskRepository.save(new Task("task_list1", testProject, 1, "TASK-1", "Task 1", testUser));
    taskRepository.save(new Task("task_list2", testProject, 2, "TASK-2", "Task 2", testUser));
    taskRepository.save(new Task("task_list3", testProject, 3, "TASK-3", "Task 3", testUser));

    mockMvc
        .perform(
            get("/api/projects/{projectRef}/tasks", testProject.getPublicId())
                .with(user("user"))
                .param("limit", "2"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data").isArray())
        .andExpect(jsonPath("$.data.length()").value(2))
        .andExpect(jsonPath("$.pagination.total").value(3))
        .andExpect(jsonPath("$.pagination.hasMore").value(true))
        .andExpect(jsonPath("$.pagination.nextCursor").exists());
  }

  @Test
  void listTasks_withStatusFilter_shouldReturnFilteredList() throws Exception {
    Task task1 = new Task("task_backlog", testProject, 1, "TASK-1", "Backlog Task", testUser);
    task1.setStatus(TaskStatus.BACKLOG);
    taskRepository.save(task1);

    Task task2 = new Task("task_prog", testProject, 2, "TASK-2", "In Progress Task", testUser);
    task2.setStatus(TaskStatus.IN_PROGRESS);
    taskRepository.save(task2);

    mockMvc
        .perform(
            get("/api/projects/{projectRef}/tasks", testProject.getPublicId())
                .with(user("user"))
                .param("status", "IN_PROGRESS"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data.length()").value(1))
        .andExpect(jsonPath("$.data[0].status").value("IN_PROGRESS"));
  }

  @Test
  void listTasks_withPriorityFilter_shouldReturnFilteredList() throws Exception {
    Task task1 = new Task("task_low", testProject, 1, "TASK-1", "Low Priority", testUser);
    task1.setPriority(TaskPriority.LOW);
    taskRepository.save(task1);

    Task task2 = new Task("task_high", testProject, 2, "TASK-2", "High Priority", testUser);
    task2.setPriority(TaskPriority.HIGH);
    taskRepository.save(task2);

    mockMvc
        .perform(
            get("/api/projects/{projectRef}/tasks", testProject.getPublicId())
                .with(user("user"))
                .param("priority", "HIGH"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data.length()").value(1))
        .andExpect(jsonPath("$.data[0].priority").value("HIGH"));
  }

  @Test
  void listTasks_withEpicFilter_shouldReturnFilteredList() throws Exception {
    Task task1 = new Task("task_epic", testProject, 1, "TASK-1", "Task with Epic", testUser);
    task1.setEpic(testEpic);
    taskRepository.save(task1);

    Task task2 = new Task("task_noepic", testProject, 2, "TASK-2", "Task without Epic", testUser);
    taskRepository.save(task2);

    mockMvc
        .perform(
            get("/api/projects/{projectRef}/tasks", testProject.getPublicId())
                .with(user("user"))
                .param("epicRef", testEpic.getPublicId()))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data.length()").value(1))
        .andExpect(jsonPath("$.data[0].epicId").value(testEpic.getPublicId()));
  }

  @Test
  void listTasks_withSearch_shouldReturnMatchingTasks() throws Exception {
    taskRepository.save(
        new Task("task_auth", testProject, 1, "TASK-1", "Implement authentication", testUser));
    taskRepository.save(
        new Task("task_ui", testProject, 2, "TASK-2", "Build UI components", testUser));

    mockMvc
        .perform(
            get("/api/projects/{projectRef}/tasks", testProject.getPublicId())
                .with(user("user"))
                .param("search", "auth"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data.length()").value(1))
        .andExpect(jsonPath("$.data[0].title").value("Implement authentication"));
  }

  @Test
  void listTasks_withSort_shouldReturnSortedList() throws Exception {
    taskRepository.save(new Task("task_z", testProject, 1, "TASK-1", "Zeta Task", testUser));
    taskRepository.save(new Task("task_a", testProject, 2, "TASK-2", "Alpha Task", testUser));

    mockMvc
        .perform(
            get("/api/projects/{projectRef}/tasks", testProject.getPublicId())
                .with(user("user"))
                .param("sort", "title")
                .param("order", "asc"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data[0].title").value("Alpha Task"))
        .andExpect(jsonPath("$.data[1].title").value("Zeta Task"));
  }

  @Test
  void listTasks_emptyProject_shouldReturnEmptyList() throws Exception {
    mockMvc
        .perform(
            get("/api/projects/{projectRef}/tasks", testProject.getPublicId()).with(user("user")))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data").isArray())
        .andExpect(jsonPath("$.data.length()").value(0))
        .andExpect(jsonPath("$.pagination.total").value(0))
        .andExpect(jsonPath("$.pagination.hasMore").value(false));
  }

  @Test
  void listTasks_withPrdTagFilter_shouldReturnTasksFromMatchingPrdOnly() throws Exception {
    // Create PRD with tag
    Prd prd =
        new Prd(
            "prd_tagged",
            testProject,
            1,
            "TASK-P1",
            "Tagged PRD",
            ".specflux/prds/tagged",
            testUser);
    prd.setTag("mvp-phase1");
    prdRepository.save(prd);

    // Create epic linked to PRD
    Epic epicWithPrd =
        new Epic("epic_withprd", testProject, 2, "TASK-E2", "Epic with PRD", testUser);
    epicWithPrd.setPrdId(prd.getId());
    epicRepository.save(epicWithPrd);

    // Create task linked to epic with PRD
    Task taskWithPrd =
        new Task("task_withprd", testProject, 1, "TASK-1", "Task with PRD Tag", testUser);
    taskWithPrd.setEpic(epicWithPrd);
    taskRepository.save(taskWithPrd);

    // Create task without PRD link
    Task taskNoPrd = new Task("task_noprd", testProject, 2, "TASK-2", "Task without PRD", testUser);
    taskNoPrd.setEpic(testEpic); // testEpic has no PRD
    taskRepository.save(taskNoPrd);

    // Filter by prdTag should return only taskWithPrd
    mockMvc
        .perform(
            get("/api/projects/{projectRef}/tasks", testProject.getPublicId())
                .with(user("user"))
                .param("prdTag", "mvp-phase1"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data.length()").value(1))
        .andExpect(jsonPath("$.data[0].id").value("task_withprd"));
  }

  @Test
  void listTasks_withStatusNotFilter_shouldExcludeMatchingStatuses() throws Exception {
    Task task1 = new Task("task_backlog2", testProject, 1, "TASK-1", "Backlog Task", testUser);
    task1.setStatus(TaskStatus.BACKLOG);
    taskRepository.save(task1);

    Task task2 = new Task("task_completed", testProject, 2, "TASK-2", "Completed Task", testUser);
    task2.setStatus(TaskStatus.COMPLETED);
    taskRepository.save(task2);

    Task task3 = new Task("task_cancelled", testProject, 3, "TASK-3", "Cancelled Task", testUser);
    task3.setStatus(TaskStatus.CANCELLED);
    taskRepository.save(task3);

    Task task4 = new Task("task_inprog2", testProject, 4, "TASK-4", "In Progress Task", testUser);
    task4.setStatus(TaskStatus.IN_PROGRESS);
    taskRepository.save(task4);

    // Filter out COMPLETED and CANCELLED
    mockMvc
        .perform(
            get("/api/projects/{projectRef}/tasks", testProject.getPublicId())
                .with(user("user"))
                .param("statusNot", "COMPLETED,CANCELLED"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data.length()").value(2))
        .andExpect(jsonPath("$.data[?(@.id=='task_backlog2')]").exists())
        .andExpect(jsonPath("$.data[?(@.id=='task_inprog2')]").exists())
        .andExpect(jsonPath("$.data[?(@.id=='task_completed')]").doesNotExist())
        .andExpect(jsonPath("$.data[?(@.id=='task_cancelled')]").doesNotExist());
  }

  @Test
  void listTasks_withPrdTagAndStatusNot_shouldCombineFilters() throws Exception {
    // Create PRD with tag
    Prd prd =
        new Prd(
            "prd_combo", testProject, 1, "TASK-P2", "Combo PRD", ".specflux/prds/combo", testUser);
    prd.setTag("combo-tag");
    prdRepository.save(prd);

    // Create epic linked to PRD
    Epic epicWithPrd = new Epic("epic_combo", testProject, 3, "TASK-E3", "Epic Combo", testUser);
    epicWithPrd.setPrdId(prd.getId());
    epicRepository.save(epicWithPrd);

    // Create tasks with various statuses linked to PRD
    Task task1 = new Task("task_combo1", testProject, 1, "TASK-1", "Combo Task 1", testUser);
    task1.setEpic(epicWithPrd);
    task1.setStatus(TaskStatus.BACKLOG);
    taskRepository.save(task1);

    Task task2 = new Task("task_combo2", testProject, 2, "TASK-2", "Combo Task 2", testUser);
    task2.setEpic(epicWithPrd);
    task2.setStatus(TaskStatus.COMPLETED);
    taskRepository.save(task2);

    // Task not linked to PRD with combo-tag
    Task task3 = new Task("task_other", testProject, 3, "TASK-3", "Other Task", testUser);
    task3.setStatus(TaskStatus.BACKLOG);
    taskRepository.save(task3);

    // Filter by prdTag=combo-tag AND statusNot=COMPLETED should return only task_combo1
    mockMvc
        .perform(
            get("/api/projects/{projectRef}/tasks", testProject.getPublicId())
                .with(user("user"))
                .param("prdTag", "combo-tag")
                .param("statusNot", "COMPLETED"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data.length()").value(1))
        .andExpect(jsonPath("$.data[0].id").value("task_combo1"));
  }

  @Test
  void createTask_sequentialNumbers_shouldIncrementCorrectly() throws Exception {
    // Create first task
    CreateTaskRequestDto request1 = new CreateTaskRequestDto();
    request1.setTitle("First Task");
    mockMvc
        .perform(
            post("/api/projects/{projectRef}/tasks", testProject.getPublicId())
                .with(user("user"))
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request1)))
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.displayKey").value("TASK-1"));

    // Create second task
    CreateTaskRequestDto request2 = new CreateTaskRequestDto();
    request2.setTitle("Second Task");
    mockMvc
        .perform(
            post("/api/projects/{projectRef}/tasks", testProject.getPublicId())
                .with(user("user"))
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request2)))
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.displayKey").value("TASK-2"));
  }

  @Test
  void listTasks_withoutAuth_shouldReturn403() throws Exception {
    mockMvc
        .perform(get("/api/projects/{projectRef}/tasks", testProject.getPublicId()))
        .andExpect(status().isForbidden());
  }

  @Test
  void createTask_withoutAuth_shouldReturn403() throws Exception {
    CreateTaskRequestDto request = new CreateTaskRequestDto();
    request.setTitle("Test Task");

    mockMvc
        .perform(
            post("/api/projects/{projectRef}/tasks", testProject.getPublicId())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
        .andExpect(status().isForbidden());
  }

  // ==================== TASK DEPENDENCY TESTS ====================

  @Test
  void addTaskDependency_shouldCreateDependency() throws Exception {
    Task task1 =
        taskRepository.save(new Task("task_a", testProject, 1, "TASK-1", "Task A", testUser));
    Task task2 =
        taskRepository.save(new Task("task_b", testProject, 2, "TASK-2", "Task B", testUser));

    AddTaskDependencyRequestDto request = new AddTaskDependencyRequestDto();
    request.setDependsOnTaskRef(task1.getPublicId());

    mockMvc
        .perform(
            post(
                    "/api/projects/{projectRef}/tasks/{taskRef}/dependencies",
                    testProject.getPublicId(),
                    task2.getPublicId())
                .with(user("user"))
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.taskId").value(task2.getPublicId()))
        .andExpect(jsonPath("$.dependsOnTaskId").value(task1.getPublicId()))
        .andExpect(jsonPath("$.dependsOnDisplayKey").value("TASK-1"))
        .andExpect(jsonPath("$.createdAt").exists());
  }

  @Test
  void addTaskDependency_usingDisplayKey_shouldCreateDependency() throws Exception {
    Task task1 =
        taskRepository.save(new Task("task_dep1", testProject, 1, "TASK-1", "Task 1", testUser));
    Task task2 =
        taskRepository.save(new Task("task_dep2", testProject, 2, "TASK-2", "Task 2", testUser));

    AddTaskDependencyRequestDto request = new AddTaskDependencyRequestDto();
    request.setDependsOnTaskRef("TASK-1");

    mockMvc
        .perform(
            post(
                    "/api/projects/{projectRef}/tasks/{taskRef}/dependencies",
                    testProject.getProjectKey(),
                    "TASK-2")
                .with(user("user"))
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.dependsOnDisplayKey").value("TASK-1"));
  }

  @Test
  void listTaskDependencies_shouldReturnDependencies() throws Exception {
    Task task1 =
        taskRepository.save(new Task("task_list1", testProject, 1, "TASK-1", "Task 1", testUser));
    Task task2 =
        taskRepository.save(new Task("task_list2", testProject, 2, "TASK-2", "Task 2", testUser));

    // Add dependency: task2 depends on task1
    AddTaskDependencyRequestDto request = new AddTaskDependencyRequestDto();
    request.setDependsOnTaskRef(task1.getPublicId());

    mockMvc
        .perform(
            post(
                    "/api/projects/{projectRef}/tasks/{taskRef}/dependencies",
                    testProject.getPublicId(),
                    task2.getPublicId())
                .with(user("user"))
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
        .andExpect(status().isCreated());

    // List dependencies
    mockMvc
        .perform(
            get(
                    "/api/projects/{projectRef}/tasks/{taskRef}/dependencies",
                    testProject.getPublicId(),
                    task2.getPublicId())
                .with(user("user")))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data").isArray())
        .andExpect(jsonPath("$.data.length()").value(1))
        .andExpect(jsonPath("$.data[0].dependsOnTaskId").value(task1.getPublicId()));
  }

  @Test
  void listTaskDependencies_noDependencies_shouldReturnEmptyList() throws Exception {
    Task task =
        taskRepository.save(new Task("task_nodep", testProject, 1, "TASK-1", "No Deps", testUser));

    mockMvc
        .perform(
            get(
                    "/api/projects/{projectRef}/tasks/{taskRef}/dependencies",
                    testProject.getPublicId(),
                    task.getPublicId())
                .with(user("user")))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data").isArray())
        .andExpect(jsonPath("$.data.length()").value(0));
  }

  @Test
  void removeTaskDependency_shouldRemoveDependency() throws Exception {
    Task task1 =
        taskRepository.save(new Task("task_rem1", testProject, 1, "TASK-1", "Task 1", testUser));
    Task task2 =
        taskRepository.save(new Task("task_rem2", testProject, 2, "TASK-2", "Task 2", testUser));

    // Add dependency
    AddTaskDependencyRequestDto request = new AddTaskDependencyRequestDto();
    request.setDependsOnTaskRef(task1.getPublicId());

    mockMvc
        .perform(
            post(
                    "/api/projects/{projectRef}/tasks/{taskRef}/dependencies",
                    testProject.getPublicId(),
                    task2.getPublicId())
                .with(user("user"))
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
        .andExpect(status().isCreated());

    // Remove dependency
    mockMvc
        .perform(
            delete(
                    "/api/projects/{projectRef}/tasks/{taskRef}/dependencies/{dependsOnTaskRef}",
                    testProject.getPublicId(),
                    task2.getPublicId(),
                    task1.getPublicId())
                .with(user("user")))
        .andExpect(status().isNoContent());

    // Verify removal
    mockMvc
        .perform(
            get(
                    "/api/projects/{projectRef}/tasks/{taskRef}/dependencies",
                    testProject.getPublicId(),
                    task2.getPublicId())
                .with(user("user")))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data.length()").value(0));
  }

  @Test
  void removeTaskDependency_notFound_shouldReturn404() throws Exception {
    Task task1 =
        taskRepository.save(new Task("task_nf1", testProject, 1, "TASK-1", "Task 1", testUser));
    Task task2 =
        taskRepository.save(new Task("task_nf2", testProject, 2, "TASK-2", "Task 2", testUser));

    mockMvc
        .perform(
            delete(
                    "/api/projects/{projectRef}/tasks/{taskRef}/dependencies/{dependsOnTaskRef}",
                    testProject.getPublicId(),
                    task2.getPublicId(),
                    task1.getPublicId())
                .with(user("user")))
        .andExpect(status().isNotFound());
  }

  @Test
  void listTaskDependencies_withoutAuth_shouldReturn403() throws Exception {
    mockMvc
        .perform(
            get(
                "/api/projects/{projectRef}/tasks/{taskRef}/dependencies",
                testProject.getPublicId(),
                "task_123"))
        .andExpect(status().isForbidden());
  }

  // ==================== TASK ACCEPTANCE CRITERIA TESTS ====================

  @Test
  void createTaskAcceptanceCriteria_shouldReturnCreatedCriteria() throws Exception {
    Task task =
        taskRepository.save(new Task("task_ac1", testProject, 1, "TASK-1", "Test Task", testUser));

    CreateAcceptanceCriteriaRequestDto request = new CreateAcceptanceCriteriaRequestDto();
    request.setCriteria("Feature must support offline mode");
    request.setOrderIndex(0);

    mockMvc
        .perform(
            post(
                    "/api/projects/{projectRef}/tasks/{taskRef}/acceptance-criteria",
                    testProject.getPublicId(),
                    task.getPublicId())
                .with(user("user"))
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.id").exists())
        .andExpect(jsonPath("$.criteria").value("Feature must support offline mode"))
        .andExpect(jsonPath("$.isMet").value(false))
        .andExpect(jsonPath("$.orderIndex").value(0))
        .andExpect(jsonPath("$.createdAt").exists());
  }

  @Test
  void createTaskAcceptanceCriteria_autoOrderIndex_shouldIncrementCorrectly() throws Exception {
    Task task =
        taskRepository.save(new Task("task_ac2", testProject, 1, "TASK-1", "Test Task", testUser));

    // Create first criteria
    CreateAcceptanceCriteriaRequestDto request1 = new CreateAcceptanceCriteriaRequestDto();
    request1.setCriteria("First criterion");
    mockMvc
        .perform(
            post(
                    "/api/projects/{projectRef}/tasks/{taskRef}/acceptance-criteria",
                    testProject.getPublicId(),
                    task.getPublicId())
                .with(user("user"))
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request1)))
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.orderIndex").value(0));

    // Create second criteria without orderIndex
    CreateAcceptanceCriteriaRequestDto request2 = new CreateAcceptanceCriteriaRequestDto();
    request2.setCriteria("Second criterion");
    mockMvc
        .perform(
            post(
                    "/api/projects/{projectRef}/tasks/{taskRef}/acceptance-criteria",
                    testProject.getPublicId(),
                    task.getPublicId())
                .with(user("user"))
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request2)))
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.orderIndex").value(1));
  }

  @Test
  void listTaskAcceptanceCriteria_shouldReturnOrderedList() throws Exception {
    Task task =
        taskRepository.save(new Task("task_ac3", testProject, 1, "TASK-1", "Test Task", testUser));

    // Create criteria in reverse order
    CreateAcceptanceCriteriaRequestDto request1 = new CreateAcceptanceCriteriaRequestDto();
    request1.setCriteria("Second item");
    request1.setOrderIndex(1);
    mockMvc
        .perform(
            post(
                    "/api/projects/{projectRef}/tasks/{taskRef}/acceptance-criteria",
                    testProject.getPublicId(),
                    task.getPublicId())
                .with(user("user"))
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request1)))
        .andExpect(status().isCreated());

    CreateAcceptanceCriteriaRequestDto request2 = new CreateAcceptanceCriteriaRequestDto();
    request2.setCriteria("First item");
    request2.setOrderIndex(0);
    mockMvc
        .perform(
            post(
                    "/api/projects/{projectRef}/tasks/{taskRef}/acceptance-criteria",
                    testProject.getPublicId(),
                    task.getPublicId())
                .with(user("user"))
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request2)))
        .andExpect(status().isCreated());

    // List should return in order
    mockMvc
        .perform(
            get(
                    "/api/projects/{projectRef}/tasks/{taskRef}/acceptance-criteria",
                    testProject.getPublicId(),
                    task.getPublicId())
                .with(user("user")))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data").isArray())
        .andExpect(jsonPath("$.data.length()").value(2))
        .andExpect(jsonPath("$.data[0].criteria").value("First item"))
        .andExpect(jsonPath("$.data[0].orderIndex").value(0))
        .andExpect(jsonPath("$.data[1].criteria").value("Second item"))
        .andExpect(jsonPath("$.data[1].orderIndex").value(1));
  }

  @Test
  void listTaskAcceptanceCriteria_emptyTask_shouldReturnEmptyList() throws Exception {
    Task task =
        taskRepository.save(new Task("task_ac4", testProject, 1, "TASK-1", "Test Task", testUser));

    mockMvc
        .perform(
            get(
                    "/api/projects/{projectRef}/tasks/{taskRef}/acceptance-criteria",
                    testProject.getPublicId(),
                    task.getPublicId())
                .with(user("user")))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data").isArray())
        .andExpect(jsonPath("$.data.length()").value(0));
  }

  @Test
  void getTaskAcceptanceCriteria_shouldReturnCriteria() throws Exception {
    Task task =
        taskRepository.save(new Task("task_ac5", testProject, 1, "TASK-1", "Test Task", testUser));

    // Create criteria
    CreateAcceptanceCriteriaRequestDto request = new CreateAcceptanceCriteriaRequestDto();
    request.setCriteria("Test criterion");

    String response =
        mockMvc
            .perform(
                post(
                        "/api/projects/{projectRef}/tasks/{taskRef}/acceptance-criteria",
                        testProject.getPublicId(),
                        task.getPublicId())
                    .with(user("user"))
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isCreated())
            .andReturn()
            .getResponse()
            .getContentAsString();

    Long criteriaId = objectMapper.readTree(response).get("id").asLong();

    // Get the specific criteria
    mockMvc
        .perform(
            get(
                    "/api/projects/{projectRef}/tasks/{taskRef}/acceptance-criteria/{criteriaId}",
                    testProject.getPublicId(),
                    task.getPublicId(),
                    criteriaId)
                .with(user("user")))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.id").value(criteriaId))
        .andExpect(jsonPath("$.criteria").value("Test criterion"));
  }

  @Test
  void updateTaskAcceptanceCriteria_shouldUpdateFields() throws Exception {
    Task task =
        taskRepository.save(new Task("task_ac6", testProject, 1, "TASK-1", "Test Task", testUser));

    // Create criteria
    CreateAcceptanceCriteriaRequestDto createRequest = new CreateAcceptanceCriteriaRequestDto();
    createRequest.setCriteria("Original criterion");

    String response =
        mockMvc
            .perform(
                post(
                        "/api/projects/{projectRef}/tasks/{taskRef}/acceptance-criteria",
                        testProject.getPublicId(),
                        task.getPublicId())
                    .with(user("user"))
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(createRequest)))
            .andExpect(status().isCreated())
            .andReturn()
            .getResponse()
            .getContentAsString();

    Long criteriaId = objectMapper.readTree(response).get("id").asLong();

    // Update criteria
    UpdateAcceptanceCriteriaRequestDto updateRequest = new UpdateAcceptanceCriteriaRequestDto();
    updateRequest.setCriteria("Updated criterion");
    updateRequest.setIsMet(true);
    updateRequest.setOrderIndex(5);

    mockMvc
        .perform(
            put(
                    "/api/projects/{projectRef}/tasks/{taskRef}/acceptance-criteria/{criteriaId}",
                    testProject.getPublicId(),
                    task.getPublicId(),
                    criteriaId)
                .with(user("user"))
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(updateRequest)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.id").value(criteriaId))
        .andExpect(jsonPath("$.criteria").value("Updated criterion"))
        .andExpect(jsonPath("$.isMet").value(true))
        .andExpect(jsonPath("$.orderIndex").value(5));
  }

  @Test
  void deleteTaskAcceptanceCriteria_shouldDeleteCriteria() throws Exception {
    Task task =
        taskRepository.save(new Task("task_ac7", testProject, 1, "TASK-1", "Test Task", testUser));

    // Create criteria
    CreateAcceptanceCriteriaRequestDto createRequest = new CreateAcceptanceCriteriaRequestDto();
    createRequest.setCriteria("To be deleted");

    String response =
        mockMvc
            .perform(
                post(
                        "/api/projects/{projectRef}/tasks/{taskRef}/acceptance-criteria",
                        testProject.getPublicId(),
                        task.getPublicId())
                    .with(user("user"))
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(createRequest)))
            .andExpect(status().isCreated())
            .andReturn()
            .getResponse()
            .getContentAsString();

    Long criteriaId = objectMapper.readTree(response).get("id").asLong();

    // Delete criteria
    mockMvc
        .perform(
            delete(
                    "/api/projects/{projectRef}/tasks/{taskRef}/acceptance-criteria/{criteriaId}",
                    testProject.getPublicId(),
                    task.getPublicId(),
                    criteriaId)
                .with(user("user")))
        .andExpect(status().isNoContent());

    // Verify deletion
    mockMvc
        .perform(
            get(
                    "/api/projects/{projectRef}/tasks/{taskRef}/acceptance-criteria/{criteriaId}",
                    testProject.getPublicId(),
                    task.getPublicId(),
                    criteriaId)
                .with(user("user")))
        .andExpect(status().isNotFound());
  }

  @Test
  void getTaskAcceptanceCriteria_notFound_shouldReturn404() throws Exception {
    Task task =
        taskRepository.save(new Task("task_ac8", testProject, 1, "TASK-1", "Test Task", testUser));

    mockMvc
        .perform(
            get(
                    "/api/projects/{projectRef}/tasks/{taskRef}/acceptance-criteria/{criteriaId}",
                    testProject.getPublicId(),
                    task.getPublicId(),
                    999999L)
                .with(user("user")))
        .andExpect(status().isNotFound());
  }

  @Test
  void listTaskAcceptanceCriteria_withoutAuth_shouldReturn403() throws Exception {
    mockMvc
        .perform(
            get(
                "/api/projects/{projectRef}/tasks/{taskRef}/acceptance-criteria",
                testProject.getPublicId(),
                "task_123"))
        .andExpect(status().isForbidden());
  }

  // ==================== EMPTY-STRING-CLEARS CONVENTION TESTS ====================

  @Test
  void updateTask_emptyString_shouldClearDescription() throws Exception {
    Task task = new Task("task_clearDesc", testProject, 1, "TASK-1", "Test Task", testUser);
    task.setDescription("Original description");
    taskRepository.save(task);

    // Send empty string to clear description
    UpdateTaskRequestDto request = new UpdateTaskRequestDto();
    request.setDescription("");

    mockMvc
        .perform(
            patch(
                    "/api/projects/{projectRef}/tasks/{taskRef}",
                    testProject.getPublicId(),
                    "task_clearDesc")
                .with(user("user"))
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.description").doesNotExist());
  }

  @Test
  void updateTask_emptyString_shouldClearGithubPrUrl() throws Exception {
    Task task = new Task("task_clearPr", testProject, 1, "TASK-2", "Test Task", testUser);
    task.setGithubPrUrl("https://github.com/example/repo/pull/123");
    taskRepository.save(task);

    // Send empty string to clear githubPrUrl
    UpdateTaskRequestDto request = new UpdateTaskRequestDto();
    request.setGithubPrUrl("");

    mockMvc
        .perform(
            patch(
                    "/api/projects/{projectRef}/tasks/{taskRef}",
                    testProject.getPublicId(),
                    "task_clearPr")
                .with(user("user"))
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.githubPrUrl").doesNotExist());
  }

  @Test
  void updateTask_emptyString_shouldClearEpicRef() throws Exception {
    // Create task with epic assignment
    Task task = new Task("task_clearEpic", testProject, 1, "TASK-3", "Test Task", testUser);
    task.setEpic(testEpic);
    taskRepository.save(task);

    // Verify epic is set
    mockMvc
        .perform(
            get(
                    "/api/projects/{projectRef}/tasks/{taskRef}",
                    testProject.getPublicId(),
                    "task_clearEpic")
                .with(user("user")))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.epicId").value(testEpic.getPublicId()));

    // Send empty string to clear epic
    UpdateTaskRequestDto request = new UpdateTaskRequestDto();
    request.setEpicRef("");

    mockMvc
        .perform(
            patch(
                    "/api/projects/{projectRef}/tasks/{taskRef}",
                    testProject.getPublicId(),
                    "task_clearEpic")
                .with(user("user"))
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.epicId").doesNotExist());
  }

  @Test
  void updateTask_nullField_shouldNotChangeValue() throws Exception {
    Task task = new Task("task_nullNoChange", testProject, 1, "TASK-4", "Test Task", testUser);
    task.setDescription("Original description");
    task.setGithubPrUrl("https://github.com/example/repo/pull/456");
    task.setEpic(testEpic);
    taskRepository.save(task);

    // Send request with only title - other fields should remain unchanged
    UpdateTaskRequestDto request = new UpdateTaskRequestDto();
    request.setTitle("Updated Title");
    // description, githubPrUrl, and epicRef are null (not set)

    mockMvc
        .perform(
            patch(
                    "/api/projects/{projectRef}/tasks/{taskRef}",
                    testProject.getPublicId(),
                    "task_nullNoChange")
                .with(user("user"))
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.title").value("Updated Title"))
        .andExpect(jsonPath("$.description").value("Original description"))
        .andExpect(jsonPath("$.githubPrUrl").value("https://github.com/example/repo/pull/456"))
        .andExpect(jsonPath("$.epicId").value(testEpic.getPublicId()));
  }
}
