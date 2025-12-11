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
import com.specflux.api.generated.model.CreateEpicRequestAcceptanceCriteriaInnerDto;
import com.specflux.api.generated.model.CreateEpicRequestDto;
import com.specflux.api.generated.model.EpicStatusDto;
import com.specflux.api.generated.model.UpdateAcceptanceCriteriaRequestDto;
import com.specflux.api.generated.model.UpdateEpicRequestDto;
import com.specflux.common.AbstractControllerIntegrationTest;
import com.specflux.epic.domain.Epic;
import com.specflux.epic.domain.EpicRepository;
import com.specflux.epic.domain.EpicStatus;
import com.specflux.prd.domain.Prd;
import com.specflux.prd.domain.PrdRepository;
import com.specflux.project.domain.Project;
import com.specflux.project.domain.ProjectRepository;
import com.specflux.release.domain.Release;
import com.specflux.release.domain.ReleaseRepository;

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
  @Autowired private PrdRepository prdRepository;
  @Autowired private ReleaseRepository releaseRepository;

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
    request.addAcceptanceCriteriaItem(
        new CreateEpicRequestAcceptanceCriteriaInnerDto().criteria("Users can log in with OAuth2"));

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
    request.addAcceptanceCriteriaItem(
        new CreateEpicRequestAcceptanceCriteriaInnerDto().criteria("Dashboard displays metrics"));

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
    request.addAcceptanceCriteriaItem(
        new CreateEpicRequestAcceptanceCriteriaInnerDto().criteria("Test criteria"));

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
  void updateEpic_withNotes_shouldUpdateNotes() throws Exception {
    Epic epic =
        epicRepository.save(
            new Epic("epic_notes", testProject, 1, "EPIC-E1", "Epic With Notes", testUser));

    UpdateEpicRequestDto request = new UpdateEpicRequestDto();
    request.setNotes("Session handoff: Completed task 1, working on task 2");

    mockMvc
        .perform(
            put(
                    "/api/projects/{projectRef}/epics/{epicRef}",
                    testProject.getPublicId(),
                    "epic_notes")
                .with(user("user"))
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
        .andExpect(status().isOk())
        .andExpect(
            jsonPath("$.notes").value("Session handoff: Completed task 1, working on task 2"));
  }

  @Test
  void getEpic_shouldReturnNotesField() throws Exception {
    Epic epic = new Epic("epic_getnotes", testProject, 1, "EPIC-E1", "Epic With Notes", testUser);
    epic.setNotes("Some session notes");
    epicRepository.save(epic);

    mockMvc
        .perform(
            get(
                    "/api/projects/{projectRef}/epics/{epicRef}",
                    testProject.getPublicId(),
                    "epic_getnotes")
                .with(user("user")))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.notes").value("Some session notes"));
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
  void listEpics_withPrdRefFilter_shouldReturnFilteredList() throws Exception {
    // Create PRDs
    Prd prd1 =
        prdRepository.save(
            new Prd(
                "prd_test1",
                testProject,
                1,
                "EPIC-P1",
                "Auth PRD",
                ".specflux/prds/auth",
                testUser));
    Prd prd2 =
        prdRepository.save(
            new Prd(
                "prd_test2",
                testProject,
                2,
                "EPIC-P2",
                "Dashboard PRD",
                ".specflux/prds/dashboard",
                testUser));

    // Create epics linked to PRDs
    Epic epic1 = new Epic("epic_prd1", testProject, 1, "EPIC-E1", "Auth Epic", testUser);
    epic1.setPrdId(prd1.getId());
    epicRepository.save(epic1);

    Epic epic2 = new Epic("epic_prd2", testProject, 2, "EPIC-E2", "Dashboard Epic", testUser);
    epic2.setPrdId(prd2.getId());
    epicRepository.save(epic2);

    Epic epic3 = new Epic("epic_noprd", testProject, 3, "EPIC-E3", "No PRD Epic", testUser);
    epicRepository.save(epic3);

    // Filter by prdRef (public ID)
    mockMvc
        .perform(
            get("/api/projects/{projectRef}/epics", testProject.getPublicId())
                .with(user("user"))
                .param("prdRef", prd1.getPublicId()))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data.length()").value(1))
        .andExpect(jsonPath("$.data[0].title").value("Auth Epic"));
  }

  @Test
  void listEpics_withPrdRefFilter_byDisplayKey_shouldReturnFilteredList() throws Exception {
    // Create PRD
    Prd prd =
        prdRepository.save(
            new Prd(
                "prd_dispkey",
                testProject,
                1,
                "EPIC-P1",
                "Feature PRD",
                ".specflux/prds/feature",
                testUser));

    // Create epic linked to PRD
    Epic epic = new Epic("epic_dispkey", testProject, 1, "EPIC-E1", "Feature Epic", testUser);
    epic.setPrdId(prd.getId());
    epicRepository.save(epic);

    // Filter by prdRef using display key
    mockMvc
        .perform(
            get("/api/projects/{projectRef}/epics", testProject.getPublicId())
                .with(user("user"))
                .param("prdRef", "EPIC-P1"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data.length()").value(1))
        .andExpect(jsonPath("$.data[0].title").value("Feature Epic"));
  }

  @Test
  void listEpics_withPrdRefAndStatusFilter_shouldReturnFilteredList() throws Exception {
    // Create PRD
    Prd prd =
        prdRepository.save(
            new Prd(
                "prd_combined",
                testProject,
                1,
                "EPIC-P1",
                "Combined PRD",
                ".specflux/prds/combined",
                testUser));

    // Create epics with different statuses linked to same PRD
    Epic epic1 = new Epic("epic_comb1", testProject, 1, "EPIC-E1", "Planning Epic", testUser);
    epic1.setPrdId(prd.getId());
    epic1.setStatus(EpicStatus.PLANNING);
    epicRepository.save(epic1);

    Epic epic2 = new Epic("epic_comb2", testProject, 2, "EPIC-E2", "In Progress Epic", testUser);
    epic2.setPrdId(prd.getId());
    epic2.setStatus(EpicStatus.IN_PROGRESS);
    epicRepository.save(epic2);

    // Filter by both prdRef and status
    mockMvc
        .perform(
            get("/api/projects/{projectRef}/epics", testProject.getPublicId())
                .with(user("user"))
                .param("prdRef", prd.getPublicId())
                .param("status", "IN_PROGRESS"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data.length()").value(1))
        .andExpect(jsonPath("$.data[0].title").value("In Progress Epic"))
        .andExpect(jsonPath("$.data[0].status").value("IN_PROGRESS"));
  }

  @Test
  void listEpics_withInvalidPrdRef_shouldReturn404() throws Exception {
    mockMvc
        .perform(
            get("/api/projects/{projectRef}/epics", testProject.getPublicId())
                .with(user("user"))
                .param("prdRef", "nonexistent_prd"))
        .andExpect(status().isNotFound())
        .andExpect(jsonPath("$.code").value("NOT_FOUND"));
  }

  @Test
  void listEpics_withReleaseRefFilter_shouldReturnFilteredList() throws Exception {
    // Create releases
    Release release1 =
        releaseRepository.save(new Release("rel_test1", testProject, 1, "EPIC-R1", "v1.0"));
    Release release2 =
        releaseRepository.save(new Release("rel_test2", testProject, 2, "EPIC-R2", "v2.0"));

    // Create epics linked to releases
    Epic epic1 = new Epic("epic_rel1", testProject, 1, "EPIC-E1", "Release 1 Epic", testUser);
    epic1.setReleaseId(release1.getId());
    epicRepository.save(epic1);

    Epic epic2 = new Epic("epic_rel2", testProject, 2, "EPIC-E2", "Release 2 Epic", testUser);
    epic2.setReleaseId(release2.getId());
    epicRepository.save(epic2);

    Epic epic3 = new Epic("epic_norel", testProject, 3, "EPIC-E3", "No Release Epic", testUser);
    epicRepository.save(epic3);

    // Filter by releaseRef (public ID)
    mockMvc
        .perform(
            get("/api/projects/{projectRef}/epics", testProject.getPublicId())
                .with(user("user"))
                .param("releaseRef", release1.getPublicId()))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data.length()").value(1))
        .andExpect(jsonPath("$.data[0].title").value("Release 1 Epic"));
  }

  @Test
  void listEpics_withReleaseRefFilter_byDisplayKey_shouldReturnFilteredList() throws Exception {
    // Create release
    Release release =
        releaseRepository.save(new Release("rel_dispkey", testProject, 1, "EPIC-R1", "v1.0-MVP"));

    // Create epic linked to release
    Epic epic = new Epic("epic_reldispkey", testProject, 1, "EPIC-E1", "MVP Epic", testUser);
    epic.setReleaseId(release.getId());
    epicRepository.save(epic);

    // Filter by releaseRef using display key
    mockMvc
        .perform(
            get("/api/projects/{projectRef}/epics", testProject.getPublicId())
                .with(user("user"))
                .param("releaseRef", "EPIC-R1"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data.length()").value(1))
        .andExpect(jsonPath("$.data[0].title").value("MVP Epic"));
  }

  @Test
  void listEpics_withReleaseRefAndStatusFilter_shouldReturnFilteredList() throws Exception {
    // Create release
    Release release =
        releaseRepository.save(new Release("rel_combined", testProject, 1, "EPIC-R1", "v1.0"));

    // Create epics with different statuses linked to same release
    Epic epic1 = new Epic("epic_relcomb1", testProject, 1, "EPIC-E1", "Planning Epic", testUser);
    epic1.setReleaseId(release.getId());
    epic1.setStatus(EpicStatus.PLANNING);
    epicRepository.save(epic1);

    Epic epic2 = new Epic("epic_relcomb2", testProject, 2, "EPIC-E2", "In Progress Epic", testUser);
    epic2.setReleaseId(release.getId());
    epic2.setStatus(EpicStatus.IN_PROGRESS);
    epicRepository.save(epic2);

    // Filter by both releaseRef and status
    mockMvc
        .perform(
            get("/api/projects/{projectRef}/epics", testProject.getPublicId())
                .with(user("user"))
                .param("releaseRef", release.getPublicId())
                .param("status", "IN_PROGRESS"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data.length()").value(1))
        .andExpect(jsonPath("$.data[0].title").value("In Progress Epic"))
        .andExpect(jsonPath("$.data[0].status").value("IN_PROGRESS"));
  }

  @Test
  void listEpics_withInvalidReleaseRef_shouldReturn404() throws Exception {
    mockMvc
        .perform(
            get("/api/projects/{projectRef}/epics", testProject.getPublicId())
                .with(user("user"))
                .param("releaseRef", "nonexistent_release"))
        .andExpect(status().isNotFound())
        .andExpect(jsonPath("$.code").value("NOT_FOUND"));
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
    request1.addAcceptanceCriteriaItem(
        new CreateEpicRequestAcceptanceCriteriaInnerDto().criteria("First epic criteria"));
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
    request2.addAcceptanceCriteriaItem(
        new CreateEpicRequestAcceptanceCriteriaInnerDto().criteria("Second epic criteria"));
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
    request.addAcceptanceCriteriaItem(
        new CreateEpicRequestAcceptanceCriteriaInnerDto().criteria("Test criteria"));

    mockMvc
        .perform(
            post("/api/projects/{projectRef}/epics", testProject.getPublicId())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
        .andExpect(status().isForbidden());
  }

  @Test
  void createEpic_withoutAcceptanceCriteria_shouldReturn400() throws Exception {
    CreateEpicRequestDto request = new CreateEpicRequestDto();
    request.setTitle("Test Epic Without AC");
    // No acceptance criteria added

    mockMvc
        .perform(
            post("/api/projects/{projectRef}/epics", testProject.getPublicId())
                .with(user("user"))
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
        .andExpect(status().isBadRequest());
  }

  @Test
  void createEpic_withEmptyAcceptanceCriteria_shouldReturn400() throws Exception {
    // Test with explicitly empty array
    String jsonRequest = "{\"title\": \"Test Epic\", \"acceptanceCriteria\": []}";

    mockMvc
        .perform(
            post("/api/projects/{projectRef}/epics", testProject.getPublicId())
                .with(user("user"))
                .contentType(MediaType.APPLICATION_JSON)
                .content(jsonRequest))
        .andExpect(status().isBadRequest());
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
