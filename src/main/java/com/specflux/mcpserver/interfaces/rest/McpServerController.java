package com.specflux.mcpserver.interfaces.rest;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

import com.specflux.api.generated.McpServersApi;
import com.specflux.api.generated.model.CreateMcpServerRequestDto;
import com.specflux.api.generated.model.McpServerDto;
import com.specflux.api.generated.model.McpServerListResponseDto;
import com.specflux.api.generated.model.UpdateMcpServerRequestDto;
import com.specflux.mcpserver.application.McpServerApplicationService;

import lombok.RequiredArgsConstructor;

/** REST controller for McpServer endpoints. Implements generated OpenAPI interface. */
@RestController
@RequiredArgsConstructor
public class McpServerController implements McpServersApi {

  private final McpServerApplicationService mcpServerService;

  @Override
  public ResponseEntity<McpServerListResponseDto> listMcpServers(String projectRef) {
    return ResponseEntity.ok(mcpServerService.listMcpServers(projectRef));
  }

  @Override
  public ResponseEntity<McpServerDto> createMcpServer(
      String projectRef, CreateMcpServerRequestDto createMcpServerRequestDto) {
    return ResponseEntity.status(HttpStatus.CREATED)
        .body(mcpServerService.createMcpServer(projectRef, createMcpServerRequestDto));
  }

  @Override
  public ResponseEntity<McpServerDto> getMcpServer(String projectRef, String serverRef) {
    return ResponseEntity.ok(mcpServerService.getMcpServer(projectRef, serverRef));
  }

  @Override
  public ResponseEntity<McpServerDto> updateMcpServer(
      String projectRef, String serverRef, UpdateMcpServerRequestDto updateMcpServerRequestDto) {
    return ResponseEntity.ok(
        mcpServerService.updateMcpServer(projectRef, serverRef, updateMcpServerRequestDto));
  }

  @Override
  public ResponseEntity<Void> deleteMcpServer(String projectRef, String serverRef) {
    mcpServerService.deleteMcpServer(projectRef, serverRef);
    return ResponseEntity.noContent().build();
  }

  @Override
  public ResponseEntity<McpServerDto> toggleMcpServer(String projectRef, String serverRef) {
    return ResponseEntity.ok(mcpServerService.toggleMcpServer(projectRef, serverRef));
  }
}
