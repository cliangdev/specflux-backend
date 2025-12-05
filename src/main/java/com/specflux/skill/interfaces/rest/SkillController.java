package com.specflux.skill.interfaces.rest;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

import com.specflux.api.generated.SkillsApi;
import com.specflux.api.generated.model.CreateSkillRequestDto;
import com.specflux.api.generated.model.SkillDto;
import com.specflux.api.generated.model.SkillListResponseDto;
import com.specflux.api.generated.model.UpdateSkillRequestDto;
import com.specflux.skill.application.SkillApplicationService;

import lombok.RequiredArgsConstructor;

/** REST controller for Skill endpoints. Implements generated OpenAPI interface. */
@RestController
@RequiredArgsConstructor
public class SkillController implements SkillsApi {

  private final SkillApplicationService skillService;

  @Override
  public ResponseEntity<SkillListResponseDto> listSkills(String projectRef) {
    return ResponseEntity.ok(skillService.listSkills(projectRef));
  }

  @Override
  public ResponseEntity<SkillDto> createSkill(
      String projectRef, CreateSkillRequestDto createSkillRequestDto) {
    return ResponseEntity.status(HttpStatus.CREATED)
        .body(skillService.createSkill(projectRef, createSkillRequestDto));
  }

  @Override
  public ResponseEntity<SkillDto> getSkill(String projectRef, String skillRef) {
    return ResponseEntity.ok(skillService.getSkill(projectRef, skillRef));
  }

  @Override
  public ResponseEntity<SkillDto> updateSkill(
      String projectRef, String skillRef, UpdateSkillRequestDto updateSkillRequestDto) {
    return ResponseEntity.ok(skillService.updateSkill(projectRef, skillRef, updateSkillRequestDto));
  }

  @Override
  public ResponseEntity<Void> deleteSkill(String projectRef, String skillRef) {
    skillService.deleteSkill(projectRef, skillRef);
    return ResponseEntity.noContent().build();
  }
}
