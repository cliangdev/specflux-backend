package com.specflux.prd.domain;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

/** Repository for Prd aggregate root. */
public interface PrdRepository extends JpaRepository<Prd, Long> {

  Optional<Prd> findByPublicId(String publicId);

  Optional<Prd> findByPublicIdAndProjectId(String publicId, Long projectId);

  Optional<Prd> findByProjectIdAndDisplayKey(Long projectId, String displayKey);

  Optional<Prd> findByProjectIdAndFolderPath(Long projectId, String folderPath);

  List<Prd> findByProjectId(Long projectId);

  List<Prd> findByProjectIdAndStatus(Long projectId, PrdStatus status);

  int countByProjectId(Long projectId);
}
