package com.specflux.apikey.interfaces.rest;

import java.time.Instant;

import com.specflux.apikey.domain.ApiKey;

/** Response containing API key metadata (no secret). */
public record ApiKeyResponse(
    String id,
    String name,
    String keyPrefix,
    Instant createdAt,
    Instant expiresAt,
    Instant lastUsedAt,
    boolean isExpired,
    boolean isRevoked) {

  /** Creates a response from an ApiKey entity. */
  public static ApiKeyResponse from(ApiKey apiKey) {
    return new ApiKeyResponse(
        apiKey.getPublicId(),
        apiKey.getName(),
        apiKey.getMaskedPrefix(),
        apiKey.getCreatedAt(),
        apiKey.getExpiresAt(),
        apiKey.getLastUsedAt(),
        apiKey.isExpired(),
        apiKey.isRevoked());
  }
}
