package com.specflux.repository.domain;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

/** JPA repository for Repository entities. */
public interface RepositoryRepository extends JpaRepository<Repository, Long> {

  Optional<Repository> findByPublicId(String publicId);

  List<Repository> findByProjectId(Long projectId);

  boolean existsByProjectIdAndPath(Long projectId, String path);

  boolean existsByProjectIdAndName(Long projectId, String name);
}
