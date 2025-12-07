package com.specflux.prd.domain;

import java.time.Instant;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** Document within a PRD (e.g., prd.md, wireframe.png, mockup.html). */
@Entity
@Table(name = "prd_documents")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class PrdDocument {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "prd_id", nullable = false)
  private Prd prd;

  @Column(name = "file_name", nullable = false)
  private String fileName;

  @Column(name = "file_path", nullable = false, length = 500)
  private String filePath;

  @Setter
  @Enumerated(EnumType.STRING)
  @Column(name = "document_type", nullable = false, length = 20)
  private PrdDocumentType documentType = PrdDocumentType.OTHER;

  @Setter
  @Column(name = "is_primary", nullable = false)
  private boolean isPrimary = false;

  @Setter
  @Column(name = "order_index", nullable = false)
  private int orderIndex = 0;

  @Column(name = "created_at", nullable = false, updatable = false)
  private Instant createdAt;

  @Column(name = "updated_at", nullable = false)
  private Instant updatedAt;

  public PrdDocument(Prd prd, String fileName, String filePath) {
    this.prd = prd;
    this.fileName = fileName;
    this.filePath = filePath;
    this.documentType = PrdDocumentType.OTHER;
    this.isPrimary = false;
    this.orderIndex = 0;
    this.createdAt = Instant.now();
    this.updatedAt = Instant.now();
  }

  public PrdDocument(
      Prd prd,
      String fileName,
      String filePath,
      PrdDocumentType documentType,
      boolean isPrimary,
      int orderIndex) {
    this.prd = prd;
    this.fileName = fileName;
    this.filePath = filePath;
    this.documentType = documentType;
    this.isPrimary = isPrimary;
    this.orderIndex = orderIndex;
    this.createdAt = Instant.now();
    this.updatedAt = Instant.now();
  }

  @PreUpdate
  protected void onUpdate() {
    this.updatedAt = Instant.now();
  }
}
