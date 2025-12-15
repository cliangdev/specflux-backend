package com.specflux.apikey.domain;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

/** Repository for ApiKey aggregate root. */
public interface ApiKeyRepository extends JpaRepository<ApiKey, Long> {

  /**
   * Finds an API key by its prefix (used for authentication lookup).
   *
   * @param keyPrefix the key prefix (first 12 chars after sfx_)
   * @return the API key if found
   */
  Optional<ApiKey> findByKeyPrefix(String keyPrefix);

  /**
   * Finds an API key by its prefix with the user eagerly fetched.
   *
   * <p>Use this method for authentication to avoid LazyInitializationException when accessing the
   * user outside a transaction.
   *
   * @param keyPrefix the key prefix (first 12 chars after sfx_)
   * @return the API key with user if found
   */
  @Query("SELECT k FROM ApiKey k JOIN FETCH k.user WHERE k.keyPrefix = :keyPrefix")
  Optional<ApiKey> findByKeyPrefixWithUser(@Param("keyPrefix") String keyPrefix);

  /**
   * Finds an API key by its public ID.
   *
   * @param publicId the public identifier (key_xxx format)
   * @return the API key if found
   */
  Optional<ApiKey> findByPublicId(String publicId);

  /**
   * Finds all API keys for a user (should be at most 1 for MVP).
   *
   * @param userId the user's internal ID
   * @return list of API keys
   */
  List<ApiKey> findByUserId(Long userId);

  /**
   * Finds the active (non-revoked) API key for a user.
   *
   * @param userId the user's internal ID
   * @return the active API key if exists
   */
  @Query("SELECT k FROM ApiKey k WHERE k.user.id = :userId AND k.revokedAt IS NULL")
  Optional<ApiKey> findActiveByUserId(@Param("userId") Long userId);

  /**
   * Checks if a user already has an active API key.
   *
   * @param userId the user's internal ID
   * @return true if user has an active (non-revoked) key
   */
  @Query("SELECT COUNT(k) > 0 FROM ApiKey k WHERE k.user.id = :userId AND k.revokedAt IS NULL")
  boolean existsActiveByUserId(@Param("userId") Long userId);

  /**
   * Checks if a key prefix already exists.
   *
   * @param keyPrefix the key prefix to check
   * @return true if prefix exists
   */
  boolean existsByKeyPrefix(String keyPrefix);
}
