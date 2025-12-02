package com.specflux.task.domain;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

/** Repository for TaskDependency entities. */
public interface TaskDependencyRepository extends JpaRepository<TaskDependency, Long> {

  /** Find all dependencies for a task (tasks that this task depends on). */
  List<TaskDependency> findByTaskId(Long taskId);

  /** Find all dependents of a task (tasks that depend on this task). */
  List<TaskDependency> findByDependsOnTaskId(Long dependsOnTaskId);

  /** Find a specific dependency relationship. */
  Optional<TaskDependency> findByTaskIdAndDependsOnTaskId(Long taskId, Long dependsOnTaskId);

  /** Check if a dependency relationship exists. */
  boolean existsByTaskIdAndDependsOnTaskId(Long taskId, Long dependsOnTaskId);

  /** Delete a specific dependency relationship. */
  void deleteByTaskIdAndDependsOnTaskId(Long taskId, Long dependsOnTaskId);
}
