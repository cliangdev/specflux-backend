package com.specflux.task.domain;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

/** JPA converter for TaskPriority enum to store lowercase values in database. */
@Converter(autoApply = true)
public class TaskPriorityConverter implements AttributeConverter<TaskPriority, String> {

  @Override
  public String convertToDatabaseColumn(TaskPriority priority) {
    return priority == null ? null : priority.getValue();
  }

  @Override
  public TaskPriority convertToEntityAttribute(String value) {
    return value == null ? null : TaskPriority.fromValue(value);
  }
}
