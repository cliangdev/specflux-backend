package com.specflux.release.domain;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

/** JPA converter for ReleaseStatus enum to store lowercase values in database. */
@Converter(autoApply = true)
public class ReleaseStatusConverter implements AttributeConverter<ReleaseStatus, String> {

  @Override
  public String convertToDatabaseColumn(ReleaseStatus status) {
    return status == null ? null : status.getValue();
  }

  @Override
  public ReleaseStatus convertToEntityAttribute(String value) {
    return value == null ? null : ReleaseStatus.fromValue(value);
  }
}
