package com.specflux.apikey.application;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Instant;
import java.util.Base64;
import java.util.HexFormat;
import java.util.List;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.specflux.apikey.domain.ApiKey;
import com.specflux.apikey.domain.ApiKeyRepository;
import com.specflux.shared.domain.EntityType;
import com.specflux.shared.domain.PublicId;
import com.specflux.user.domain.User;
import com.specflux.user.domain.UserRepository;

/**
 * Application service for API key management.
 *
 * <p>Handles key generation, validation, and lifecycle operations.
 */
@Service
@Transactional
public class ApiKeyService {

  private static final Logger log = LoggerFactory.getLogger(ApiKeyService.class);
  private static final String KEY_PREFIX = "sfx_";
  private static final int KEY_BYTES = 32; // 256 bits
  private static final int PREFIX_LENGTH = 12; // Characters to extract for prefix lookup

  private final ApiKeyRepository apiKeyRepository;
  private final UserRepository userRepository;
  private final SecureRandom secureRandom;

  public ApiKeyService(ApiKeyRepository apiKeyRepository, UserRepository userRepository) {
    this.apiKeyRepository = apiKeyRepository;
    this.userRepository = userRepository;
    this.secureRandom = new SecureRandom();
  }

  /**
   * Creates a new API key for a user.
   *
   * @param userId the user's internal ID
   * @param name optional display name for the key
   * @param expiresAt optional expiration timestamp
   * @return result containing the full key (only available at creation)
   * @throws IllegalStateException if user already has an active key
   */
  public ApiKeyCreationResult createApiKey(Long userId, String name, Instant expiresAt) {
    User user =
        userRepository
            .findById(userId)
            .orElseThrow(() -> new IllegalArgumentException("User not found: " + userId));

    // Check for existing active key (MVP: one key per user)
    if (apiKeyRepository.existsActiveByUserId(userId)) {
      throw new IllegalStateException("User already has an active API key. Revoke it first.");
    }

    // Generate secure random key
    String fullKey = generateKey();
    String keyPrefix = extractPrefix(fullKey);
    String keyHash = hashKey(fullKey);

    // Ensure prefix is unique (extremely unlikely to collide, but check anyway)
    while (apiKeyRepository.existsByKeyPrefix(keyPrefix)) {
      log.warn("API key prefix collision detected, regenerating...");
      fullKey = generateKey();
      keyPrefix = extractPrefix(fullKey);
      keyHash = hashKey(fullKey);
    }

    String publicId = PublicId.generate(EntityType.API_KEY).getValue();

    ApiKey apiKey = new ApiKey(publicId, user, keyPrefix, keyHash, name, expiresAt);

    apiKey = apiKeyRepository.save(apiKey);

    log.info("Created API key {} for user {}", publicId, user.getPublicId());

    return new ApiKeyCreationResult(apiKey, fullKey);
  }

  /**
   * Validates an API key and returns the associated user if valid.
   *
   * @param fullKey the full API key (sfx_...)
   * @return the user if key is valid, empty if invalid
   */
  @Transactional
  public Optional<User> validateKey(String fullKey) {
    if (fullKey == null || !fullKey.startsWith(KEY_PREFIX)) {
      return Optional.empty();
    }

    String keyPrefix = extractPrefix(fullKey);
    Optional<ApiKey> apiKeyOpt = apiKeyRepository.findByKeyPrefix(keyPrefix);

    if (apiKeyOpt.isEmpty()) {
      log.debug("API key not found for prefix: {}", keyPrefix);
      return Optional.empty();
    }

    ApiKey apiKey = apiKeyOpt.get();

    // Verify hash matches
    String providedHash = hashKey(fullKey);
    if (!apiKey.getKeyHash().equals(providedHash)) {
      log.warn("API key hash mismatch for prefix: {}", keyPrefix);
      return Optional.empty();
    }

    // Check if key is still valid
    if (!apiKey.isValid()) {
      if (apiKey.isRevoked()) {
        log.debug("API key {} has been revoked", apiKey.getPublicId());
      } else if (apiKey.isExpired()) {
        log.debug("API key {} has expired", apiKey.getPublicId());
      }
      return Optional.empty();
    }

    // Update last used timestamp
    apiKey.recordUsage();
    apiKeyRepository.save(apiKey);

    return Optional.of(apiKey.getUser());
  }

  /**
   * Revokes an API key.
   *
   * @param publicId the key's public ID
   * @param userId the user's internal ID (for authorization)
   * @throws IllegalArgumentException if key not found
   * @throws IllegalStateException if key doesn't belong to user
   */
  public void revokeKey(String publicId, Long userId) {
    ApiKey apiKey =
        apiKeyRepository
            .findByPublicId(publicId)
            .orElseThrow(() -> new IllegalArgumentException("API key not found: " + publicId));

    if (!apiKey.getUser().getId().equals(userId)) {
      throw new IllegalStateException("API key does not belong to this user");
    }

    apiKey.revoke();
    apiKeyRepository.save(apiKey);

    log.info("Revoked API key {}", publicId);
  }

  /**
   * Lists all API keys for a user.
   *
   * @param userId the user's internal ID
   * @return list of API keys (without secrets)
   */
  @Transactional(readOnly = true)
  public List<ApiKey> listKeys(Long userId) {
    return apiKeyRepository.findByUserId(userId);
  }

  /**
   * Gets the active API key for a user.
   *
   * @param userId the user's internal ID
   * @return the active key if exists
   */
  @Transactional(readOnly = true)
  public Optional<ApiKey> getActiveKey(Long userId) {
    return apiKeyRepository.findActiveByUserId(userId);
  }

  /**
   * Generates a secure random API key.
   *
   * @return the full key in format sfx_<base64url>
   */
  private String generateKey() {
    byte[] randomBytes = new byte[KEY_BYTES];
    secureRandom.nextBytes(randomBytes);
    String encoded = Base64.getUrlEncoder().withoutPadding().encodeToString(randomBytes);
    return KEY_PREFIX + encoded;
  }

  /**
   * Extracts the prefix from a full key for lookup.
   *
   * @param fullKey the full API key
   * @return the prefix (first 12 chars after sfx_)
   */
  String extractPrefix(String fullKey) {
    if (fullKey == null || !fullKey.startsWith(KEY_PREFIX)) {
      throw new IllegalArgumentException("Invalid key format");
    }
    String keyPart = fullKey.substring(KEY_PREFIX.length());
    return KEY_PREFIX + keyPart.substring(0, Math.min(PREFIX_LENGTH, keyPart.length()));
  }

  /**
   * Computes SHA-256 hash of a key.
   *
   * @param key the key to hash
   * @return hex-encoded SHA-256 hash
   */
  String hashKey(String key) {
    try {
      MessageDigest digest = MessageDigest.getInstance("SHA-256");
      byte[] hashBytes = digest.digest(key.getBytes(StandardCharsets.UTF_8));
      return HexFormat.of().formatHex(hashBytes);
    } catch (NoSuchAlgorithmException e) {
      throw new RuntimeException("SHA-256 not available", e);
    }
  }

  /**
   * Checks if a token is an API key (starts with sfx_).
   *
   * @param token the token to check
   * @return true if this is an API key
   */
  public static boolean isApiKey(String token) {
    return token != null && token.startsWith(KEY_PREFIX);
  }

  /** Result of API key creation, containing the key entity and the full key (shown once). */
  public record ApiKeyCreationResult(ApiKey apiKey, String fullKey) {}
}
