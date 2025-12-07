package com.specflux.prd.interfaces.rest;

import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;

import org.springframework.stereotype.Service;

import com.specflux.api.generated.model.PrdDocumentDto;
import com.specflux.api.generated.model.PrdDocumentTypeDto;
import com.specflux.api.generated.model.PrdDto;
import com.specflux.api.generated.model.PrdStatusDto;
import com.specflux.prd.domain.Prd;
import com.specflux.prd.domain.PrdDocument;
import com.specflux.prd.domain.PrdDocumentType;
import com.specflux.prd.domain.PrdStatus;

import lombok.RequiredArgsConstructor;

/** Mapper for converting between PRD domain entities and API DTOs. */
@Service
@RequiredArgsConstructor
public class PrdMapper {

  /**
   * Converts a domain Prd entity to an API Prd DTO with all fields.
   *
   * @param domain the domain entity
   * @return the API DTO
   */
  public PrdDto toDto(Prd domain) {
    PrdDto dto = new PrdDto();
    dto.setId(domain.getPublicId());
    dto.setDisplayKey(domain.getDisplayKey());
    dto.setProjectId(domain.getProject().getPublicId());
    dto.setTitle(domain.getTitle());
    dto.setDescription(domain.getDescription());
    dto.setFolderPath(domain.getFolderPath());
    dto.setStatus(toApiStatus(domain.getStatus()));
    dto.setCreatedById(domain.getCreatedBy().getPublicId());
    dto.setCreatedAt(toOffsetDateTime(domain.getCreatedAt()));
    dto.setUpdatedAt(toOffsetDateTime(domain.getUpdatedAt()));

    // Include documents
    List<PrdDocumentDto> documents =
        domain.getDocuments().stream().map(this::toDocumentDto).toList();
    dto.setDocuments(documents);
    dto.setDocumentCount(documents.size());

    return dto;
  }

  /**
   * Converts a domain Prd entity to a simplified DTO for list views.
   *
   * @param domain the domain entity
   * @return the API DTO (with documents but less detailed)
   */
  public PrdDto toDtoSimple(Prd domain) {
    PrdDto dto = new PrdDto();
    dto.setId(domain.getPublicId());
    dto.setDisplayKey(domain.getDisplayKey());
    dto.setProjectId(domain.getProject().getPublicId());
    dto.setTitle(domain.getTitle());
    dto.setDescription(domain.getDescription());
    dto.setFolderPath(domain.getFolderPath());
    dto.setStatus(toApiStatus(domain.getStatus()));
    dto.setCreatedById(domain.getCreatedBy().getPublicId());
    dto.setCreatedAt(toOffsetDateTime(domain.getCreatedAt()));
    dto.setUpdatedAt(toOffsetDateTime(domain.getUpdatedAt()));

    // Include document count but not full documents for list view
    dto.setDocumentCount(domain.getDocuments().size());

    return dto;
  }

  /**
   * Converts a domain PrdDocument entity to an API DTO.
   *
   * @param domain the domain entity
   * @return the API DTO
   */
  public PrdDocumentDto toDocumentDto(PrdDocument domain) {
    PrdDocumentDto dto = new PrdDocumentDto();
    dto.setId(domain.getId());
    dto.setFileName(domain.getFileName());
    dto.setFilePath(domain.getFilePath());
    dto.setDocumentType(toApiDocumentType(domain.getDocumentType()));
    dto.setIsPrimary(domain.isPrimary());
    dto.setOrderIndex(domain.getOrderIndex());
    dto.setCreatedAt(toOffsetDateTime(domain.getCreatedAt()));
    dto.setUpdatedAt(toOffsetDateTime(domain.getUpdatedAt()));
    return dto;
  }

  /**
   * Converts an API PrdStatusDto to a domain PrdStatus.
   *
   * @param apiStatus the API status
   * @return the domain status
   */
  public PrdStatus toDomainStatus(PrdStatusDto apiStatus) {
    if (apiStatus == null) {
      return null;
    }
    return switch (apiStatus) {
      case DRAFT -> PrdStatus.DRAFT;
      case IN_REVIEW -> PrdStatus.IN_REVIEW;
      case APPROVED -> PrdStatus.APPROVED;
      case ARCHIVED -> PrdStatus.ARCHIVED;
    };
  }

  /**
   * Converts a domain PrdStatus to an API PrdStatusDto.
   *
   * @param domainStatus the domain status
   * @return the API status
   */
  public PrdStatusDto toApiStatus(PrdStatus domainStatus) {
    return switch (domainStatus) {
      case DRAFT -> PrdStatusDto.DRAFT;
      case IN_REVIEW -> PrdStatusDto.IN_REVIEW;
      case APPROVED -> PrdStatusDto.APPROVED;
      case ARCHIVED -> PrdStatusDto.ARCHIVED;
    };
  }

  /**
   * Converts an API PrdDocumentTypeDto to a domain PrdDocumentType.
   *
   * @param apiType the API type
   * @return the domain type
   */
  public PrdDocumentType toDomainDocumentType(PrdDocumentTypeDto apiType) {
    if (apiType == null) {
      return PrdDocumentType.OTHER;
    }
    return switch (apiType) {
      case PRD -> PrdDocumentType.PRD;
      case WIREFRAME -> PrdDocumentType.WIREFRAME;
      case MOCKUP -> PrdDocumentType.MOCKUP;
      case DESIGN -> PrdDocumentType.DESIGN;
      case OTHER -> PrdDocumentType.OTHER;
    };
  }

  /**
   * Converts a domain PrdDocumentType to an API PrdDocumentTypeDto.
   *
   * @param domainType the domain type
   * @return the API type
   */
  public PrdDocumentTypeDto toApiDocumentType(PrdDocumentType domainType) {
    return switch (domainType) {
      case PRD -> PrdDocumentTypeDto.PRD;
      case WIREFRAME -> PrdDocumentTypeDto.WIREFRAME;
      case MOCKUP -> PrdDocumentTypeDto.MOCKUP;
      case DESIGN -> PrdDocumentTypeDto.DESIGN;
      case OTHER -> PrdDocumentTypeDto.OTHER;
    };
  }

  private OffsetDateTime toOffsetDateTime(Instant instant) {
    return instant != null ? instant.atOffset(ZoneOffset.UTC) : null;
  }
}
