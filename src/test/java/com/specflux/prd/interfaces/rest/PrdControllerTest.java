package com.specflux.prd.interfaces.rest;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

import com.specflux.api.generated.model.AddPrdDocumentRequestDto;
import com.specflux.api.generated.model.CreatePrdRequestDto;
import com.specflux.api.generated.model.PrdDocumentTypeDto;
import com.specflux.api.generated.model.PrdStatusDto;
import com.specflux.api.generated.model.UpdatePrdDocumentRequestDto;
import com.specflux.api.generated.model.UpdatePrdRequestDto;
import com.specflux.common.AbstractControllerIntegrationTest;
import com.specflux.prd.domain.Prd;
import com.specflux.prd.domain.PrdDocument;
import com.specflux.prd.domain.PrdDocumentType;
import com.specflux.prd.domain.PrdRepository;
import com.specflux.prd.domain.PrdStatus;
import com.specflux.project.domain.Project;
import com.specflux.project.domain.ProjectRepository;

/**
 * Integration tests for PrdController.
 *
 * <p>Uses schema isolation for parallel test execution.
 */
class PrdControllerTest extends AbstractControllerIntegrationTest {

  @DynamicPropertySource
  static void configureSchema(DynamicPropertyRegistry registry) {
    configureSchemaForClass(registry, PrdControllerTest.class);
  }

  @Autowired private ProjectRepository projectRepository;
  @Autowired private PrdRepository prdRepository;

  private Project testProject;

  @BeforeEach
  void setUpProject() {
    testProject =
        projectRepository.save(new Project("proj_prd_test", "PRDT", "PRD Test Project", testUser));
  }

  // ==================== CREATE PRD TESTS ====================

  @Test
  void createPrd_shouldReturnCreatedPrd() throws Exception {
    CreatePrdRequestDto request = new CreatePrdRequestDto();
    request.setTitle("Authentication System");
    request.setDescription("User authentication with OAuth2");

    mockMvc
        .perform(
            post("/api/projects/{projectRef}/prds", testProject.getPublicId())
                .with(user("user"))
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.id").exists())
        .andExpect(jsonPath("$.displayKey").value("PRDT-P1"))
        .andExpect(jsonPath("$.projectId").value(testProject.getPublicId()))
        .andExpect(jsonPath("$.title").value("Authentication System"))
        .andExpect(jsonPath("$.description").value("User authentication with OAuth2"))
        .andExpect(jsonPath("$.status").value("DRAFT"))
        .andExpect(jsonPath("$.folderPath").value(".specflux/prds/authentication-system"))
        .andExpect(jsonPath("$.documents").isArray())
        .andExpect(jsonPath("$.documents.length()").value(0))
        .andExpect(jsonPath("$.documentCount").value(0))
        .andExpect(jsonPath("$.createdById").value(testUser.getPublicId()))
        .andExpect(jsonPath("$.createdAt").exists())
        .andExpect(jsonPath("$.updatedAt").exists());
  }

  @Test
  void createPrd_usingProjectKey_shouldReturnCreatedPrd() throws Exception {
    CreatePrdRequestDto request = new CreatePrdRequestDto();
    request.setTitle("Dashboard Feature");

    mockMvc
        .perform(
            post("/api/projects/{projectRef}/prds", testProject.getProjectKey())
                .with(user("user"))
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.title").value("Dashboard Feature"));
  }

  @Test
  void createPrd_withCustomFolderPath_shouldUseFolderPath() throws Exception {
    CreatePrdRequestDto request = new CreatePrdRequestDto();
    request.setTitle("Custom PRD");
    request.setFolderPath(".specflux/prds/custom-path");

    mockMvc
        .perform(
            post("/api/projects/{projectRef}/prds", testProject.getPublicId())
                .with(user("user"))
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.folderPath").value(".specflux/prds/custom-path"));
  }

  @Test
  void createPrd_projectNotFound_shouldReturn404() throws Exception {
    CreatePrdRequestDto request = new CreatePrdRequestDto();
    request.setTitle("Test PRD");

    mockMvc
        .perform(
            post("/api/projects/{projectRef}/prds", "nonexistent")
                .with(user("user"))
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
        .andExpect(status().isNotFound())
        .andExpect(jsonPath("$.code").value("NOT_FOUND"));
  }

  @Test
  void createPrd_withoutAuth_shouldReturn403() throws Exception {
    CreatePrdRequestDto request = new CreatePrdRequestDto();
    request.setTitle("Test PRD");

    mockMvc
        .perform(
            post("/api/projects/{projectRef}/prds", testProject.getPublicId())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
        .andExpect(status().isForbidden());
  }

  // ==================== GET PRD TESTS ====================

  @Test
  void getPrd_byPublicId_shouldReturnPrd() throws Exception {
    Prd prd =
        prdRepository.save(
            new Prd(
                "prd_test123",
                testProject,
                1,
                "PRDT-P1",
                "Test PRD",
                ".specflux/prds/test-prd",
                testUser));

    mockMvc
        .perform(
            get(
                    "/api/projects/{projectRef}/prds/{prdRef}",
                    testProject.getPublicId(),
                    "prd_test123")
                .with(user("user")))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.id").value("prd_test123"))
        .andExpect(jsonPath("$.displayKey").value("PRDT-P1"))
        .andExpect(jsonPath("$.title").value("Test PRD"))
        .andExpect(jsonPath("$.status").value("DRAFT"));
  }

  @Test
  void getPrd_byDisplayKey_shouldReturnPrd() throws Exception {
    Prd prd =
        prdRepository.save(
            new Prd(
                "prd_bykey",
                testProject,
                2,
                "PRDT-P2",
                "PRD By Key",
                ".specflux/prds/prd-by-key",
                testUser));

    mockMvc
        .perform(
            get("/api/projects/{projectRef}/prds/{prdRef}", testProject.getProjectKey(), "PRDT-P2")
                .with(user("user")))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.id").value("prd_bykey"))
        .andExpect(jsonPath("$.displayKey").value("PRDT-P2"));
  }

  @Test
  void getPrd_notFound_shouldReturn404() throws Exception {
    mockMvc
        .perform(
            get(
                    "/api/projects/{projectRef}/prds/{prdRef}",
                    testProject.getPublicId(),
                    "nonexistent")
                .with(user("user")))
        .andExpect(status().isNotFound())
        .andExpect(jsonPath("$.code").value("NOT_FOUND"));
  }

  // ==================== UPDATE PRD TESTS ====================

  @Test
  void updatePrd_shouldReturnUpdatedPrd() throws Exception {
    Prd prd =
        prdRepository.save(
            new Prd(
                "prd_update",
                testProject,
                1,
                "PRDT-P1",
                "Original Title",
                ".specflux/prds/original",
                testUser));

    UpdatePrdRequestDto request = new UpdatePrdRequestDto();
    request.setTitle("Updated Title");
    request.setDescription("New description");
    request.setStatus(PrdStatusDto.IN_REVIEW);

    mockMvc
        .perform(
            put("/api/projects/{projectRef}/prds/{prdRef}", testProject.getPublicId(), "prd_update")
                .with(user("user"))
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.title").value("Updated Title"))
        .andExpect(jsonPath("$.description").value("New description"))
        .andExpect(jsonPath("$.status").value("IN_REVIEW"));
  }

  @Test
  void updatePrd_partialUpdate_shouldOnlyUpdateProvidedFields() throws Exception {
    Prd prd =
        new Prd(
            "prd_partial",
            testProject,
            1,
            "PRDT-P1",
            "Original Title",
            ".specflux/prds/partial",
            testUser);
    prd.setDescription("Original description");
    prdRepository.save(prd);

    UpdatePrdRequestDto request = new UpdatePrdRequestDto();
    request.setTitle("New Title");
    // other fields not set

    mockMvc
        .perform(
            put(
                    "/api/projects/{projectRef}/prds/{prdRef}",
                    testProject.getPublicId(),
                    "prd_partial")
                .with(user("user"))
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.title").value("New Title"))
        .andExpect(jsonPath("$.description").value("Original description"))
        .andExpect(jsonPath("$.status").value("DRAFT"));
  }

  @Test
  void updatePrd_statusTransitions_shouldWork() throws Exception {
    Prd prd =
        prdRepository.save(
            new Prd(
                "prd_status",
                testProject,
                1,
                "PRDT-P1",
                "Status Test",
                ".specflux/prds/status",
                testUser));

    // DRAFT -> IN_REVIEW
    UpdatePrdRequestDto request1 = new UpdatePrdRequestDto();
    request1.setStatus(PrdStatusDto.IN_REVIEW);
    mockMvc
        .perform(
            put("/api/projects/{projectRef}/prds/{prdRef}", testProject.getPublicId(), "prd_status")
                .with(user("user"))
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request1)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.status").value("IN_REVIEW"));

    // IN_REVIEW -> APPROVED
    UpdatePrdRequestDto request2 = new UpdatePrdRequestDto();
    request2.setStatus(PrdStatusDto.APPROVED);
    mockMvc
        .perform(
            put("/api/projects/{projectRef}/prds/{prdRef}", testProject.getPublicId(), "prd_status")
                .with(user("user"))
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request2)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.status").value("APPROVED"));

    // APPROVED -> ARCHIVED
    UpdatePrdRequestDto request3 = new UpdatePrdRequestDto();
    request3.setStatus(PrdStatusDto.ARCHIVED);
    mockMvc
        .perform(
            put("/api/projects/{projectRef}/prds/{prdRef}", testProject.getPublicId(), "prd_status")
                .with(user("user"))
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request3)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.status").value("ARCHIVED"));
  }

  // ==================== DELETE PRD TESTS ====================

  @Test
  void deletePrd_shouldReturn204() throws Exception {
    Prd prd =
        prdRepository.save(
            new Prd(
                "prd_delete",
                testProject,
                1,
                "PRDT-P1",
                "To Delete",
                ".specflux/prds/to-delete",
                testUser));

    mockMvc
        .perform(
            delete(
                    "/api/projects/{projectRef}/prds/{prdRef}",
                    testProject.getPublicId(),
                    "prd_delete")
                .with(user("user")))
        .andExpect(status().isNoContent());

    // Verify deletion
    mockMvc
        .perform(
            get("/api/projects/{projectRef}/prds/{prdRef}", testProject.getPublicId(), "prd_delete")
                .with(user("user")))
        .andExpect(status().isNotFound());
  }

  // ==================== LIST PRDS TESTS ====================

  @Test
  void listPrds_shouldReturnPaginatedList() throws Exception {
    prdRepository.save(
        new Prd("prd_list1", testProject, 1, "PRDT-P1", "PRD 1", ".specflux/prds/prd-1", testUser));
    prdRepository.save(
        new Prd("prd_list2", testProject, 2, "PRDT-P2", "PRD 2", ".specflux/prds/prd-2", testUser));
    prdRepository.save(
        new Prd("prd_list3", testProject, 3, "PRDT-P3", "PRD 3", ".specflux/prds/prd-3", testUser));

    mockMvc
        .perform(
            get("/api/projects/{projectRef}/prds", testProject.getPublicId())
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
  void listPrds_withStatusFilter_shouldReturnFilteredList() throws Exception {
    Prd prd1 =
        new Prd(
            "prd_draft", testProject, 1, "PRDT-P1", "Draft PRD", ".specflux/prds/draft", testUser);
    prd1.setStatus(PrdStatus.DRAFT);
    prdRepository.save(prd1);

    Prd prd2 =
        new Prd(
            "prd_approved",
            testProject,
            2,
            "PRDT-P2",
            "Approved PRD",
            ".specflux/prds/approved",
            testUser);
    prd2.setStatus(PrdStatus.APPROVED);
    prdRepository.save(prd2);

    mockMvc
        .perform(
            get("/api/projects/{projectRef}/prds", testProject.getPublicId())
                .with(user("user"))
                .param("status", "APPROVED"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data.length()").value(1))
        .andExpect(jsonPath("$.data[0].status").value("APPROVED"));
  }

  @Test
  void listPrds_withSort_shouldReturnSortedList() throws Exception {
    prdRepository.save(
        new Prd("prd_z", testProject, 1, "PRDT-P1", "Zeta PRD", ".specflux/prds/zeta", testUser));
    prdRepository.save(
        new Prd("prd_a", testProject, 2, "PRDT-P2", "Alpha PRD", ".specflux/prds/alpha", testUser));

    mockMvc
        .perform(
            get("/api/projects/{projectRef}/prds", testProject.getPublicId())
                .with(user("user"))
                .param("sort", "title")
                .param("order", "asc"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data[0].title").value("Alpha PRD"))
        .andExpect(jsonPath("$.data[1].title").value("Zeta PRD"));
  }

  @Test
  void listPrds_emptyProject_shouldReturnEmptyList() throws Exception {
    mockMvc
        .perform(
            get("/api/projects/{projectRef}/prds", testProject.getPublicId()).with(user("user")))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data").isArray())
        .andExpect(jsonPath("$.data.length()").value(0))
        .andExpect(jsonPath("$.pagination.total").value(0))
        .andExpect(jsonPath("$.pagination.hasMore").value(false));
  }

  @Test
  void listPrds_withoutAuth_shouldReturn403() throws Exception {
    mockMvc
        .perform(get("/api/projects/{projectRef}/prds", testProject.getPublicId()))
        .andExpect(status().isForbidden());
  }

  @Test
  void createPrd_sequentialNumbers_shouldIncrementCorrectly() throws Exception {
    // Create first PRD
    CreatePrdRequestDto request1 = new CreatePrdRequestDto();
    request1.setTitle("First PRD");
    mockMvc
        .perform(
            post("/api/projects/{projectRef}/prds", testProject.getPublicId())
                .with(user("user"))
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request1)))
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.displayKey").value("PRDT-P1"));

    // Create second PRD
    CreatePrdRequestDto request2 = new CreatePrdRequestDto();
    request2.setTitle("Second PRD");
    mockMvc
        .perform(
            post("/api/projects/{projectRef}/prds", testProject.getPublicId())
                .with(user("user"))
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request2)))
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.displayKey").value("PRDT-P2"));
  }

  // ==================== DOCUMENT TESTS ====================

  @Test
  void addPrdDocument_shouldReturnPrdWithDocument() throws Exception {
    Prd prd =
        prdRepository.save(
            new Prd(
                "prd_doc1",
                testProject,
                1,
                "PRDT-P1",
                "PRD with Docs",
                ".specflux/prds/with-docs",
                testUser));

    AddPrdDocumentRequestDto request = new AddPrdDocumentRequestDto();
    request.setFileName("prd.md");
    request.setFilePath(".specflux/prds/with-docs/prd.md");
    request.setDocumentType(PrdDocumentTypeDto.PRD);
    request.setIsPrimary(true);

    mockMvc
        .perform(
            post(
                    "/api/projects/{projectRef}/prds/{prdRef}/documents",
                    testProject.getPublicId(),
                    "prd_doc1")
                .with(user("user"))
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.documents").isArray())
        .andExpect(jsonPath("$.documents.length()").value(1))
        .andExpect(jsonPath("$.documents[0].fileName").value("prd.md"))
        .andExpect(jsonPath("$.documents[0].filePath").value(".specflux/prds/with-docs/prd.md"))
        .andExpect(jsonPath("$.documents[0].documentType").value("PRD"))
        .andExpect(jsonPath("$.documents[0].isPrimary").value(true))
        .andExpect(jsonPath("$.documentCount").value(1));
  }

  @Test
  void addPrdDocument_multipleDocuments_shouldReturnAll() throws Exception {
    Prd prd =
        prdRepository.save(
            new Prd(
                "prd_multidoc",
                testProject,
                1,
                "PRDT-P1",
                "Multi Doc PRD",
                ".specflux/prds/multi",
                testUser));

    // Add primary PRD document
    AddPrdDocumentRequestDto request1 = new AddPrdDocumentRequestDto();
    request1.setFileName("prd.md");
    request1.setFilePath(".specflux/prds/multi/prd.md");
    request1.setDocumentType(PrdDocumentTypeDto.PRD);
    request1.setIsPrimary(true);
    mockMvc
        .perform(
            post(
                    "/api/projects/{projectRef}/prds/{prdRef}/documents",
                    testProject.getPublicId(),
                    "prd_multidoc")
                .with(user("user"))
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request1)))
        .andExpect(status().isCreated());

    // Add wireframe
    AddPrdDocumentRequestDto request2 = new AddPrdDocumentRequestDto();
    request2.setFileName("wireframe.png");
    request2.setFilePath(".specflux/prds/multi/wireframe.png");
    request2.setDocumentType(PrdDocumentTypeDto.WIREFRAME);
    mockMvc
        .perform(
            post(
                    "/api/projects/{projectRef}/prds/{prdRef}/documents",
                    testProject.getPublicId(),
                    "prd_multidoc")
                .with(user("user"))
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request2)))
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.documents.length()").value(2))
        .andExpect(jsonPath("$.documentCount").value(2));
  }

  @Test
  void updatePrdDocument_shouldUpdateDocumentMetadata() throws Exception {
    Prd prd =
        new Prd(
            "prd_updatedoc",
            testProject,
            1,
            "PRDT-P1",
            "Update Doc PRD",
            ".specflux/prds/update-doc",
            testUser);
    PrdDocument doc = new PrdDocument(prd, "doc.md", ".specflux/prds/update-doc/doc.md");
    prd.addDocument(doc);
    prd = prdRepository.save(prd);

    Long docId = prd.getDocuments().get(0).getId();

    UpdatePrdDocumentRequestDto request = new UpdatePrdDocumentRequestDto();
    request.setDocumentType(PrdDocumentTypeDto.DESIGN);
    request.setIsPrimary(true);
    request.setOrderIndex(5);

    mockMvc
        .perform(
            put(
                    "/api/projects/{projectRef}/prds/{prdRef}/documents/{docId}",
                    testProject.getPublicId(),
                    "prd_updatedoc",
                    docId)
                .with(user("user"))
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.documents[0].documentType").value("DESIGN"))
        .andExpect(jsonPath("$.documents[0].isPrimary").value(true))
        .andExpect(jsonPath("$.documents[0].orderIndex").value(5));
  }

  @Test
  void deletePrdDocument_shouldRemoveDocument() throws Exception {
    Prd prd =
        new Prd(
            "prd_deldoc",
            testProject,
            1,
            "PRDT-P1",
            "Delete Doc PRD",
            ".specflux/prds/del-doc",
            testUser);
    PrdDocument doc1 =
        new PrdDocument(
            prd, "prd.md", ".specflux/prds/del-doc/prd.md", PrdDocumentType.PRD, true, 0);
    PrdDocument doc2 =
        new PrdDocument(
            prd,
            "wireframe.png",
            ".specflux/prds/del-doc/wireframe.png",
            PrdDocumentType.WIREFRAME,
            false,
            1);
    prd.addDocument(doc1);
    prd.addDocument(doc2);
    prd = prdRepository.save(prd);

    Long docIdToDelete = prd.getDocuments().get(1).getId();

    mockMvc
        .perform(
            delete(
                    "/api/projects/{projectRef}/prds/{prdRef}/documents/{docId}",
                    testProject.getPublicId(),
                    "prd_deldoc",
                    docIdToDelete)
                .with(user("user")))
        .andExpect(status().isNoContent());

    // Verify deletion - should only have 1 document now
    mockMvc
        .perform(
            get("/api/projects/{projectRef}/prds/{prdRef}", testProject.getPublicId(), "prd_deldoc")
                .with(user("user")))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.documents.length()").value(1))
        .andExpect(jsonPath("$.documents[0].fileName").value("prd.md"));
  }

  @Test
  void deletePrdDocument_notFound_shouldReturn404() throws Exception {
    Prd prd =
        prdRepository.save(
            new Prd(
                "prd_nodoc",
                testProject,
                1,
                "PRDT-P1",
                "No Doc PRD",
                ".specflux/prds/no-doc",
                testUser));

    mockMvc
        .perform(
            delete(
                    "/api/projects/{projectRef}/prds/{prdRef}/documents/{docId}",
                    testProject.getPublicId(),
                    "prd_nodoc",
                    999999L)
                .with(user("user")))
        .andExpect(status().isNotFound());
  }

  @Test
  void getPrd_withDocuments_shouldReturnEmbeddedDocuments() throws Exception {
    Prd prd =
        new Prd(
            "prd_embedded",
            testProject,
            1,
            "PRDT-P1",
            "Embedded Docs",
            ".specflux/prds/embedded",
            testUser);
    PrdDocument doc1 =
        new PrdDocument(
            prd, "prd.md", ".specflux/prds/embedded/prd.md", PrdDocumentType.PRD, true, 0);
    PrdDocument doc2 =
        new PrdDocument(
            prd,
            "mockup.html",
            ".specflux/prds/embedded/mockup.html",
            PrdDocumentType.MOCKUP,
            false,
            1);
    prd.addDocument(doc1);
    prd.addDocument(doc2);
    prdRepository.save(prd);

    mockMvc
        .perform(
            get(
                    "/api/projects/{projectRef}/prds/{prdRef}",
                    testProject.getPublicId(),
                    "prd_embedded")
                .with(user("user")))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.documents").isArray())
        .andExpect(jsonPath("$.documents.length()").value(2))
        .andExpect(jsonPath("$.documents[0].fileName").value("prd.md"))
        .andExpect(jsonPath("$.documents[0].documentType").value("PRD"))
        .andExpect(jsonPath("$.documents[0].isPrimary").value(true))
        .andExpect(jsonPath("$.documents[1].fileName").value("mockup.html"))
        .andExpect(jsonPath("$.documents[1].documentType").value("MOCKUP"))
        .andExpect(jsonPath("$.documentCount").value(2));
  }
}
