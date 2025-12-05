package com.specflux.project.interfaces.rest;

import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;

import com.specflux.api.generated.model.ProjectDto;
import com.specflux.project.domain.Project;

import lombok.experimental.UtilityClass;

/** Mapper for converting between Project domain entities and API DTOs. */
@UtilityClass
public class ProjectMapper {

  /**
   * Converts a domain Project entity to an API Project DTO.
   *
   * @param domain the domain entity
   * @return the API DTO
   */
  public ProjectDto toDto(Project domain) {
    ProjectDto dto = new ProjectDto();
    dto.setId(domain.getPublicId());
    dto.setProjectKey(domain.getProjectKey());
    dto.setName(domain.getName());
    dto.setDescription(domain.getDescription());
    dto.setLocalPath(domain.getLocalPath());
    dto.setOwnerId(domain.getOwner().getPublicId());
    dto.setCreatedAt(toOffsetDateTime(domain.getCreatedAt()));
    dto.setUpdatedAt(toOffsetDateTime(domain.getUpdatedAt()));
    return dto;
  }

  private OffsetDateTime toOffsetDateTime(Instant instant) {
    return instant != null ? instant.atOffset(ZoneOffset.UTC) : null;
  }
}
