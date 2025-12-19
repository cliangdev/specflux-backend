package com.specflux.user.interfaces.rest;

import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;

import org.openapitools.jackson.nullable.JsonNullable;

import com.specflux.api.generated.model.UserDto;
import com.specflux.user.domain.User;

import lombok.experimental.UtilityClass;

/** Mapper for converting between User domain entities and API DTOs. */
@UtilityClass
public class UserMapper {

  /**
   * Converts a domain User entity to an API User DTO.
   *
   * @param domain the domain entity
   * @return the API DTO
   */
  public UserDto toDto(User domain) {
    UserDto dto = new UserDto();
    dto.setId(domain.getPublicId());
    dto.setEmail(domain.getEmail());
    dto.setDisplayName(domain.getDisplayName());
    dto.setAvatarUrl(JsonNullable.of(domain.getAvatarUrl()));
    dto.setCreatedAt(toOffsetDateTime(domain.getCreatedAt()));
    return dto;
  }

  private OffsetDateTime toOffsetDateTime(Instant instant) {
    return instant != null ? instant.atOffset(ZoneOffset.UTC) : null;
  }
}
