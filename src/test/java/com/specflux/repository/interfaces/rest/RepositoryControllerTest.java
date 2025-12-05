package com.specflux.repository.interfaces.rest;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

import com.specflux.api.generated.model.CreateRepositoryRequestDto;
import com.specflux.api.generated.model.UpdateRepositoryRequestDto;
import com.specflux.common.AbstractControllerIntegrationTest;
import com.specflux.project.domain.Project;
import com.specflux.project.domain.ProjectRepository;
import com.specflux.repository.domain.Repository;
import com.specflux.repository.domain.RepositoryRepository;

/**
 * Integration tests for RepositoryController.
 *
 * <p>Uses schema isolation for parallel test execution.
 */
class RepositoryControllerTest extends AbstractControllerIntegrationTest {

  private static final String SCHEMA_NAME = "repository_controller_test";

  @DynamicPropertySource
  static void configureSchema(DynamicPropertyRegistry registry) {
    AbstractControllerIntegrationTest.configureSchema(registry, SCHEMA_NAME);
  }

  @Autowired private ProjectRepository projectRepository;
  @Autowired private RepositoryRepository repositoryRepository;

  private Project testProject;

  @BeforeEach
  void setUpProject() {
    testProject =
        projectRepository.save(new Project("proj_repo_test", "REPO", "Repo Test", testUser));
  }

  @Test
  void createRepository_shouldReturnCreatedRepository() throws Exception {
    CreateRepositoryRequestDto request = new CreateRepositoryRequestDto();
    request.setName("backend");
    request.setPath("/Users/dev/projects/backend");
    request.setGitUrl("https://github.com/org/backend.git");

    mockMvc
        .perform(
            post("/api/projects/{projectRef}/repositories", testProject.getPublicId())
                .with(user("user"))
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.id").exists())
        .andExpect(jsonPath("$.name").value("backend"))
        .andExpect(jsonPath("$.path").value("/Users/dev/projects/backend"))
        .andExpect(jsonPath("$.gitUrl").value("https://github.com/org/backend.git"))
        .andExpect(jsonPath("$.status").value("READY"))
        .andExpect(jsonPath("$.createdAt").exists())
        .andExpect(jsonPath("$.updatedAt").exists());
  }

  @Test
  void createRepository_withDuplicatePath_shouldReturn409() throws Exception {
    // Create existing repository
    repositoryRepository.save(
        new Repository("repo_existing", testProject, "existing", "/Users/dev/existing"));

    CreateRepositoryRequestDto request = new CreateRepositoryRequestDto();
    request.setName("new-repo");
    request.setPath("/Users/dev/existing"); // Same path

    mockMvc
        .perform(
            post("/api/projects/{projectRef}/repositories", testProject.getPublicId())
                .with(user("user"))
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
        .andExpect(status().isConflict())
        .andExpect(jsonPath("$.code").value("CONFLICT"));
  }

  @Test
  void createRepository_withMissingName_shouldReturn400() throws Exception {
    CreateRepositoryRequestDto request = new CreateRepositoryRequestDto();
    request.setPath("/Users/dev/projects/backend");
    // name is missing

    mockMvc
        .perform(
            post("/api/projects/{projectRef}/repositories", testProject.getPublicId())
                .with(user("user"))
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));
  }

  @Test
  void getRepository_byPublicId_shouldReturnRepository() throws Exception {
    Repository repo =
        repositoryRepository.save(
            new Repository("repo_get_test", testProject, "test-repo", "/Users/dev/test"));

    mockMvc
        .perform(
            get(
                    "/api/projects/{projectRef}/repositories/{repoRef}",
                    testProject.getPublicId(),
                    repo.getPublicId())
                .with(user("user")))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.id").value("repo_get_test"))
        .andExpect(jsonPath("$.name").value("test-repo"))
        .andExpect(jsonPath("$.path").value("/Users/dev/test"));
  }

  @Test
  void getRepository_notFound_shouldReturn404() throws Exception {
    mockMvc
        .perform(
            get(
                    "/api/projects/{projectRef}/repositories/{repoRef}",
                    testProject.getPublicId(),
                    "nonexistent")
                .with(user("user")))
        .andExpect(status().isNotFound())
        .andExpect(jsonPath("$.code").value("NOT_FOUND"));
  }

  @Test
  void updateRepository_shouldReturnUpdatedRepository() throws Exception {
    Repository repo =
        repositoryRepository.save(
            new Repository("repo_update", testProject, "original", "/Users/dev/original"));

    UpdateRepositoryRequestDto request = new UpdateRepositoryRequestDto();
    request.setName("updated-name");
    request.setGitUrl("https://github.com/new/url.git");

    mockMvc
        .perform(
            put(
                    "/api/projects/{projectRef}/repositories/{repoRef}",
                    testProject.getPublicId(),
                    repo.getPublicId())
                .with(user("user"))
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.name").value("updated-name"))
        .andExpect(jsonPath("$.gitUrl").value("https://github.com/new/url.git"));
  }

  @Test
  void deleteRepository_shouldReturn204() throws Exception {
    Repository repo =
        repositoryRepository.save(
            new Repository("repo_delete", testProject, "to-delete", "/Users/dev/delete"));

    mockMvc
        .perform(
            delete(
                    "/api/projects/{projectRef}/repositories/{repoRef}",
                    testProject.getPublicId(),
                    repo.getPublicId())
                .with(user("user")))
        .andExpect(status().isNoContent());

    // Verify deletion
    mockMvc
        .perform(
            get(
                    "/api/projects/{projectRef}/repositories/{repoRef}",
                    testProject.getPublicId(),
                    repo.getPublicId())
                .with(user("user")))
        .andExpect(status().isNotFound());
  }

  @Test
  void listRepositories_shouldReturnList() throws Exception {
    repositoryRepository.save(new Repository("repo_list1", testProject, "repo1", "/dev/repo1"));
    repositoryRepository.save(new Repository("repo_list2", testProject, "repo2", "/dev/repo2"));

    mockMvc
        .perform(
            get("/api/projects/{projectRef}/repositories", testProject.getPublicId())
                .with(user("user")))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data").isArray())
        .andExpect(jsonPath("$.data.length()").value(2));
  }

  @Test
  void listRepositories_emptyProject_shouldReturnEmptyList() throws Exception {
    mockMvc
        .perform(
            get("/api/projects/{projectRef}/repositories", testProject.getPublicId())
                .with(user("user")))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data").isArray())
        .andExpect(jsonPath("$.data.length()").value(0));
  }

  @Test
  void listRepositories_withoutAuth_shouldReturn403() throws Exception {
    mockMvc
        .perform(get("/api/projects/{projectRef}/repositories", testProject.getPublicId()))
        .andExpect(status().isForbidden());
  }
}
