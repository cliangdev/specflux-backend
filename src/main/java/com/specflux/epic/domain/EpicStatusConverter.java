package com.specflux.epic.domain;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

/** JPA converter for EpicStatus enum to store lowercase values in database. */
@Converter(autoApply = true)
public class EpicStatusConverter implements AttributeConverter<EpicStatus, String> {

  @Override
  public String convertToDatabaseColumn(EpicStatus status) {
    return status == null ? null : status.getValue();
  }

  @Override
  public EpicStatus convertToEntityAttribute(String value) {
    return value == null ? null : EpicStatus.fromValue(value);
  }
}
