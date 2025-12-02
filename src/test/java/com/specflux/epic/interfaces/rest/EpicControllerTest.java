package com.specflux.epic.interfaces.rest;

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
import com.specflux.api.generated.model.CreateEpicRequest;
import com.specflux.api.generated.model.UpdateEpicRequest;
import com.specflux.common.AbstractIntegrationTest;
import com.specflux.epic.domain.Epic;
import com.specflux.epic.domain.EpicRepository;
import com.specflux.epic.domain.EpicStatus;
import com.specflux.project.domain.Project;
import com.specflux.project.domain.ProjectRepository;
import com.specflux.shared.application.CurrentUserService;
import com.specflux.user.domain.User;
import com.specflux.user.domain.UserRepository;

/**
 * Integration tests for EpicController.
 *
 * <p>Uses schema isolation for parallel test execution.
 */
@AutoConfigureMockMvc
@Transactional
class EpicControllerTest extends AbstractIntegrationTest {

  private static final String SCHEMA_NAME = "epic_controller_test";

  @DynamicPropertySource
  static void configureSchema(DynamicPropertyRegistry registry) {
    AbstractIntegrationTest.configureSchema(registry, SCHEMA_NAME);
  }

  @Autowired private MockMvc mockMvc;
  @Autowired private UserRepository userRepository;
  @Autowired private ProjectRepository projectRepository;
  @Autowired private EpicRepository epicRepository;
  @MockitoBean private CurrentUserService currentUserService;

  private final ObjectMapper objectMapper = new ObjectMapper();

  private User testUser;
  private Project testProject;

  @BeforeEach
  void setUp() {
    // Create test user
    testUser =
        userRepository.save(
            new User("user_epic_test", "firebase_epic", "epic@example.com", "Epic Test User"));
    // Create test project
    testProject =
        projectRepository.save(
            new Project("proj_epic_test", "EPIC", "Epic Test Project", testUser));
    // Mock current user service to return the test user
    when(currentUserService.getCurrentUser()).thenReturn(testUser);
  }

  @Test
  @WithMockUser(username = "user")
  void createEpic_shouldReturnCreatedEpic() throws Exception {
    CreateEpicRequest request = new CreateEpicRequest();
    request.setTitle("User Authentication Feature");
    request.setDescription("Implement OAuth2 authentication");

    mockMvc
        .perform(
            post("/projects/{projectRef}/epics", testProject.getPublicId())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.publicId").exists())
        .andExpect(jsonPath("$.displayKey").value("EPIC-E1"))
        .andExpect(jsonPath("$.projectId").value(testProject.getPublicId()))
        .andExpect(jsonPath("$.title").value("User Authentication Feature"))
        .andExpect(jsonPath("$.description").value("Implement OAuth2 authentication"))
        .andExpect(jsonPath("$.status").value("PLANNING"))
        .andExpect(jsonPath("$.createdById").value(testUser.getPublicId()))
        .andExpect(jsonPath("$.createdAt").exists())
        .andExpect(jsonPath("$.updatedAt").exists());
  }

  @Test
  @WithMockUser(username = "user")
  void createEpic_usingProjectKey_shouldReturnCreatedEpic() throws Exception {
    CreateEpicRequest request = new CreateEpicRequest();
    request.setTitle("Dashboard Feature");

    mockMvc
        .perform(
            post("/projects/{projectRef}/epics", testProject.getProjectKey())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.title").value("Dashboard Feature"));
  }

  @Test
  @WithMockUser(username = "user")
  void createEpic_projectNotFound_shouldReturn404() throws Exception {
    CreateEpicRequest request = new CreateEpicRequest();
    request.setTitle("Test Epic");

    mockMvc
        .perform(
            post("/projects/{projectRef}/epics", "nonexistent")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
        .andExpect(status().isNotFound())
        .andExpect(jsonPath("$.code").value("NOT_FOUND"));
  }

  @Test
  @WithMockUser(username = "user")
  void getEpic_byPublicId_shouldReturnEpic() throws Exception {
    Epic epic =
        epicRepository.save(
            new Epic("epic_test123", testProject, 1, "EPIC-E1", "Test Epic", testUser));

    mockMvc
        .perform(
            get(
                "/projects/{projectRef}/epics/{epicRef}",
                testProject.getPublicId(),
                "epic_test123"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.publicId").value("epic_test123"))
        .andExpect(jsonPath("$.displayKey").value("EPIC-E1"))
        .andExpect(jsonPath("$.title").value("Test Epic"));
  }

  @Test
  @WithMockUser(username = "user")
  void getEpic_byDisplayKey_shouldReturnEpic() throws Exception {
    Epic epic =
        epicRepository.save(
            new Epic("epic_bykey", testProject, 2, "EPIC-E2", "Epic By Key", testUser));

    mockMvc
        .perform(
            get("/projects/{projectRef}/epics/{epicRef}", testProject.getProjectKey(), "EPIC-E2"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.publicId").value("epic_bykey"))
        .andExpect(jsonPath("$.displayKey").value("EPIC-E2"));
  }

  @Test
  @WithMockUser(username = "user")
  void getEpic_notFound_shouldReturn404() throws Exception {
    mockMvc
        .perform(
            get("/projects/{projectRef}/epics/{epicRef}", testProject.getPublicId(), "nonexistent"))
        .andExpect(status().isNotFound())
        .andExpect(jsonPath("$.code").value("NOT_FOUND"));
  }

  @Test
  @WithMockUser(username = "user")
  void updateEpic_shouldReturnUpdatedEpic() throws Exception {
    Epic epic =
        epicRepository.save(
            new Epic("epic_update", testProject, 1, "EPIC-E1", "Original Title", testUser));

    UpdateEpicRequest request = new UpdateEpicRequest();
    request.setTitle("Updated Title");
    request.setDescription("New description");
    request.setStatus(com.specflux.api.generated.model.EpicStatus.IN_PROGRESS);

    mockMvc
        .perform(
            put("/projects/{projectRef}/epics/{epicRef}", testProject.getPublicId(), "epic_update")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.title").value("Updated Title"))
        .andExpect(jsonPath("$.description").value("New description"))
        .andExpect(jsonPath("$.status").value("IN_PROGRESS"));
  }

  @Test
  @WithMockUser(username = "user")
  void updateEpic_partialUpdate_shouldOnlyUpdateProvidedFields() throws Exception {
    Epic epic = new Epic("epic_partial", testProject, 1, "EPIC-E1", "Original Title", testUser);
    epic.setDescription("Original description");
    epicRepository.save(epic);

    UpdateEpicRequest request = new UpdateEpicRequest();
    request.setTitle("New Title");
    // other fields not set

    mockMvc
        .perform(
            put("/projects/{projectRef}/epics/{epicRef}", testProject.getPublicId(), "epic_partial")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.title").value("New Title"))
        .andExpect(jsonPath("$.description").value("Original description"))
        .andExpect(jsonPath("$.status").value("PLANNING"));
  }

  @Test
  @WithMockUser(username = "user")
  void deleteEpic_shouldReturn204() throws Exception {
    Epic epic =
        epicRepository.save(
            new Epic("epic_delete", testProject, 1, "EPIC-E1", "To Delete", testUser));

    mockMvc
        .perform(
            delete(
                "/projects/{projectRef}/epics/{epicRef}", testProject.getPublicId(), "epic_delete"))
        .andExpect(status().isNoContent());

    // Verify deletion
    mockMvc
        .perform(
            get("/projects/{projectRef}/epics/{epicRef}", testProject.getPublicId(), "epic_delete"))
        .andExpect(status().isNotFound());
  }

  @Test
  @WithMockUser(username = "user")
  void listEpics_shouldReturnPaginatedList() throws Exception {
    // Create test epics
    epicRepository.save(new Epic("epic_list1", testProject, 1, "EPIC-E1", "Epic 1", testUser));
    epicRepository.save(new Epic("epic_list2", testProject, 2, "EPIC-E2", "Epic 2", testUser));
    epicRepository.save(new Epic("epic_list3", testProject, 3, "EPIC-E3", "Epic 3", testUser));

    mockMvc
        .perform(get("/projects/{projectRef}/epics", testProject.getPublicId()).param("limit", "2"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data").isArray())
        .andExpect(jsonPath("$.data.length()").value(2))
        .andExpect(jsonPath("$.pagination.total").value(3))
        .andExpect(jsonPath("$.pagination.hasMore").value(true))
        .andExpect(jsonPath("$.pagination.nextCursor").exists());
  }

  @Test
  @WithMockUser(username = "user")
  void listEpics_withStatusFilter_shouldReturnFilteredList() throws Exception {
    Epic epic1 = new Epic("epic_plan", testProject, 1, "EPIC-E1", "Planning Epic", testUser);
    epic1.setStatus(EpicStatus.PLANNING);
    epicRepository.save(epic1);

    Epic epic2 = new Epic("epic_prog", testProject, 2, "EPIC-E2", "In Progress Epic", testUser);
    epic2.setStatus(EpicStatus.IN_PROGRESS);
    epicRepository.save(epic2);

    mockMvc
        .perform(
            get("/projects/{projectRef}/epics", testProject.getPublicId())
                .param("status", "IN_PROGRESS"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data.length()").value(1))
        .andExpect(jsonPath("$.data[0].status").value("IN_PROGRESS"));
  }

  @Test
  @WithMockUser(username = "user")
  void listEpics_withSort_shouldReturnSortedList() throws Exception {
    epicRepository.save(new Epic("epic_z", testProject, 1, "EPIC-E1", "Zeta Epic", testUser));
    epicRepository.save(new Epic("epic_a", testProject, 2, "EPIC-E2", "Alpha Epic", testUser));

    mockMvc
        .perform(
            get("/projects/{projectRef}/epics", testProject.getPublicId())
                .param("sort", "title")
                .param("order", "asc"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data[0].title").value("Alpha Epic"))
        .andExpect(jsonPath("$.data[1].title").value("Zeta Epic"));
  }

  @Test
  @WithMockUser(username = "user")
  void listEpics_emptyProject_shouldReturnEmptyList() throws Exception {
    mockMvc
        .perform(get("/projects/{projectRef}/epics", testProject.getPublicId()))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data").isArray())
        .andExpect(jsonPath("$.data.length()").value(0))
        .andExpect(jsonPath("$.pagination.total").value(0))
        .andExpect(jsonPath("$.pagination.hasMore").value(false));
  }

  @Test
  @WithMockUser(username = "user")
  void createEpic_sequentialNumbers_shouldIncrementCorrectly() throws Exception {
    // Create first epic
    CreateEpicRequest request1 = new CreateEpicRequest();
    request1.setTitle("First Epic");
    mockMvc
        .perform(
            post("/projects/{projectRef}/epics", testProject.getPublicId())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request1)))
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.displayKey").value("EPIC-E1"));

    // Create second epic
    CreateEpicRequest request2 = new CreateEpicRequest();
    request2.setTitle("Second Epic");
    mockMvc
        .perform(
            post("/projects/{projectRef}/epics", testProject.getPublicId())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request2)))
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.displayKey").value("EPIC-E2"));
  }
}
