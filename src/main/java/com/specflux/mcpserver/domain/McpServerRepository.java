package com.specflux.mcpserver.domain;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

/** JPA repository for McpServer entities. */
public interface McpServerRepository extends JpaRepository<McpServer, Long> {

  Optional<McpServer> findByPublicId(String publicId);

  List<McpServer> findByProjectId(Long projectId);

  boolean existsByProjectIdAndName(Long projectId, String name);

  Optional<McpServer> findByProjectIdAndName(Long projectId, String name);
}
