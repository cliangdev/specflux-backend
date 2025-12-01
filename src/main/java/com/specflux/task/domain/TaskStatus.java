package com.specflux.task.domain;

/** Status values for a Task. */
public enum TaskStatus {
  BACKLOG("backlog"),
  READY("ready"),
  IN_PROGRESS("in_progress"),
  IN_REVIEW("in_review"),
  BLOCKED("blocked"),
  COMPLETED("completed"),
  CANCELLED("cancelled");

  private final String value;

  TaskStatus(String value) {
    this.value = value;
  }

  public String getValue() {
    return value;
  }

  public static TaskStatus fromValue(String value) {
    for (TaskStatus status : values()) {
      if (status.value.equals(value)) {
        return status;
      }
    }
    throw new IllegalArgumentException("Unknown task status: " + value);
  }
}
