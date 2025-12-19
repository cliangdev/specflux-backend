package com.specflux.skill.interfaces.rest;

import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;

import org.openapitools.jackson.nullable.JsonNullable;

import com.specflux.api.generated.model.SkillDto;
import com.specflux.skill.domain.Skill;

import lombok.experimental.UtilityClass;

/** Mapper for Skill domain and DTOs. */
@UtilityClass
public class SkillMapper {

  public SkillDto toDto(Skill domain) {
    SkillDto dto = new SkillDto();
    dto.setId(domain.getPublicId());
    dto.setProjectId(domain.getProject().getPublicId());
    dto.setName(domain.getName());
    dto.setDescription(JsonNullable.of(domain.getDescription()));
    dto.setFolderPath(JsonNullable.of(domain.getFolderPath()));
    dto.setCreatedAt(toOffsetDateTime(domain.getCreatedAt()));
    dto.setUpdatedAt(toOffsetDateTime(domain.getUpdatedAt()));
    return dto;
  }

  private OffsetDateTime toOffsetDateTime(Instant instant) {
    return instant != null ? instant.atOffset(ZoneOffset.UTC) : null;
  }
}
