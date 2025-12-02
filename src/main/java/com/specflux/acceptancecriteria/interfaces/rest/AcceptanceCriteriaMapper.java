package com.specflux.acceptancecriteria.interfaces.rest;

import java.time.ZoneOffset;

import com.specflux.acceptancecriteria.domain.AcceptanceCriteria;
import com.specflux.api.generated.model.AcceptanceCriteriaDto;

import lombok.experimental.UtilityClass;

/** Utility class for mapping AcceptanceCriteria domain objects to DTOs. */
@UtilityClass
public class AcceptanceCriteriaMapper {

  /**
   * Converts an AcceptanceCriteria entity to a DTO.
   *
   * @param entity the acceptance criteria entity
   * @return the DTO
   */
  public AcceptanceCriteriaDto toDto(AcceptanceCriteria entity) {
    AcceptanceCriteriaDto dto = new AcceptanceCriteriaDto();
    dto.setId(entity.getId());
    dto.setCriteria(entity.getCriteria());
    dto.setIsMet(entity.getIsMet());
    dto.setOrderIndex(entity.getOrderIndex());
    dto.setCreatedAt(entity.getCreatedAt().atOffset(ZoneOffset.UTC));
    return dto;
  }
}
