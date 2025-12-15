package com.specflux.shared.domain;

import java.security.SecureRandom;
import java.util.Objects;
import java.util.regex.Pattern;

/**
 * Value object representing a public identifier for entities.
 *
 * <p>Public IDs are used as external-facing identifiers instead of database IDs. Format:
 * {entity_prefix}_{random_suffix}
 *
 * <p>Examples: user_a1b2c3d4e5f6, proj_x7y8z9, epic_abc123
 */
public final class PublicId extends ValueObject {

  private static final int SUFFIX_LENGTH = 16;
  private static final int MAX_LENGTH = 24;
  private static final String ALPHABET = "abcdefghijklmnopqrstuvwxyz0123456789";
  private static final Pattern VALID_PATTERN =
      Pattern.compile("^(user|proj|epic|task|rel|key)_[a-z0-9]+$");
  private static final SecureRandom RANDOM = new SecureRandom();

  private final String value;

  private PublicId(String value) {
    this.value = value;
  }

  /**
   * Generates a new public ID for the given entity type.
   *
   * @param entityType the type of entity
   * @return a new unique public ID
   */
  public static PublicId generate(EntityType entityType) {
    Objects.requireNonNull(entityType, "entityType must not be null");

    String prefix = entityType.getPrefix();
    int suffixLength = Math.min(SUFFIX_LENGTH, MAX_LENGTH - prefix.length() - 1);

    StringBuilder suffix = new StringBuilder(suffixLength);
    for (int i = 0; i < suffixLength; i++) {
      suffix.append(ALPHABET.charAt(RANDOM.nextInt(ALPHABET.length())));
    }

    return new PublicId(prefix + "_" + suffix);
  }

  /**
   * Creates a PublicId from an existing string value.
   *
   * @param value the public ID string
   * @return the PublicId instance
   * @throws IllegalArgumentException if the value is invalid
   */
  public static PublicId of(String value) {
    Objects.requireNonNull(value, "value must not be null");

    if (value.length() > MAX_LENGTH) {
      throw new IllegalArgumentException(
          "Public ID must not exceed " + MAX_LENGTH + " characters: " + value);
    }

    if (!VALID_PATTERN.matcher(value).matches()) {
      throw new IllegalArgumentException("Invalid public ID format: " + value);
    }

    return new PublicId(value);
  }

  /**
   * Returns the string value of this public ID.
   *
   * @return the public ID string
   */
  public String getValue() {
    return value;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;
    PublicId publicId = (PublicId) o;
    return Objects.equals(value, publicId.value);
  }

  @Override
  public int hashCode() {
    return Objects.hash(value);
  }

  @Override
  public String toString() {
    return value;
  }
}
