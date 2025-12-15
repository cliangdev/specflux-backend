package com.specflux.apikey.domain;

import java.time.Instant;

import com.specflux.shared.domain.AggregateRoot;
import com.specflux.user.domain.User;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * API Key entity for secure programmatic access.
 *
 * <p>Keys are stored as SHA-256 hashes. The full key is only available at creation time and is
 * never stored in plaintext.
 */
@Entity
@Table(name = "api_keys")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ApiKey extends AggregateRoot<Long> {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(name = "public_id", nullable = false, unique = true, length = 24)
  private String publicId;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "user_id", nullable = false)
  private User user;

  @Column(name = "key_prefix", nullable = false, unique = true, length = 16)
  private String keyPrefix;

  @Column(name = "key_hash", nullable = false, length = 64)
  private String keyHash;

  @Column(length = 100)
  private String name;

  @Column(name = "created_at", nullable = false, updatable = false)
  private Instant createdAt;

  @Column(name = "expires_at")
  private Instant expiresAt;

  @Column(name = "last_used_at")
  private Instant lastUsedAt;

  @Column(name = "revoked_at")
  private Instant revokedAt;

  /**
   * Creates a new API key.
   *
   * @param publicId the public identifier (key_xxx format)
   * @param user the user who owns this key
   * @param keyPrefix the first 12 chars after sfx_ for lookup
   * @param keyHash the SHA-256 hash of the full key
   * @param name optional display name
   * @param expiresAt optional expiration timestamp
   */
  public ApiKey(
      String publicId,
      User user,
      String keyPrefix,
      String keyHash,
      String name,
      Instant expiresAt) {
    this.publicId = publicId;
    this.user = user;
    this.keyPrefix = keyPrefix;
    this.keyHash = keyHash;
    this.name = name;
    this.expiresAt = expiresAt;
    this.createdAt = Instant.now();
  }

  /** Updates the last used timestamp. Called on each successful authentication. */
  public void recordUsage() {
    this.lastUsedAt = Instant.now();
  }

  /** Revokes this API key. The key will no longer be valid for authentication. */
  public void revoke() {
    if (this.revokedAt == null) {
      this.revokedAt = Instant.now();
    }
  }

  /**
   * Checks if this key is currently valid (not expired and not revoked).
   *
   * @return true if the key can be used for authentication
   */
  public boolean isValid() {
    if (revokedAt != null) {
      return false;
    }
    if (expiresAt != null && Instant.now().isAfter(expiresAt)) {
      return false;
    }
    return true;
  }

  /**
   * Checks if this key has been revoked.
   *
   * @return true if the key has been revoked
   */
  public boolean isRevoked() {
    return revokedAt != null;
  }

  /**
   * Checks if this key has expired.
   *
   * @return true if the key has expired
   */
  public boolean isExpired() {
    return expiresAt != null && Instant.now().isAfter(expiresAt);
  }

  /**
   * Returns a masked version of the key prefix for display.
   *
   * @return masked prefix like "sfx_a1B2****"
   */
  public String getMaskedPrefix() {
    if (keyPrefix == null || keyPrefix.length() < 8) {
      return keyPrefix;
    }
    return keyPrefix.substring(0, 8) + "****";
  }
}
