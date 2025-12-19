package com.specflux.agent.application;

import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;

import com.specflux.agent.domain.Agent;
import com.specflux.agent.domain.AgentRepository;
import com.specflux.agent.interfaces.rest.AgentMapper;
import com.specflux.api.generated.model.AgentDto;
import com.specflux.api.generated.model.AgentListResponseDto;
import com.specflux.api.generated.model.CreateAgentRequestDto;
import com.specflux.api.generated.model.UpdateAgentRequestDto;
import com.specflux.project.domain.Project;
import com.specflux.shared.interfaces.rest.GlobalExceptionHandler.ResourceConflictException;
import com.specflux.shared.interfaces.rest.GlobalExceptionHandler.ResourceNotFoundException;
import com.specflux.shared.interfaces.rest.RefResolver;

import lombok.RequiredArgsConstructor;

/** Application service for Agent operations. */
@Service
@RequiredArgsConstructor
public class AgentApplicationService {

  private final AgentRepository agentRepository;
  private final RefResolver refResolver;
  private final TransactionTemplate transactionTemplate;

  public AgentListResponseDto listAgents(String projectRef) {
    Project project = refResolver.resolveProject(projectRef);
    List<Agent> agents = agentRepository.findByProjectId(project.getId());

    AgentListResponseDto response = new AgentListResponseDto();
    response.setData(agents.stream().map(AgentMapper::toDto).toList());
    return response;
  }

  public AgentDto createAgent(String projectRef, CreateAgentRequestDto request) {
    Project project = refResolver.resolveProject(projectRef);

    if (agentRepository.existsByProjectIdAndName(project.getId(), request.getName())) {
      throw new ResourceConflictException("Agent with this name already exists");
    }

    String publicId = generatePublicId("agent");
    Agent agent = new Agent(publicId, project, request.getName());

    if (request.getDescription() != null && request.getDescription().isPresent()) {
      agent.setDescription(request.getDescription().get());
    }
    if (request.getFilePath() != null && request.getFilePath().isPresent()) {
      agent.setFilePath(request.getFilePath().get());
    }

    Agent saved = transactionTemplate.execute(status -> agentRepository.save(agent));
    return AgentMapper.toDto(saved);
  }

  public AgentDto getAgent(String projectRef, String agentRef) {
    refResolver.resolveProject(projectRef);
    Agent agent = resolveAgent(agentRef);
    return AgentMapper.toDto(agent);
  }

  public AgentDto updateAgent(String projectRef, String agentRef, UpdateAgentRequestDto request) {
    refResolver.resolveProject(projectRef);
    Agent agent = resolveAgent(agentRef);

    if (request.getName() != null) {
      agent.setName(request.getName());
    }
    if (request.getDescription() != null && request.getDescription().isPresent()) {
      agent.setDescription(request.getDescription().get());
    }
    if (request.getFilePath() != null && request.getFilePath().isPresent()) {
      agent.setFilePath(request.getFilePath().get());
    }

    Agent saved = transactionTemplate.execute(status -> agentRepository.save(agent));
    return AgentMapper.toDto(saved);
  }

  public void deleteAgent(String projectRef, String agentRef) {
    refResolver.resolveProject(projectRef);
    Agent agent = resolveAgent(agentRef);
    transactionTemplate.executeWithoutResult(status -> agentRepository.delete(agent));
  }

  private Agent resolveAgent(String ref) {
    return agentRepository
        .findByPublicId(ref)
        .orElseThrow(() -> new ResourceNotFoundException("Agent not found: " + ref));
  }

  private String generatePublicId(String prefix) {
    return prefix + "_" + UUID.randomUUID().toString().replace("-", "").substring(0, 16);
  }
}
