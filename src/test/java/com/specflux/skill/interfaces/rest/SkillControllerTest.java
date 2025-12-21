package com.specflux.skill.interfaces.rest;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

import com.specflux.api.generated.model.CreateSkillRequestDto;
import com.specflux.api.generated.model.UpdateSkillRequestDto;
import com.specflux.common.AbstractControllerIntegrationTest;
import com.specflux.project.domain.Project;
import com.specflux.project.domain.ProjectRepository;
import com.specflux.skill.domain.Skill;
import com.specflux.skill.domain.SkillRepository;

/**
 * Integration tests for SkillController.
 *
 * <p>Uses schema isolation for parallel test execution.
 */
class SkillControllerTest extends AbstractControllerIntegrationTest {

  @DynamicPropertySource
  static void configureSchema(DynamicPropertyRegistry registry) {
    configureSchemaForClass(registry, SkillControllerTest.class);
  }

  @Autowired private ProjectRepository projectRepository;
  @Autowired private SkillRepository skillRepository;

  private Project testProject;

  @BeforeEach
  void setUpProject() {
    testProject =
        projectRepository.save(new Project("proj_skill_test", "SKIL", "Skill Test", testUser));
  }

  @Test
  void createSkill_shouldReturnCreatedSkill() throws Exception {
    CreateSkillRequestDto request = new CreateSkillRequestDto();
    request.setName("typescript-patterns");
    request.setDescription("TypeScript best practices and patterns");
    request.setFolderPath(".claude/skills/typescript-patterns");

    mockMvc
        .perform(
            post("/api/projects/{projectRef}/skills", testProject.getPublicId())
                .with(user("user"))
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.id").exists())
        .andExpect(jsonPath("$.name").value("typescript-patterns"))
        .andExpect(jsonPath("$.description").value("TypeScript best practices and patterns"))
        .andExpect(jsonPath("$.folderPath").value(".claude/skills/typescript-patterns"))
        .andExpect(jsonPath("$.createdAt").exists())
        .andExpect(jsonPath("$.updatedAt").exists());
  }

  @Test
  void createSkill_withDuplicateName_shouldReturn409() throws Exception {
    // Create existing skill
    skillRepository.save(new Skill("skill_existing", testProject, "duplicate-skill"));

    CreateSkillRequestDto request = new CreateSkillRequestDto();
    request.setName("duplicate-skill");

    mockMvc
        .perform(
            post("/api/projects/{projectRef}/skills", testProject.getPublicId())
                .with(user("user"))
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
        .andExpect(status().isConflict())
        .andExpect(jsonPath("$.code").value("CONFLICT"));
  }

  @Test
  void createSkill_withMissingName_shouldReturn400() throws Exception {
    CreateSkillRequestDto request = new CreateSkillRequestDto();
    request.setDescription("A skill without a name");
    // name is missing

    mockMvc
        .perform(
            post("/api/projects/{projectRef}/skills", testProject.getPublicId())
                .with(user("user"))
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));
  }

  @Test
  void getSkill_byPublicId_shouldReturnSkill() throws Exception {
    Skill skill = new Skill("skill_get_test", testProject, "test-skill");
    skill.setDescription("Test description");
    skillRepository.save(skill);

    mockMvc
        .perform(
            get(
                    "/api/projects/{projectRef}/skills/{skillRef}",
                    testProject.getPublicId(),
                    skill.getPublicId())
                .with(user("user")))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.id").value("skill_get_test"))
        .andExpect(jsonPath("$.name").value("test-skill"))
        .andExpect(jsonPath("$.description").value("Test description"));
  }

  @Test
  void getSkill_notFound_shouldReturn404() throws Exception {
    mockMvc
        .perform(
            get(
                    "/api/projects/{projectRef}/skills/{skillRef}",
                    testProject.getPublicId(),
                    "nonexistent")
                .with(user("user")))
        .andExpect(status().isNotFound())
        .andExpect(jsonPath("$.code").value("NOT_FOUND"));
  }

  @Test
  void updateSkill_shouldReturnUpdatedSkill() throws Exception {
    Skill skill = skillRepository.save(new Skill("skill_update", testProject, "original-skill"));

    UpdateSkillRequestDto request = new UpdateSkillRequestDto();
    request.setName("updated-skill");
    request.setDescription("Updated description");

    mockMvc
        .perform(
            put(
                    "/api/projects/{projectRef}/skills/{skillRef}",
                    testProject.getPublicId(),
                    skill.getPublicId())
                .with(user("user"))
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.name").value("updated-skill"))
        .andExpect(jsonPath("$.description").value("Updated description"));
  }

  @Test
  void deleteSkill_shouldReturn204() throws Exception {
    Skill skill = skillRepository.save(new Skill("skill_delete", testProject, "to-delete"));

    mockMvc
        .perform(
            delete(
                    "/api/projects/{projectRef}/skills/{skillRef}",
                    testProject.getPublicId(),
                    skill.getPublicId())
                .with(user("user")))
        .andExpect(status().isNoContent());

    // Verify deletion
    mockMvc
        .perform(
            get(
                    "/api/projects/{projectRef}/skills/{skillRef}",
                    testProject.getPublicId(),
                    skill.getPublicId())
                .with(user("user")))
        .andExpect(status().isNotFound());
  }

  @Test
  void listSkills_shouldReturnList() throws Exception {
    skillRepository.save(new Skill("skill_list1", testProject, "skill1"));
    skillRepository.save(new Skill("skill_list2", testProject, "skill2"));
    skillRepository.save(new Skill("skill_list3", testProject, "skill3"));

    mockMvc
        .perform(
            get("/api/projects/{projectRef}/skills", testProject.getPublicId()).with(user("user")))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data").isArray())
        .andExpect(jsonPath("$.data.length()").value(3));
  }

  @Test
  void listSkills_emptyProject_shouldReturnEmptyList() throws Exception {
    mockMvc
        .perform(
            get("/api/projects/{projectRef}/skills", testProject.getPublicId()).with(user("user")))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data").isArray())
        .andExpect(jsonPath("$.data.length()").value(0));
  }

  @Test
  void listSkills_withoutAuth_shouldReturn403() throws Exception {
    mockMvc
        .perform(get("/api/projects/{projectRef}/skills", testProject.getPublicId()))
        .andExpect(status().isForbidden());
  }

  // ==================== EMPTY-STRING-CLEARS CONVENTION TESTS ====================

  @Test
  void updateSkill_emptyString_shouldClearDescription() throws Exception {
    Skill skill = new Skill("skill_clearDesc", testProject, "clear-desc-skill");
    skill.setDescription("Original description");
    skillRepository.save(skill);

    // Send empty string to clear description
    UpdateSkillRequestDto request = new UpdateSkillRequestDto();
    request.setDescription("");

    mockMvc
        .perform(
            put(
                    "/api/projects/{projectRef}/skills/{skillRef}",
                    testProject.getPublicId(),
                    skill.getPublicId())
                .with(user("user"))
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.description").doesNotExist());
  }

  @Test
  void updateSkill_nullField_shouldNotChangeValue() throws Exception {
    Skill skill = new Skill("skill_nullNoChange", testProject, "null-no-change-skill");
    skill.setDescription("Original description");
    skill.setFolderPath("/original/path");
    skillRepository.save(skill);

    // Send request with only name - other fields should remain unchanged
    UpdateSkillRequestDto request = new UpdateSkillRequestDto();
    request.setName("Updated Name");

    mockMvc
        .perform(
            put(
                    "/api/projects/{projectRef}/skills/{skillRef}",
                    testProject.getPublicId(),
                    skill.getPublicId())
                .with(user("user"))
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.name").value("Updated Name"))
        .andExpect(jsonPath("$.description").value("Original description"))
        .andExpect(jsonPath("$.folderPath").value("/original/path"));
  }
}
