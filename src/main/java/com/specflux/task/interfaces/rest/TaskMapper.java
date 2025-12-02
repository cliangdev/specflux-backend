package com.specflux.task.interfaces.rest;

import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;

import org.springframework.stereotype.Component;

import com.specflux.task.domain.Task;
import com.specflux.task.domain.TaskPriority;
import com.specflux.task.domain.TaskStatus;

/** Mapper for converting between Task domain entities and API DTOs. */
@Component
public class TaskMapper {

  /**
   * Converts a domain Task entity to an API Task DTO.
   *
   * @param domain the domain entity
   * @return the API DTO
   */
  public com.specflux.api.generated.model.Task toDto(Task domain) {
    com.specflux.api.generated.model.Task dto = new com.specflux.api.generated.model.Task();
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
   * Converts an API TaskStatus to a domain TaskStatus.
   *
   * @param apiStatus the API status
   * @return the domain status
   */
  public TaskStatus toDomainStatus(com.specflux.api.generated.model.TaskStatus apiStatus) {
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
   * Converts an API TaskPriority to a domain TaskPriority.
   *
   * @param apiPriority the API priority
   * @return the domain priority
   */
  public TaskPriority toDomainPriority(com.specflux.api.generated.model.TaskPriority apiPriority) {
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

  private com.specflux.api.generated.model.TaskStatus toApiStatus(TaskStatus domainStatus) {
    return switch (domainStatus) {
      case BACKLOG -> com.specflux.api.generated.model.TaskStatus.BACKLOG;
      case READY -> com.specflux.api.generated.model.TaskStatus.READY;
      case IN_PROGRESS -> com.specflux.api.generated.model.TaskStatus.IN_PROGRESS;
      case IN_REVIEW -> com.specflux.api.generated.model.TaskStatus.IN_REVIEW;
      case BLOCKED -> com.specflux.api.generated.model.TaskStatus.BLOCKED;
      case COMPLETED -> com.specflux.api.generated.model.TaskStatus.COMPLETED;
      case CANCELLED -> com.specflux.api.generated.model.TaskStatus.CANCELLED;
    };
  }

  private com.specflux.api.generated.model.TaskPriority toApiPriority(TaskPriority domainPriority) {
    if (domainPriority == null) {
      return null;
    }
    return switch (domainPriority) {
      case LOW -> com.specflux.api.generated.model.TaskPriority.LOW;
      case MEDIUM -> com.specflux.api.generated.model.TaskPriority.MEDIUM;
      case HIGH -> com.specflux.api.generated.model.TaskPriority.HIGH;
      case CRITICAL -> com.specflux.api.generated.model.TaskPriority.CRITICAL;
    };
  }

  private OffsetDateTime toOffsetDateTime(Instant instant) {
    return instant != null ? instant.atOffset(ZoneOffset.UTC) : null;
  }
}
