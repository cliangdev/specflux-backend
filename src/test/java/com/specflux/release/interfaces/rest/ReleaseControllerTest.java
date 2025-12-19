package com.specflux.release.interfaces.rest;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.openapitools.jackson.nullable.JsonNullable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

import com.specflux.api.generated.model.CreateReleaseRequestDto;
import com.specflux.api.generated.model.ReleaseStatusDto;
import com.specflux.api.generated.model.UpdateReleaseRequestDto;
import com.specflux.common.AbstractControllerIntegrationTest;
import com.specflux.epic.domain.Epic;
import com.specflux.epic.domain.EpicRepository;
import com.specflux.project.domain.Project;
import com.specflux.project.domain.ProjectRepository;
import com.specflux.release.domain.Release;
import com.specflux.release.domain.ReleaseRepository;
import com.specflux.release.domain.ReleaseStatus;
import com.specflux.task.domain.Task;
import com.specflux.task.domain.TaskRepository;

/**
 * Integration tests for ReleaseController.
 *
 * <p>Uses schema isolation for parallel test execution.
 */
class ReleaseControllerTest extends AbstractControllerIntegrationTest {

  @DynamicPropertySource
  static void configureSchema(DynamicPropertyRegistry registry) {
    configureSchemaForClass(registry, ReleaseControllerTest.class);
  }

  @Autowired private ProjectRepository projectRepository;
  @Autowired private ReleaseRepository releaseRepository;
  @Autowired private EpicRepository epicRepository;
  @Autowired private TaskRepository taskRepository;

  private Project testProject;

  @BeforeEach
  void setUpProject() {
    testProject =
        projectRepository.save(
            new Project("proj_release_test", "REL", "Release Test Project", testUser));
  }

  @Test
  void createRelease_shouldReturnCreatedRelease() throws Exception {
    CreateReleaseRequestDto request = new CreateReleaseRequestDto();
    request.setName("v1.0.0");
    request.setDescription(JsonNullable.of("Initial release"));

    mockMvc
        .perform(
            post("/api/projects/{projectRef}/releases", testProject.getPublicId())
                .with(user("user"))
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.id").exists())
        .andExpect(jsonPath("$.displayKey").value("REL-R1"))
        .andExpect(jsonPath("$.projectId").value(testProject.getPublicId()))
        .andExpect(jsonPath("$.name").value("v1.0.0"))
        .andExpect(jsonPath("$.description").value("Initial release"))
        .andExpect(jsonPath("$.status").value("PLANNED"))
        .andExpect(jsonPath("$.createdAt").exists())
        .andExpect(jsonPath("$.updatedAt").exists());
  }

  @Test
  void createRelease_usingProjectKey_shouldReturnCreatedRelease() throws Exception {
    CreateReleaseRequestDto request = new CreateReleaseRequestDto();
    request.setName("v2.0.0");

    mockMvc
        .perform(
            post("/api/projects/{projectRef}/releases", testProject.getProjectKey())
                .with(user("user"))
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.name").value("v2.0.0"));
  }

  @Test
  void createRelease_projectNotFound_shouldReturn404() throws Exception {
    CreateReleaseRequestDto request = new CreateReleaseRequestDto();
    request.setName("Test Release");

    mockMvc
        .perform(
            post("/api/projects/{projectRef}/releases", "nonexistent")
                .with(user("user"))
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
        .andExpect(status().isNotFound())
        .andExpect(jsonPath("$.code").value("NOT_FOUND"));
  }

  @Test
  void getRelease_byPublicId_shouldReturnRelease() throws Exception {
    Release release =
        releaseRepository.save(new Release("rel_test123", testProject, 1, "REL-R1", "v1.0.0"));

    mockMvc
        .perform(
            get(
                    "/api/projects/{projectRef}/releases/{releaseRef}",
                    testProject.getPublicId(),
                    "rel_test123")
                .with(user("user")))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.id").value("rel_test123"))
        .andExpect(jsonPath("$.displayKey").value("REL-R1"))
        .andExpect(jsonPath("$.name").value("v1.0.0"));
  }

  @Test
  void getRelease_byDisplayKey_shouldReturnRelease() throws Exception {
    Release release =
        releaseRepository.save(new Release("rel_bykey", testProject, 2, "REL-R2", "v2.0.0"));

    mockMvc
        .perform(
            get(
                    "/api/projects/{projectRef}/releases/{releaseRef}",
                    testProject.getProjectKey(),
                    "REL-R2")
                .with(user("user")))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.id").value("rel_bykey"))
        .andExpect(jsonPath("$.displayKey").value("REL-R2"));
  }

  @Test
  void getRelease_notFound_shouldReturn404() throws Exception {
    mockMvc
        .perform(
            get(
                    "/api/projects/{projectRef}/releases/{releaseRef}",
                    testProject.getPublicId(),
                    "nonexistent")
                .with(user("user")))
        .andExpect(status().isNotFound())
        .andExpect(jsonPath("$.code").value("NOT_FOUND"));
  }

  @Test
  void updateRelease_shouldReturnUpdatedRelease() throws Exception {
    Release release =
        releaseRepository.save(
            new Release("rel_update", testProject, 1, "REL-R1", "Original Name"));

    UpdateReleaseRequestDto request = new UpdateReleaseRequestDto();
    request.setName("Updated Name");
    request.setDescription(JsonNullable.of("New description"));
    request.setStatus(ReleaseStatusDto.IN_PROGRESS);

    mockMvc
        .perform(
            put(
                    "/api/projects/{projectRef}/releases/{releaseRef}",
                    testProject.getPublicId(),
                    "rel_update")
                .with(user("user"))
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.name").value("Updated Name"))
        .andExpect(jsonPath("$.description").value("New description"))
        .andExpect(jsonPath("$.status").value("IN_PROGRESS"));
  }

  @Test
  void updateRelease_partialUpdate_shouldOnlyUpdateProvidedFields() throws Exception {
    Release release = new Release("rel_partial", testProject, 1, "REL-R1", "Original Name");
    release.setDescription("Original description");
    releaseRepository.save(release);

    UpdateReleaseRequestDto request = new UpdateReleaseRequestDto();
    request.setName("New Name");
    // other fields not set

    mockMvc
        .perform(
            put(
                    "/api/projects/{projectRef}/releases/{releaseRef}",
                    testProject.getPublicId(),
                    "rel_partial")
                .with(user("user"))
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.name").value("New Name"))
        .andExpect(jsonPath("$.description").value("Original description"))
        .andExpect(jsonPath("$.status").value("PLANNED"));
  }

  @Test
  void deleteRelease_shouldReturn204() throws Exception {
    Release release =
        releaseRepository.save(new Release("rel_delete", testProject, 1, "REL-R1", "To Delete"));

    mockMvc
        .perform(
            delete(
                    "/api/projects/{projectRef}/releases/{releaseRef}",
                    testProject.getPublicId(),
                    "rel_delete")
                .with(user("user")))
        .andExpect(status().isNoContent());

    // Verify deletion
    mockMvc
        .perform(
            get(
                    "/api/projects/{projectRef}/releases/{releaseRef}",
                    testProject.getPublicId(),
                    "rel_delete")
                .with(user("user")))
        .andExpect(status().isNotFound());
  }

  @Test
  void listReleases_shouldReturnPaginatedList() throws Exception {
    // Create test releases
    releaseRepository.save(new Release("rel_list1", testProject, 1, "REL-R1", "Release 1"));
    releaseRepository.save(new Release("rel_list2", testProject, 2, "REL-R2", "Release 2"));
    releaseRepository.save(new Release("rel_list3", testProject, 3, "REL-R3", "Release 3"));

    mockMvc
        .perform(
            get("/api/projects/{projectRef}/releases", testProject.getPublicId())
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
  void listReleases_withStatusFilter_shouldReturnFilteredList() throws Exception {
    Release release1 = new Release("rel_plan", testProject, 1, "REL-R1", "Planned Release");
    release1.setStatus(ReleaseStatus.PLANNED);
    releaseRepository.save(release1);

    Release release2 = new Release("rel_prog", testProject, 2, "REL-R2", "In Progress Release");
    release2.setStatus(ReleaseStatus.IN_PROGRESS);
    releaseRepository.save(release2);

    mockMvc
        .perform(
            get("/api/projects/{projectRef}/releases", testProject.getPublicId())
                .with(user("user"))
                .param("status", "IN_PROGRESS"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data.length()").value(1))
        .andExpect(jsonPath("$.data[0].status").value("IN_PROGRESS"));
  }

  @Test
  void listReleases_withSort_shouldReturnSortedList() throws Exception {
    releaseRepository.save(new Release("rel_z", testProject, 1, "REL-R1", "Zeta Release"));
    releaseRepository.save(new Release("rel_a", testProject, 2, "REL-R2", "Alpha Release"));

    mockMvc
        .perform(
            get("/api/projects/{projectRef}/releases", testProject.getPublicId())
                .with(user("user"))
                .param("sort", "title")
                .param("order", "asc"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data[0].name").value("Alpha Release"))
        .andExpect(jsonPath("$.data[1].name").value("Zeta Release"));
  }

  @Test
  void listReleases_emptyProject_shouldReturnEmptyList() throws Exception {
    mockMvc
        .perform(
            get("/api/projects/{projectRef}/releases", testProject.getPublicId())
                .with(user("user")))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data").isArray())
        .andExpect(jsonPath("$.data.length()").value(0))
        .andExpect(jsonPath("$.pagination.total").value(0))
        .andExpect(jsonPath("$.pagination.hasMore").value(false));
  }

  @Test
  void createRelease_sequentialNumbers_shouldIncrementCorrectly() throws Exception {
    // Create first release
    CreateReleaseRequestDto request1 = new CreateReleaseRequestDto();
    request1.setName("First Release");
    mockMvc
        .perform(
            post("/api/projects/{projectRef}/releases", testProject.getPublicId())
                .with(user("user"))
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request1)))
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.displayKey").value("REL-R1"));

    // Create second release
    CreateReleaseRequestDto request2 = new CreateReleaseRequestDto();
    request2.setName("Second Release");
    mockMvc
        .perform(
            post("/api/projects/{projectRef}/releases", testProject.getPublicId())
                .with(user("user"))
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request2)))
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.displayKey").value("REL-R2"));
  }

  @Test
  void listReleases_withoutAuth_shouldReturn403() throws Exception {
    mockMvc
        .perform(get("/api/projects/{projectRef}/releases", testProject.getPublicId()))
        .andExpect(status().isForbidden());
  }

  @Test
  void createRelease_withoutAuth_shouldReturn403() throws Exception {
    CreateReleaseRequestDto request = new CreateReleaseRequestDto();
    request.setName("Test Release");

    mockMvc
        .perform(
            post("/api/projects/{projectRef}/releases", testProject.getPublicId())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
        .andExpect(status().isForbidden());
  }

  @Test
  void getRelease_withoutInclude_shouldReturnReleaseOnly() throws Exception {
    Release release =
        releaseRepository.save(
            new Release("rel_noinclude", testProject, 1, "REL-R1", "Release Without Include"));

    // Create an epic assigned to this release
    Epic epic = new Epic("epic_noinclude", testProject, 1, "REL-E1", "Epic 1", testUser);
    epic.setReleaseId(release.getId());
    epicRepository.save(epic);

    mockMvc
        .perform(
            get(
                    "/api/projects/{projectRef}/releases/{releaseRef}",
                    testProject.getPublicId(),
                    "rel_noinclude")
                .with(user("user")))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.id").value("rel_noinclude"))
        .andExpect(jsonPath("$.name").value("Release Without Include"))
        // With JsonNullable serialization, undefined fields are not serialized at all
        .andExpect(jsonPath("$.epics").doesNotExist());
  }

  @Test
  void getRelease_withIncludeEpics_shouldReturnNestedEpics() throws Exception {
    Release release =
        releaseRepository.save(
            new Release("rel_incepics", testProject, 1, "REL-R1", "Release With Epics"));

    // Create epics assigned to this release
    Epic epic1 = new Epic("epic_inc1", testProject, 1, "REL-E1", "Epic 1", testUser);
    epic1.setReleaseId(release.getId());
    epicRepository.save(epic1);

    Epic epic2 = new Epic("epic_inc2", testProject, 2, "REL-E2", "Epic 2", testUser);
    epic2.setReleaseId(release.getId());
    epicRepository.save(epic2);

    mockMvc
        .perform(
            get(
                    "/api/projects/{projectRef}/releases/{releaseRef}",
                    testProject.getPublicId(),
                    "rel_incepics")
                .with(user("user"))
                .param("include", "epics"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.id").value("rel_incepics"))
        .andExpect(jsonPath("$.name").value("Release With Epics"))
        .andExpect(jsonPath("$.epics").isArray())
        .andExpect(jsonPath("$.epics.length()").value(2))
        .andExpect(jsonPath("$.epics[?(@.id=='epic_inc1')].title").value("Epic 1"))
        .andExpect(jsonPath("$.epics[?(@.id=='epic_inc2')].title").value("Epic 2"));
  }

  @Test
  void getRelease_withIncludeEpicsTasks_shouldReturnFullContext() throws Exception {
    Release release =
        releaseRepository.save(
            new Release("rel_fullinc", testProject, 1, "REL-R1", "Release Full Context"));

    // Create epic assigned to this release
    Epic epic = new Epic("epic_fullinc", testProject, 1, "REL-E1", "Epic With Tasks", testUser);
    epic.setReleaseId(release.getId());
    epicRepository.save(epic);

    // Create tasks assigned to the epic
    Task task1 = new Task("task_inc1", testProject, 1, "REL-T1", "Task 1", testUser);
    task1.setEpic(epic);
    taskRepository.save(task1);

    Task task2 = new Task("task_inc2", testProject, 2, "REL-T2", "Task 2", testUser);
    task2.setEpic(epic);
    taskRepository.save(task2);

    mockMvc
        .perform(
            get(
                    "/api/projects/{projectRef}/releases/{releaseRef}",
                    testProject.getPublicId(),
                    "rel_fullinc")
                .with(user("user"))
                .param("include", "epics,tasks"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.id").value("rel_fullinc"))
        .andExpect(jsonPath("$.name").value("Release Full Context"))
        .andExpect(jsonPath("$.epics").isArray())
        .andExpect(jsonPath("$.epics.length()").value(1))
        .andExpect(jsonPath("$.epics[0].id").value("epic_fullinc"))
        .andExpect(jsonPath("$.epics[0].tasks").isArray())
        .andExpect(jsonPath("$.epics[0].tasks.length()").value(2));
  }
}
