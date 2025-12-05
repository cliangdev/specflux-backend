package com.specflux.agent.interfaces.rest;

import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;

import com.specflux.agent.domain.Agent;
import com.specflux.api.generated.model.AgentDto;

import lombok.experimental.UtilityClass;

/** Mapper for Agent domain and DTOs. */
@UtilityClass
public class AgentMapper {

  public AgentDto toDto(Agent domain) {
    AgentDto dto = new AgentDto();
    dto.setId(domain.getPublicId());
    dto.setProjectId(domain.getProject().getPublicId());
    dto.setName(domain.getName());
    dto.setDescription(domain.getDescription());
    dto.setFilePath(domain.getFilePath());
    dto.setCreatedAt(toOffsetDateTime(domain.getCreatedAt()));
    dto.setUpdatedAt(toOffsetDateTime(domain.getUpdatedAt()));
    return dto;
  }

  private OffsetDateTime toOffsetDateTime(Instant instant) {
    return instant != null ? instant.atOffset(ZoneOffset.UTC) : null;
  }
}
