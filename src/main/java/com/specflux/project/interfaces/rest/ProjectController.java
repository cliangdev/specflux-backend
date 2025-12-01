package com.specflux.project.interfaces.rest;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

import com.specflux.api.generated.ProjectsApi;
import com.specflux.api.generated.model.CreateProjectRequest;
import com.specflux.api.generated.model.Project;
import com.specflux.api.generated.model.ProjectListResponse;
import com.specflux.api.generated.model.UpdateProjectRequest;
import com.specflux.project.application.ProjectApplicationService;

/** REST controller for Project operations. Implements generated OpenAPI interface. */
@RestController
public class ProjectController implements ProjectsApi {

  private final ProjectApplicationService projectApplicationService;

  public ProjectController(ProjectApplicationService projectApplicationService) {
    this.projectApplicationService = projectApplicationService;
  }

  @Override
  public ResponseEntity<Project> createProject(CreateProjectRequest request) {
    Project created = projectApplicationService.createProject(request);
    return ResponseEntity.status(HttpStatus.CREATED).body(created);
  }

  @Override
  public ResponseEntity<Project> getProject(String ref) {
    Project project = projectApplicationService.getProject(ref);
    return ResponseEntity.ok(project);
  }

  @Override
  public ResponseEntity<Project> updateProject(String ref, UpdateProjectRequest request) {
    Project updated = projectApplicationService.updateProject(ref, request);
    return ResponseEntity.ok(updated);
  }

  @Override
  public ResponseEntity<Void> deleteProject(String ref) {
    projectApplicationService.deleteProject(ref);
    return ResponseEntity.noContent().build();
  }

  @Override
  public ResponseEntity<ProjectListResponse> listProjects(
      String cursor, Integer limit, String sort, String order) {
    ProjectListResponse response =
        projectApplicationService.listProjects(cursor, limit, sort, order);
    return ResponseEntity.ok(response);
  }
}
