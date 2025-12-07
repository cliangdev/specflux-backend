package com.specflux.prd.domain;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

import com.specflux.project.domain.Project;
import com.specflux.shared.domain.AggregateRoot;
import com.specflux.user.domain.User;

import jakarta.persistence.CascadeType;
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
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** PRD aggregate root representing a product requirements document. */
@Entity
@Table(name = "prds")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Prd extends AggregateRoot<Long> {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(name = "public_id", nullable = false, unique = true, length = 24)
  private String publicId;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "project_id", nullable = false)
  private Project project;

  @Column(name = "sequence_number", nullable = false)
  private Integer sequenceNumber;

  @Column(name = "display_key", nullable = false, length = 20)
  private String displayKey;

  @Setter
  @Column(nullable = false)
  private String title;

  @Setter
  @Column(columnDefinition = "TEXT")
  private String description;

  @Column(name = "folder_path", nullable = false, length = 500)
  private String folderPath;

  @Setter
  @Enumerated(EnumType.STRING)
  @Column(nullable = false, length = 20)
  private PrdStatus status = PrdStatus.DRAFT;

  @OneToMany(mappedBy = "prd", cascade = CascadeType.ALL, orphanRemoval = true)
  @OrderBy("orderIndex ASC, id ASC")
  private List<PrdDocument> documents = new ArrayList<>();

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "created_by_id", nullable = false)
  private User createdBy;

  @Column(name = "created_at", nullable = false, updatable = false)
  private Instant createdAt;

  @Column(name = "updated_at", nullable = false)
  private Instant updatedAt;

  public Prd(
      String publicId,
      Project project,
      int sequenceNumber,
      String displayKey,
      String title,
      String folderPath,
      User createdBy) {
    this.publicId = publicId;
    this.project = project;
    this.sequenceNumber = sequenceNumber;
    this.displayKey = displayKey;
    this.title = title;
    this.folderPath = folderPath;
    this.createdBy = createdBy;
    this.status = PrdStatus.DRAFT;
    this.createdAt = Instant.now();
    this.updatedAt = Instant.now();
  }

  @PreUpdate
  protected void onUpdate() {
    this.updatedAt = Instant.now();
  }

  public void addDocument(PrdDocument document) {
    documents.add(document);
  }

  public void removeDocument(PrdDocument document) {
    documents.remove(document);
  }
}
