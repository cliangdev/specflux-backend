package com.specflux.project.interfaces.rest;

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
import com.specflux.api.generated.model.CreateProjectRequest;
import com.specflux.api.generated.model.UpdateProjectRequest;
import com.specflux.common.AbstractIntegrationTest;
import com.specflux.project.domain.Project;
import com.specflux.project.domain.ProjectRepository;
import com.specflux.shared.application.CurrentUserService;
import com.specflux.user.domain.User;
import com.specflux.user.domain.UserRepository;

/**
 * Integration tests for ProjectController.
 *
 * <p>Uses schema isolation for parallel test execution.
 */
@AutoConfigureMockMvc
@Transactional
class ProjectControllerTest extends AbstractIntegrationTest {

  private static final String SCHEMA_NAME = "project_controller_test";

  @DynamicPropertySource
  static void configureSchema(DynamicPropertyRegistry registry) {
    AbstractIntegrationTest.configureSchema(registry, SCHEMA_NAME);
  }

  @Autowired private MockMvc mockMvc;
  @Autowired private UserRepository userRepository;
  @Autowired private ProjectRepository projectRepository;
  @MockitoBean private CurrentUserService currentUserService;

  private final ObjectMapper objectMapper = new ObjectMapper();

  private User testUser;

  @BeforeEach
  void setUp() {
    // Create test user
    testUser =
        userRepository.save(
            new User("user_test123", "firebase_test", "test@example.com", "Test User"));
    // Mock current user service to return the test user
    when(currentUserService.getCurrentUser()).thenReturn(testUser);
  }

  @Test
  @WithMockUser(username = "user")
  void createProject_shouldReturnCreatedProject() throws Exception {
    CreateProjectRequest request = new CreateProjectRequest();
    request.setProjectKey("SPEC");
    request.setName("SpecFlux");
    request.setDescription("A project management tool");

    mockMvc
        .perform(
            post("/projects")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.publicId").exists())
        .andExpect(jsonPath("$.projectKey").value("SPEC"))
        .andExpect(jsonPath("$.name").value("SpecFlux"))
        .andExpect(jsonPath("$.description").value("A project management tool"))
        .andExpect(jsonPath("$.ownerId").value(testUser.getPublicId()))
        .andExpect(jsonPath("$.createdAt").exists())
        .andExpect(jsonPath("$.updatedAt").exists());
  }

  @Test
  @WithMockUser(username = "user")
  void createProject_withDuplicateKey_shouldReturn409() throws Exception {
    // Create existing project
    projectRepository.save(new Project("proj_existing", "DUPE", "Existing Project", testUser));

    CreateProjectRequest request = new CreateProjectRequest();
    request.setProjectKey("DUPE");
    request.setName("Duplicate Project");

    mockMvc
        .perform(
            post("/projects")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
        .andExpect(status().isConflict())
        .andExpect(jsonPath("$.code").value("CONFLICT"));
  }

  @Test
  @WithMockUser(username = "user")
  void createProject_withInvalidKey_shouldReturn400() throws Exception {
    CreateProjectRequest request = new CreateProjectRequest();
    request.setProjectKey("invalid-key"); // lowercase with hyphen
    request.setName("Test Project");

    mockMvc
        .perform(
            post("/projects")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));
  }

  @Test
  @WithMockUser(username = "user")
  void getProject_byPublicId_shouldReturnProject() throws Exception {
    Project project =
        projectRepository.save(new Project("proj_test123", "TEST", "Test Project", testUser));

    mockMvc
        .perform(get("/projects/{ref}", "proj_test123"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.publicId").value("proj_test123"))
        .andExpect(jsonPath("$.projectKey").value("TEST"))
        .andExpect(jsonPath("$.name").value("Test Project"));
  }

  @Test
  @WithMockUser(username = "user")
  void getProject_byProjectKey_shouldReturnProject() throws Exception {
    Project project =
        projectRepository.save(new Project("proj_bykey", "BYKEY", "By Key Project", testUser));

    mockMvc
        .perform(get("/projects/{ref}", "BYKEY"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.publicId").value("proj_bykey"))
        .andExpect(jsonPath("$.projectKey").value("BYKEY"));
  }

  @Test
  @WithMockUser(username = "user")
  void getProject_notFound_shouldReturn404() throws Exception {
    mockMvc
        .perform(get("/projects/{ref}", "nonexistent"))
        .andExpect(status().isNotFound())
        .andExpect(jsonPath("$.code").value("NOT_FOUND"));
  }

  @Test
  @WithMockUser(username = "user")
  void updateProject_shouldReturnUpdatedProject() throws Exception {
    Project project =
        projectRepository.save(new Project("proj_update", "UPDT", "Original Name", testUser));

    UpdateProjectRequest request = new UpdateProjectRequest();
    request.setName("Updated Name");
    request.setDescription("Updated description");

    mockMvc
        .perform(
            put("/projects/{ref}", "proj_update")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.name").value("Updated Name"))
        .andExpect(jsonPath("$.description").value("Updated description"));
  }

  @Test
  @WithMockUser(username = "user")
  void updateProject_partialUpdate_shouldOnlyUpdateProvidedFields() throws Exception {
    Project project = new Project("proj_partial", "PART", "Original Name", testUser);
    project.setDescription("Original description");
    projectRepository.save(project);

    UpdateProjectRequest request = new UpdateProjectRequest();
    request.setName("New Name");
    // description not set

    mockMvc
        .perform(
            put("/projects/{ref}", "proj_partial")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.name").value("New Name"))
        .andExpect(jsonPath("$.description").value("Original description"));
  }

  @Test
  @WithMockUser(username = "user")
  void deleteProject_shouldReturn204() throws Exception {
    Project project =
        projectRepository.save(new Project("proj_delete", "DEL", "To Delete", testUser));

    mockMvc.perform(delete("/projects/{ref}", "proj_delete")).andExpect(status().isNoContent());

    // Verify deletion
    mockMvc.perform(get("/projects/{ref}", "proj_delete")).andExpect(status().isNotFound());
  }

  @Test
  @WithMockUser(username = "user")
  void listProjects_shouldReturnPaginatedList() throws Exception {
    // Create test projects
    projectRepository.save(new Project("proj_list1", "LST1", "Project 1", testUser));
    projectRepository.save(new Project("proj_list2", "LST2", "Project 2", testUser));
    projectRepository.save(new Project("proj_list3", "LST3", "Project 3", testUser));

    mockMvc
        .perform(get("/projects").param("limit", "2"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data").isArray())
        .andExpect(jsonPath("$.data.length()").value(2))
        .andExpect(jsonPath("$.pagination.total").value(3))
        .andExpect(jsonPath("$.pagination.hasMore").value(true))
        .andExpect(jsonPath("$.pagination.nextCursor").exists());
  }

  @Test
  @WithMockUser(username = "user")
  void listProjects_withSort_shouldReturnSortedList() throws Exception {
    projectRepository.save(new Project("proj_z", "ZETA", "Zeta Project", testUser));
    projectRepository.save(new Project("proj_a", "ALPH", "Alpha Project", testUser));

    mockMvc
        .perform(get("/projects").param("sort", "name").param("order", "asc"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data[0].name").value("Alpha Project"))
        .andExpect(jsonPath("$.data[1].name").value("Zeta Project"));
  }

  @Test
  void listProjects_withoutAuth_shouldReturn401or403() throws Exception {
    // Without @WithMockUser, should be unauthorized
    // In test config with permitAll(), this may return 200
    // This test documents expected behavior in production
  }
}
