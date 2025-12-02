package com.specflux.task.interfaces.rest;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

import com.specflux.api.generated.TasksApi;
import com.specflux.api.generated.model.CreateTaskRequest;
import com.specflux.api.generated.model.Task;
import com.specflux.api.generated.model.TaskListResponse;
import com.specflux.api.generated.model.TaskPriority;
import com.specflux.api.generated.model.TaskStatus;
import com.specflux.api.generated.model.UpdateTaskRequest;
import com.specflux.task.application.TaskApplicationService;

/** REST controller for Task operations. Implements generated OpenAPI interface. */
@RestController
public class TaskController implements TasksApi {

  private final TaskApplicationService taskApplicationService;

  public TaskController(TaskApplicationService taskApplicationService) {
    this.taskApplicationService = taskApplicationService;
  }

  @Override
  public ResponseEntity<Task> createTask(String projectRef, CreateTaskRequest request) {
    Task created = taskApplicationService.createTask(projectRef, request);
    return ResponseEntity.status(HttpStatus.CREATED).body(created);
  }

  @Override
  public ResponseEntity<Task> getTask(String projectRef, String taskRef) {
    Task task = taskApplicationService.getTask(projectRef, taskRef);
    return ResponseEntity.ok(task);
  }

  @Override
  public ResponseEntity<Task> updateTask(
      String projectRef, String taskRef, UpdateTaskRequest request) {
    Task updated = taskApplicationService.updateTask(projectRef, taskRef, request);
    return ResponseEntity.ok(updated);
  }

  @Override
  public ResponseEntity<Void> deleteTask(String projectRef, String taskRef) {
    taskApplicationService.deleteTask(projectRef, taskRef);
    return ResponseEntity.noContent().build();
  }

  @Override
  public ResponseEntity<TaskListResponse> listTasks(
      String projectRef,
      String cursor,
      Integer limit,
      String sort,
      String order,
      TaskStatus status,
      TaskPriority priority,
      String epicRef,
      String assignedToRef,
      String search) {
    TaskListResponse response =
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
}
