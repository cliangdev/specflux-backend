package com.specflux.project.interfaces.rest;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

import com.specflux.api.generated.ProjectsApi;
import com.specflux.api.generated.model.CreateProjectRequestDto;
import com.specflux.api.generated.model.ProjectDto;
import com.specflux.api.generated.model.ProjectListResponseDto;
import com.specflux.api.generated.model.UpdateProjectRequestDto;
import com.specflux.project.application.ProjectApplicationService;

import lombok.RequiredArgsConstructor;

/** REST controller for Project operations. Implements generated OpenAPI interface. */
@RestController
@RequiredArgsConstructor
public class ProjectController implements ProjectsApi {

  private final ProjectApplicationService projectApplicationService;

  @Override
  public ResponseEntity<ProjectDto> createProject(CreateProjectRequestDto request) {
    ProjectDto created = projectApplicationService.createProject(request);
    return ResponseEntity.status(HttpStatus.CREATED).body(created);
  }

  @Override
  public ResponseEntity<ProjectDto> getProject(String ref) {
    ProjectDto project = projectApplicationService.getProject(ref);
    return ResponseEntity.ok(project);
  }

  @Override
  public ResponseEntity<ProjectDto> updateProject(String ref, UpdateProjectRequestDto request) {
    ProjectDto updated = projectApplicationService.updateProject(ref, request);
    return ResponseEntity.ok(updated);
  }

  @Override
  public ResponseEntity<Void> deleteProject(String ref) {
    projectApplicationService.deleteProject(ref);
    return ResponseEntity.noContent().build();
  }

  @Override
  public ResponseEntity<ProjectListResponseDto> listProjects(
      String cursor, Integer limit, String sort, String order) {
    ProjectListResponseDto response =
        projectApplicationService.listProjects(cursor, limit, sort, order);
    return ResponseEntity.ok(response);
  }
}
