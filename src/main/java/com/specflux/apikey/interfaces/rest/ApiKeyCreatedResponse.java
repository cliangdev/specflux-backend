package com.specflux.apikey.interfaces.rest;

import java.time.Instant;

import com.specflux.apikey.application.ApiKeyService.ApiKeyCreationResult;

/**
 * Response containing the full API key (shown only once at creation).
 *
 * <p>WARNING: The 'key' field contains the full API key and is only returned at creation time. It
 * should be copied immediately and stored securely.
 */
public record ApiKeyCreatedResponse(
    String id,
    String key, // Full key - only shown at creation
    String name,
    String keyPrefix,
    Instant createdAt,
    Instant expiresAt) {

  /** Creates a response from an ApiKeyCreationResult. */
  public static ApiKeyCreatedResponse from(ApiKeyCreationResult result) {
    var apiKey = result.apiKey();
    return new ApiKeyCreatedResponse(
        apiKey.getPublicId(),
        result.fullKey(),
        apiKey.getName(),
        apiKey.getKeyPrefix(),
        apiKey.getCreatedAt(),
        apiKey.getExpiresAt());
  }
}
