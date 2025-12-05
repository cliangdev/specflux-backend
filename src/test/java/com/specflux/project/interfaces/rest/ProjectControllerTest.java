package com.specflux.project.interfaces.rest;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

import com.specflux.api.generated.model.CreateProjectRequestDto;
import com.specflux.api.generated.model.UpdateProjectRequestDto;
import com.specflux.common.AbstractControllerIntegrationTest;
import com.specflux.project.domain.Project;
import com.specflux.project.domain.ProjectMember;
import com.specflux.project.domain.ProjectMemberRepository;
import com.specflux.project.domain.ProjectRepository;

/**
 * Integration tests for ProjectController.
 *
 * <p>Uses schema isolation for parallel test execution.
 */
class ProjectControllerTest extends AbstractControllerIntegrationTest {

  @DynamicPropertySource
  static void configureSchema(DynamicPropertyRegistry registry) {
    configureSchemaForClass(registry, ProjectControllerTest.class);
  }

  @Autowired private ProjectRepository projectRepository;
  @Autowired private ProjectMemberRepository projectMemberRepository;

  @Test
  void createProject_shouldReturnCreatedProject() throws Exception {
    CreateProjectRequestDto request = new CreateProjectRequestDto();
    request.setProjectKey("SPEC");
    request.setName("SpecFlux");
    request.setDescription("A project management tool");

    mockMvc
        .perform(
            post("/api/projects")
                .with(user("user"))
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.id").exists())
        .andExpect(jsonPath("$.projectKey").value("SPEC"))
        .andExpect(jsonPath("$.name").value("SpecFlux"))
        .andExpect(jsonPath("$.description").value("A project management tool"))
        .andExpect(jsonPath("$.ownerId").value(testUser.getPublicId()))
        .andExpect(jsonPath("$.createdAt").exists())
        .andExpect(jsonPath("$.updatedAt").exists());

    // Verify that the creator was added as a member with 'owner' role
    Project createdProject = projectRepository.findByProjectKey("SPEC").orElseThrow();
    ProjectMember membership =
        projectMemberRepository
            .findByProjectIdAndUserId(createdProject.getId(), testUser.getId())
            .orElseThrow(
                () -> new AssertionError("Membership should be created for project creator"));
    assert membership.getRole() == ProjectMember.Role.owner : "Creator should have 'owner' role";
  }

  @Test
  void createProject_withDuplicateKey_shouldReturn409() throws Exception {
    // Create existing project
    projectRepository.save(new Project("proj_existing", "DUPE", "Existing Project", testUser));

    CreateProjectRequestDto request = new CreateProjectRequestDto();
    request.setProjectKey("DUPE");
    request.setName("Duplicate Project");

    mockMvc
        .perform(
            post("/api/projects")
                .with(user("user"))
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
        .andExpect(status().isConflict())
        .andExpect(jsonPath("$.code").value("CONFLICT"));
  }

  @Test
  void createProject_withInvalidKey_shouldReturn400() throws Exception {
    CreateProjectRequestDto request = new CreateProjectRequestDto();
    request.setProjectKey("invalid-key"); // lowercase with hyphen
    request.setName("Test Project");

    mockMvc
        .perform(
            post("/api/projects")
                .with(user("user"))
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));
  }

  @Test
  void getProject_byPublicId_shouldReturnProject() throws Exception {
    Project project =
        projectRepository.save(new Project("proj_test123", "TEST", "Test Project", testUser));

    mockMvc
        .perform(get("/api/projects/{ref}", "proj_test123").with(user("user")))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.id").value("proj_test123"))
        .andExpect(jsonPath("$.projectKey").value("TEST"))
        .andExpect(jsonPath("$.name").value("Test Project"));
  }

  @Test
  void getProject_byProjectKey_shouldReturnProject() throws Exception {
    Project project =
        projectRepository.save(new Project("proj_bykey", "BYKEY", "By Key Project", testUser));

    mockMvc
        .perform(get("/api/projects/{ref}", "BYKEY").with(user("user")))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.id").value("proj_bykey"))
        .andExpect(jsonPath("$.projectKey").value("BYKEY"));
  }

  @Test
  void getProject_notFound_shouldReturn404() throws Exception {
    mockMvc
        .perform(get("/api/projects/{ref}", "nonexistent").with(user("user")))
        .andExpect(status().isNotFound())
        .andExpect(jsonPath("$.code").value("NOT_FOUND"));
  }

  @Test
  void updateProject_shouldReturnUpdatedProject() throws Exception {
    Project project =
        projectRepository.save(new Project("proj_update", "UPDT", "Original Name", testUser));

    UpdateProjectRequestDto request = new UpdateProjectRequestDto();
    request.setName("Updated Name");
    request.setDescription("Updated description");

    mockMvc
        .perform(
            put("/api/projects/{ref}", "proj_update")
                .with(user("user"))
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.name").value("Updated Name"))
        .andExpect(jsonPath("$.description").value("Updated description"));
  }

  @Test
  void updateProject_partialUpdate_shouldOnlyUpdateProvidedFields() throws Exception {
    Project project = new Project("proj_partial", "PART", "Original Name", testUser);
    project.setDescription("Original description");
    projectRepository.save(project);

    UpdateProjectRequestDto request = new UpdateProjectRequestDto();
    request.setName("New Name");
    // description not set

    mockMvc
        .perform(
            put("/api/projects/{ref}", "proj_partial")
                .with(user("user"))
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.name").value("New Name"))
        .andExpect(jsonPath("$.description").value("Original description"));
  }

  @Test
  void deleteProject_shouldReturn204() throws Exception {
    Project project =
        projectRepository.save(new Project("proj_delete", "DEL", "To Delete", testUser));

    mockMvc
        .perform(delete("/api/projects/{ref}", "proj_delete").with(user("user")))
        .andExpect(status().isNoContent());

    // Verify deletion
    mockMvc
        .perform(get("/api/projects/{ref}", "proj_delete").with(user("user")))
        .andExpect(status().isNotFound());
  }

  @Test
  void listProjects_shouldReturnPaginatedList() throws Exception {
    // Create test projects with memberships
    Project project1 =
        projectRepository.save(new Project("proj_list1", "LST1", "Project 1", testUser));
    projectMemberRepository.save(ProjectMember.createOwner(project1, testUser));
    Project project2 =
        projectRepository.save(new Project("proj_list2", "LST2", "Project 2", testUser));
    projectMemberRepository.save(ProjectMember.createOwner(project2, testUser));
    Project project3 =
        projectRepository.save(new Project("proj_list3", "LST3", "Project 3", testUser));
    projectMemberRepository.save(ProjectMember.createOwner(project3, testUser));

    mockMvc
        .perform(get("/api/projects").with(user("user")).param("limit", "2"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data").isArray())
        .andExpect(jsonPath("$.data.length()").value(2))
        .andExpect(jsonPath("$.pagination.total").value(3))
        .andExpect(jsonPath("$.pagination.hasMore").value(true))
        .andExpect(jsonPath("$.pagination.nextCursor").exists());
  }

  @Test
  void listProjects_withSort_shouldReturnSortedList() throws Exception {
    // Create test projects with memberships
    Project projectZ =
        projectRepository.save(new Project("proj_z", "ZETA", "Zeta Project", testUser));
    projectMemberRepository.save(ProjectMember.createOwner(projectZ, testUser));
    Project projectA =
        projectRepository.save(new Project("proj_a", "ALPH", "Alpha Project", testUser));
    projectMemberRepository.save(ProjectMember.createOwner(projectA, testUser));

    mockMvc
        .perform(
            get("/api/projects").with(user("user")).param("sort", "name").param("order", "asc"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data[0].name").value("Alpha Project"))
        .andExpect(jsonPath("$.data[1].name").value("Zeta Project"));
  }

  @Test
  void listProjects_withoutAuth_shouldReturn403() throws Exception {
    // Without authentication, should be forbidden
    mockMvc.perform(get("/api/projects")).andExpect(status().isForbidden());
  }

  @Test
  void createProject_withoutAuth_shouldReturn403() throws Exception {
    CreateProjectRequestDto request = new CreateProjectRequestDto();
    request.setProjectKey("TEST");
    request.setName("Test Project");

    mockMvc
        .perform(
            post("/api/projects")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
        .andExpect(status().isForbidden());
  }
}
