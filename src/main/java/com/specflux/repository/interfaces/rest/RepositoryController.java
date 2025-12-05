package com.specflux.repository.interfaces.rest;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

import com.specflux.api.generated.RepositoriesApi;
import com.specflux.api.generated.model.CreateRepositoryRequestDto;
import com.specflux.api.generated.model.RepositoryDto;
import com.specflux.api.generated.model.RepositoryListResponseDto;
import com.specflux.api.generated.model.UpdateRepositoryRequestDto;
import com.specflux.repository.application.RepositoryApplicationService;

import lombok.RequiredArgsConstructor;

/** REST controller for Repository endpoints. Implements generated OpenAPI interface. */
@RestController
@RequiredArgsConstructor
public class RepositoryController implements RepositoriesApi {

  private final RepositoryApplicationService repositoryService;

  @Override
  public ResponseEntity<RepositoryListResponseDto> listRepositories(String projectRef) {
    return ResponseEntity.ok(repositoryService.listRepositories(projectRef));
  }

  @Override
  public ResponseEntity<RepositoryDto> createRepository(
      String projectRef, CreateRepositoryRequestDto createRepositoryRequestDto) {
    return ResponseEntity.status(HttpStatus.CREATED)
        .body(repositoryService.createRepository(projectRef, createRepositoryRequestDto));
  }

  @Override
  public ResponseEntity<RepositoryDto> getRepository(String projectRef, String repoRef) {
    return ResponseEntity.ok(repositoryService.getRepository(projectRef, repoRef));
  }

  @Override
  public ResponseEntity<RepositoryDto> updateRepository(
      String projectRef, String repoRef, UpdateRepositoryRequestDto updateRepositoryRequestDto) {
    return ResponseEntity.ok(
        repositoryService.updateRepository(projectRef, repoRef, updateRepositoryRequestDto));
  }

  @Override
  public ResponseEntity<Void> deleteRepository(String projectRef, String repoRef) {
    repositoryService.deleteRepository(projectRef, repoRef);
    return ResponseEntity.noContent().build();
  }
}
