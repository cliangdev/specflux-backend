package com.specflux.task.application;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;
import java.util.stream.Stream;

import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.specflux.api.generated.model.CreateTaskRequestDto;
import com.specflux.api.generated.model.CursorPaginationDto;
import com.specflux.api.generated.model.TaskDto;
import com.specflux.api.generated.model.TaskListResponseDto;
import com.specflux.api.generated.model.TaskPriorityDto;
import com.specflux.api.generated.model.TaskStatusDto;
import com.specflux.api.generated.model.UpdateTaskRequestDto;
import com.specflux.epic.domain.Epic;
import com.specflux.project.domain.Project;
import com.specflux.shared.application.CurrentUserService;
import com.specflux.shared.interfaces.rest.RefResolver;
import com.specflux.task.domain.Task;
import com.specflux.task.domain.TaskPriority;
import com.specflux.task.domain.TaskRepository;
import com.specflux.task.interfaces.rest.TaskMapper;
import com.specflux.user.domain.User;

import lombok.RequiredArgsConstructor;

/** Application service for Task operations. */
@Service
@RequiredArgsConstructor
public class TaskApplicationService {

  private final TaskRepository taskRepository;
  private final RefResolver refResolver;
  private final CurrentUserService currentUserService;
  private final TransactionTemplate transactionTemplate;
  private final ObjectMapper objectMapper = new ObjectMapper();

  /**
   * Creates a new task in a project.
   *
   * @param projectRef the project reference
   * @param request the create request
   * @return the created task DTO
   */
  public TaskDto createTask(String projectRef, CreateTaskRequestDto request) {
    Project project = refResolver.resolveProject(projectRef);
    User currentUser = currentUserService.getCurrentUser();

    String publicId = generatePublicId("task");
    int sequenceNumber = getNextSequenceNumber(project);
    String displayKey = project.getProjectKey() + "-" + sequenceNumber;

    Task task =
        new Task(publicId, project, sequenceNumber, displayKey, request.getTitle(), currentUser);
    task.setDescription(request.getDescription());

    // Set optional fields
    if (request.getEpicRef() != null) {
      Epic epic = refResolver.resolveEpic(project, request.getEpicRef());
      task.setEpic(epic);
    }
    if (request.getPriority() != null) {
      task.setPriority(TaskMapper.toDomainPriority(request.getPriority()));
    }
    if (request.getRequiresApproval() != null) {
      task.setRequiresApproval(request.getRequiresApproval());
    }
    if (request.getEstimatedDuration() != null) {
      task.setEstimatedDuration(request.getEstimatedDuration());
    }
    if (request.getAssignedToRef() != null) {
      User assignee = refResolver.resolveUser(request.getAssignedToRef());
      task.setAssignedTo(assignee);
    }

    Task saved = transactionTemplate.execute(_ -> taskRepository.save(task));
    return TaskMapper.toDto(saved);
  }

  /**
   * Gets a task by reference within a project.
   *
   * @param projectRef the project reference
   * @param taskRef the task reference (publicId or displayKey)
   * @return the task DTO
   */
  public TaskDto getTask(String projectRef, String taskRef) {
    Project project = refResolver.resolveProject(projectRef);
    Task task = refResolver.resolveTask(project, taskRef);
    return TaskMapper.toDto(task);
  }

  /**
   * Updates a task (partial update via PATCH).
   *
   * @param projectRef the project reference
   * @param taskRef the task reference
   * @param request the update request
   * @return the updated task DTO
   */
  public TaskDto updateTask(String projectRef, String taskRef, UpdateTaskRequestDto request) {
    Project project = refResolver.resolveProject(projectRef);
    Task task = refResolver.resolveTask(project, taskRef);

    if (request.getTitle() != null) {
      task.setTitle(request.getTitle());
    }
    if (request.getDescription() != null) {
      task.setDescription(request.getDescription());
    }
    if (request.getStatus() != null) {
      task.setStatus(TaskMapper.toDomainStatus(request.getStatus()));
    }
    if (request.getPriority() != null) {
      task.setPriority(TaskMapper.toDomainPriority(request.getPriority()));
    }
    if (request.getRequiresApproval() != null) {
      task.setRequiresApproval(request.getRequiresApproval());
    }
    if (request.getEstimatedDuration() != null) {
      task.setEstimatedDuration(request.getEstimatedDuration());
    }
    if (request.getActualDuration() != null) {
      task.setActualDuration(request.getActualDuration());
    }
    if (request.getGithubPrUrl() != null) {
      task.setGithubPrUrl(request.getGithubPrUrl());
    }
    if (request.getEpicRef() != null) {
      Epic epic = refResolver.resolveEpicOptional(project, request.getEpicRef());
      task.setEpic(epic);
    }
    if (request.getAssignedToRef() != null) {
      User assignee = refResolver.resolveUserOptional(request.getAssignedToRef());
      task.setAssignedTo(assignee);
    }

    Task saved = transactionTemplate.execute(_ -> taskRepository.save(task));
    return TaskMapper.toDto(saved);
  }

  /**
   * Deletes a task.
   *
   * @param projectRef the project reference
   * @param taskRef the task reference
   */
  public void deleteTask(String projectRef, String taskRef) {
    Project project = refResolver.resolveProject(projectRef);
    Task task = refResolver.resolveTask(project, taskRef);
    transactionTemplate.executeWithoutResult(_ -> taskRepository.delete(task));
  }

  /**
   * Lists tasks in a project with cursor-based pagination and filters.
   *
   * @param projectRef the project reference
   * @param cursor the pagination cursor (optional)
   * @param limit the page size
   * @param sort the sort field
   * @param order the sort order (asc/desc)
   * @param status optional status filter
   * @param priority optional priority filter
   * @param epicRef optional epic filter
   * @param assignedToRef optional assignee filter
   * @param search optional search term
   * @return the paginated task list
   */
  public TaskListResponseDto listTasks(
      String projectRef,
      String cursor,
      int limit,
      String sort,
      String order,
      TaskStatusDto status,
      TaskPriorityDto priority,
      String epicRef,
      String assignedToRef,
      String search) {

    Project project = refResolver.resolveProject(projectRef);

    // Parse cursor if present
    CursorData cursorData = decodeCursor(cursor);
    int offset = cursorData != null ? cursorData.offset() : 0;

    // Get all tasks for project
    List<Task> allTasks = taskRepository.findByProjectId(project.getId());

    // Apply filters
    Stream<Task> taskStream = allTasks.stream();

    if (status != null) {
      var domainStatus = TaskMapper.toDomainStatus(status);
      taskStream = taskStream.filter(t -> t.getStatus() == domainStatus);
    }
    if (priority != null) {
      TaskPriority domainPriority = TaskMapper.toDomainPriority(priority);
      taskStream = taskStream.filter(t -> t.getPriority() == domainPriority);
    }
    if (epicRef != null && !epicRef.isBlank()) {
      Epic epic = refResolver.resolveEpic(project, epicRef);
      taskStream =
          taskStream.filter(t -> t.getEpic() != null && t.getEpic().getId().equals(epic.getId()));
    }
    if (assignedToRef != null && !assignedToRef.isBlank()) {
      User assignee = refResolver.resolveUser(assignedToRef);
      taskStream =
          taskStream.filter(
              t -> t.getAssignedTo() != null && t.getAssignedTo().getId().equals(assignee.getId()));
    }
    if (search != null && !search.isBlank()) {
      String searchLower = search.toLowerCase();
      taskStream =
          taskStream.filter(
              t ->
                  t.getTitle().toLowerCase().contains(searchLower)
                      || (t.getDescription() != null
                          && t.getDescription().toLowerCase().contains(searchLower)));
    }

    List<Task> filteredTasks = taskStream.toList();
    long total = filteredTasks.size();

    // Sort field mapping
    String sortField = mapSortField(sort);
    boolean ascending = "asc".equalsIgnoreCase(order);

    // Sort and paginate
    Comparator<Task> comparator = getComparator(sortField);
    if (!ascending) {
      comparator = comparator.reversed();
    }

    List<Task> sortedTasks =
        filteredTasks.stream().sorted(comparator).skip(offset).limit(limit + 1).toList();

    boolean hasMore = sortedTasks.size() > limit;
    List<Task> resultTasks = hasMore ? sortedTasks.subList(0, limit) : sortedTasks;

    // Build response
    TaskListResponseDto response = new TaskListResponseDto();
    response.setData(resultTasks.stream().map(TaskMapper::toDto).toList());

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

  private Comparator<Task> getComparator(String field) {
    return switch (field) {
      case "title" -> Comparator.comparing(Task::getTitle, String.CASE_INSENSITIVE_ORDER);
      case "status" -> Comparator.comparing(t -> t.getStatus().name());
      case "priority" ->
          Comparator.comparing(
              t -> t.getPriority() != null ? t.getPriority().ordinal() : Integer.MAX_VALUE);
      case "updatedAt" -> Comparator.comparing(Task::getUpdatedAt);
      default -> Comparator.comparing(Task::getCreatedAt);
    };
  }

  private String mapSortField(String sort) {
    return switch (sort) {
      case "title" -> "title";
      case "status" -> "status";
      case "priority" -> "priority";
      case "updated_at" -> "updatedAt";
      default -> "createdAt";
    };
  }

  private int getNextSequenceNumber(Project project) {
    List<Task> tasks = taskRepository.findByProjectId(project.getId());
    return tasks.stream().mapToInt(Task::getSequenceNumber).max().orElse(0) + 1;
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
