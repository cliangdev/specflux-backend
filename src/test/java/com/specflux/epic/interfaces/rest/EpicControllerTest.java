package com.specflux.epic.interfaces.rest;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

import com.specflux.api.generated.model.CreateAcceptanceCriteriaRequestDto;
import com.specflux.api.generated.model.CreateEpicRequestDto;
import com.specflux.api.generated.model.EpicStatusDto;
import com.specflux.api.generated.model.UpdateAcceptanceCriteriaRequestDto;
import com.specflux.api.generated.model.UpdateEpicRequestDto;
import com.specflux.common.AbstractControllerIntegrationTest;
import com.specflux.epic.domain.Epic;
import com.specflux.epic.domain.EpicRepository;
import com.specflux.epic.domain.EpicStatus;
import com.specflux.project.domain.Project;
import com.specflux.project.domain.ProjectRepository;

/**
 * Integration tests for EpicController.
 *
 * <p>Uses schema isolation for parallel test execution.
 */
class EpicControllerTest extends AbstractControllerIntegrationTest {

  @DynamicPropertySource
  static void configureSchema(DynamicPropertyRegistry registry) {
    configureSchemaForClass(registry, EpicControllerTest.class);
  }

  @Autowired private ProjectRepository projectRepository;
  @Autowired private EpicRepository epicRepository;

  private Project testProject;

  @BeforeEach
  void setUpProject() {
    testProject =
        projectRepository.save(
            new Project("proj_epic_test", "EPIC", "Epic Test Project", testUser));
  }

  @Test
  void createEpic_shouldReturnCreatedEpic() throws Exception {
    CreateEpicRequestDto request = new CreateEpicRequestDto();
    request.setTitle("User Authentication Feature");
    request.setDescription("Implement OAuth2 authentication");

    mockMvc
        .perform(
            post("/api/projects/{projectRef}/epics", testProject.getPublicId())
                .with(user("user"))
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.id").exists())
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
  void createEpic_usingProjectKey_shouldReturnCreatedEpic() throws Exception {
    CreateEpicRequestDto request = new CreateEpicRequestDto();
    request.setTitle("Dashboard Feature");

    mockMvc
        .perform(
            post("/api/projects/{projectRef}/epics", testProject.getProjectKey())
                .with(user("user"))
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.title").value("Dashboard Feature"));
  }

  @Test
  void createEpic_projectNotFound_shouldReturn404() throws Exception {
    CreateEpicRequestDto request = new CreateEpicRequestDto();
    request.setTitle("Test Epic");

    mockMvc
        .perform(
            post("/api/projects/{projectRef}/epics", "nonexistent")
                .with(user("user"))
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
        .andExpect(status().isNotFound())
        .andExpect(jsonPath("$.code").value("NOT_FOUND"));
  }

  @Test
  void getEpic_byPublicId_shouldReturnEpic() throws Exception {
    Epic epic =
        epicRepository.save(
            new Epic("epic_test123", testProject, 1, "EPIC-E1", "Test Epic", testUser));

    mockMvc
        .perform(
            get(
                    "/api/projects/{projectRef}/epics/{epicRef}",
                    testProject.getPublicId(),
                    "epic_test123")
                .with(user("user")))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.id").value("epic_test123"))
        .andExpect(jsonPath("$.displayKey").value("EPIC-E1"))
        .andExpect(jsonPath("$.title").value("Test Epic"));
  }

  @Test
  void getEpic_byDisplayKey_shouldReturnEpic() throws Exception {
    Epic epic =
        epicRepository.save(
            new Epic("epic_bykey", testProject, 2, "EPIC-E2", "Epic By Key", testUser));

    mockMvc
        .perform(
            get(
                    "/api/projects/{projectRef}/epics/{epicRef}",
                    testProject.getProjectKey(),
                    "EPIC-E2")
                .with(user("user")))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.id").value("epic_bykey"))
        .andExpect(jsonPath("$.displayKey").value("EPIC-E2"));
  }

  @Test
  void getEpic_notFound_shouldReturn404() throws Exception {
    mockMvc
        .perform(
            get(
                    "/api/projects/{projectRef}/epics/{epicRef}",
                    testProject.getPublicId(),
                    "nonexistent")
                .with(user("user")))
        .andExpect(status().isNotFound())
        .andExpect(jsonPath("$.code").value("NOT_FOUND"));
  }

  @Test
  void updateEpic_shouldReturnUpdatedEpic() throws Exception {
    Epic epic =
        epicRepository.save(
            new Epic("epic_update", testProject, 1, "EPIC-E1", "Original Title", testUser));

    UpdateEpicRequestDto request = new UpdateEpicRequestDto();
    request.setTitle("Updated Title");
    request.setDescription("New description");
    request.setStatus(EpicStatusDto.IN_PROGRESS);

    mockMvc
        .perform(
            put(
                    "/api/projects/{projectRef}/epics/{epicRef}",
                    testProject.getPublicId(),
                    "epic_update")
                .with(user("user"))
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.title").value("Updated Title"))
        .andExpect(jsonPath("$.description").value("New description"))
        .andExpect(jsonPath("$.status").value("IN_PROGRESS"));
  }

  @Test
  void updateEpic_partialUpdate_shouldOnlyUpdateProvidedFields() throws Exception {
    Epic epic = new Epic("epic_partial", testProject, 1, "EPIC-E1", "Original Title", testUser);
    epic.setDescription("Original description");
    epicRepository.save(epic);

    UpdateEpicRequestDto request = new UpdateEpicRequestDto();
    request.setTitle("New Title");
    // other fields not set

    mockMvc
        .perform(
            put(
                    "/api/projects/{projectRef}/epics/{epicRef}",
                    testProject.getPublicId(),
                    "epic_partial")
                .with(user("user"))
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.title").value("New Title"))
        .andExpect(jsonPath("$.description").value("Original description"))
        .andExpect(jsonPath("$.status").value("PLANNING"));
  }

  @Test
  void deleteEpic_shouldReturn204() throws Exception {
    Epic epic =
        epicRepository.save(
            new Epic("epic_delete", testProject, 1, "EPIC-E1", "To Delete", testUser));

    mockMvc
        .perform(
            delete(
                    "/api/projects/{projectRef}/epics/{epicRef}",
                    testProject.getPublicId(),
                    "epic_delete")
                .with(user("user")))
        .andExpect(status().isNoContent());

    // Verify deletion
    mockMvc
        .perform(
            get(
                    "/api/projects/{projectRef}/epics/{epicRef}",
                    testProject.getPublicId(),
                    "epic_delete")
                .with(user("user")))
        .andExpect(status().isNotFound());
  }

  @Test
  void listEpics_shouldReturnPaginatedList() throws Exception {
    // Create test epics
    epicRepository.save(new Epic("epic_list1", testProject, 1, "EPIC-E1", "Epic 1", testUser));
    epicRepository.save(new Epic("epic_list2", testProject, 2, "EPIC-E2", "Epic 2", testUser));
    epicRepository.save(new Epic("epic_list3", testProject, 3, "EPIC-E3", "Epic 3", testUser));

    mockMvc
        .perform(
            get("/api/projects/{projectRef}/epics", testProject.getPublicId())
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
  void listEpics_withStatusFilter_shouldReturnFilteredList() throws Exception {
    Epic epic1 = new Epic("epic_plan", testProject, 1, "EPIC-E1", "Planning Epic", testUser);
    epic1.setStatus(EpicStatus.PLANNING);
    epicRepository.save(epic1);

    Epic epic2 = new Epic("epic_prog", testProject, 2, "EPIC-E2", "In Progress Epic", testUser);
    epic2.setStatus(EpicStatus.IN_PROGRESS);
    epicRepository.save(epic2);

    mockMvc
        .perform(
            get("/api/projects/{projectRef}/epics", testProject.getPublicId())
                .with(user("user"))
                .param("status", "IN_PROGRESS"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data.length()").value(1))
        .andExpect(jsonPath("$.data[0].status").value("IN_PROGRESS"));
  }

  @Test
  void listEpics_withSort_shouldReturnSortedList() throws Exception {
    epicRepository.save(new Epic("epic_z", testProject, 1, "EPIC-E1", "Zeta Epic", testUser));
    epicRepository.save(new Epic("epic_a", testProject, 2, "EPIC-E2", "Alpha Epic", testUser));

    mockMvc
        .perform(
            get("/api/projects/{projectRef}/epics", testProject.getPublicId())
                .with(user("user"))
                .param("sort", "title")
                .param("order", "asc"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data[0].title").value("Alpha Epic"))
        .andExpect(jsonPath("$.data[1].title").value("Zeta Epic"));
  }

  @Test
  void listEpics_emptyProject_shouldReturnEmptyList() throws Exception {
    mockMvc
        .perform(
            get("/api/projects/{projectRef}/epics", testProject.getPublicId()).with(user("user")))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data").isArray())
        .andExpect(jsonPath("$.data.length()").value(0))
        .andExpect(jsonPath("$.pagination.total").value(0))
        .andExpect(jsonPath("$.pagination.hasMore").value(false));
  }

  @Test
  void createEpic_sequentialNumbers_shouldIncrementCorrectly() throws Exception {
    // Create first epic
    CreateEpicRequestDto request1 = new CreateEpicRequestDto();
    request1.setTitle("First Epic");
    mockMvc
        .perform(
            post("/api/projects/{projectRef}/epics", testProject.getPublicId())
                .with(user("user"))
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request1)))
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.displayKey").value("EPIC-E1"));

    // Create second epic
    CreateEpicRequestDto request2 = new CreateEpicRequestDto();
    request2.setTitle("Second Epic");
    mockMvc
        .perform(
            post("/api/projects/{projectRef}/epics", testProject.getPublicId())
                .with(user("user"))
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request2)))
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.displayKey").value("EPIC-E2"));
  }

  @Test
  void listEpics_withoutAuth_shouldReturn403() throws Exception {
    mockMvc
        .perform(get("/api/projects/{projectRef}/epics", testProject.getPublicId()))
        .andExpect(status().isForbidden());
  }

  @Test
  void createEpic_withoutAuth_shouldReturn403() throws Exception {
    CreateEpicRequestDto request = new CreateEpicRequestDto();
    request.setTitle("Test Epic");

    mockMvc
        .perform(
            post("/api/projects/{projectRef}/epics", testProject.getPublicId())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
        .andExpect(status().isForbidden());
  }

  // ==================== EPIC ACCEPTANCE CRITERIA TESTS ====================

  @Test
  void createEpicAcceptanceCriteria_shouldReturnCreatedCriteria() throws Exception {
    Epic epic =
        epicRepository.save(new Epic("epic_ac1", testProject, 1, "EPIC-E1", "Test Epic", testUser));

    CreateAcceptanceCriteriaRequestDto request = new CreateAcceptanceCriteriaRequestDto();
    request.setCriteria("All user stories must be implemented");
    request.setOrderIndex(0);

    mockMvc
        .perform(
            post(
                    "/api/projects/{projectRef}/epics/{epicRef}/acceptance-criteria",
                    testProject.getPublicId(),
                    epic.getPublicId())
                .with(user("user"))
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.id").exists())
        .andExpect(jsonPath("$.criteria").value("All user stories must be implemented"))
        .andExpect(jsonPath("$.isMet").value(false))
        .andExpect(jsonPath("$.orderIndex").value(0))
        .andExpect(jsonPath("$.createdAt").exists());
  }

  @Test
  void listEpicAcceptanceCriteria_shouldReturnOrderedList() throws Exception {
    Epic epic =
        epicRepository.save(new Epic("epic_ac2", testProject, 1, "EPIC-E1", "Test Epic", testUser));

    // Create criteria
    CreateAcceptanceCriteriaRequestDto request1 = new CreateAcceptanceCriteriaRequestDto();
    request1.setCriteria("First criterion");
    request1.setOrderIndex(0);
    mockMvc
        .perform(
            post(
                    "/api/projects/{projectRef}/epics/{epicRef}/acceptance-criteria",
                    testProject.getPublicId(),
                    epic.getPublicId())
                .with(user("user"))
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request1)))
        .andExpect(status().isCreated());

    CreateAcceptanceCriteriaRequestDto request2 = new CreateAcceptanceCriteriaRequestDto();
    request2.setCriteria("Second criterion");
    request2.setOrderIndex(1);
    mockMvc
        .perform(
            post(
                    "/api/projects/{projectRef}/epics/{epicRef}/acceptance-criteria",
                    testProject.getPublicId(),
                    epic.getPublicId())
                .with(user("user"))
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request2)))
        .andExpect(status().isCreated());

    // List should return in order
    mockMvc
        .perform(
            get(
                    "/api/projects/{projectRef}/epics/{epicRef}/acceptance-criteria",
                    testProject.getPublicId(),
                    epic.getPublicId())
                .with(user("user")))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data").isArray())
        .andExpect(jsonPath("$.data.length()").value(2))
        .andExpect(jsonPath("$.data[0].criteria").value("First criterion"))
        .andExpect(jsonPath("$.data[1].criteria").value("Second criterion"));
  }

  @Test
  void getEpicAcceptanceCriteria_shouldReturnCriteria() throws Exception {
    Epic epic =
        epicRepository.save(new Epic("epic_ac3", testProject, 1, "EPIC-E1", "Test Epic", testUser));

    // Create criteria
    CreateAcceptanceCriteriaRequestDto request = new CreateAcceptanceCriteriaRequestDto();
    request.setCriteria("Test epic criterion");

    String response =
        mockMvc
            .perform(
                post(
                        "/api/projects/{projectRef}/epics/{epicRef}/acceptance-criteria",
                        testProject.getPublicId(),
                        epic.getPublicId())
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
                    "/api/projects/{projectRef}/epics/{epicRef}/acceptance-criteria/{criteriaId}",
                    testProject.getPublicId(),
                    epic.getPublicId(),
                    criteriaId)
                .with(user("user")))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.id").value(criteriaId))
        .andExpect(jsonPath("$.criteria").value("Test epic criterion"));
  }

  @Test
  void updateEpicAcceptanceCriteria_shouldUpdateFields() throws Exception {
    Epic epic =
        epicRepository.save(new Epic("epic_ac4", testProject, 1, "EPIC-E1", "Test Epic", testUser));

    // Create criteria
    CreateAcceptanceCriteriaRequestDto createRequest = new CreateAcceptanceCriteriaRequestDto();
    createRequest.setCriteria("Original criterion");

    String response =
        mockMvc
            .perform(
                post(
                        "/api/projects/{projectRef}/epics/{epicRef}/acceptance-criteria",
                        testProject.getPublicId(),
                        epic.getPublicId())
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

    mockMvc
        .perform(
            put(
                    "/api/projects/{projectRef}/epics/{epicRef}/acceptance-criteria/{criteriaId}",
                    testProject.getPublicId(),
                    epic.getPublicId(),
                    criteriaId)
                .with(user("user"))
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(updateRequest)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.id").value(criteriaId))
        .andExpect(jsonPath("$.criteria").value("Updated criterion"))
        .andExpect(jsonPath("$.isMet").value(true));
  }

  @Test
  void deleteEpicAcceptanceCriteria_shouldDeleteCriteria() throws Exception {
    Epic epic =
        epicRepository.save(new Epic("epic_ac5", testProject, 1, "EPIC-E1", "Test Epic", testUser));

    // Create criteria
    CreateAcceptanceCriteriaRequestDto createRequest = new CreateAcceptanceCriteriaRequestDto();
    createRequest.setCriteria("To be deleted");

    String response =
        mockMvc
            .perform(
                post(
                        "/api/projects/{projectRef}/epics/{epicRef}/acceptance-criteria",
                        testProject.getPublicId(),
                        epic.getPublicId())
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
                    "/api/projects/{projectRef}/epics/{epicRef}/acceptance-criteria/{criteriaId}",
                    testProject.getPublicId(),
                    epic.getPublicId(),
                    criteriaId)
                .with(user("user")))
        .andExpect(status().isNoContent());

    // Verify deletion
    mockMvc
        .perform(
            get(
                    "/api/projects/{projectRef}/epics/{epicRef}/acceptance-criteria/{criteriaId}",
                    testProject.getPublicId(),
                    epic.getPublicId(),
                    criteriaId)
                .with(user("user")))
        .andExpect(status().isNotFound());
  }

  @Test
  void getEpicAcceptanceCriteria_notFound_shouldReturn404() throws Exception {
    Epic epic =
        epicRepository.save(new Epic("epic_ac6", testProject, 1, "EPIC-E1", "Test Epic", testUser));

    mockMvc
        .perform(
            get(
                    "/api/projects/{projectRef}/epics/{epicRef}/acceptance-criteria/{criteriaId}",
                    testProject.getPublicId(),
                    epic.getPublicId(),
                    999999L)
                .with(user("user")))
        .andExpect(status().isNotFound());
  }

  @Test
  void listEpicAcceptanceCriteria_withoutAuth_shouldReturn403() throws Exception {
    mockMvc
        .perform(
            get(
                "/api/projects/{projectRef}/epics/{epicRef}/acceptance-criteria",
                testProject.getPublicId(),
                "epic_123"))
        .andExpect(status().isForbidden());
  }
}
