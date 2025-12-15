package com.specflux.apikey.interfaces.rest;

import java.time.Instant;

import jakarta.validation.constraints.Size;

/** Request to create a new API key. */
public record CreateApiKeyRequest(
    @Size(max = 100, message = "Name must not exceed 100 characters") String name,
    Instant expiresAt) {}
