package com.specflux.prd.domain;

/** Status values for a PRD. */
public enum PrdStatus {
  DRAFT("draft"),
  IN_REVIEW("in_review"),
  APPROVED("approved"),
  ARCHIVED("archived");

  private final String value;

  PrdStatus(String value) {
    this.value = value;
  }

  public String getValue() {
    return value;
  }

  public static PrdStatus fromValue(String value) {
    for (PrdStatus status : values()) {
      if (status.value.equals(value)) {
        return status;
      }
    }
    throw new IllegalArgumentException("Unknown PRD status: " + value);
  }
}
