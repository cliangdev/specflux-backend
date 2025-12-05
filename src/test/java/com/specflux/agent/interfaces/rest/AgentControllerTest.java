package com.specflux.agent.interfaces.rest;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

import com.specflux.agent.domain.Agent;
import com.specflux.agent.domain.AgentRepository;
import com.specflux.api.generated.model.CreateAgentRequestDto;
import com.specflux.api.generated.model.UpdateAgentRequestDto;
import com.specflux.common.AbstractControllerIntegrationTest;
import com.specflux.project.domain.Project;
import com.specflux.project.domain.ProjectRepository;

/**
 * Integration tests for AgentController.
 *
 * <p>Uses schema isolation for parallel test execution.
 */
class AgentControllerTest extends AbstractControllerIntegrationTest {

  private static final String SCHEMA_NAME = "agent_controller_test";

  @DynamicPropertySource
  static void configureSchema(DynamicPropertyRegistry registry) {
    AbstractControllerIntegrationTest.configureSchema(registry, SCHEMA_NAME);
  }

  @Autowired private ProjectRepository projectRepository;
  @Autowired private AgentRepository agentRepository;

  private Project testProject;

  @BeforeEach
  void setUpProject() {
    testProject =
        projectRepository.save(new Project("proj_agent_test", "AGNT", "Agent Test", testUser));
  }

  @Test
  void createAgent_shouldReturnCreatedAgent() throws Exception {
    CreateAgentRequestDto request = new CreateAgentRequestDto();
    request.setName("backend-dev");
    request.setDescription("Backend development specialist");
    request.setFilePath(".claude/agents/backend-dev.md");

    mockMvc
        .perform(
            post("/api/projects/{projectRef}/agents", testProject.getPublicId())
                .with(user("user"))
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.id").exists())
        .andExpect(jsonPath("$.name").value("backend-dev"))
        .andExpect(jsonPath("$.description").value("Backend development specialist"))
        .andExpect(jsonPath("$.filePath").value(".claude/agents/backend-dev.md"))
        .andExpect(jsonPath("$.createdAt").exists())
        .andExpect(jsonPath("$.updatedAt").exists());
  }

  @Test
  void createAgent_withDuplicateName_shouldReturn409() throws Exception {
    // Create existing agent
    agentRepository.save(new Agent("agent_existing", testProject, "duplicate-agent"));

    CreateAgentRequestDto request = new CreateAgentRequestDto();
    request.setName("duplicate-agent");

    mockMvc
        .perform(
            post("/api/projects/{projectRef}/agents", testProject.getPublicId())
                .with(user("user"))
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
        .andExpect(status().isConflict())
        .andExpect(jsonPath("$.code").value("CONFLICT"));
  }

  @Test
  void createAgent_withMissingName_shouldReturn400() throws Exception {
    CreateAgentRequestDto request = new CreateAgentRequestDto();
    request.setDescription("An agent without a name");
    // name is missing

    mockMvc
        .perform(
            post("/api/projects/{projectRef}/agents", testProject.getPublicId())
                .with(user("user"))
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));
  }

  @Test
  void getAgent_byPublicId_shouldReturnAgent() throws Exception {
    Agent agent = new Agent("agent_get_test", testProject, "test-agent");
    agent.setDescription("Test description");
    agentRepository.save(agent);

    mockMvc
        .perform(
            get(
                    "/api/projects/{projectRef}/agents/{agentRef}",
                    testProject.getPublicId(),
                    agent.getPublicId())
                .with(user("user")))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.id").value("agent_get_test"))
        .andExpect(jsonPath("$.name").value("test-agent"))
        .andExpect(jsonPath("$.description").value("Test description"));
  }

  @Test
  void getAgent_notFound_shouldReturn404() throws Exception {
    mockMvc
        .perform(
            get(
                    "/api/projects/{projectRef}/agents/{agentRef}",
                    testProject.getPublicId(),
                    "nonexistent")
                .with(user("user")))
        .andExpect(status().isNotFound())
        .andExpect(jsonPath("$.code").value("NOT_FOUND"));
  }

  @Test
  void updateAgent_shouldReturnUpdatedAgent() throws Exception {
    Agent agent = agentRepository.save(new Agent("agent_update", testProject, "original-agent"));

    UpdateAgentRequestDto request = new UpdateAgentRequestDto();
    request.setName("updated-agent");
    request.setDescription("Updated description");

    mockMvc
        .perform(
            put(
                    "/api/projects/{projectRef}/agents/{agentRef}",
                    testProject.getPublicId(),
                    agent.getPublicId())
                .with(user("user"))
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.name").value("updated-agent"))
        .andExpect(jsonPath("$.description").value("Updated description"));
  }

  @Test
  void deleteAgent_shouldReturn204() throws Exception {
    Agent agent = agentRepository.save(new Agent("agent_delete", testProject, "to-delete"));

    mockMvc
        .perform(
            delete(
                    "/api/projects/{projectRef}/agents/{agentRef}",
                    testProject.getPublicId(),
                    agent.getPublicId())
                .with(user("user")))
        .andExpect(status().isNoContent());

    // Verify deletion
    mockMvc
        .perform(
            get(
                    "/api/projects/{projectRef}/agents/{agentRef}",
                    testProject.getPublicId(),
                    agent.getPublicId())
                .with(user("user")))
        .andExpect(status().isNotFound());
  }

  @Test
  void listAgents_shouldReturnList() throws Exception {
    agentRepository.save(new Agent("agent_list1", testProject, "agent1"));
    agentRepository.save(new Agent("agent_list2", testProject, "agent2"));
    agentRepository.save(new Agent("agent_list3", testProject, "agent3"));

    mockMvc
        .perform(
            get("/api/projects/{projectRef}/agents", testProject.getPublicId()).with(user("user")))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data").isArray())
        .andExpect(jsonPath("$.data.length()").value(3));
  }

  @Test
  void listAgents_emptyProject_shouldReturnEmptyList() throws Exception {
    mockMvc
        .perform(
            get("/api/projects/{projectRef}/agents", testProject.getPublicId()).with(user("user")))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data").isArray())
        .andExpect(jsonPath("$.data.length()").value(0));
  }

  @Test
  void listAgents_withoutAuth_shouldReturn403() throws Exception {
    mockMvc
        .perform(get("/api/projects/{projectRef}/agents", testProject.getPublicId()))
        .andExpect(status().isForbidden());
  }
}
