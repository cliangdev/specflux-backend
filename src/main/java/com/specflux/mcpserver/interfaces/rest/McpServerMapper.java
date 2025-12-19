package com.specflux.mcpserver.interfaces.rest;

import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.openapitools.jackson.nullable.JsonNullable;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.specflux.api.generated.model.McpServerDto;
import com.specflux.mcpserver.domain.McpServer;

import lombok.experimental.UtilityClass;

/** Mapper for McpServer domain and DTOs. */
@UtilityClass
public class McpServerMapper {

  public McpServerDto toDto(McpServer domain, ObjectMapper objectMapper) {
    McpServerDto dto = new McpServerDto();
    dto.setId(domain.getPublicId());
    dto.setProjectId(domain.getProject().getPublicId());
    dto.setName(domain.getName());
    dto.setCommand(domain.getCommand());
    dto.setArgs(parseArgs(domain.getArgs(), objectMapper));
    dto.setEnvVars(JsonNullable.of(parseEnvVars(domain.getEnvVars(), objectMapper)));
    dto.setIsActive(domain.getIsActive());
    dto.setCreatedAt(toOffsetDateTime(domain.getCreatedAt()));
    dto.setUpdatedAt(toOffsetDateTime(domain.getUpdatedAt()));
    return dto;
  }

  private List<String> parseArgs(String argsJson, ObjectMapper objectMapper) {
    if (argsJson == null || argsJson.isBlank()) {
      return Collections.emptyList();
    }
    try {
      return objectMapper.readValue(argsJson, new TypeReference<List<String>>() {});
    } catch (JsonProcessingException e) {
      return Collections.emptyList();
    }
  }

  private Map<String, String> parseEnvVars(String envVarsJson, ObjectMapper objectMapper) {
    if (envVarsJson == null || envVarsJson.isBlank()) {
      return null;
    }
    try {
      return objectMapper.readValue(envVarsJson, new TypeReference<Map<String, String>>() {});
    } catch (JsonProcessingException e) {
      return null;
    }
  }

  private OffsetDateTime toOffsetDateTime(Instant instant) {
    return instant != null ? instant.atOffset(ZoneOffset.UTC) : null;
  }
}
