package com.specflux.task.interfaces.rest;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

import com.specflux.acceptancecriteria.application.AcceptanceCriteriaApplicationService;
import com.specflux.api.generated.TasksApi;
import com.specflux.api.generated.model.AcceptanceCriteriaDto;
import com.specflux.api.generated.model.AcceptanceCriteriaListResponseDto;
import com.specflux.api.generated.model.AddTaskDependencyRequestDto;
import com.specflux.api.generated.model.CreateAcceptanceCriteriaRequestDto;
import com.specflux.api.generated.model.CreateTaskRequestDto;
import com.specflux.api.generated.model.TaskDependencyDto;
import com.specflux.api.generated.model.TaskDependencyListResponseDto;
import com.specflux.api.generated.model.TaskDto;
import com.specflux.api.generated.model.TaskListResponseDto;
import com.specflux.api.generated.model.TaskPriorityDto;
import com.specflux.api.generated.model.TaskStatusDto;
import com.specflux.api.generated.model.UpdateAcceptanceCriteriaRequestDto;
import com.specflux.api.generated.model.UpdateTaskRequestDto;
import com.specflux.task.application.TaskApplicationService;

import lombok.RequiredArgsConstructor;

/** REST controller for Task operations. Implements generated OpenAPI interface. */
@RestController
@RequiredArgsConstructor
public class TaskController implements TasksApi {

  private final TaskApplicationService taskApplicationService;
  private final AcceptanceCriteriaApplicationService acceptanceCriteriaApplicationService;

  @Override
  public ResponseEntity<TaskDto> createTask(String projectRef, CreateTaskRequestDto request) {
    TaskDto created = taskApplicationService.createTask(projectRef, request);
    return ResponseEntity.status(HttpStatus.CREATED).body(created);
  }

  @Override
  public ResponseEntity<TaskDto> getTask(String projectRef, String taskRef) {
    TaskDto task = taskApplicationService.getTask(projectRef, taskRef);
    return ResponseEntity.ok(task);
  }

  @Override
  public ResponseEntity<TaskDto> updateTask(
      String projectRef, String taskRef, UpdateTaskRequestDto request) {
    TaskDto updated = taskApplicationService.updateTask(projectRef, taskRef, request);
    return ResponseEntity.ok(updated);
  }

  @Override
  public ResponseEntity<Void> deleteTask(String projectRef, String taskRef) {
    taskApplicationService.deleteTask(projectRef, taskRef);
    return ResponseEntity.noContent().build();
  }

  @Override
  public ResponseEntity<TaskListResponseDto> listTasks(
      String projectRef,
      String cursor,
      Integer limit,
      String sort,
      String order,
      TaskStatusDto status,
      TaskPriorityDto priority,
      String epicRef,
      String assignedToRef,
      String search) {
    TaskListResponseDto response =
        taskApplicationService.listTasks(
            projectRef,
            cursor,
            limit,
            sort,
            order,
            status,
            priority,
            epicRef,
            assignedToRef,
            search);
    return ResponseEntity.ok(response);
  }

  @Override
  public ResponseEntity<TaskDependencyListResponseDto> listTaskDependencies(
      String projectRef, String taskRef) {
    TaskDependencyListResponseDto response =
        taskApplicationService.listTaskDependencies(projectRef, taskRef);
    return ResponseEntity.ok(response);
  }

  @Override
  public ResponseEntity<TaskDependencyDto> addTaskDependency(
      String projectRef, String taskRef, AddTaskDependencyRequestDto request) {
    TaskDependencyDto created =
        taskApplicationService.addTaskDependency(projectRef, taskRef, request);
    return ResponseEntity.status(HttpStatus.CREATED).body(created);
  }

  @Override
  public ResponseEntity<Void> removeTaskDependency(
      String projectRef, String taskRef, String dependsOnTaskRef) {
    taskApplicationService.removeTaskDependency(projectRef, taskRef, dependsOnTaskRef);
    return ResponseEntity.noContent().build();
  }

  // ==================== ACCEPTANCE CRITERIA ====================

  @Override
  public ResponseEntity<AcceptanceCriteriaListResponseDto> listTaskAcceptanceCriteria(
      String projectRef, String taskRef) {
    AcceptanceCriteriaListResponseDto response =
        acceptanceCriteriaApplicationService.listTaskAcceptanceCriteria(projectRef, taskRef);
    return ResponseEntity.ok(response);
  }

  @Override
  public ResponseEntity<AcceptanceCriteriaDto> createTaskAcceptanceCriteria(
      String projectRef, String taskRef, CreateAcceptanceCriteriaRequestDto request) {
    AcceptanceCriteriaDto created =
        acceptanceCriteriaApplicationService.createTaskAcceptanceCriteria(
            projectRef, taskRef, request);
    return ResponseEntity.status(HttpStatus.CREATED).body(created);
  }

  @Override
  public ResponseEntity<AcceptanceCriteriaDto> getTaskAcceptanceCriteria(
      String projectRef, String taskRef, Long criteriaId) {
    AcceptanceCriteriaDto ac =
        acceptanceCriteriaApplicationService.getTaskAcceptanceCriteria(
            projectRef, taskRef, criteriaId);
    return ResponseEntity.ok(ac);
  }

  @Override
  public ResponseEntity<AcceptanceCriteriaDto> updateTaskAcceptanceCriteria(
      String projectRef,
      String taskRef,
      Long criteriaId,
      UpdateAcceptanceCriteriaRequestDto request) {
    AcceptanceCriteriaDto updated =
        acceptanceCriteriaApplicationService.updateTaskAcceptanceCriteria(
            projectRef, taskRef, criteriaId, request);
    return ResponseEntity.ok(updated);
  }

  @Override
  public ResponseEntity<Void> deleteTaskAcceptanceCriteria(
      String projectRef, String taskRef, Long criteriaId) {
    acceptanceCriteriaApplicationService.deleteTaskAcceptanceCriteria(
        projectRef, taskRef, criteriaId);
    return ResponseEntity.noContent().build();
  }
}
