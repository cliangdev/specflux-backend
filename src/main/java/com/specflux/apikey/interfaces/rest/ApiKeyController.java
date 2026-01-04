package com.specflux.apikey.interfaces.rest;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

import com.specflux.api.generated.ApiKeysApi;
import com.specflux.api.generated.model.ApiKeyCreatedResponseDto;
import com.specflux.api.generated.model.ApiKeyResponseDto;
import com.specflux.api.generated.model.CreateApiKeyRequestDto;
import com.specflux.apikey.application.ApiKeyService;
import com.specflux.apikey.application.ApiKeyService.ApiKeyCreationResult;
import com.specflux.apikey.domain.ApiKey;
import com.specflux.shared.application.CurrentUserService;
import com.specflux.user.domain.User;

import lombok.RequiredArgsConstructor;

/**
 * REST controller for API key management.
 *
 * <p>Endpoints for creating, listing, and revoking API keys for the authenticated user. Implements
 * the generated ApiKeysApi interface from OpenAPI specification.
 */
@RestController
@RequiredArgsConstructor
public class ApiKeyController implements ApiKeysApi {

  private static final Logger log = LoggerFactory.getLogger(ApiKeyController.class);

  private final ApiKeyService apiKeyService;
  private final CurrentUserService currentUserService;

  /**
   * {@inheritDoc}
   *
   * <p>Creates a new API key. The full secret is only returned once at creation time.
   */
  @Override
  public ResponseEntity<ApiKeyCreatedResponseDto> createApiKey(
      CreateApiKeyRequestDto createApiKeyRequestDto) {
    User currentUser = currentUserService.getCurrentUser();

    try {
      String name = createApiKeyRequestDto != null ? createApiKeyRequestDto.getName() : null;
      var expiresAt =
          createApiKeyRequestDto != null && createApiKeyRequestDto.getExpiresAt() != null
              ? createApiKeyRequestDto.getExpiresAt().toInstant()
              : null;

      ApiKeyCreationResult result =
          apiKeyService.createApiKey(currentUser.getId(), name, expiresAt);

      log.info(
          "Created API key {} for user {}",
          result.apiKey().getPublicId(),
          currentUser.getPublicId());

      return ResponseEntity.status(HttpStatus.CREATED).body(toCreatedDto(result));
    } catch (IllegalStateException e) {
      // User already has an active key - return 409 Conflict
      log.warn(
          "Failed to create API key for user {}: {}", currentUser.getPublicId(), e.getMessage());
      // The interface expects ApiKeyCreatedResponseDto, but we need to return 409
      // Using ResponseEntity.status() to override
      return ResponseEntity.status(HttpStatus.CONFLICT).build();
    }
  }

  /**
   * {@inheritDoc}
   *
   * <p>Returns all API keys for the authenticated user. Secret keys are masked.
   */
  @Override
  public ResponseEntity<List<ApiKeyResponseDto>> listApiKeys() {
    User currentUser = currentUserService.getCurrentUser();

    List<ApiKeyResponseDto> keys =
        apiKeyService.listKeys(currentUser.getId()).stream().map(this::toResponseDto).toList();

    return ResponseEntity.ok(keys);
  }

  /**
   * {@inheritDoc}
   *
   * <p>Permanently revokes the specified API key.
   */
  @Override
  public ResponseEntity<Void> revokeApiKey(String keyId) {
    User currentUser = currentUserService.getCurrentUser();

    try {
      apiKeyService.revokeKey(keyId, currentUser.getId());
      log.info("Revoked API key {} for user {}", keyId, currentUser.getPublicId());
      return ResponseEntity.noContent().build();
    } catch (IllegalArgumentException e) {
      // Key not found
      return ResponseEntity.notFound().build();
    } catch (IllegalStateException e) {
      // Key doesn't belong to user
      return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
    }
  }

  /** Converts an ApiKey entity to the generated response DTO. */
  private ApiKeyResponseDto toResponseDto(ApiKey apiKey) {
    ApiKeyResponseDto dto = new ApiKeyResponseDto();
    dto.setId(apiKey.getPublicId());
    dto.setName(apiKey.getName());
    dto.setKeyPrefix(apiKey.getMaskedPrefix());
    dto.setCreatedAt(OffsetDateTime.ofInstant(apiKey.getCreatedAt(), ZoneOffset.UTC));
    if (apiKey.getExpiresAt() != null) {
      dto.setExpiresAt(OffsetDateTime.ofInstant(apiKey.getExpiresAt(), ZoneOffset.UTC));
    }
    if (apiKey.getLastUsedAt() != null) {
      dto.setLastUsedAt(OffsetDateTime.ofInstant(apiKey.getLastUsedAt(), ZoneOffset.UTC));
    }
    dto.setIsExpired(apiKey.isExpired());
    dto.setIsRevoked(apiKey.isRevoked());
    return dto;
  }

  /** Converts an ApiKeyCreationResult to the generated created response DTO. */
  private ApiKeyCreatedResponseDto toCreatedDto(ApiKeyCreationResult result) {
    ApiKey apiKey = result.apiKey();
    ApiKeyCreatedResponseDto dto = new ApiKeyCreatedResponseDto();
    dto.setId(apiKey.getPublicId());
    dto.setKey(result.fullKey());
    dto.setName(apiKey.getName());
    dto.setKeyPrefix(apiKey.getKeyPrefix());
    dto.setCreatedAt(OffsetDateTime.ofInstant(apiKey.getCreatedAt(), ZoneOffset.UTC));
    if (apiKey.getExpiresAt() != null) {
      dto.setExpiresAt(OffsetDateTime.ofInstant(apiKey.getExpiresAt(), ZoneOffset.UTC));
    }
    return dto;
  }
}
