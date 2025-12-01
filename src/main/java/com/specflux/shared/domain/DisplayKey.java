package com.specflux.shared.domain;

import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Value object representing a display key for epics, tasks, and releases.
 *
 * <p>Display keys are human-readable identifiers in the format: {PROJECT_KEY}-{sequence_number}
 *
 * <p>Examples: SPEC-1, SPEC-42, PROJ-123
 */
public final class DisplayKey extends ValueObject {

  private static final Pattern VALID_PATTERN = Pattern.compile("^([A-Z0-9]+)-(\\d+)$");
  private static final int MAX_PROJECT_KEY_LENGTH = 10;

  private final String projectKey;
  private final int sequenceNumber;
  private final String value;

  private DisplayKey(String projectKey, int sequenceNumber) {
    this.projectKey = projectKey;
    this.sequenceNumber = sequenceNumber;
    this.value = projectKey + "-" + sequenceNumber;
  }

  /**
   * Creates a display key from project key and sequence number.
   *
   * @param projectKey the project key (uppercase, max 10 chars)
   * @param sequenceNumber the sequence number (positive integer)
   * @return the DisplayKey instance
   * @throws IllegalArgumentException if arguments are invalid
   */
  public static DisplayKey of(String projectKey, int sequenceNumber) {
    Objects.requireNonNull(projectKey, "projectKey must not be null");

    if (projectKey.isEmpty()) {
      throw new IllegalArgumentException("projectKey must not be empty");
    }

    if (projectKey.length() > MAX_PROJECT_KEY_LENGTH) {
      throw new IllegalArgumentException(
          "projectKey must not exceed " + MAX_PROJECT_KEY_LENGTH + " characters");
    }

    if (!projectKey.matches("^[A-Z0-9]+$")) {
      throw new IllegalArgumentException(
          "projectKey must contain only uppercase letters and numbers: " + projectKey);
    }

    if (sequenceNumber < 1) {
      throw new IllegalArgumentException("sequenceNumber must be positive: " + sequenceNumber);
    }

    return new DisplayKey(projectKey, sequenceNumber);
  }

  /**
   * Parses a display key string into a DisplayKey object.
   *
   * @param value the display key string (e.g., "SPEC-42")
   * @return the DisplayKey instance
   * @throws IllegalArgumentException if the format is invalid
   */
  public static DisplayKey parse(String value) {
    Objects.requireNonNull(value, "value must not be null");

    Matcher matcher = VALID_PATTERN.matcher(value);
    if (!matcher.matches()) {
      throw new IllegalArgumentException("Invalid display key format: " + value);
    }

    String projectKey = matcher.group(1);
    int sequenceNumber = Integer.parseInt(matcher.group(2));

    return of(projectKey, sequenceNumber);
  }

  /**
   * Returns the project key portion.
   *
   * @return the project key
   */
  public String getProjectKey() {
    return projectKey;
  }

  /**
   * Returns the sequence number portion.
   *
   * @return the sequence number
   */
  public int getSequenceNumber() {
    return sequenceNumber;
  }

  /**
   * Returns the full display key string.
   *
   * @return the display key (e.g., "SPEC-42")
   */
  public String getValue() {
    return value;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;
    DisplayKey that = (DisplayKey) o;
    return sequenceNumber == that.sequenceNumber && Objects.equals(projectKey, that.projectKey);
  }

  @Override
  public int hashCode() {
    return Objects.hash(projectKey, sequenceNumber);
  }

  @Override
  public String toString() {
    return value;
  }
}
