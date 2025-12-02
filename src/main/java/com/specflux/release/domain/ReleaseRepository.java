package com.specflux.release.domain;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

/** Repository for Release aggregate root. */
public interface ReleaseRepository extends JpaRepository<Release, Long> {

  Optional<Release> findByPublicId(String publicId);

  Optional<Release> findByPublicIdAndProjectId(String publicId, Long projectId);

  Optional<Release> findByProjectIdAndDisplayKey(Long projectId, String displayKey);

  List<Release> findByProjectId(Long projectId);

  List<Release> findByProjectIdAndStatus(Long projectId, ReleaseStatus status);
}
