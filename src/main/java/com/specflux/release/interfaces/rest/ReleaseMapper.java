package com.specflux.release.interfaces.rest;

import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;

import org.openapitools.jackson.nullable.JsonNullable;
import org.springframework.stereotype.Service;

import com.specflux.acceptancecriteria.domain.AcceptanceCriteria;
import com.specflux.acceptancecriteria.domain.AcceptanceCriteriaRepository;
import com.specflux.api.generated.model.AcceptanceCriteriaDto;
import com.specflux.api.generated.model.EpicStatusDto;
import com.specflux.api.generated.model.EpicWithTasksDto;
import com.specflux.api.generated.model.ReleaseDto;
import com.specflux.api.generated.model.ReleaseStatusDto;
import com.specflux.api.generated.model.ReleaseWithEpicsDto;
import com.specflux.api.generated.model.TaskPriorityDto;
import com.specflux.api.generated.model.TaskStatsDto;
import com.specflux.api.generated.model.TaskStatusDto;
import com.specflux.api.generated.model.TaskWithCriteriaDto;
import com.specflux.epic.domain.Epic;
import com.specflux.epic.domain.EpicDependency;
import com.specflux.epic.domain.EpicDependencyRepository;
import com.specflux.epic.domain.EpicStatus;
import com.specflux.prd.domain.Prd;
import com.specflux.prd.domain.PrdRepository;
import com.specflux.release.domain.Release;
import com.specflux.release.domain.ReleaseRepository;
import com.specflux.release.domain.ReleaseStatus;
import com.specflux.task.domain.Task;
import com.specflux.task.domain.TaskPriority;
import com.specflux.task.domain.TaskRepository;
import com.specflux.task.domain.TaskStatus;

import lombok.RequiredArgsConstructor;

/** Mapper for converting between Release domain entities and API DTOs. */
@Service
@RequiredArgsConstructor
public class ReleaseMapper {

  private final EpicDependencyRepository epicDependencyRepository;
  private final TaskRepository taskRepository;
  private final AcceptanceCriteriaRepository acceptanceCriteriaRepository;
  private final PrdRepository prdRepository;
  private final ReleaseRepository releaseRepository;

  /**
   * Converts a domain Release entity to an API Release DTO.
   *
   * @param domain the domain entity
   * @return the API DTO
   */
  public ReleaseDto toDto(Release domain) {
    ReleaseDto dto = new ReleaseDto();
    dto.setId(domain.getPublicId());
    dto.setDisplayKey(domain.getDisplayKey());
    dto.setProjectId(domain.getProject().getPublicId());
    dto.setName(domain.getName());
    dto.setDescription(JsonNullable.of(domain.getDescription()));
    dto.setTargetDate(JsonNullable.of(domain.getTargetDate()));
    dto.setStatus(toApiStatus(domain.getStatus()));
    dto.setCreatedAt(toOffsetDateTime(domain.getCreatedAt()));
    dto.setUpdatedAt(toOffsetDateTime(domain.getUpdatedAt()));
    return dto;
  }

  /**
   * Converts a domain Release entity to an API ReleaseWithEpics DTO with optional nested data.
   *
   * @param domain the domain entity
   * @param epics the epics to include (empty list if not requested)
   * @param includeTasks whether to include tasks within each epic
   * @return the API DTO with optional nested epics/tasks
   */
  public ReleaseWithEpicsDto toDtoWithEpics(
      Release domain, List<Epic> epics, boolean includeTasks) {
    ReleaseWithEpicsDto dto = new ReleaseWithEpicsDto();
    dto.setId(domain.getPublicId());
    dto.setDisplayKey(domain.getDisplayKey());
    dto.setProjectId(domain.getProject().getPublicId());
    dto.setName(domain.getName());
    dto.setDescription(JsonNullable.of(domain.getDescription()));
    dto.setTargetDate(JsonNullable.of(domain.getTargetDate()));
    dto.setStatus(toApiStatus(domain.getStatus()));
    dto.setCreatedAt(toOffsetDateTime(domain.getCreatedAt()));
    dto.setUpdatedAt(toOffsetDateTime(domain.getUpdatedAt()));

    // Include epics if requested
    if (!epics.isEmpty()) {
      List<EpicWithTasksDto> epicDtos =
          epics.stream().map(epic -> toEpicWithTasksDto(epic, includeTasks)).toList();
      dto.setEpics(JsonNullable.of(epicDtos));
    }

    return dto;
  }

  /**
   * Converts an Epic entity to EpicWithTasksDto.
   *
   * @param epic the epic entity
   * @param includeTasks whether to include tasks
   * @return the DTO
   */
  private EpicWithTasksDto toEpicWithTasksDto(Epic epic, boolean includeTasks) {
    EpicWithTasksDto dto = new EpicWithTasksDto();
    dto.setId(epic.getPublicId());
    dto.setDisplayKey(epic.getDisplayKey());
    dto.setProjectId(epic.getProject().getPublicId());
    dto.setTitle(epic.getTitle());
    dto.setDescription(JsonNullable.of(epic.getDescription()));
    dto.setStatus(toEpicApiStatus(epic.getStatus()));
    dto.setTargetDate(JsonNullable.of(epic.getTargetDate()));
    dto.setCreatedById(epic.getCreatedBy().getPublicId());
    dto.setCreatedAt(toOffsetDateTime(epic.getCreatedAt()));
    dto.setUpdatedAt(toOffsetDateTime(epic.getUpdatedAt()));
    dto.setNotes(JsonNullable.of(epic.getNotes()));

    // PRD reference
    if (epic.getPrdId() != null) {
      prdRepository
          .findById(epic.getPrdId())
          .map(Prd::getPublicId)
          .ifPresent(prdId -> dto.setPrdId(JsonNullable.of(prdId)));
    }
    dto.setPrdFilePath(JsonNullable.of(epic.getPrdFilePath()));
    dto.setEpicFilePath(JsonNullable.of(epic.getEpicFilePath()));

    // Release ID
    if (epic.getReleaseId() != null) {
      releaseRepository
          .findById(epic.getReleaseId())
          .map(Release::getPublicId)
          .ifPresent(releaseId -> dto.setReleaseId(JsonNullable.of(releaseId)));
    }

    // Task stats
    List<Task> tasks = taskRepository.findByEpicId(epic.getId());
    TaskStatsDto taskStats = computeTaskStats(tasks);
    dto.setTaskStats(taskStats);
    dto.setProgressPercentage(computeProgressPercentage(taskStats));

    // Dependencies
    List<EpicDependency> dependencies = epicDependencyRepository.findByEpicId(epic.getId());
    List<String> dependsOn =
        dependencies.stream().map(dep -> dep.getDependsOnEpic().getPublicId()).toList();
    dto.setDependsOn(dependsOn);

    // Phase computation
    dto.setPhase(computePhase(epic.getId()));

    // Include tasks if requested
    if (includeTasks) {
      List<TaskWithCriteriaDto> taskDtos = tasks.stream().map(this::toTaskWithCriteriaDto).toList();
      dto.setTasks(JsonNullable.of(taskDtos));
    }

    return dto;
  }

  /**
   * Converts a Task entity to TaskWithCriteriaDto.
   *
   * @param task the task entity
   * @return the DTO
   */
  private TaskWithCriteriaDto toTaskWithCriteriaDto(Task task) {
    TaskWithCriteriaDto dto = new TaskWithCriteriaDto();
    dto.setId(task.getPublicId());
    dto.setDisplayKey(task.getDisplayKey());
    dto.setProjectId(task.getProject().getPublicId());
    dto.setTitle(task.getTitle());
    dto.setDescription(JsonNullable.of(task.getDescription()));
    dto.setStatus(toTaskApiStatus(task.getStatus()));
    dto.setPriority(toTaskApiPriority(task.getPriority()));
    dto.setRequiresApproval(task.getRequiresApproval());
    dto.setEstimatedDuration(
        JsonNullable.of(
            task.getEstimatedDuration() != null ? task.getEstimatedDuration().toString() : null));
    dto.setActualDuration(
        JsonNullable.of(
            task.getActualDuration() != null ? task.getActualDuration().toString() : null));
    dto.setGithubPrUrl(JsonNullable.of(task.getGithubPrUrl()));
    dto.setCreatedById(task.getCreatedBy().getPublicId());
    dto.setCreatedAt(toOffsetDateTime(task.getCreatedAt()));
    dto.setUpdatedAt(toOffsetDateTime(task.getUpdatedAt()));

    if (task.getAssignedTo() != null) {
      dto.setAssignedToId(JsonNullable.of(task.getAssignedTo().getPublicId()));
    }

    if (task.getEpic() != null) {
      dto.setEpicId(JsonNullable.of(task.getEpic().getPublicId()));
      dto.setEpicDisplayKey(JsonNullable.of(task.getEpic().getDisplayKey()));
    }

    // Acceptance criteria
    List<AcceptanceCriteria> criteria =
        acceptanceCriteriaRepository.findByTaskIdOrderByOrderIndexAsc(task.getId());
    List<AcceptanceCriteriaDto> criteriaDtos =
        criteria.stream().map(this::toAcceptanceCriteriaDto).toList();
    dto.setAcceptanceCriteria(criteriaDtos);

    return dto;
  }

  private AcceptanceCriteriaDto toAcceptanceCriteriaDto(AcceptanceCriteria ac) {
    AcceptanceCriteriaDto dto = new AcceptanceCriteriaDto();
    dto.setId(ac.getId());
    dto.setCriteria(ac.getCriteria());
    dto.setIsMet(ac.getIsMet());
    dto.setOrderIndex(ac.getOrderIndex());
    dto.setCreatedAt(toOffsetDateTime(ac.getCreatedAt()));
    return dto;
  }

  private TaskStatsDto computeTaskStats(List<Task> tasks) {
    TaskStatsDto stats = new TaskStatsDto();
    int total = tasks.size();
    int done = (int) tasks.stream().filter(t -> t.getStatus() == TaskStatus.COMPLETED).count();
    int inProgress =
        (int) tasks.stream().filter(t -> t.getStatus() == TaskStatus.IN_PROGRESS).count();
    int backlog = total - done - inProgress;

    stats.setTotal(total);
    stats.setDone(done);
    stats.setInProgress(inProgress);
    stats.setBacklog(backlog);
    return stats;
  }

  private int computeProgressPercentage(TaskStatsDto stats) {
    if (stats.getTotal() == 0) {
      return 0;
    }
    return (stats.getDone() * 100) / stats.getTotal();
  }

  private int computePhase(Long epicId) {
    return computePhaseRecursive(epicId, new java.util.HashSet<>());
  }

  private int computePhaseRecursive(Long epicId, java.util.Set<Long> visited) {
    if (visited.contains(epicId)) {
      return 1;
    }
    visited.add(epicId);

    List<EpicDependency> dependencies = epicDependencyRepository.findByEpicId(epicId);
    if (dependencies.isEmpty()) {
      return 1;
    }

    int maxDepPhase = 0;
    for (EpicDependency dep : dependencies) {
      int depPhase = computePhaseRecursive(dep.getDependsOnEpic().getId(), visited);
      maxDepPhase = Math.max(maxDepPhase, depPhase);
    }

    return maxDepPhase + 1;
  }

  /**
   * Converts an API ReleaseStatusDto to a domain ReleaseStatus.
   *
   * @param apiStatus the API status
   * @return the domain status
   */
  public static ReleaseStatus toDomainStatus(ReleaseStatusDto apiStatus) {
    if (apiStatus == null) {
      return null;
    }
    return switch (apiStatus) {
      case PLANNED -> ReleaseStatus.PLANNED;
      case IN_PROGRESS -> ReleaseStatus.IN_PROGRESS;
      case RELEASED -> ReleaseStatus.RELEASED;
      case CANCELLED -> ReleaseStatus.CANCELLED;
    };
  }

  private ReleaseStatusDto toApiStatus(ReleaseStatus domainStatus) {
    return switch (domainStatus) {
      case PLANNED -> ReleaseStatusDto.PLANNED;
      case IN_PROGRESS -> ReleaseStatusDto.IN_PROGRESS;
      case RELEASED -> ReleaseStatusDto.RELEASED;
      case CANCELLED -> ReleaseStatusDto.CANCELLED;
    };
  }

  private EpicStatusDto toEpicApiStatus(EpicStatus status) {
    return switch (status) {
      case PLANNING -> EpicStatusDto.PLANNING;
      case IN_PROGRESS -> EpicStatusDto.IN_PROGRESS;
      case BLOCKED -> EpicStatusDto.BLOCKED;
      case COMPLETED -> EpicStatusDto.COMPLETED;
      case CANCELLED -> EpicStatusDto.CANCELLED;
    };
  }

  private TaskStatusDto toTaskApiStatus(TaskStatus status) {
    return switch (status) {
      case BACKLOG -> TaskStatusDto.BACKLOG;
      case READY -> TaskStatusDto.READY;
      case IN_PROGRESS -> TaskStatusDto.IN_PROGRESS;
      case IN_REVIEW -> TaskStatusDto.IN_REVIEW;
      case BLOCKED -> TaskStatusDto.BLOCKED;
      case COMPLETED -> TaskStatusDto.COMPLETED;
      case CANCELLED -> TaskStatusDto.CANCELLED;
    };
  }

  private TaskPriorityDto toTaskApiPriority(TaskPriority priority) {
    return switch (priority) {
      case LOW -> TaskPriorityDto.LOW;
      case MEDIUM -> TaskPriorityDto.MEDIUM;
      case HIGH -> TaskPriorityDto.HIGH;
      case CRITICAL -> TaskPriorityDto.CRITICAL;
    };
  }

  private OffsetDateTime toOffsetDateTime(Instant instant) {
    return instant != null ? instant.atOffset(ZoneOffset.UTC) : null;
  }
}
