package com.specflux.epic.application;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.specflux.api.generated.model.CreateEpicRequest;
import com.specflux.api.generated.model.CursorPagination;
import com.specflux.api.generated.model.EpicListResponse;
import com.specflux.api.generated.model.UpdateEpicRequest;
import com.specflux.epic.domain.Epic;
import com.specflux.epic.domain.EpicRepository;
import com.specflux.epic.interfaces.rest.EpicMapper;
import com.specflux.project.domain.Project;
import com.specflux.shared.application.CurrentUserService;
import com.specflux.shared.interfaces.rest.RefResolver;
import com.specflux.user.domain.User;

/** Application service for Epic operations. */
@Service
@Transactional
public class EpicApplicationService {

  private final EpicRepository epicRepository;
  private final RefResolver refResolver;
  private final CurrentUserService currentUserService;
  private final EpicMapper epicMapper;
  private final ObjectMapper objectMapper = new ObjectMapper();

  public EpicApplicationService(
      EpicRepository epicRepository,
      RefResolver refResolver,
      CurrentUserService currentUserService,
      EpicMapper epicMapper) {
    this.epicRepository = epicRepository;
    this.refResolver = refResolver;
    this.currentUserService = currentUserService;
    this.epicMapper = epicMapper;
  }

  /**
   * Creates a new epic in a project.
   *
   * @param projectRef the project reference
   * @param request the create request
   * @return the created epic DTO
   */
  public com.specflux.api.generated.model.Epic createEpic(
      String projectRef, CreateEpicRequest request) {
    Project project = refResolver.resolveProject(projectRef);
    User currentUser = currentUserService.getCurrentUser();

    String publicId = generatePublicId("epic");
    int sequenceNumber = getNextSequenceNumber(project);
    String displayKey = project.getProjectKey() + "-E" + sequenceNumber;

    Epic epic =
        new Epic(publicId, project, sequenceNumber, displayKey, request.getTitle(), currentUser);
    epic.setDescription(request.getDescription());
    epic.setTargetDate(request.getTargetDate());

    Epic saved = epicRepository.save(epic);
    return epicMapper.toDto(saved);
  }

  /**
   * Gets an epic by reference within a project.
   *
   * @param projectRef the project reference
   * @param epicRef the epic reference (publicId or displayKey)
   * @return the epic DTO
   */
  @Transactional(readOnly = true)
  public com.specflux.api.generated.model.Epic getEpic(String projectRef, String epicRef) {
    Project project = refResolver.resolveProject(projectRef);
    Epic epic = refResolver.resolveEpic(project, epicRef);
    return epicMapper.toDto(epic);
  }

  /**
   * Updates an epic.
   *
   * @param projectRef the project reference
   * @param epicRef the epic reference
   * @param request the update request
   * @return the updated epic DTO
   */
  public com.specflux.api.generated.model.Epic updateEpic(
      String projectRef, String epicRef, UpdateEpicRequest request) {
    Project project = refResolver.resolveProject(projectRef);
    Epic epic = refResolver.resolveEpic(project, epicRef);

    if (request.getTitle() != null) {
      epic.setTitle(request.getTitle());
    }
    if (request.getDescription() != null) {
      epic.setDescription(request.getDescription());
    }
    if (request.getStatus() != null) {
      epic.setStatus(epicMapper.toDomainStatus(request.getStatus()));
    }
    if (request.getTargetDate() != null) {
      epic.setTargetDate(request.getTargetDate());
    }

    Epic saved = epicRepository.save(epic);
    return epicMapper.toDto(saved);
  }

  /**
   * Deletes an epic.
   *
   * @param projectRef the project reference
   * @param epicRef the epic reference
   */
  public void deleteEpic(String projectRef, String epicRef) {
    Project project = refResolver.resolveProject(projectRef);
    Epic epic = refResolver.resolveEpic(project, epicRef);
    epicRepository.delete(epic);
  }

  /**
   * Lists epics in a project with cursor-based pagination.
   *
   * @param projectRef the project reference
   * @param cursor the pagination cursor (optional)
   * @param limit the page size
   * @param sort the sort field
   * @param order the sort order (asc/desc)
   * @param status optional status filter
   * @return the paginated epic list
   */
  @Transactional(readOnly = true)
  public EpicListResponse listEpics(
      String projectRef,
      String cursor,
      int limit,
      String sort,
      String order,
      com.specflux.api.generated.model.EpicStatus status) {

    Project project = refResolver.resolveProject(projectRef);

    // Parse cursor if present
    CursorData cursorData = decodeCursor(cursor);
    int offset = cursorData != null ? cursorData.offset() : 0;

    // Get epics for project with optional status filter
    List<Epic> allEpics;
    if (status != null) {
      allEpics =
          epicRepository.findByProjectIdAndStatus(
              project.getId(), epicMapper.toDomainStatus(status));
    } else {
      allEpics = epicRepository.findByProjectId(project.getId());
    }

    long total = allEpics.size();

    // Sort field mapping
    String sortField = mapSortField(sort);
    boolean ascending = "asc".equalsIgnoreCase(order);

    // Sort and paginate
    Comparator<Epic> comparator = getComparator(sortField);
    if (!ascending) {
      comparator = comparator.reversed();
    }

    List<Epic> sortedEpics =
        allEpics.stream().sorted(comparator).skip(offset).limit(limit + 1).toList();

    boolean hasMore = sortedEpics.size() > limit;
    List<Epic> resultEpics = hasMore ? sortedEpics.subList(0, limit) : sortedEpics;

    // Build response
    EpicListResponse response = new EpicListResponse();
    response.setData(resultEpics.stream().map(epicMapper::toDto).toList());

    CursorPagination pagination = new CursorPagination();
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

  private Comparator<Epic> getComparator(String field) {
    return switch (field) {
      case "title" -> Comparator.comparing(Epic::getTitle, String.CASE_INSENSITIVE_ORDER);
      case "status" -> Comparator.comparing(e -> e.getStatus().name());
      case "targetDate" ->
          Comparator.comparing(
              Epic::getTargetDate, Comparator.nullsLast(Comparator.naturalOrder()));
      case "updatedAt" -> Comparator.comparing(Epic::getUpdatedAt);
      default -> Comparator.comparing(Epic::getCreatedAt);
    };
  }

  private String mapSortField(String sort) {
    return switch (sort) {
      case "title" -> "title";
      case "status" -> "status";
      case "target_date" -> "targetDate";
      case "updated_at" -> "updatedAt";
      default -> "createdAt";
    };
  }

  private int getNextSequenceNumber(Project project) {
    List<Epic> epics = epicRepository.findByProjectId(project.getId());
    return epics.stream().mapToInt(Epic::getSequenceNumber).max().orElse(0) + 1;
  }

  private String generatePublicId(String prefix) {
    return prefix + "_" + UUID.randomUUID().toString().replace("-", "").substring(0, 16);
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
