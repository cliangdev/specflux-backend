package com.specflux.github.domain;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

/** Repository for GithubInstallation aggregate root. */
public interface GithubInstallationRepository extends JpaRepository<GithubInstallation, Long> {

  Optional<GithubInstallation> findByUserId(Long userId);

  Optional<GithubInstallation> findByPublicId(String publicId);

  Optional<GithubInstallation> findByInstallationId(Long installationId);

  boolean existsByUserId(Long userId);

  void deleteByUserId(Long userId);
}
