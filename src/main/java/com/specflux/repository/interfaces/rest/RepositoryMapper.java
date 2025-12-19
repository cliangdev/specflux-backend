package com.specflux.repository.interfaces.rest;

import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;

import org.openapitools.jackson.nullable.JsonNullable;

import com.specflux.api.generated.model.RepositoryDto;
import com.specflux.api.generated.model.RepositoryStatusDto;
import com.specflux.repository.domain.Repository;

import lombok.experimental.UtilityClass;

/** Mapper for Repository domain and DTOs. */
@UtilityClass
public class RepositoryMapper {

  public RepositoryDto toDto(Repository domain) {
    RepositoryDto dto = new RepositoryDto();
    dto.setId(domain.getPublicId());
    dto.setProjectId(domain.getProject().getPublicId());
    dto.setName(domain.getName());
    dto.setPath(domain.getPath());
    dto.setGitUrl(JsonNullable.of(domain.getGitUrl()));
    dto.setDefaultBranch(domain.getDefaultBranch());
    dto.setStatus(RepositoryStatusDto.valueOf(domain.getStatus().name()));
    dto.setCreatedAt(toOffsetDateTime(domain.getCreatedAt()));
    dto.setUpdatedAt(toOffsetDateTime(domain.getUpdatedAt()));
    return dto;
  }

  private OffsetDateTime toOffsetDateTime(Instant instant) {
    return instant != null ? instant.atOffset(ZoneOffset.UTC) : null;
  }
}
