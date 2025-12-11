package com.specflux.epic.interfaces.rest;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

import com.specflux.acceptancecriteria.application.AcceptanceCriteriaApplicationService;
import com.specflux.api.generated.EpicsApi;
import com.specflux.api.generated.model.AcceptanceCriteriaDto;
import com.specflux.api.generated.model.AcceptanceCriteriaListResponseDto;
import com.specflux.api.generated.model.AddEpicDependencyRequestDto;
import com.specflux.api.generated.model.CreateAcceptanceCriteriaRequestDto;
import com.specflux.api.generated.model.CreateEpicRequestDto;
import com.specflux.api.generated.model.EpicDto;
import com.specflux.api.generated.model.EpicListResponseDto;
import com.specflux.api.generated.model.EpicStatusDto;
import com.specflux.api.generated.model.TaskListResponseDto;
import com.specflux.api.generated.model.TaskStatusDto;
import com.specflux.api.generated.model.UpdateAcceptanceCriteriaRequestDto;
import com.specflux.api.generated.model.UpdateEpicRequestDto;
import com.specflux.epic.application.EpicApplicationService;

import lombok.RequiredArgsConstructor;

/** REST controller for Epic operations. Implements generated OpenAPI interface. */
@RestController
@RequiredArgsConstructor
public class EpicController implements EpicsApi {

  private final EpicApplicationService epicApplicationService;
  private final AcceptanceCriteriaApplicationService acceptanceCriteriaApplicationService;

  @Override
  public ResponseEntity<EpicDto> createEpic(String projectRef, CreateEpicRequestDto request) {
    EpicDto created = epicApplicationService.createEpic(projectRef, request);
    return ResponseEntity.status(HttpStatus.CREATED).body(created);
  }

  @Override
  public ResponseEntity<EpicDto> getEpic(String projectRef, String epicRef) {
    EpicDto epic = epicApplicationService.getEpic(projectRef, epicRef);
    return ResponseEntity.ok(epic);
  }

  @Override
  public ResponseEntity<EpicDto> updateEpic(
      String projectRef, String epicRef, UpdateEpicRequestDto request) {
    EpicDto updated = epicApplicationService.updateEpic(projectRef, epicRef, request);
    return ResponseEntity.ok(updated);
  }

  @Override
  public ResponseEntity<Void> deleteEpic(String projectRef, String epicRef) {
    epicApplicationService.deleteEpic(projectRef, epicRef);
    return ResponseEntity.noContent().build();
  }

  @Override
  public ResponseEntity<EpicListResponseDto> listEpics(
      String projectRef,
      String cursor,
      Integer limit,
      String sort,
      String order,
      EpicStatusDto status,
      String prdRef,
      String releaseRef) {
    EpicListResponseDto response =
        epicApplicationService.listEpics(
            projectRef, cursor, limit, sort, order, status, prdRef, releaseRef);
    return ResponseEntity.ok(response);
  }

  // ==================== ACCEPTANCE CRITERIA ====================

  @Override
  public ResponseEntity<AcceptanceCriteriaListResponseDto> listEpicAcceptanceCriteria(
      String projectRef, String epicRef) {
    AcceptanceCriteriaListResponseDto response =
        acceptanceCriteriaApplicationService.listEpicAcceptanceCriteria(projectRef, epicRef);
    return ResponseEntity.ok(response);
  }

  @Override
  public ResponseEntity<AcceptanceCriteriaDto> createEpicAcceptanceCriteria(
      String projectRef, String epicRef, CreateAcceptanceCriteriaRequestDto request) {
    AcceptanceCriteriaDto created =
        acceptanceCriteriaApplicationService.createEpicAcceptanceCriteria(
            projectRef, epicRef, request);
    return ResponseEntity.status(HttpStatus.CREATED).body(created);
  }

  @Override
  public ResponseEntity<AcceptanceCriteriaDto> getEpicAcceptanceCriteria(
      String projectRef, String epicRef, Long criteriaId) {
    AcceptanceCriteriaDto ac =
        acceptanceCriteriaApplicationService.getEpicAcceptanceCriteria(
            projectRef, epicRef, criteriaId);
    return ResponseEntity.ok(ac);
  }

  @Override
  public ResponseEntity<AcceptanceCriteriaDto> updateEpicAcceptanceCriteria(
      String projectRef,
      String epicRef,
      Long criteriaId,
      UpdateAcceptanceCriteriaRequestDto request) {
    AcceptanceCriteriaDto updated =
        acceptanceCriteriaApplicationService.updateEpicAcceptanceCriteria(
            projectRef, epicRef, criteriaId, request);
    return ResponseEntity.ok(updated);
  }

  @Override
  public ResponseEntity<Void> deleteEpicAcceptanceCriteria(
      String projectRef, String epicRef, Long criteriaId) {
    acceptanceCriteriaApplicationService.deleteEpicAcceptanceCriteria(
        projectRef, epicRef, criteriaId);
    return ResponseEntity.noContent().build();
  }

  // ==================== TASKS ====================

  @Override
  public ResponseEntity<TaskListResponseDto> listEpicTasks(
      String projectRef, String epicRef, String cursor, Integer limit, TaskStatusDto status) {
    String statusStr = status != null ? status.name() : null;
    TaskListResponseDto response =
        epicApplicationService.listEpicTasks(projectRef, epicRef, cursor, limit, statusStr);
    return ResponseEntity.ok(response);
  }

  // ==================== DEPENDENCIES ====================

  @Override
  public ResponseEntity<EpicListResponseDto> listEpicDependencies(
      String projectRef, String epicRef) {
    EpicListResponseDto response = epicApplicationService.listEpicDependencies(projectRef, epicRef);
    return ResponseEntity.ok(response);
  }

  @Override
  public ResponseEntity<EpicDto> addEpicDependency(
      String projectRef, String epicRef, AddEpicDependencyRequestDto request) {
    EpicDto result =
        epicApplicationService.addEpicDependency(
            projectRef, epicRef, request.getDependsOnEpicRef());
    return ResponseEntity.status(HttpStatus.CREATED).body(result);
  }

  @Override
  public ResponseEntity<Void> removeEpicDependency(
      String projectRef, String epicRef, String depEpicRef) {
    epicApplicationService.removeEpicDependency(projectRef, epicRef, depEpicRef);
    return ResponseEntity.noContent().build();
  }
}
