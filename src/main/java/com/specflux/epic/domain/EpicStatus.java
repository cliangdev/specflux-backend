package com.specflux.epic.domain;

/** Status values for an Epic. */
public enum EpicStatus {
  PLANNING("planning"),
  IN_PROGRESS("in_progress"),
  BLOCKED("blocked"),
  COMPLETED("completed"),
  CANCELLED("cancelled");

  private final String value;

  EpicStatus(String value) {
    this.value = value;
  }

  public String getValue() {
    return value;
  }

  public static EpicStatus fromValue(String value) {
    for (EpicStatus status : values()) {
      if (status.value.equals(value)) {
        return status;
      }
    }
    throw new IllegalArgumentException("Unknown epic status: " + value);
  }
}
