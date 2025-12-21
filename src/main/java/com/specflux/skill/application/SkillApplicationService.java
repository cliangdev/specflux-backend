package com.specflux.skill.application;

import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;

import com.specflux.api.generated.model.CreateSkillRequestDto;
import com.specflux.api.generated.model.SkillDto;
import com.specflux.api.generated.model.SkillListResponseDto;
import com.specflux.api.generated.model.UpdateSkillRequestDto;
import com.specflux.project.domain.Project;
import com.specflux.shared.application.UpdateHelper;
import com.specflux.shared.interfaces.rest.GlobalExceptionHandler.ResourceConflictException;
import com.specflux.shared.interfaces.rest.GlobalExceptionHandler.ResourceNotFoundException;
import com.specflux.shared.interfaces.rest.RefResolver;
import com.specflux.skill.domain.Skill;
import com.specflux.skill.domain.SkillRepository;
import com.specflux.skill.interfaces.rest.SkillMapper;

import lombok.RequiredArgsConstructor;

/** Application service for Skill operations. */
@Service
@RequiredArgsConstructor
public class SkillApplicationService {

  private final SkillRepository skillRepository;
  private final RefResolver refResolver;
  private final TransactionTemplate transactionTemplate;

  public SkillListResponseDto listSkills(String projectRef) {
    Project project = refResolver.resolveProject(projectRef);
    List<Skill> skills = skillRepository.findByProjectId(project.getId());

    SkillListResponseDto response = new SkillListResponseDto();
    response.setData(skills.stream().map(SkillMapper::toDto).toList());
    return response;
  }

  public SkillDto createSkill(String projectRef, CreateSkillRequestDto request) {
    Project project = refResolver.resolveProject(projectRef);

    if (skillRepository.existsByProjectIdAndName(project.getId(), request.getName())) {
      throw new ResourceConflictException("Skill with this name already exists");
    }

    String publicId = generatePublicId("skill");
    Skill skill = new Skill(publicId, project, request.getName());

    if (request.getDescription() != null) {
      skill.setDescription(request.getDescription());
    }
    if (request.getFolderPath() != null) {
      skill.setFolderPath(request.getFolderPath());
    }

    Skill saved = transactionTemplate.execute(status -> skillRepository.save(skill));
    return SkillMapper.toDto(saved);
  }

  public SkillDto getSkill(String projectRef, String skillRef) {
    refResolver.resolveProject(projectRef);
    Skill skill = resolveSkill(skillRef);
    return SkillMapper.toDto(skill);
  }

  public SkillDto updateSkill(String projectRef, String skillRef, UpdateSkillRequestDto request) {
    refResolver.resolveProject(projectRef);
    Skill skill = resolveSkill(skillRef);

    UpdateHelper.applyValue(request.getName(), skill::setName);
    UpdateHelper.applyString(request.getDescription(), skill::setDescription);
    UpdateHelper.applyValue(request.getFolderPath(), skill::setFolderPath);

    Skill saved = transactionTemplate.execute(status -> skillRepository.save(skill));
    return SkillMapper.toDto(saved);
  }

  public void deleteSkill(String projectRef, String skillRef) {
    refResolver.resolveProject(projectRef);
    Skill skill = resolveSkill(skillRef);
    transactionTemplate.executeWithoutResult(status -> skillRepository.delete(skill));
  }

  private Skill resolveSkill(String ref) {
    return skillRepository
        .findByPublicId(ref)
        .orElseThrow(() -> new ResourceNotFoundException("Skill not found: " + ref));
  }

  private String generatePublicId(String prefix) {
    return prefix + "_" + UUID.randomUUID().toString().replace("-", "").substring(0, 16);
  }
}
