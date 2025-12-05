package com.specflux.skill.domain;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

/** JPA repository for Skill entities. */
public interface SkillRepository extends JpaRepository<Skill, Long> {

  Optional<Skill> findByPublicId(String publicId);

  List<Skill> findByProjectId(Long projectId);

  boolean existsByProjectIdAndName(Long projectId, String name);

  Optional<Skill> findByProjectIdAndName(Long projectId, String name);
}
