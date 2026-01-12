package com.specflux.epic.interfaces.rest;

import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;

import org.springframework.stereotype.Service;

import com.specflux.api.generated.model.EpicDto;
import com.specflux.api.generated.model.EpicStatusDto;
import com.specflux.api.generated.model.TaskStatsDto;
import com.specflux.epic.domain.Epic;
import com.specflux.epic.domain.EpicDependency;
import com.specflux.epic.domain.EpicDependencyRepository;
import com.specflux.epic.domain.EpicStatus;
import com.specflux.prd.domain.Prd;
import com.specflux.prd.domain.PrdRepository;
import com.specflux.task.domain.Task;
import com.specflux.task.domain.TaskRepository;
import com.specflux.task.domain.TaskStatus;

import lombok.RequiredArgsConstructor;

/** Mapper for converting between Epic domain entities and API DTOs. */
@Service
@RequiredArgsConstructor
public class EpicMapper {

  private final TaskRepository taskRepository;
  private final EpicDependencyRepository epicDependencyRepository;
  private final PrdRepository prdRepository;

  /**
   * Converts a domain Epic entity to an API Epic DTO with computed fields.
   *
   * @param domain the domain entity
   * @return the API DTO
   */
  public EpicDto toDto(Epic domain) {
    EpicDto dto = new EpicDto();
    dto.setId(domain.getPublicId());
    dto.setDisplayKey(domain.getDisplayKey());
    dto.setProjectId(domain.getProject().getPublicId());
    dto.setTitle(domain.getTitle());
    dto.setDescription(domain.getDescription());
    dto.setStatus(toApiStatus(domain.getStatus()));
    dto.setTargetDate(domain.getTargetDate());
    dto.setCreatedById(domain.getCreatedBy().getPublicId());
    dto.setCreatedAt(toOffsetDateTime(domain.getCreatedAt()));
    dto.setUpdatedAt(toOffsetDateTime(domain.getUpdatedAt()));

    // PRD reference
    if (domain.getPrdId() != null) {
      prdRepository.findById(domain.getPrdId()).map(Prd::getPublicId).ifPresent(dto::setPrdId);
    }
    dto.setPrdFilePath(domain.getPrdFilePath());
    dto.setEpicFilePath(domain.getEpicFilePath());
    dto.setNotes(domain.getNotes());

    // Compute task stats
    List<Task> tasks = taskRepository.findByEpicId(domain.getId());
    TaskStatsDto taskStats = computeTaskStats(tasks);
    dto.setTaskStats(taskStats);

    // Compute progress percentage
    int progressPercentage = computeProgressPercentage(taskStats);
    dto.setProgressPercentage(progressPercentage);

    // Dependencies
    List<EpicDependency> dependencies = epicDependencyRepository.findByEpicId(domain.getId());
    List<String> dependsOn =
        dependencies.stream().map(dep -> dep.getDependsOnEpic().getPublicId()).toList();
    dto.setDependsOn(dependsOn);

    // Compute phase based on dependency depth
    int phase = computePhase(domain.getId());
    dto.setPhase(phase);

    return dto;
  }

  /**
   * Conversion with all fields needed for list views including graph visualization. Includes
   * dependsOn, taskStats, progressPercentage, and phase.
   *
   * @param domain the domain entity
   * @return the API DTO with all fields
   */
  public EpicDto toDtoSimple(Epic domain) {
    EpicDto dto = new EpicDto();
    dto.setId(domain.getPublicId());
    dto.setDisplayKey(domain.getDisplayKey());
    dto.setProjectId(domain.getProject().getPublicId());
    dto.setTitle(domain.getTitle());
    dto.setDescription(domain.getDescription());
    dto.setStatus(toApiStatus(domain.getStatus()));
    dto.setTargetDate(domain.getTargetDate());
    dto.setCreatedById(domain.getCreatedBy().getPublicId());
    dto.setCreatedAt(toOffsetDateTime(domain.getCreatedAt()));
    dto.setUpdatedAt(toOffsetDateTime(domain.getUpdatedAt()));

    // PRD reference
    if (domain.getPrdId() != null) {
      prdRepository.findById(domain.getPrdId()).map(Prd::getPublicId).ifPresent(dto::setPrdId);
    }
    dto.setPrdFilePath(domain.getPrdFilePath());
    dto.setEpicFilePath(domain.getEpicFilePath());
    dto.setNotes(domain.getNotes());

    // Compute task stats
    List<Task> tasks = taskRepository.findByEpicId(domain.getId());
    TaskStatsDto taskStats = computeTaskStats(tasks);
    dto.setTaskStats(taskStats);

    // Compute progress percentage
    int progressPercentage = computeProgressPercentage(taskStats);
    dto.setProgressPercentage(progressPercentage);

    // Include dependsOn for dependency graph support
    List<EpicDependency> dependencies = epicDependencyRepository.findByEpicId(domain.getId());
    List<String> dependsOn =
        dependencies.stream().map(dep -> dep.getDependsOnEpic().getPublicId()).toList();
    dto.setDependsOn(dependsOn);

    // Compute phase based on dependency depth
    int phase = computePhase(domain.getId());
    dto.setPhase(phase);

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
    // Phase = 1 + max depth of dependency chain
    return computePhaseRecursive(epicId, new java.util.HashSet<>());
  }

  private int computePhaseRecursive(Long epicId, java.util.Set<Long> visited) {
    if (visited.contains(epicId)) {
      return 1; // Cycle detected, break recursion
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
   * Converts an API EpicStatusDto to a domain EpicStatus.
   *
   * @param apiStatus the API status
   * @return the domain status
   */
  public EpicStatus toDomainStatus(EpicStatusDto apiStatus) {
    if (apiStatus == null) {
      return null;
    }
    return switch (apiStatus) {
      case PLANNING -> EpicStatus.PLANNING;
      case IN_PROGRESS -> EpicStatus.IN_PROGRESS;
      case BLOCKED -> EpicStatus.BLOCKED;
      case COMPLETED -> EpicStatus.COMPLETED;
      case CANCELLED -> EpicStatus.CANCELLED;
    };
  }

  /** Converts domain status to API status. */
  public EpicStatusDto toApiStatus(EpicStatus domainStatus) {
    return switch (domainStatus) {
      case PLANNING -> EpicStatusDto.PLANNING;
      case IN_PROGRESS -> EpicStatusDto.IN_PROGRESS;
      case BLOCKED -> EpicStatusDto.BLOCKED;
      case COMPLETED -> EpicStatusDto.COMPLETED;
      case CANCELLED -> EpicStatusDto.CANCELLED;
    };
  }

  private OffsetDateTime toOffsetDateTime(Instant instant) {
    return instant != null ? instant.atOffset(ZoneOffset.UTC) : null;
  }
}
