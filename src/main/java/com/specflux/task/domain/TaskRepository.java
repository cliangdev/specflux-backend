package com.specflux.task.domain;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

/** Repository for Task aggregate root. */
public interface TaskRepository extends JpaRepository<Task, Long> {

  Optional<Task> findByPublicId(String publicId);

  Optional<Task> findByPublicIdAndProjectId(String publicId, Long projectId);

  Optional<Task> findByProjectIdAndDisplayKey(Long projectId, String displayKey);

  List<Task> findByProjectId(Long projectId);

  List<Task> findByProjectIdAndStatus(Long projectId, TaskStatus status);

  List<Task> findByEpicId(Long epicId);

  List<Task> findByAssignedToId(Long userId);

  List<Task> findByCreatedById(Long userId);
}
