package com.specflux.mcpserver.interfaces.rest;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

import com.specflux.api.generated.model.CreateMcpServerRequestDto;
import com.specflux.api.generated.model.UpdateMcpServerRequestDto;
import com.specflux.common.AbstractControllerIntegrationTest;
import com.specflux.mcpserver.domain.McpServer;
import com.specflux.mcpserver.domain.McpServerRepository;
import com.specflux.project.domain.Project;
import com.specflux.project.domain.ProjectRepository;

/**
 * Integration tests for McpServerController.
 *
 * <p>Uses schema isolation for parallel test execution.
 */
class McpServerControllerTest extends AbstractControllerIntegrationTest {

  private static final String SCHEMA_NAME = "mcp_server_controller_test";

  @DynamicPropertySource
  static void configureSchema(DynamicPropertyRegistry registry) {
    AbstractControllerIntegrationTest.configureSchema(registry, SCHEMA_NAME);
  }

  @Autowired private ProjectRepository projectRepository;
  @Autowired private McpServerRepository mcpServerRepository;

  private Project testProject;

  @BeforeEach
  void setUpProject() {
    testProject = projectRepository.save(new Project("proj_mcp_test", "MCP", "MCP Test", testUser));
  }

  @Test
  void createMcpServer_shouldReturnCreatedMcpServer() throws Exception {
    CreateMcpServerRequestDto request = new CreateMcpServerRequestDto();
    request.setName("playwright");
    request.setCommand("npx");
    request.setArgs(java.util.List.of("@anthropic/mcp-server-playwright"));

    mockMvc
        .perform(
            post("/api/projects/{projectRef}/mcp-servers", testProject.getPublicId())
                .with(user("user"))
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.id").exists())
        .andExpect(jsonPath("$.name").value("playwright"))
        .andExpect(jsonPath("$.command").value("npx"))
        .andExpect(jsonPath("$.args[0]").value("@anthropic/mcp-server-playwright"))
        .andExpect(jsonPath("$.isActive").value(true))
        .andExpect(jsonPath("$.createdAt").exists())
        .andExpect(jsonPath("$.updatedAt").exists());
  }

  @Test
  void createMcpServer_withDuplicateName_shouldReturn409() throws Exception {
    // Create existing server
    mcpServerRepository.save(
        new McpServer("mcp_existing", testProject, "duplicate-server", "node"));

    CreateMcpServerRequestDto request = new CreateMcpServerRequestDto();
    request.setName("duplicate-server");
    request.setCommand("npx");

    mockMvc
        .perform(
            post("/api/projects/{projectRef}/mcp-servers", testProject.getPublicId())
                .with(user("user"))
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
        .andExpect(status().isConflict())
        .andExpect(jsonPath("$.code").value("CONFLICT"));
  }

  @Test
  void createMcpServer_withMissingName_shouldReturn400() throws Exception {
    CreateMcpServerRequestDto request = new CreateMcpServerRequestDto();
    request.setCommand("npx");
    // name is missing

    mockMvc
        .perform(
            post("/api/projects/{projectRef}/mcp-servers", testProject.getPublicId())
                .with(user("user"))
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));
  }

  @Test
  void createMcpServer_withMissingCommand_shouldReturn400() throws Exception {
    CreateMcpServerRequestDto request = new CreateMcpServerRequestDto();
    request.setName("test-server");
    // command is missing

    mockMvc
        .perform(
            post("/api/projects/{projectRef}/mcp-servers", testProject.getPublicId())
                .with(user("user"))
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));
  }

  @Test
  void getMcpServer_byPublicId_shouldReturnMcpServer() throws Exception {
    McpServer server = new McpServer("mcp_get_test", testProject, "test-server", "npx");
    mcpServerRepository.save(server);

    mockMvc
        .perform(
            get(
                    "/api/projects/{projectRef}/mcp-servers/{serverRef}",
                    testProject.getPublicId(),
                    server.getPublicId())
                .with(user("user")))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.id").value("mcp_get_test"))
        .andExpect(jsonPath("$.name").value("test-server"))
        .andExpect(jsonPath("$.command").value("npx"));
  }

  @Test
  void getMcpServer_notFound_shouldReturn404() throws Exception {
    mockMvc
        .perform(
            get(
                    "/api/projects/{projectRef}/mcp-servers/{serverRef}",
                    testProject.getPublicId(),
                    "nonexistent")
                .with(user("user")))
        .andExpect(status().isNotFound())
        .andExpect(jsonPath("$.code").value("NOT_FOUND"));
  }

  @Test
  void updateMcpServer_shouldReturnUpdatedMcpServer() throws Exception {
    McpServer server =
        mcpServerRepository.save(new McpServer("mcp_update", testProject, "original", "node"));

    UpdateMcpServerRequestDto request = new UpdateMcpServerRequestDto();
    request.setName("updated-server");
    request.setCommand("npx");

    mockMvc
        .perform(
            put(
                    "/api/projects/{projectRef}/mcp-servers/{serverRef}",
                    testProject.getPublicId(),
                    server.getPublicId())
                .with(user("user"))
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.name").value("updated-server"))
        .andExpect(jsonPath("$.command").value("npx"));
  }

  @Test
  void deleteMcpServer_shouldReturn204() throws Exception {
    McpServer server =
        mcpServerRepository.save(new McpServer("mcp_delete", testProject, "to-delete", "node"));

    mockMvc
        .perform(
            delete(
                    "/api/projects/{projectRef}/mcp-servers/{serverRef}",
                    testProject.getPublicId(),
                    server.getPublicId())
                .with(user("user")))
        .andExpect(status().isNoContent());

    // Verify deletion
    mockMvc
        .perform(
            get(
                    "/api/projects/{projectRef}/mcp-servers/{serverRef}",
                    testProject.getPublicId(),
                    server.getPublicId())
                .with(user("user")))
        .andExpect(status().isNotFound());
  }

  @Test
  void toggleMcpServer_shouldToggleIsActive() throws Exception {
    McpServer server =
        mcpServerRepository.save(new McpServer("mcp_toggle", testProject, "toggle-test", "node"));
    // Server starts as active (true)

    // First toggle: active -> inactive
    mockMvc
        .perform(
            post(
                    "/api/projects/{projectRef}/mcp-servers/{serverRef}/toggle",
                    testProject.getPublicId(),
                    server.getPublicId())
                .with(user("user")))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.isActive").value(false));

    // Second toggle: inactive -> active
    mockMvc
        .perform(
            post(
                    "/api/projects/{projectRef}/mcp-servers/{serverRef}/toggle",
                    testProject.getPublicId(),
                    server.getPublicId())
                .with(user("user")))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.isActive").value(true));
  }

  @Test
  void toggleMcpServer_notFound_shouldReturn404() throws Exception {
    mockMvc
        .perform(
            post(
                    "/api/projects/{projectRef}/mcp-servers/{serverRef}/toggle",
                    testProject.getPublicId(),
                    "nonexistent")
                .with(user("user")))
        .andExpect(status().isNotFound())
        .andExpect(jsonPath("$.code").value("NOT_FOUND"));
  }

  @Test
  void listMcpServers_shouldReturnList() throws Exception {
    mcpServerRepository.save(new McpServer("mcp_list1", testProject, "server1", "node"));
    mcpServerRepository.save(new McpServer("mcp_list2", testProject, "server2", "npx"));

    mockMvc
        .perform(
            get("/api/projects/{projectRef}/mcp-servers", testProject.getPublicId())
                .with(user("user")))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data").isArray())
        .andExpect(jsonPath("$.data.length()").value(2));
  }

  @Test
  void listMcpServers_emptyProject_shouldReturnEmptyList() throws Exception {
    mockMvc
        .perform(
            get("/api/projects/{projectRef}/mcp-servers", testProject.getPublicId())
                .with(user("user")))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data").isArray())
        .andExpect(jsonPath("$.data.length()").value(0));
  }

  @Test
  void listMcpServers_withoutAuth_shouldReturn403() throws Exception {
    mockMvc
        .perform(get("/api/projects/{projectRef}/mcp-servers", testProject.getPublicId()))
        .andExpect(status().isForbidden());
  }
}
