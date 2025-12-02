package com.specflux.task.interfaces.rest;

import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;

import com.specflux.api.generated.model.TaskDto;
import com.specflux.api.generated.model.TaskPriorityDto;
import com.specflux.api.generated.model.TaskStatusDto;
import com.specflux.task.domain.Task;
import com.specflux.task.domain.TaskPriority;
import com.specflux.task.domain.TaskStatus;

import lombok.experimental.UtilityClass;

/** Mapper for converting between Task domain entities and API DTOs. */
@UtilityClass
public class TaskMapper {

  /**
   * Converts a domain Task entity to an API Task DTO.
   *
   * @param domain the domain entity
   * @return the API DTO
   */
  public TaskDto toDto(Task domain) {
    TaskDto dto = new TaskDto();
    dto.setPublicId(domain.getPublicId());
    dto.setDisplayKey(domain.getDisplayKey());
    dto.setProjectId(domain.getProject().getPublicId());
    dto.setTitle(domain.getTitle());
    dto.setDescription(domain.getDescription());
    dto.setStatus(toApiStatus(domain.getStatus()));
    dto.setPriority(toApiPriority(domain.getPriority()));
    dto.setRequiresApproval(domain.getRequiresApproval());
    dto.setEstimatedDuration(domain.getEstimatedDuration());
    dto.setActualDuration(domain.getActualDuration());
    dto.setGithubPrUrl(domain.getGithubPrUrl());
    dto.setCreatedById(domain.getCreatedBy().getPublicId());
    if (domain.getAssignedTo() != null) {
      dto.setAssignedToId(domain.getAssignedTo().getPublicId());
    }
    if (domain.getEpic() != null) {
      dto.setEpicId(domain.getEpic().getPublicId());
      dto.setEpicDisplayKey(domain.getEpic().getDisplayKey());
    }
    dto.setCreatedAt(toOffsetDateTime(domain.getCreatedAt()));
    dto.setUpdatedAt(toOffsetDateTime(domain.getUpdatedAt()));
    return dto;
  }

  /**
   * Converts an API TaskStatusDto to a domain TaskStatus.
   *
   * @param apiStatus the API status
   * @return the domain status
   */
  public TaskStatus toDomainStatus(TaskStatusDto apiStatus) {
    if (apiStatus == null) {
      return null;
    }
    return switch (apiStatus) {
      case BACKLOG -> TaskStatus.BACKLOG;
      case READY -> TaskStatus.READY;
      case IN_PROGRESS -> TaskStatus.IN_PROGRESS;
      case IN_REVIEW -> TaskStatus.IN_REVIEW;
      case BLOCKED -> TaskStatus.BLOCKED;
      case COMPLETED -> TaskStatus.COMPLETED;
      case CANCELLED -> TaskStatus.CANCELLED;
    };
  }

  /**
   * Converts an API TaskPriorityDto to a domain TaskPriority.
   *
   * @param apiPriority the API priority
   * @return the domain priority
   */
  public TaskPriority toDomainPriority(TaskPriorityDto apiPriority) {
    if (apiPriority == null) {
      return null;
    }
    return switch (apiPriority) {
      case LOW -> TaskPriority.LOW;
      case MEDIUM -> TaskPriority.MEDIUM;
      case HIGH -> TaskPriority.HIGH;
      case CRITICAL -> TaskPriority.CRITICAL;
    };
  }

  private TaskStatusDto toApiStatus(TaskStatus domainStatus) {
    return switch (domainStatus) {
      case BACKLOG -> TaskStatusDto.BACKLOG;
      case READY -> TaskStatusDto.READY;
      case IN_PROGRESS -> TaskStatusDto.IN_PROGRESS;
      case IN_REVIEW -> TaskStatusDto.IN_REVIEW;
      case BLOCKED -> TaskStatusDto.BLOCKED;
      case COMPLETED -> TaskStatusDto.COMPLETED;
      case CANCELLED -> TaskStatusDto.CANCELLED;
    };
  }

  private TaskPriorityDto toApiPriority(TaskPriority domainPriority) {
    if (domainPriority == null) {
      return null;
    }
    return switch (domainPriority) {
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
