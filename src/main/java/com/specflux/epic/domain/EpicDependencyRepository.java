package com.specflux.epic.domain;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

/** Repository for epic dependency operations. */
@Repository
public interface EpicDependencyRepository extends JpaRepository<EpicDependency, Long> {

  /** Find all dependencies for an epic (epics that this epic depends on). */
  List<EpicDependency> findByEpicId(Long epicId);

  /** Find all dependents of an epic (epics that depend on this epic). */
  List<EpicDependency> findByDependsOnEpicId(Long epicId);

  /** Find a specific dependency relationship. */
  Optional<EpicDependency> findByEpicIdAndDependsOnEpicId(Long epicId, Long dependsOnEpicId);

  /** Delete all dependencies for an epic. */
  void deleteByEpicId(Long epicId);

  /** Delete all dependencies where this epic is the target. */
  void deleteByDependsOnEpicId(Long epicId);

  /** Find all dependencies for epics in a project. */
  @Query("SELECT ed FROM EpicDependency ed WHERE ed.epic.project.id = :projectId")
  List<EpicDependency> findByProjectId(@Param("projectId") Long projectId);
}
