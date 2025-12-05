package com.specflux.agent.interfaces.rest;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

import com.specflux.agent.application.AgentApplicationService;
import com.specflux.api.generated.AgentsApi;
import com.specflux.api.generated.model.AgentDto;
import com.specflux.api.generated.model.AgentListResponseDto;
import com.specflux.api.generated.model.CreateAgentRequestDto;
import com.specflux.api.generated.model.UpdateAgentRequestDto;

import lombok.RequiredArgsConstructor;

/** REST controller for Agent endpoints. Implements generated OpenAPI interface. */
@RestController
@RequiredArgsConstructor
public class AgentController implements AgentsApi {

  private final AgentApplicationService agentService;

  @Override
  public ResponseEntity<AgentListResponseDto> listAgents(String projectRef) {
    return ResponseEntity.ok(agentService.listAgents(projectRef));
  }

  @Override
  public ResponseEntity<AgentDto> createAgent(
      String projectRef, CreateAgentRequestDto createAgentRequestDto) {
    return ResponseEntity.status(HttpStatus.CREATED)
        .body(agentService.createAgent(projectRef, createAgentRequestDto));
  }

  @Override
  public ResponseEntity<AgentDto> getAgent(String projectRef, String agentRef) {
    return ResponseEntity.ok(agentService.getAgent(projectRef, agentRef));
  }

  @Override
  public ResponseEntity<AgentDto> updateAgent(
      String projectRef, String agentRef, UpdateAgentRequestDto updateAgentRequestDto) {
    return ResponseEntity.ok(agentService.updateAgent(projectRef, agentRef, updateAgentRequestDto));
  }

  @Override
  public ResponseEntity<Void> deleteAgent(String projectRef, String agentRef) {
    agentService.deleteAgent(projectRef, agentRef);
    return ResponseEntity.noContent().build();
  }
}
