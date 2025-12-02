package com.specflux.project.interfaces.rest;

import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;

import org.springframework.stereotype.Component;

import com.specflux.api.generated.model.ProjectDto;
import com.specflux.project.domain.Project;

/** Mapper for converting between Project domain entities and API DTOs. */
@Component
public class ProjectMapper {

  /**
   * Converts a domain Project entity to an API Project DTO.
   *
   * @param domain the domain entity
   * @return the API DTO
   */
  public ProjectDto toDto(Project domain) {
    ProjectDto dto = new ProjectDto();
    dto.setPublicId(domain.getPublicId());
    dto.setProjectKey(domain.getProjectKey());
    dto.setName(domain.getName());
    dto.setDescription(domain.getDescription());
    dto.setOwnerId(domain.getOwner().getPublicId());
    dto.setCreatedAt(toOffsetDateTime(domain.getCreatedAt()));
    dto.setUpdatedAt(toOffsetDateTime(domain.getUpdatedAt()));
    return dto;
  }

  private OffsetDateTime toOffsetDateTime(Instant instant) {
    return instant != null ? instant.atOffset(ZoneOffset.UTC) : null;
  }
}
