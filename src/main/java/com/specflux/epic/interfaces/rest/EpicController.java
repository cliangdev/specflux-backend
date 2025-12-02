package com.specflux.epic.interfaces.rest;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

import com.specflux.api.generated.EpicsApi;
import com.specflux.api.generated.model.CreateEpicRequestDto;
import com.specflux.api.generated.model.EpicDto;
import com.specflux.api.generated.model.EpicListResponseDto;
import com.specflux.api.generated.model.EpicStatusDto;
import com.specflux.api.generated.model.UpdateEpicRequestDto;
import com.specflux.epic.application.EpicApplicationService;

import lombok.RequiredArgsConstructor;

/** REST controller for Epic operations. Implements generated OpenAPI interface. */
@RestController
@RequiredArgsConstructor
public class EpicController implements EpicsApi {

  private final EpicApplicationService epicApplicationService;

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
      EpicStatusDto status) {
    EpicListResponseDto response =
        epicApplicationService.listEpics(projectRef, cursor, limit, sort, order, status);
    return ResponseEntity.ok(response);
  }
}
