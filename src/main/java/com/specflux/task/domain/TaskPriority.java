package com.specflux.task.domain;

/** Priority values for a Task. */
public enum TaskPriority {
  LOW("low"),
  MEDIUM("medium"),
  HIGH("high"),
  CRITICAL("critical");

  private final String value;

  TaskPriority(String value) {
    this.value = value;
  }

  public String getValue() {
    return value;
  }

  public static TaskPriority fromValue(String value) {
    for (TaskPriority priority : values()) {
      if (priority.value.equals(value)) {
        return priority;
      }
    }
    throw new IllegalArgumentException("Unknown task priority: " + value);
  }
}
