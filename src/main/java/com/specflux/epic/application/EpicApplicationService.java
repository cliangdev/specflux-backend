package com.specflux.epic.application;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.specflux.api.generated.model.CreateEpicRequestDto;
import com.specflux.api.generated.model.CursorPaginationDto;
import com.specflux.api.generated.model.EpicDto;
import com.specflux.api.generated.model.EpicListResponseDto;
import com.specflux.api.generated.model.EpicStatusDto;
import com.specflux.api.generated.model.TaskListResponseDto;
import com.specflux.api.generated.model.UpdateEpicRequestDto;
import com.specflux.epic.domain.Epic;
import com.specflux.epic.domain.EpicDependency;
import com.specflux.epic.domain.EpicDependencyRepository;
import com.specflux.epic.domain.EpicRepository;
import com.specflux.epic.interfaces.rest.EpicMapper;
import com.specflux.project.domain.Project;
import com.specflux.release.domain.Release;
import com.specflux.shared.application.CurrentUserService;
import com.specflux.shared.interfaces.rest.RefResolver;
import com.specflux.task.domain.Task;
import com.specflux.task.domain.TaskRepository;
import com.specflux.task.interfaces.rest.TaskMapper;
import com.specflux.user.domain.User;

import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/** Application service for Epic operations. */
@Slf4j
@Service
@RequiredArgsConstructor
public class EpicApplicationService {

  private final EpicRepository epicRepository;
  private final EpicDependencyRepository epicDependencyRepository;
  private final TaskRepository taskRepository;
  private final RefResolver refResolver;
  private final CurrentUserService currentUserService;
  private final TransactionTemplate transactionTemplate;
  private final EpicMapper epicMapper;
  private final ObjectMapper objectMapper = new ObjectMapper();

  /**
   * Creates a new epic in a project.
   *
   * @param projectRef the project reference
   * @param request the create request
   * @return the created epic DTO
   */
  public EpicDto createEpic(String projectRef, CreateEpicRequestDto request) {
    return transactionTemplate.execute(
        status -> {
          Project project = refResolver.resolveProject(projectRef);
          User currentUser = currentUserService.getCurrentUser();

          String publicId = generatePublicId("epic");
          int sequenceNumber = getNextSequenceNumber(project);
          String displayKey = project.getProjectKey() + "-E" + sequenceNumber;

          Epic epic =
              new Epic(
                  publicId, project, sequenceNumber, displayKey, request.getTitle(), currentUser);
          epic.setDescription(request.getDescription());
          epic.setTargetDate(request.getTargetDate());

          Epic saved = epicRepository.save(epic);
          return epicMapper.toDto(saved);
        });
  }

  /**
   * Gets an epic by reference within a project.
   *
   * @param projectRef the project reference
   * @param epicRef the epic reference (publicId or displayKey)
   * @return the epic DTO
   */
  public EpicDto getEpic(String projectRef, String epicRef) {
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
  public EpicDto updateEpic(String projectRef, String epicRef, UpdateEpicRequestDto request) {
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
    if (request.getPrdFilePath() != null) {
      epic.setPrdFilePath(request.getPrdFilePath());
    }
    if (request.getReleaseRef() != null) {
      if (request.getReleaseRef().isBlank()) {
        epic.setReleaseId(null);
      } else {
        Release release = refResolver.resolveRelease(project, request.getReleaseRef());
        epic.setReleaseId(release.getId());
      }
    }

    Epic saved = transactionTemplate.execute(status -> epicRepository.save(epic));
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
    transactionTemplate.executeWithoutResult(status -> epicRepository.delete(epic));
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
  public EpicListResponseDto listEpics(
      String projectRef,
      String cursor,
      int limit,
      String sort,
      String order,
      EpicStatusDto status) {

    log.info(
        "[listEpics] Starting - projectRef={}, status={}, limit={}", projectRef, status, limit);

    // Get current user for logging
    try {
      User currentUser = currentUserService.getCurrentUser();
      log.info(
          "[listEpics] Current user: id={}, email={}, publicId={}",
          currentUser.getId(),
          currentUser.getEmail(),
          currentUser.getPublicId());
    } catch (Exception e) {
      log.warn("[listEpics] Could not get current user: {}", e.getMessage());
    }

    Project project = refResolver.resolveProject(projectRef);
    log.info(
        "[listEpics] Resolved project: id={}, key={}, publicId={}",
        project.getId(),
        project.getProjectKey(),
        project.getPublicId());

    // Parse cursor if present
    CursorData cursorData = decodeCursor(cursor);
    int offset = cursorData != null ? cursorData.offset() : 0;

    // Get epics for project with optional status filter
    List<Epic> allEpics;
    if (status != null) {
      allEpics =
          epicRepository.findByProjectIdAndStatus(
              project.getId(), epicMapper.toDomainStatus(status));
      log.info("[listEpics] Querying with status filter: {}", status);
    } else {
      allEpics = epicRepository.findByProjectId(project.getId());
    }
    log.info("[listEpics] Found {} epics for project {}", allEpics.size(), project.getId());

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

    // Build response - use simple DTO for list (performance)
    EpicListResponseDto response = new EpicListResponseDto();
    response.setData(resultEpics.stream().map(epicMapper::toDtoSimple).toList());

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

  // ==================== TASKS ====================

  /**
   * Lists tasks belonging to an epic.
   *
   * @param projectRef the project reference
   * @param epicRef the epic reference
   * @param cursor pagination cursor
   * @param limit page size
   * @param status optional status filter
   * @return paginated task list
   */
  public TaskListResponseDto listEpicTasks(
      String projectRef, String epicRef, String cursor, Integer limit, String status) {
    Project project = refResolver.resolveProject(projectRef);
    Epic epic = refResolver.resolveEpic(project, epicRef);

    List<Task> tasks = taskRepository.findByEpicId(epic.getId());

    // Filter by status if provided
    if (status != null && !status.isBlank()) {
      tasks = tasks.stream().filter(t -> t.getStatus().name().equalsIgnoreCase(status)).toList();
    }

    long total = tasks.size();

    // Parse cursor to get offset
    CursorData cursorData = decodeCursor(cursor);
    int offset = cursorData != null ? cursorData.offset() : 0;

    // Apply pagination with cursor offset
    int effectiveLimit = limit != null ? Math.min(limit, 100) : 20;
    List<Task> pagedTasks = tasks.stream().skip(offset).limit(effectiveLimit + 1).toList();

    boolean hasMore = pagedTasks.size() > effectiveLimit;
    List<Task> resultTasks = hasMore ? pagedTasks.subList(0, effectiveLimit) : pagedTasks;

    TaskListResponseDto response = new TaskListResponseDto();
    response.setData(resultTasks.stream().map(TaskMapper::toDto).toList());

    CursorPaginationDto pagination = new CursorPaginationDto();
    pagination.setTotal(total);
    pagination.setHasMore(hasMore);

    if (hasMore) {
      int nextOffset = offset + effectiveLimit;
      pagination.setNextCursor(encodeCursor(new CursorData(nextOffset)));
    }
    if (offset > 0) {
      int prevOffset = Math.max(0, offset - effectiveLimit);
      pagination.setPrevCursor(encodeCursor(new CursorData(prevOffset)));
    }

    response.setPagination(pagination);

    return response;
  }

  // ==================== DEPENDENCIES ====================

  /**
   * Lists epics that this epic depends on.
   *
   * @param projectRef the project reference
   * @param epicRef the epic reference
   * @return list of dependency epics
   */
  public EpicListResponseDto listEpicDependencies(String projectRef, String epicRef) {
    Project project = refResolver.resolveProject(projectRef);
    Epic epic = refResolver.resolveEpic(project, epicRef);

    List<EpicDependency> dependencies = epicDependencyRepository.findByEpicId(epic.getId());
    List<Epic> dependencyEpics =
        dependencies.stream().map(EpicDependency::getDependsOnEpic).toList();

    EpicListResponseDto response = new EpicListResponseDto();
    response.setData(dependencyEpics.stream().map(epicMapper::toDtoSimple).toList());

    CursorPaginationDto pagination = new CursorPaginationDto();
    pagination.setTotal((long) dependencyEpics.size());
    pagination.setHasMore(false);
    response.setPagination(pagination);

    return response;
  }

  /**
   * Adds a dependency to an epic.
   *
   * @param projectRef the project reference
   * @param epicRef the epic reference
   * @param dependsOnEpicRef the epic to depend on
   * @return the updated epic DTO
   */
  public EpicDto addEpicDependency(String projectRef, String epicRef, String dependsOnEpicRef) {
    Project project = refResolver.resolveProject(projectRef);
    Epic epic = refResolver.resolveEpic(project, epicRef);
    Epic dependsOnEpic = refResolver.resolveEpic(project, dependsOnEpicRef);

    // Check for self-dependency
    if (epic.getId().equals(dependsOnEpic.getId())) {
      throw new IllegalArgumentException("Epic cannot depend on itself");
    }

    // Check if dependency already exists
    if (epicDependencyRepository
        .findByEpicIdAndDependsOnEpicId(epic.getId(), dependsOnEpic.getId())
        .isPresent()) {
      throw new IllegalArgumentException("Dependency already exists");
    }

    // TODO: Check for circular dependencies

    EpicDependency dependency = new EpicDependency(epic, dependsOnEpic);
    transactionTemplate.executeWithoutResult(status -> epicDependencyRepository.save(dependency));

    return epicMapper.toDto(epic);
  }

  /**
   * Removes a dependency from an epic.
   *
   * @param projectRef the project reference
   * @param epicRef the epic reference
   * @param depEpicRef the dependency epic to remove
   */
  public void removeEpicDependency(String projectRef, String epicRef, String depEpicRef) {
    Project project = refResolver.resolveProject(projectRef);
    Epic epic = refResolver.resolveEpic(project, epicRef);
    Epic dependsOnEpic = refResolver.resolveEpic(project, depEpicRef);

    EpicDependency dependency =
        epicDependencyRepository
            .findByEpicIdAndDependsOnEpicId(epic.getId(), dependsOnEpic.getId())
            .orElseThrow(() -> new EntityNotFoundException("Dependency not found"));

    transactionTemplate.executeWithoutResult(status -> epicDependencyRepository.delete(dependency));
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
