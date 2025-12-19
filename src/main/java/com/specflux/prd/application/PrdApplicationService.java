package com.specflux.prd.application;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.specflux.api.generated.model.AddPrdDocumentRequestDto;
import com.specflux.api.generated.model.CreatePrdRequestDto;
import com.specflux.api.generated.model.CursorPaginationDto;
import com.specflux.api.generated.model.PrdDto;
import com.specflux.api.generated.model.PrdListResponseDto;
import com.specflux.api.generated.model.PrdStatusDto;
import com.specflux.api.generated.model.UpdatePrdDocumentRequestDto;
import com.specflux.api.generated.model.UpdatePrdRequestDto;
import com.specflux.prd.domain.Prd;
import com.specflux.prd.domain.PrdDocument;
import com.specflux.prd.domain.PrdDocumentRepository;
import com.specflux.prd.domain.PrdDocumentType;
import com.specflux.prd.domain.PrdRepository;
import com.specflux.prd.interfaces.rest.PrdMapper;
import com.specflux.project.domain.Project;
import com.specflux.shared.application.CurrentUserService;
import com.specflux.shared.application.UpdateHelper;
import com.specflux.shared.interfaces.rest.RefResolver;
import com.specflux.user.domain.User;

import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/** Application service for PRD operations. */
@Slf4j
@Service
@RequiredArgsConstructor
public class PrdApplicationService {

  private final PrdRepository prdRepository;
  private final PrdDocumentRepository prdDocumentRepository;
  private final RefResolver refResolver;
  private final CurrentUserService currentUserService;
  private final TransactionTemplate transactionTemplate;
  private final PrdMapper prdMapper;
  private final ObjectMapper objectMapper = new ObjectMapper();

  /**
   * Creates a new PRD in a project.
   *
   * @param projectRef the project reference
   * @param request the create request
   * @return the created PRD DTO
   */
  public PrdDto createPrd(String projectRef, CreatePrdRequestDto request) {
    return transactionTemplate.execute(
        status -> {
          Project project = refResolver.resolveProject(projectRef);
          User currentUser = currentUserService.getCurrentUser();

          String publicId = generatePublicId("prd");
          int sequenceNumber = getNextSequenceNumber(project);
          String displayKey = project.getProjectKey() + "-P" + sequenceNumber;

          // Generate folder path from title if not provided
          String folderPath = request.getFolderPath();
          if (folderPath == null || folderPath.isBlank()) {
            String slug = slugify(request.getTitle());
            folderPath = ".specflux/prds/" + slug;
          }

          Prd prd =
              new Prd(
                  publicId,
                  project,
                  sequenceNumber,
                  displayKey,
                  request.getTitle(),
                  folderPath,
                  currentUser);
          prd.setDescription(request.getDescription());

          Prd saved = prdRepository.save(prd);
          return prdMapper.toDto(saved);
        });
  }

  /**
   * Gets a PRD by reference within a project.
   *
   * @param projectRef the project reference
   * @param prdRef the PRD reference (publicId or displayKey)
   * @return the PRD DTO
   */
  public PrdDto getPrd(String projectRef, String prdRef) {
    Project project = refResolver.resolveProject(projectRef);
    Prd prd = resolvePrd(project, prdRef);
    return prdMapper.toDto(prd);
  }

  /**
   * Updates a PRD.
   *
   * @param projectRef the project reference
   * @param prdRef the PRD reference
   * @param request the update request
   * @return the updated PRD DTO
   */
  public PrdDto updatePrd(String projectRef, String prdRef, UpdatePrdRequestDto request) {
    Project project = refResolver.resolveProject(projectRef);
    Prd prd = resolvePrd(project, prdRef);

    UpdateHelper.applyValue(request.getTitle(), prd::setTitle);
    UpdateHelper.applyString(request.getDescription(), prd::setDescription);
    UpdateHelper.applyValue(request.getStatus(), s -> prd.setStatus(prdMapper.toDomainStatus(s)));

    Prd saved = transactionTemplate.execute(s -> prdRepository.save(prd));
    return prdMapper.toDto(saved);
  }

  /**
   * Deletes a PRD.
   *
   * @param projectRef the project reference
   * @param prdRef the PRD reference
   */
  public void deletePrd(String projectRef, String prdRef) {
    Project project = refResolver.resolveProject(projectRef);
    Prd prd = resolvePrd(project, prdRef);
    transactionTemplate.executeWithoutResult(status -> prdRepository.delete(prd));
  }

  /**
   * Lists PRDs in a project with cursor-based pagination.
   *
   * @param projectRef the project reference
   * @param cursor the pagination cursor (optional)
   * @param limit the page size
   * @param sort the sort field
   * @param order the sort order (asc/desc)
   * @param status optional status filter
   * @return the paginated PRD list
   */
  public PrdListResponseDto listPrds(
      String projectRef, String cursor, int limit, String sort, String order, PrdStatusDto status) {

    log.debug(
        "[listPrds] Starting - projectRef={}, status={}, limit={}", projectRef, status, limit);

    Project project = refResolver.resolveProject(projectRef);

    // Parse cursor if present
    CursorData cursorData = decodeCursor(cursor);
    int offset = cursorData != null ? cursorData.offset() : 0;

    // Get PRDs for project with optional status filter
    List<Prd> allPrds;
    if (status != null) {
      allPrds =
          prdRepository.findByProjectIdAndStatus(project.getId(), prdMapper.toDomainStatus(status));
      log.debug("[listPrds] Querying with status filter: {}", status);
    } else {
      allPrds = prdRepository.findByProjectId(project.getId());
    }
    log.debug("[listPrds] Found {} PRDs for project {}", allPrds.size(), project.getId());

    long total = allPrds.size();

    // Sort field mapping
    String sortField = mapSortField(sort);
    boolean ascending = "asc".equalsIgnoreCase(order);

    // Sort and paginate
    Comparator<Prd> comparator = getComparator(sortField);
    if (!ascending) {
      comparator = comparator.reversed();
    }

    List<Prd> sortedPrds =
        allPrds.stream().sorted(comparator).skip(offset).limit(limit + 1).toList();

    boolean hasMore = sortedPrds.size() > limit;
    List<Prd> resultPrds = hasMore ? sortedPrds.subList(0, limit) : sortedPrds;

    PrdListResponseDto response = new PrdListResponseDto();
    response.setData(resultPrds.stream().map(prdMapper::toDtoSimple).toList());

    CursorPaginationDto pagination = new CursorPaginationDto();
    pagination.setTotal(total);
    pagination.setHasMore(hasMore);

    if (hasMore) {
      int nextOffset = offset + limit;
      pagination.setNextCursor(encodeCursor(new CursorData(nextOffset)));
    }
    if (offset > 0) {
      int prevOffset = Math.max(0, offset - limit);
      pagination.setPrevCursor(encodeCursor(new CursorData(prevOffset)));
    }

    response.setPagination(pagination);
    return response;
  }

  // ==================== DOCUMENTS ====================

  /**
   * Adds a document to a PRD.
   *
   * @param projectRef the project reference
   * @param prdRef the PRD reference
   * @param request the add document request
   * @return the updated PRD DTO
   */
  public PrdDto addDocument(String projectRef, String prdRef, AddPrdDocumentRequestDto request) {
    Project project = refResolver.resolveProject(projectRef);
    Prd prd = resolvePrd(project, prdRef);

    // Check if document with same path already exists
    if (prdDocumentRepository
        .findByPrdIdAndFilePath(prd.getId(), request.getFilePath())
        .isPresent()) {
      throw new IllegalArgumentException(
          "Document with path already exists: " + request.getFilePath());
    }

    int orderIndex =
        request.getOrderIndex() != null
            ? request.getOrderIndex()
            : prdDocumentRepository.countByPrdId(prd.getId());

    PrdDocumentType docType =
        request.getDocumentType() != null
            ? prdMapper.toDomainDocumentType(request.getDocumentType())
            : PrdDocumentType.OTHER;

    PrdDocument document =
        new PrdDocument(
            prd,
            request.getFileName(),
            request.getFilePath(),
            docType,
            request.getIsPrimary() != null && request.getIsPrimary(),
            orderIndex);

    prd.addDocument(document);
    Prd saved = transactionTemplate.execute(s -> prdRepository.save(prd));
    return prdMapper.toDto(saved);
  }

  /**
   * Updates a document's metadata.
   *
   * @param projectRef the project reference
   * @param prdRef the PRD reference
   * @param docId the document ID
   * @param request the update request
   * @return the updated PRD DTO
   */
  public PrdDto updateDocument(
      String projectRef, String prdRef, Long docId, UpdatePrdDocumentRequestDto request) {
    Project project = refResolver.resolveProject(projectRef);
    Prd prd = resolvePrd(project, prdRef);

    PrdDocument document =
        prdDocumentRepository
            .findByIdAndPrdId(docId, prd.getId())
            .orElseThrow(() -> new EntityNotFoundException("Document not found: " + docId));

    if (request.getDocumentType() != null) {
      document.setDocumentType(prdMapper.toDomainDocumentType(request.getDocumentType()));
    }
    if (request.getIsPrimary() != null) {
      document.setPrimary(request.getIsPrimary());
    }
    if (request.getOrderIndex() != null) {
      document.setOrderIndex(request.getOrderIndex());
    }

    transactionTemplate.executeWithoutResult(s -> prdDocumentRepository.save(document));
    return prdMapper.toDto(prd);
  }

  /**
   * Removes a document from a PRD.
   *
   * @param projectRef the project reference
   * @param prdRef the PRD reference
   * @param docId the document ID
   */
  public void deleteDocument(String projectRef, String prdRef, Long docId) {
    Project project = refResolver.resolveProject(projectRef);
    Prd prd = resolvePrd(project, prdRef);

    PrdDocument document =
        prdDocumentRepository
            .findByIdAndPrdId(docId, prd.getId())
            .orElseThrow(() -> new EntityNotFoundException("Document not found: " + docId));

    prd.removeDocument(document);
    transactionTemplate.executeWithoutResult(s -> prdRepository.save(prd));
  }

  // ==================== HELPERS ====================

  /**
   * Resolves a PRD reference to a Prd entity within a project.
   *
   * @param project The parent project
   * @param ref PRD public ID (prd_xxx) or display key (PROJ-P1)
   * @return The resolved Prd
   * @throws EntityNotFoundException if PRD not found
   */
  public Prd resolvePrd(Project project, String ref) {
    if (ref == null || ref.isBlank()) {
      throw new IllegalArgumentException("PRD reference is required");
    }

    // Check if it's a public ID (starts with "prd_")
    if (ref.startsWith("prd_")) {
      return prdRepository
          .findByPublicIdAndProjectId(ref, project.getId())
          .orElseThrow(() -> new EntityNotFoundException("PRD not found: " + ref));
    }

    // Treat as display key (e.g., PROJ-P1)
    return prdRepository
        .findByProjectIdAndDisplayKey(project.getId(), ref)
        .orElseThrow(() -> new EntityNotFoundException("PRD not found: " + ref));
  }

  private Comparator<Prd> getComparator(String field) {
    return switch (field) {
      case "title" -> Comparator.comparing(Prd::getTitle, String.CASE_INSENSITIVE_ORDER);
      case "status" -> Comparator.comparing(p -> p.getStatus().name());
      case "updatedAt" -> Comparator.comparing(Prd::getUpdatedAt);
      default -> Comparator.comparing(Prd::getCreatedAt);
    };
  }

  private String mapSortField(String sort) {
    return switch (sort) {
      case "title" -> "title";
      case "status" -> "status";
      case "updated_at" -> "updatedAt";
      default -> "createdAt";
    };
  }

  private int getNextSequenceNumber(Project project) {
    return prdRepository.countByProjectId(project.getId()) + 1;
  }

  private String generatePublicId(String prefix) {
    return prefix + "_" + UUID.randomUUID().toString().replace("-", "").substring(0, 16);
  }

  private String slugify(String title) {
    return title
        .toLowerCase()
        .replaceAll("[^a-z0-9\\s-]", "")
        .replaceAll("\\s+", "-")
        .replaceAll("-+", "-")
        .replaceAll("^-|-$", "");
  }

  private CursorData decodeCursor(String cursor) {
    if (cursor == null || cursor.isBlank()) {
      return null;
    }
    try {
      String json = new String(Base64.getDecoder().decode(cursor), StandardCharsets.UTF_8);
      return objectMapper.readValue(json, CursorData.class);
    } catch (Exception e) {
      throw new IllegalArgumentException("Invalid cursor");
    }
  }

  private String encodeCursor(CursorData data) {
    try {
      String json = objectMapper.writeValueAsString(data);
      return Base64.getEncoder().encodeToString(json.getBytes(StandardCharsets.UTF_8));
    } catch (JsonProcessingException e) {
      throw new RuntimeException("Failed to encode cursor", e);
    }
  }

  private record CursorData(int offset) {}
}
