package com.specflux.acceptancecriteria.domain;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/** Repository for AcceptanceCriteria entity. */
@Repository
public interface AcceptanceCriteriaRepository extends JpaRepository<AcceptanceCriteria, Long> {

  /**
   * Find all acceptance criteria for a task, ordered by orderIndex.
   *
   * @param taskId the task ID
   * @return list of acceptance criteria
   */
  List<AcceptanceCriteria> findByTaskIdOrderByOrderIndexAsc(Long taskId);

  /**
   * Find all acceptance criteria for an epic, ordered by orderIndex.
   *
   * @param epicId the epic ID
   * @return list of acceptance criteria
   */
  List<AcceptanceCriteria> findByEpicIdOrderByOrderIndexAsc(Long epicId);

  /**
   * Find acceptance criteria by ID and task ID.
   *
   * @param id the criteria ID
   * @param taskId the task ID
   * @return the acceptance criteria if found
   */
  Optional<AcceptanceCriteria> findByIdAndTaskId(Long id, Long taskId);

  /**
   * Find acceptance criteria by ID and epic ID.
   *
   * @param id the criteria ID
   * @param epicId the epic ID
   * @return the acceptance criteria if found
   */
  Optional<AcceptanceCriteria> findByIdAndEpicId(Long id, Long epicId);

  /**
   * Count acceptance criteria for a task.
   *
   * @param taskId the task ID
   * @return the count
   */
  int countByTaskId(Long taskId);

  /**
   * Count acceptance criteria for an epic.
   *
   * @param epicId the epic ID
   * @return the count
   */
  int countByEpicId(Long epicId);
}
