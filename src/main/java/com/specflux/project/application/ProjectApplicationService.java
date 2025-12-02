package com.specflux.project.application;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.List;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.specflux.api.generated.model.CreateProjectRequestDto;
import com.specflux.api.generated.model.CursorPaginationDto;
import com.specflux.api.generated.model.ProjectDto;
import com.specflux.api.generated.model.ProjectListResponseDto;
import com.specflux.api.generated.model.UpdateProjectRequestDto;
import com.specflux.project.domain.Project;
import com.specflux.project.domain.ProjectRepository;
import com.specflux.project.interfaces.rest.ProjectMapper;
import com.specflux.shared.application.CurrentUserService;
import com.specflux.shared.interfaces.rest.GlobalExceptionHandler.ResourceConflictException;
import com.specflux.shared.interfaces.rest.RefResolver;
import com.specflux.user.domain.User;

import lombok.RequiredArgsConstructor;

/** Application service for Project operations. */
@Service
@RequiredArgsConstructor
public class ProjectApplicationService {

  private final ProjectRepository projectRepository;
  private final RefResolver refResolver;
  private final CurrentUserService currentUserService;
  private final TransactionTemplate transactionTemplate;
  private final ObjectMapper objectMapper = new ObjectMapper();

  /**
   * Creates a new project.
   *
   * @param request the create request
   * @return the created project DTO
   */
  public ProjectDto createProject(CreateProjectRequestDto request) {
    return transactionTemplate.execute(
        status -> {
          // Check for duplicate project key
          if (projectRepository.existsByProjectKey(request.getProjectKey())) {
            throw new ResourceConflictException(
                "Project with key '" + request.getProjectKey() + "' already exists");
          }

          User owner = currentUserService.getCurrentUser();
          String publicId = generatePublicId("proj");

          Project project =
              new Project(publicId, request.getProjectKey(), request.getName(), owner);
          project.setDescription(request.getDescription());

          Project saved = projectRepository.save(project);
          return ProjectMapper.toDto(saved);
        });
  }

  /**
   * Gets a project by reference (publicId or projectKey).
   *
   * @param ref the project reference
   * @return the project DTO
   */
  public ProjectDto getProject(String ref) {
    Project project = refResolver.resolveProject(ref);
    return ProjectMapper.toDto(project);
  }

  /**
   * Updates a project.
   *
   * @param ref the project reference
   * @param request the update request
   * @return the updated project DTO
   */
  public ProjectDto updateProject(String ref, UpdateProjectRequestDto request) {
    Project project = refResolver.resolveProject(ref);

    if (request.getName() != null) {
      project.setName(request.getName());
    }
    if (request.getDescription() != null) {
      project.setDescription(request.getDescription());
    }

    Project saved = transactionTemplate.execute(status -> projectRepository.save(project));
    return ProjectMapper.toDto(saved);
  }

  /**
   * Deletes a project.
   *
   * @param ref the project reference
   */
  public void deleteProject(String ref) {
    Project project = refResolver.resolveProject(ref);
    transactionTemplate.executeWithoutResult(status -> projectRepository.delete(project));
  }

  /**
   * Lists projects for the current user with cursor-based pagination.
   *
   * @param cursor the pagination cursor (optional)
   * @param limit the page size
   * @param sort the sort field
   * @param order the sort order (asc/desc)
   * @return the paginated project list
   */
  public ProjectListResponseDto listProjects(String cursor, int limit, String sort, String order) {
    User currentUser = currentUserService.getCurrentUser();

    // Parse cursor if present
    CursorData cursorData = decodeCursor(cursor);

    // Determine sort field
    String sortField = mapSortField(sort);
    Sort.Direction direction =
        "asc".equalsIgnoreCase(order) ? Sort.Direction.ASC : Sort.Direction.DESC;

    // Fetch one extra to determine hasMore
    PageRequest pageRequest = PageRequest.of(0, limit + 1, Sort.by(direction, sortField));

    List<Project> projects;
    long total;

    if (cursorData != null) {
      // For cursor-based pagination, we'd ideally filter by the cursor field
      // For simplicity, using offset-based under the hood
      PageRequest cursorPageRequest =
          PageRequest.of(cursorData.offset() / limit, limit + 1, Sort.by(direction, sortField));
      Page<Project> page = projectRepository.findAll(cursorPageRequest);
      projects =
          page.getContent().stream()
              .filter(p -> p.getOwner().getId().equals(currentUser.getId()))
              .toList();
      total = projectRepository.findByOwnerId(currentUser.getId()).size();
    } else {
      List<Project> allProjects = projectRepository.findByOwnerId(currentUser.getId());
      total = allProjects.size();
      projects =
          allProjects.stream()
              .sorted(
                  (a, b) -> {
                    int cmp = compareByField(a, b, sortField);
                    return direction == Sort.Direction.DESC ? -cmp : cmp;
                  })
              .limit(limit + 1)
              .toList();
    }

    boolean hasMore = projects.size() > limit;
    List<Project> resultProjects = hasMore ? projects.subList(0, limit) : projects;

    // Build response
    ProjectListResponseDto response = new ProjectListResponseDto();
    response.setData(resultProjects.stream().map(ProjectMapper::toDto).toList());

    CursorPaginationDto pagination = new CursorPaginationDto();
    pagination.setTotal(total);
    pagination.setHasMore(hasMore);

    if (hasMore) {
      int nextOffset = (cursorData != null ? cursorData.offset() : 0) + limit;
      pagination.setNextCursor(encodeCursor(new CursorData(nextOffset)));
    }
    if (cursorData != null && cursorData.offset() > 0) {
      int prevOffset = Math.max(0, cursorData.offset() - limit);
      pagination.setPrevCursor(encodeCursor(new CursorData(prevOffset)));
    }

    response.setPagination(pagination);
    return response;
  }

  private int compareByField(Project a, Project b, String field) {
    return switch (field) {
      case "name" -> a.getName().compareToIgnoreCase(b.getName());
      case "projectKey" -> a.getProjectKey().compareToIgnoreCase(b.getProjectKey());
      case "updatedAt" -> a.getUpdatedAt().compareTo(b.getUpdatedAt());
      default -> a.getCreatedAt().compareTo(b.getCreatedAt());
    };
  }

  private String mapSortField(String sort) {
    return switch (sort) {
      case "name" -> "name";
      case "project_key" -> "projectKey";
      case "updated_at" -> "updatedAt";
      default -> "createdAt";
    };
  }

  private String generatePublicId(String prefix) {
    return prefix + "_" + UUID.randomUUID().toString().replace("-", "").substring(0, 16);
  }

  private CursorData decodeCursor(String cursor) {
    if (cursor == null || cursor.isBlank()) {
      return null;
    }
    try {
      String json = new String(Base64.getDecoder().decode(cursor), StandardCharsets.UTF_8);
      return objectMapper.readValue(json, CursorData.class);
    } catch (Exception e) {
      throw new IllegalArgumentException("Invalid cursor");
    }
  }

  private String encodeCursor(CursorData data) {
    try {
      String json = objectMapper.writeValueAsString(data);
      return Base64.getEncoder().encodeToString(json.getBytes(StandardCharsets.UTF_8));
    } catch (JsonProcessingException e) {
      throw new RuntimeException("Failed to encode cursor", e);
    }
  }

  private record CursorData(int offset) {}
}
