package com.specflux.repository.application;

import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;

import com.specflux.api.generated.model.CreateRepositoryRequestDto;
import com.specflux.api.generated.model.RepositoryDto;
import com.specflux.api.generated.model.RepositoryListResponseDto;
import com.specflux.api.generated.model.UpdateRepositoryRequestDto;
import com.specflux.project.domain.Project;
import com.specflux.repository.domain.Repository;
import com.specflux.repository.domain.RepositoryRepository;
import com.specflux.repository.interfaces.rest.RepositoryMapper;
import com.specflux.shared.interfaces.rest.GlobalExceptionHandler.ResourceConflictException;
import com.specflux.shared.interfaces.rest.GlobalExceptionHandler.ResourceNotFoundException;
import com.specflux.shared.interfaces.rest.RefResolver;

import lombok.RequiredArgsConstructor;

/** Application service for Repository operations. */
@Service
@RequiredArgsConstructor
public class RepositoryApplicationService {

  private final RepositoryRepository repositoryRepository;
  private final RefResolver refResolver;
  private final TransactionTemplate transactionTemplate;

  public RepositoryListResponseDto listRepositories(String projectRef) {
    Project project = refResolver.resolveProject(projectRef);
    List<Repository> repositories = repositoryRepository.findByProjectId(project.getId());

    RepositoryListResponseDto response = new RepositoryListResponseDto();
    response.setData(repositories.stream().map(RepositoryMapper::toDto).toList());
    return response;
  }

  public RepositoryDto createRepository(String projectRef, CreateRepositoryRequestDto request) {
    Project project = refResolver.resolveProject(projectRef);

    if (repositoryRepository.existsByProjectIdAndPath(project.getId(), request.getPath())) {
      throw new ResourceConflictException("Repository with this path already exists");
    }

    String publicId = generatePublicId("repo");
    Repository repository = new Repository(publicId, project, request.getName(), request.getPath());

    if (request.getGitUrl() != null) {
      repository.setGitUrl(request.getGitUrl());
    }
    if (request.getDefaultBranch() != null) {
      repository.setDefaultBranch(request.getDefaultBranch());
    }

    Repository saved = transactionTemplate.execute(status -> repositoryRepository.save(repository));
    return RepositoryMapper.toDto(saved);
  }

  public RepositoryDto getRepository(String projectRef, String repoRef) {
    refResolver.resolveProject(projectRef); // Verify project access
    Repository repository = resolveRepository(repoRef);
    return RepositoryMapper.toDto(repository);
  }

  public RepositoryDto updateRepository(
      String projectRef, String repoRef, UpdateRepositoryRequestDto request) {
    refResolver.resolveProject(projectRef);
    Repository repository = resolveRepository(repoRef);

    if (request.getName() != null) {
      repository.setName(request.getName());
    }
    if (request.getGitUrl() != null) {
      repository.setGitUrl(request.getGitUrl());
    }
    if (request.getDefaultBranch() != null) {
      repository.setDefaultBranch(request.getDefaultBranch());
    }
    if (request.getStatus() != null) {
      repository.setStatus(Repository.Status.valueOf(request.getStatus().name()));
    }

    Repository saved = transactionTemplate.execute(status -> repositoryRepository.save(repository));
    return RepositoryMapper.toDto(saved);
  }

  public void deleteRepository(String projectRef, String repoRef) {
    refResolver.resolveProject(projectRef);
    Repository repository = resolveRepository(repoRef);
    transactionTemplate.executeWithoutResult(status -> repositoryRepository.delete(repository));
  }

  private Repository resolveRepository(String ref) {
    return repositoryRepository
        .findByPublicId(ref)
        .orElseThrow(() -> new ResourceNotFoundException("Repository not found: " + ref));
  }

  private String generatePublicId(String prefix) {
    return prefix + "_" + UUID.randomUUID().toString().replace("-", "").substring(0, 16);
  }
}
