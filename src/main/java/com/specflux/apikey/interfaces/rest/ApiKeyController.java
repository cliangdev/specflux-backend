package com.specflux.apikey.interfaces.rest;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.specflux.apikey.application.ApiKeyService;
import com.specflux.apikey.application.ApiKeyService.ApiKeyCreationResult;
import com.specflux.shared.application.CurrentUserService;
import com.specflux.user.domain.User;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

/**
 * REST controller for API key management.
 *
 * <p>Endpoints for creating, listing, and revoking API keys for the authenticated user.
 */
@RestController
@RequestMapping("/api/users/me/api-keys")
@RequiredArgsConstructor
public class ApiKeyController {

  private static final Logger log = LoggerFactory.getLogger(ApiKeyController.class);

  private final ApiKeyService apiKeyService;
  private final CurrentUserService currentUserService;

  /**
   * Creates a new API key for the authenticated user.
   *
   * <p>Returns the full key in the response. This is the ONLY time the full key is shown - it
   * cannot be retrieved again.
   *
   * @param request the creation request with optional name and expiration
   * @return the created API key with full key value
   */
  @PostMapping
  public ResponseEntity<?> createApiKey(
      @Valid @RequestBody(required = false) CreateApiKeyRequest request) {
    User currentUser = currentUserService.getCurrentUser();

    try {
      String name = request != null ? request.name() : null;
      var expiresAt = request != null ? request.expiresAt() : null;

      ApiKeyCreationResult result =
          apiKeyService.createApiKey(currentUser.getId(), name, expiresAt);

      log.info(
          "Created API key {} for user {}",
          result.apiKey().getPublicId(),
          currentUser.getPublicId());

      return ResponseEntity.status(HttpStatus.CREATED).body(ApiKeyCreatedResponse.from(result));
    } catch (IllegalStateException e) {
      // User already has an active key
      log.warn(
          "Failed to create API key for user {}: {}", currentUser.getPublicId(), e.getMessage());
      return ResponseEntity.status(HttpStatus.CONFLICT)
          .body(new ErrorResponse("conflict", e.getMessage()));
    }
  }

  /**
   * Lists all API keys for the authenticated user.
   *
   * <p>Returns metadata only - the full key is never returned after creation.
   *
   * @return list of API keys (without secrets)
   */
  @GetMapping
  public ResponseEntity<List<ApiKeyResponse>> listApiKeys() {
    User currentUser = currentUserService.getCurrentUser();

    List<ApiKeyResponse> keys =
        apiKeyService.listKeys(currentUser.getId()).stream().map(ApiKeyResponse::from).toList();

    return ResponseEntity.ok(keys);
  }

  /**
   * Revokes an API key.
   *
   * <p>The key is immediately invalidated and cannot be used for authentication.
   *
   * @param id the API key's public ID
   * @return 204 No Content on success
   */
  @DeleteMapping("/{id}")
  public ResponseEntity<?> revokeApiKey(@PathVariable String id) {
    User currentUser = currentUserService.getCurrentUser();

    try {
      apiKeyService.revokeKey(id, currentUser.getId());
      log.info("Revoked API key {} for user {}", id, currentUser.getPublicId());
      return ResponseEntity.noContent().build();
    } catch (IllegalArgumentException e) {
      // Key not found
      return ResponseEntity.status(HttpStatus.NOT_FOUND)
          .body(new ErrorResponse("not_found", e.getMessage()));
    } catch (IllegalStateException e) {
      // Key doesn't belong to user
      return ResponseEntity.status(HttpStatus.FORBIDDEN)
          .body(new ErrorResponse("forbidden", e.getMessage()));
    }
  }

  /** Simple error response record. */
  private record ErrorResponse(String error, String message) {}
}
