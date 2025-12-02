package com.specflux.epic.interfaces.rest;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

import com.specflux.api.generated.EpicsApi;
import com.specflux.api.generated.model.CreateEpicRequest;
import com.specflux.api.generated.model.Epic;
import com.specflux.api.generated.model.EpicListResponse;
import com.specflux.api.generated.model.EpicStatus;
import com.specflux.api.generated.model.UpdateEpicRequest;
import com.specflux.epic.application.EpicApplicationService;

/** REST controller for Epic operations. Implements generated OpenAPI interface. */
@RestController
public class EpicController implements EpicsApi {

  private final EpicApplicationService epicApplicationService;

  public EpicController(EpicApplicationService epicApplicationService) {
    this.epicApplicationService = epicApplicationService;
  }

  @Override
  public ResponseEntity<Epic> createEpic(String projectRef, CreateEpicRequest request) {
    Epic created = epicApplicationService.createEpic(projectRef, request);
    return ResponseEntity.status(HttpStatus.CREATED).body(created);
  }

  @Override
  public ResponseEntity<Epic> getEpic(String projectRef, String epicRef) {
    Epic epic = epicApplicationService.getEpic(projectRef, epicRef);
    return ResponseEntity.ok(epic);
  }

  @Override
  public ResponseEntity<Epic> updateEpic(
      String projectRef, String epicRef, UpdateEpicRequest request) {
    Epic updated = epicApplicationService.updateEpic(projectRef, epicRef, request);
    return ResponseEntity.ok(updated);
  }

  @Override
  public ResponseEntity<Void> deleteEpic(String projectRef, String epicRef) {
    epicApplicationService.deleteEpic(projectRef, epicRef);
    return ResponseEntity.noContent().build();
  }

  @Override
  public ResponseEntity<EpicListResponse> listEpics(
      String projectRef,
      String cursor,
      Integer limit,
      String sort,
      String order,
      EpicStatus status) {
    EpicListResponse response =
        epicApplicationService.listEpics(projectRef, cursor, limit, sort, order, status);
    return ResponseEntity.ok(response);
  }
}
