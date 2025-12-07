package com.specflux.prd.interfaces.rest;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

import com.specflux.api.generated.PrdsApi;
import com.specflux.api.generated.model.AddPrdDocumentRequestDto;
import com.specflux.api.generated.model.CreatePrdRequestDto;
import com.specflux.api.generated.model.PrdDto;
import com.specflux.api.generated.model.PrdListResponseDto;
import com.specflux.api.generated.model.PrdStatusDto;
import com.specflux.api.generated.model.UpdatePrdDocumentRequestDto;
import com.specflux.api.generated.model.UpdatePrdRequestDto;
import com.specflux.prd.application.PrdApplicationService;

import lombok.RequiredArgsConstructor;

/** REST controller for PRD operations. Implements generated OpenAPI interface. */
@RestController
@RequiredArgsConstructor
public class PrdController implements PrdsApi {

  private final PrdApplicationService prdApplicationService;

  @Override
  public ResponseEntity<PrdDto> createPrd(String projectRef, CreatePrdRequestDto request) {
    PrdDto created = prdApplicationService.createPrd(projectRef, request);
    return ResponseEntity.status(HttpStatus.CREATED).body(created);
  }

  @Override
  public ResponseEntity<PrdDto> getPrd(String projectRef, String prdRef) {
    PrdDto prd = prdApplicationService.getPrd(projectRef, prdRef);
    return ResponseEntity.ok(prd);
  }

  @Override
  public ResponseEntity<PrdDto> updatePrd(
      String projectRef, String prdRef, UpdatePrdRequestDto request) {
    PrdDto updated = prdApplicationService.updatePrd(projectRef, prdRef, request);
    return ResponseEntity.ok(updated);
  }

  @Override
  public ResponseEntity<Void> deletePrd(String projectRef, String prdRef) {
    prdApplicationService.deletePrd(projectRef, prdRef);
    return ResponseEntity.noContent().build();
  }

  @Override
  public ResponseEntity<PrdListResponseDto> listPrds(
      String projectRef,
      String cursor,
      Integer limit,
      String sort,
      String order,
      PrdStatusDto status) {
    int effectiveLimit = limit != null ? Math.min(limit, 100) : 20;
    String effectiveSort = sort != null ? sort : "created_at";
    String effectiveOrder = order != null ? order : "desc";

    PrdListResponseDto response =
        prdApplicationService.listPrds(
            projectRef, cursor, effectiveLimit, effectiveSort, effectiveOrder, status);
    return ResponseEntity.ok(response);
  }

  // ==================== DOCUMENTS ====================

  @Override
  public ResponseEntity<PrdDto> addPrdDocument(
      String projectRef, String prdRef, AddPrdDocumentRequestDto request) {
    PrdDto updated = prdApplicationService.addDocument(projectRef, prdRef, request);
    return ResponseEntity.status(HttpStatus.CREATED).body(updated);
  }

  @Override
  public ResponseEntity<PrdDto> updatePrdDocument(
      String projectRef, String prdRef, Long docId, UpdatePrdDocumentRequestDto request) {
    PrdDto updated = prdApplicationService.updateDocument(projectRef, prdRef, docId, request);
    return ResponseEntity.ok(updated);
  }

  @Override
  public ResponseEntity<Void> deletePrdDocument(String projectRef, String prdRef, Long docId) {
    prdApplicationService.deleteDocument(projectRef, prdRef, docId);
    return ResponseEntity.noContent().build();
  }
}
