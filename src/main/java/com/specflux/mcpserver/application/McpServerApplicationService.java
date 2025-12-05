package com.specflux.mcpserver.application;

import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.specflux.api.generated.model.CreateMcpServerRequestDto;
import com.specflux.api.generated.model.McpServerDto;
import com.specflux.api.generated.model.McpServerListResponseDto;
import com.specflux.api.generated.model.UpdateMcpServerRequestDto;
import com.specflux.mcpserver.domain.McpServer;
import com.specflux.mcpserver.domain.McpServerRepository;
import com.specflux.mcpserver.interfaces.rest.McpServerMapper;
import com.specflux.project.domain.Project;
import com.specflux.shared.interfaces.rest.GlobalExceptionHandler.ResourceConflictException;
import com.specflux.shared.interfaces.rest.GlobalExceptionHandler.ResourceNotFoundException;
import com.specflux.shared.interfaces.rest.RefResolver;

import lombok.RequiredArgsConstructor;

/** Application service for McpServer operations. */
@Service
@RequiredArgsConstructor
public class McpServerApplicationService {

  private final McpServerRepository mcpServerRepository;
  private final RefResolver refResolver;
  private final TransactionTemplate transactionTemplate;
  private final ObjectMapper objectMapper = new ObjectMapper();

  public McpServerListResponseDto listMcpServers(String projectRef) {
    Project project = refResolver.resolveProject(projectRef);
    List<McpServer> servers = mcpServerRepository.findByProjectId(project.getId());

    McpServerListResponseDto response = new McpServerListResponseDto();
    response.setData(servers.stream().map(s -> McpServerMapper.toDto(s, objectMapper)).toList());
    return response;
  }

  public McpServerDto createMcpServer(String projectRef, CreateMcpServerRequestDto request) {
    Project project = refResolver.resolveProject(projectRef);

    if (mcpServerRepository.existsByProjectIdAndName(project.getId(), request.getName())) {
      throw new ResourceConflictException("MCP server with this name already exists");
    }

    String publicId = generatePublicId("mcp");
    McpServer server = new McpServer(publicId, project, request.getName(), request.getCommand());

    if (request.getArgs() != null) {
      try {
        server.setArgs(objectMapper.writeValueAsString(request.getArgs()));
      } catch (JsonProcessingException e) {
        throw new RuntimeException("Failed to serialize args", e);
      }
    }
    if (request.getEnvVars() != null) {
      try {
        server.setEnvVars(objectMapper.writeValueAsString(request.getEnvVars()));
      } catch (JsonProcessingException e) {
        throw new RuntimeException("Failed to serialize envVars", e);
      }
    }
    if (request.getIsActive() != null) {
      server.setIsActive(request.getIsActive());
    }

    McpServer saved = transactionTemplate.execute(status -> mcpServerRepository.save(server));
    return McpServerMapper.toDto(saved, objectMapper);
  }

  public McpServerDto getMcpServer(String projectRef, String serverRef) {
    refResolver.resolveProject(projectRef);
    McpServer server = resolveServer(serverRef);
    return McpServerMapper.toDto(server, objectMapper);
  }

  public McpServerDto updateMcpServer(
      String projectRef, String serverRef, UpdateMcpServerRequestDto request) {
    refResolver.resolveProject(projectRef);
    McpServer server = resolveServer(serverRef);

    if (request.getName() != null) {
      server.setName(request.getName());
    }
    if (request.getCommand() != null) {
      server.setCommand(request.getCommand());
    }
    if (request.getArgs() != null) {
      try {
        server.setArgs(objectMapper.writeValueAsString(request.getArgs()));
      } catch (JsonProcessingException e) {
        throw new RuntimeException("Failed to serialize args", e);
      }
    }
    if (request.getEnvVars() != null) {
      try {
        server.setEnvVars(objectMapper.writeValueAsString(request.getEnvVars()));
      } catch (JsonProcessingException e) {
        throw new RuntimeException("Failed to serialize envVars", e);
      }
    }
    if (request.getIsActive() != null) {
      server.setIsActive(request.getIsActive());
    }

    McpServer saved = transactionTemplate.execute(status -> mcpServerRepository.save(server));
    return McpServerMapper.toDto(saved, objectMapper);
  }

  public void deleteMcpServer(String projectRef, String serverRef) {
    refResolver.resolveProject(projectRef);
    McpServer server = resolveServer(serverRef);
    transactionTemplate.executeWithoutResult(status -> mcpServerRepository.delete(server));
  }

  public McpServerDto toggleMcpServer(String projectRef, String serverRef) {
    refResolver.resolveProject(projectRef);
    McpServer server = resolveServer(serverRef);
    server.toggle();
    McpServer saved = transactionTemplate.execute(status -> mcpServerRepository.save(server));
    return McpServerMapper.toDto(saved, objectMapper);
  }

  private McpServer resolveServer(String ref) {
    return mcpServerRepository
        .findByPublicId(ref)
        .orElseThrow(() -> new ResourceNotFoundException("MCP server not found: " + ref));
  }

  private String generatePublicId(String prefix) {
    return prefix + "_" + UUID.randomUUID().toString().replace("-", "").substring(0, 16);
  }
}
