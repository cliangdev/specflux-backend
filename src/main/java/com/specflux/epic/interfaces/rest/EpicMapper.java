package com.specflux.epic.interfaces.rest;

import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;

import org.springframework.stereotype.Component;

import com.specflux.api.generated.model.EpicDto;
import com.specflux.api.generated.model.EpicStatusDto;
import com.specflux.epic.domain.Epic;
import com.specflux.epic.domain.EpicStatus;

/** Mapper for converting between Epic domain entities and API DTOs. */
@Component
public class EpicMapper {

  /**
   * Converts a domain Epic entity to an API Epic DTO.
   *
   * @param domain the domain entity
   * @return the API DTO
   */
  public EpicDto toDto(Epic domain) {
    EpicDto dto = new EpicDto();
    dto.setPublicId(domain.getPublicId());
    dto.setDisplayKey(domain.getDisplayKey());
    dto.setProjectId(domain.getProject().getPublicId());
    dto.setTitle(domain.getTitle());
    dto.setDescription(domain.getDescription());
    dto.setStatus(toApiStatus(domain.getStatus()));
    dto.setTargetDate(domain.getTargetDate());
    dto.setCreatedById(domain.getCreatedBy().getPublicId());
    dto.setCreatedAt(toOffsetDateTime(domain.getCreatedAt()));
    dto.setUpdatedAt(toOffsetDateTime(domain.getUpdatedAt()));
    return dto;
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

  private EpicStatusDto toApiStatus(EpicStatus domainStatus) {
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
