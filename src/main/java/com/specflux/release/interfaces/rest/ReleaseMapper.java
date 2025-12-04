package com.specflux.release.interfaces.rest;

import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;

import com.specflux.api.generated.model.ReleaseDto;
import com.specflux.api.generated.model.ReleaseStatusDto;
import com.specflux.release.domain.Release;
import com.specflux.release.domain.ReleaseStatus;

import lombok.experimental.UtilityClass;

/** Mapper for converting between Release domain entities and API DTOs. */
@UtilityClass
public class ReleaseMapper {

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
    dto.setDescription(domain.getDescription());
    dto.setTargetDate(domain.getTargetDate());
    dto.setStatus(toApiStatus(domain.getStatus()));
    dto.setCreatedAt(toOffsetDateTime(domain.getCreatedAt()));
    dto.setUpdatedAt(toOffsetDateTime(domain.getUpdatedAt()));
    return dto;
  }

  /**
   * Converts an API ReleaseStatusDto to a domain ReleaseStatus.
   *
   * @param apiStatus the API status
   * @return the domain status
   */
  public ReleaseStatus toDomainStatus(ReleaseStatusDto apiStatus) {
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

  private OffsetDateTime toOffsetDateTime(Instant instant) {
    return instant != null ? instant.atOffset(ZoneOffset.UTC) : null;
  }
}
