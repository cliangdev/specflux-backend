package com.specflux.acceptancecriteria.application;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;

import com.specflux.acceptancecriteria.domain.AcceptanceCriteria;
import com.specflux.acceptancecriteria.domain.AcceptanceCriteriaRepository;
import com.specflux.acceptancecriteria.interfaces.rest.AcceptanceCriteriaMapper;
import com.specflux.api.generated.model.AcceptanceCriteriaDto;
import com.specflux.api.generated.model.AcceptanceCriteriaListResponseDto;
import com.specflux.api.generated.model.CreateAcceptanceCriteriaRequestDto;
import com.specflux.api.generated.model.UpdateAcceptanceCriteriaRequestDto;
import com.specflux.epic.domain.Epic;
import com.specflux.project.domain.Project;
import com.specflux.shared.interfaces.rest.RefResolver;
import com.specflux.task.domain.Task;

import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;

/** Application service for AcceptanceCriteria operations. */
@Service
@RequiredArgsConstructor
public class AcceptanceCriteriaApplicationService {

  private final AcceptanceCriteriaRepository acceptanceCriteriaRepository;
  private final RefResolver refResolver;
  private final TransactionTemplate transactionTemplate;

  // ==================== TASK ACCEPTANCE CRITERIA ====================

  /**
   * Lists acceptance criteria for a task.
   *
   * @param projectRef the project reference
   * @param taskRef the task reference
   * @return the list of acceptance criteria
   */
  public AcceptanceCriteriaListResponseDto listTaskAcceptanceCriteria(
      String projectRef, String taskRef) {
    Project project = refResolver.resolveProject(projectRef);
    Task task = refResolver.resolveTask(project, taskRef);

    List<AcceptanceCriteria> criteria =
        acceptanceCriteriaRepository.findByTaskIdOrderByOrderIndexAsc(task.getId());

    AcceptanceCriteriaListResponseDto response = new AcceptanceCriteriaListResponseDto();
    response.setData(criteria.stream().map(AcceptanceCriteriaMapper::toDto).toList());
    return response;
  }

  /**
   * Creates acceptance criteria for a task.
   *
   * @param projectRef the project reference
   * @param taskRef the task reference
   * @param request the create request
   * @return the created acceptance criteria DTO
   */
  public AcceptanceCriteriaDto createTaskAcceptanceCriteria(
      String projectRef, String taskRef, CreateAcceptanceCriteriaRequestDto request) {
    Project project = refResolver.resolveProject(projectRef);
    Task task = refResolver.resolveTask(project, taskRef);

    int orderIndex =
        request.getOrderIndex() != null && request.getOrderIndex().isPresent()
            ? request.getOrderIndex().get()
            : acceptanceCriteriaRepository.countByTaskId(task.getId());

    AcceptanceCriteria ac = new AcceptanceCriteria(task, request.getCriteria(), orderIndex);
    AcceptanceCriteria saved =
        transactionTemplate.execute(_ -> acceptanceCriteriaRepository.save(ac));
    return AcceptanceCriteriaMapper.toDto(saved);
  }

  /**
   * Gets a specific acceptance criteria for a task.
   *
   * @param projectRef the project reference
   * @param taskRef the task reference
   * @param criteriaId the criteria ID
   * @return the acceptance criteria DTO
   */
  public AcceptanceCriteriaDto getTaskAcceptanceCriteria(
      String projectRef, String taskRef, Long criteriaId) {
    Project project = refResolver.resolveProject(projectRef);
    Task task = refResolver.resolveTask(project, taskRef);

    AcceptanceCriteria ac =
        acceptanceCriteriaRepository
            .findByIdAndTaskId(criteriaId, task.getId())
            .orElseThrow(
                () ->
                    new EntityNotFoundException(
                        "Acceptance criteria " + criteriaId + " not found for task " + taskRef));

    return AcceptanceCriteriaMapper.toDto(ac);
  }

  /**
   * Updates acceptance criteria for a task.
   *
   * @param projectRef the project reference
   * @param taskRef the task reference
   * @param criteriaId the criteria ID
   * @param request the update request
   * @return the updated acceptance criteria DTO
   */
  public AcceptanceCriteriaDto updateTaskAcceptanceCriteria(
      String projectRef,
      String taskRef,
      Long criteriaId,
      UpdateAcceptanceCriteriaRequestDto request) {
    Project project = refResolver.resolveProject(projectRef);
    Task task = refResolver.resolveTask(project, taskRef);

    AcceptanceCriteria ac =
        acceptanceCriteriaRepository
            .findByIdAndTaskId(criteriaId, task.getId())
            .orElseThrow(
                () ->
                    new EntityNotFoundException(
                        "Acceptance criteria " + criteriaId + " not found for task " + taskRef));

    if (request.getCriteria() != null) {
      ac.setCriteria(request.getCriteria());
    }
    if (request.getIsMet() != null) {
      ac.setIsMet(request.getIsMet());
    }
    if (request.getOrderIndex() != null) {
      ac.setOrderIndex(request.getOrderIndex());
    }

    AcceptanceCriteria saved =
        transactionTemplate.execute(_ -> acceptanceCriteriaRepository.save(ac));
    return AcceptanceCriteriaMapper.toDto(saved);
  }

  /**
   * Deletes acceptance criteria from a task.
   *
   * @param projectRef the project reference
   * @param taskRef the task reference
   * @param criteriaId the criteria ID
   */
  public void deleteTaskAcceptanceCriteria(String projectRef, String taskRef, Long criteriaId) {
    Project project = refResolver.resolveProject(projectRef);
    Task task = refResolver.resolveTask(project, taskRef);

    AcceptanceCriteria ac =
        acceptanceCriteriaRepository
            .findByIdAndTaskId(criteriaId, task.getId())
            .orElseThrow(
                () ->
                    new EntityNotFoundException(
                        "Acceptance criteria " + criteriaId + " not found for task " + taskRef));

    transactionTemplate.executeWithoutResult(_ -> acceptanceCriteriaRepository.delete(ac));
  }

  // ==================== EPIC ACCEPTANCE CRITERIA ====================

  /**
   * Lists acceptance criteria for an epic.
   *
   * @param projectRef the project reference
   * @param epicRef the epic reference
   * @return the list of acceptance criteria
   */
  public AcceptanceCriteriaListResponseDto listEpicAcceptanceCriteria(
      String projectRef, String epicRef) {
    Project project = refResolver.resolveProject(projectRef);
    Epic epic = refResolver.resolveEpic(project, epicRef);

    List<AcceptanceCriteria> criteria =
        acceptanceCriteriaRepository.findByEpicIdOrderByOrderIndexAsc(epic.getId());

    AcceptanceCriteriaListResponseDto response = new AcceptanceCriteriaListResponseDto();
    response.setData(criteria.stream().map(AcceptanceCriteriaMapper::toDto).toList());
    return response;
  }

  /**
   * Creates acceptance criteria for an epic.
   *
   * @param projectRef the project reference
   * @param epicRef the epic reference
   * @param request the create request
   * @return the created acceptance criteria DTO
   */
  public AcceptanceCriteriaDto createEpicAcceptanceCriteria(
      String projectRef, String epicRef, CreateAcceptanceCriteriaRequestDto request) {
    Project project = refResolver.resolveProject(projectRef);
    Epic epic = refResolver.resolveEpic(project, epicRef);

    int orderIndex =
        request.getOrderIndex() != null && request.getOrderIndex().isPresent()
            ? request.getOrderIndex().get()
            : acceptanceCriteriaRepository.countByEpicId(epic.getId());

    AcceptanceCriteria ac = new AcceptanceCriteria(epic, request.getCriteria(), orderIndex);
    AcceptanceCriteria saved =
        transactionTemplate.execute(_ -> acceptanceCriteriaRepository.save(ac));
    return AcceptanceCriteriaMapper.toDto(saved);
  }

  /**
   * Gets a specific acceptance criteria for an epic.
   *
   * @param projectRef the project reference
   * @param epicRef the epic reference
   * @param criteriaId the criteria ID
   * @return the acceptance criteria DTO
   */
  public AcceptanceCriteriaDto getEpicAcceptanceCriteria(
      String projectRef, String epicRef, Long criteriaId) {
    Project project = refResolver.resolveProject(projectRef);
    Epic epic = refResolver.resolveEpic(project, epicRef);

    AcceptanceCriteria ac =
        acceptanceCriteriaRepository
            .findByIdAndEpicId(criteriaId, epic.getId())
            .orElseThrow(
                () ->
                    new EntityNotFoundException(
                        "Acceptance criteria " + criteriaId + " not found for epic " + epicRef));

    return AcceptanceCriteriaMapper.toDto(ac);
  }

  /**
   * Updates acceptance criteria for an epic.
   *
   * @param projectRef the project reference
   * @param epicRef the epic reference
   * @param criteriaId the criteria ID
   * @param request the update request
   * @return the updated acceptance criteria DTO
   */
  public AcceptanceCriteriaDto updateEpicAcceptanceCriteria(
      String projectRef,
      String epicRef,
      Long criteriaId,
      UpdateAcceptanceCriteriaRequestDto request) {
    Project project = refResolver.resolveProject(projectRef);
    Epic epic = refResolver.resolveEpic(project, epicRef);

    AcceptanceCriteria ac =
        acceptanceCriteriaRepository
            .findByIdAndEpicId(criteriaId, epic.getId())
            .orElseThrow(
                () ->
                    new EntityNotFoundException(
                        "Acceptance criteria " + criteriaId + " not found for epic " + epicRef));

    if (request.getCriteria() != null) {
      ac.setCriteria(request.getCriteria());
    }
    if (request.getIsMet() != null) {
      ac.setIsMet(request.getIsMet());
    }
    if (request.getOrderIndex() != null) {
      ac.setOrderIndex(request.getOrderIndex());
    }

    AcceptanceCriteria saved =
        transactionTemplate.execute(_ -> acceptanceCriteriaRepository.save(ac));
    return AcceptanceCriteriaMapper.toDto(saved);
  }

  /**
   * Deletes acceptance criteria from an epic.
   *
   * @param projectRef the project reference
   * @param epicRef the epic reference
   * @param criteriaId the criteria ID
   */
  public void deleteEpicAcceptanceCriteria(String projectRef, String epicRef, Long criteriaId) {
    Project project = refResolver.resolveProject(projectRef);
    Epic epic = refResolver.resolveEpic(project, epicRef);

    AcceptanceCriteria ac =
        acceptanceCriteriaRepository
            .findByIdAndEpicId(criteriaId, epic.getId())
            .orElseThrow(
                () ->
                    new EntityNotFoundException(
                        "Acceptance criteria " + criteriaId + " not found for epic " + epicRef));

    transactionTemplate.executeWithoutResult(_ -> acceptanceCriteriaRepository.delete(ac));
  }
}
