package com.specflux.project.domain;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

/** Repository for Project aggregate root. */
public interface ProjectRepository extends JpaRepository<Project, Long> {

  Optional<Project> findByPublicId(String publicId);

  Optional<Project> findByProjectKey(String projectKey);

  List<Project> findByOwnerId(Long ownerId);

  boolean existsByProjectKey(String projectKey);
}
