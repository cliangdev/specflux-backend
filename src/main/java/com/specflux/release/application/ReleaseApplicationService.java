package com.specflux.release.application;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.specflux.api.generated.model.CreateReleaseRequestDto;
import com.specflux.api.generated.model.CursorPaginationDto;
import com.specflux.api.generated.model.ReleaseDto;
import com.specflux.api.generated.model.ReleaseListResponseDto;
import com.specflux.api.generated.model.ReleaseStatusDto;
import com.specflux.api.generated.model.UpdateReleaseRequestDto;
import com.specflux.project.domain.Project;
import com.specflux.release.domain.Release;
import com.specflux.release.domain.ReleaseRepository;
import com.specflux.release.interfaces.rest.ReleaseMapper;
import com.specflux.shared.interfaces.rest.RefResolver;

import lombok.RequiredArgsConstructor;

/** Application service for Release operations. */
@Service
@RequiredArgsConstructor
public class ReleaseApplicationService {

  private final ReleaseRepository releaseRepository;
  private final RefResolver refResolver;
  private final TransactionTemplate transactionTemplate;
  private final ObjectMapper objectMapper = new ObjectMapper();

  /**
   * Creates a new release in a project.
   *
   * @param projectRef the project reference
   * @param request the create request
   * @return the created release DTO
   */
  public ReleaseDto createRelease(String projectRef, CreateReleaseRequestDto request) {
    return transactionTemplate.execute(
        status -> {
          Project project = refResolver.resolveProject(projectRef);

          String publicId = generatePublicId("rel");
          int sequenceNumber = getNextSequenceNumber(project);
          String displayKey = project.getProjectKey() + "-R" + sequenceNumber;

          Release release =
              new Release(publicId, project, sequenceNumber, displayKey, request.getName());
          release.setDescription(request.getDescription());
          release.setTargetDate(request.getTargetDate());

          Release saved = releaseRepository.save(release);
          return ReleaseMapper.toDto(saved);
        });
  }

  /**
   * Gets a release by reference within a project.
   *
   * @param projectRef the project reference
   * @param releaseRef the release reference (publicId or displayKey)
   * @return the release DTO
   */
  public ReleaseDto getRelease(String projectRef, String releaseRef) {
    Project project = refResolver.resolveProject(projectRef);
    Release release = refResolver.resolveRelease(project, releaseRef);
    return ReleaseMapper.toDto(release);
  }

  /**
   * Updates a release.
   *
   * @param projectRef the project reference
   * @param releaseRef the release reference
   * @param request the update request
   * @return the updated release DTO
   */
  public ReleaseDto updateRelease(
      String projectRef, String releaseRef, UpdateReleaseRequestDto request) {
    Project project = refResolver.resolveProject(projectRef);
    Release release = refResolver.resolveRelease(project, releaseRef);

    if (request.getName() != null) {
      release.setName(request.getName());
    }
    if (request.getDescription() != null) {
      release.setDescription(request.getDescription());
    }
    if (request.getStatus() != null) {
      release.setStatus(ReleaseMapper.toDomainStatus(request.getStatus()));
    }
    if (request.getTargetDate() != null) {
      release.setTargetDate(request.getTargetDate());
    }

    Release saved = transactionTemplate.execute(status -> releaseRepository.save(release));
    return ReleaseMapper.toDto(saved);
  }

  /**
   * Deletes a release.
   *
   * @param projectRef the project reference
   * @param releaseRef the release reference
   */
  public void deleteRelease(String projectRef, String releaseRef) {
    Project project = refResolver.resolveProject(projectRef);
    Release release = refResolver.resolveRelease(project, releaseRef);
    transactionTemplate.executeWithoutResult(status -> releaseRepository.delete(release));
  }

  /**
   * Lists releases in a project with cursor-based pagination.
   *
   * @param projectRef the project reference
   * @param cursor the pagination cursor (optional)
   * @param limit the page size
   * @param sort the sort field
   * @param order the sort order (asc/desc)
   * @param status optional status filter
   * @return the paginated release list
   */
  public ReleaseListResponseDto listReleases(
      String projectRef,
      String cursor,
      int limit,
      String sort,
      String order,
      ReleaseStatusDto status) {

    Project project = refResolver.resolveProject(projectRef);

    // Parse cursor if present
    CursorData cursorData = decodeCursor(cursor);
    int offset = cursorData != null ? cursorData.offset() : 0;

    // Get releases for project with optional status filter
    List<Release> allReleases;
    if (status != null) {
      allReleases =
          releaseRepository.findByProjectIdAndStatus(
              project.getId(), ReleaseMapper.toDomainStatus(status));
    } else {
      allReleases = releaseRepository.findByProjectId(project.getId());
    }

    long total = allReleases.size();

    // Sort field mapping
    String sortField = mapSortField(sort);
    boolean ascending = "asc".equalsIgnoreCase(order);

    // Sort and paginate
    Comparator<Release> comparator = getComparator(sortField);
    if (!ascending) {
      comparator = comparator.reversed();
    }

    List<Release> sortedReleases =
        allReleases.stream().sorted(comparator).skip(offset).limit(limit + 1).toList();

    boolean hasMore = sortedReleases.size() > limit;
    List<Release> resultReleases = hasMore ? sortedReleases.subList(0, limit) : sortedReleases;

    // Build response
    ReleaseListResponseDto response = new ReleaseListResponseDto();
    response.setData(resultReleases.stream().map(ReleaseMapper::toDto).toList());

    CursorPaginationDto pagination = new CursorPaginationDto();
    pagination.setTotal(total);
    pagination.setHasMore(hasMore);

    if (hasMore) {
      int nextOffset = offset + limit;
      pagination.setNextCursor(encodeCursor(new CursorData(nextOffset)));
    }
    if (offset > 0) {
      int prevOffset = Math.max(0, offset - limit);
      pagination.setPrevCursor(encodeCursor(new CursorData(prevOffset)));
    }

    response.setPagination(pagination);
    return response;
  }

  private Comparator<Release> getComparator(String field) {
    return switch (field) {
      case "name" -> Comparator.comparing(Release::getName, String.CASE_INSENSITIVE_ORDER);
      case "status" -> Comparator.comparing(r -> r.getStatus().name());
      case "targetDate" ->
          Comparator.comparing(
              Release::getTargetDate, Comparator.nullsLast(Comparator.naturalOrder()));
      case "updatedAt" -> Comparator.comparing(Release::getUpdatedAt);
      default -> Comparator.comparing(Release::getCreatedAt);
    };
  }

  private String mapSortField(String sort) {
    return switch (sort) {
      case "title", "name" -> "name";
      case "status" -> "status";
      case "target_date" -> "targetDate";
      case "updated_at" -> "updatedAt";
      default -> "createdAt";
    };
  }

  private int getNextSequenceNumber(Project project) {
    List<Release> releases = releaseRepository.findByProjectId(project.getId());
    return releases.stream().mapToInt(Release::getSequenceNumber).max().orElse(0) + 1;
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
