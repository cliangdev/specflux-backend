package com.specflux.release.domain;

/** Status values for a Release. */
public enum ReleaseStatus {
  PLANNED("planned"),
  IN_PROGRESS("in_progress"),
  RELEASED("released"),
  CANCELLED("cancelled");

  private final String value;

  ReleaseStatus(String value) {
    this.value = value;
  }

  public String getValue() {
    return value;
  }

  public static ReleaseStatus fromValue(String value) {
    for (ReleaseStatus status : values()) {
      if (status.value.equals(value)) {
        return status;
      }
    }
    throw new IllegalArgumentException("Unknown release status: " + value);
  }
}
