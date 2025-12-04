package com.specflux.project.domain;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

/** Repository for ProjectMember entity. */
public interface ProjectMemberRepository extends JpaRepository<ProjectMember, Long> {

  List<ProjectMember> findByUserId(Long userId);

  List<ProjectMember> findByProjectId(Long projectId);

  Optional<ProjectMember> findByProjectIdAndUserId(Long projectId, Long userId);

  boolean existsByProjectIdAndUserId(Long projectId, Long userId);

  @Query("SELECT pm.project FROM ProjectMember pm WHERE pm.user.id = :userId")
  List<Project> findProjectsByUserId(@Param("userId") Long userId);

  @Query("SELECT COUNT(pm) FROM ProjectMember pm WHERE pm.user.id = :userId")
  long countProjectsByUserId(@Param("userId") Long userId);
}
