package com.specflux.agent.domain;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

/** JPA repository for Agent entities. */
public interface AgentRepository extends JpaRepository<Agent, Long> {

  Optional<Agent> findByPublicId(String publicId);

  List<Agent> findByProjectId(Long projectId);

  boolean existsByProjectIdAndName(Long projectId, String name);

  Optional<Agent> findByProjectIdAndName(Long projectId, String name);
}
