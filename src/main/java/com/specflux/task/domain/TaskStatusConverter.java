package com.specflux.task.domain;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

/** JPA converter for TaskStatus enum to store lowercase values in database. */
@Converter(autoApply = true)
public class TaskStatusConverter implements AttributeConverter<TaskStatus, String> {

  @Override
  public String convertToDatabaseColumn(TaskStatus status) {
    return status == null ? null : status.getValue();
  }

  @Override
  public TaskStatus convertToEntityAttribute(String value) {
    return value == null ? null : TaskStatus.fromValue(value);
  }
}
