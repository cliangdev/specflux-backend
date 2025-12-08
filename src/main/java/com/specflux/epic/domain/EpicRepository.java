package com.specflux.epic.domain;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

/** Repository for Epic aggregate root. */
public interface EpicRepository extends JpaRepository<Epic, Long> {

  Optional<Epic> findByPublicId(String publicId);

  Optional<Epic> findByPublicIdAndProjectId(String publicId, Long projectId);

  Optional<Epic> findByProjectIdAndDisplayKey(Long projectId, String displayKey);

  List<Epic> findByProjectId(Long projectId);

  List<Epic> findByProjectIdAndStatus(Long projectId, EpicStatus status);

  List<Epic> findByProjectIdAndPrdId(Long projectId, Long prdId);

  List<Epic> findByProjectIdAndStatusAndPrdId(Long projectId, EpicStatus status, Long prdId);

  List<Epic> findByReleaseId(Long releaseId);
}
