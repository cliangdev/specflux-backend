package com.specflux.release.interfaces.rest;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

import com.specflux.api.generated.ReleasesApi;
import com.specflux.api.generated.model.CreateReleaseRequestDto;
import com.specflux.api.generated.model.ReleaseDto;
import com.specflux.api.generated.model.ReleaseListResponseDto;
import com.specflux.api.generated.model.ReleaseStatusDto;
import com.specflux.api.generated.model.ReleaseWithEpicsDto;
import com.specflux.api.generated.model.UpdateReleaseRequestDto;
import com.specflux.release.application.ReleaseApplicationService;

import lombok.RequiredArgsConstructor;

/** REST controller for Release operations. Implements generated OpenAPI interface. */
@RestController
@RequiredArgsConstructor
public class ReleaseController implements ReleasesApi {

  private final ReleaseApplicationService releaseApplicationService;

  @Override
  public ResponseEntity<ReleaseDto> createRelease(
      String projectRef, CreateReleaseRequestDto request) {
    ReleaseDto created = releaseApplicationService.createRelease(projectRef, request);
    return ResponseEntity.status(HttpStatus.CREATED).body(created);
  }

  @Override
  public ResponseEntity<ReleaseWithEpicsDto> getRelease(
      String projectRef, String releaseRef, String include) {
    ReleaseWithEpicsDto release =
        releaseApplicationService.getRelease(projectRef, releaseRef, include);
    return ResponseEntity.ok(release);
  }

  @Override
  public ResponseEntity<ReleaseDto> updateRelease(
      String projectRef, String releaseRef, UpdateReleaseRequestDto request) {
    ReleaseDto updated = releaseApplicationService.updateRelease(projectRef, releaseRef, request);
    return ResponseEntity.ok(updated);
  }

  @Override
  public ResponseEntity<Void> deleteRelease(String projectRef, String releaseRef) {
    releaseApplicationService.deleteRelease(projectRef, releaseRef);
    return ResponseEntity.noContent().build();
  }

  @Override
  public ResponseEntity<ReleaseListResponseDto> listReleases(
      String projectRef,
      String cursor,
      Integer limit,
      String sort,
      String order,
      ReleaseStatusDto status) {
    ReleaseListResponseDto response =
        releaseApplicationService.listReleases(projectRef, cursor, limit, sort, order, status);
    return ResponseEntity.ok(response);
  }
}
