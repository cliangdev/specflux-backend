package com.specflux.release.domain;

import java.time.Instant;
import java.time.LocalDate;

import com.specflux.project.domain.Project;
import com.specflux.shared.domain.AggregateRoot;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
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

/** Release aggregate root representing a milestone or version in a project. */
@Entity
@Table(name = "releases")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Release extends AggregateRoot<Long> {

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
  private String name;

  @Setter
  @Column(columnDefinition = "TEXT")
  private String description;

  @Setter
  @Column(name = "target_date")
  private LocalDate targetDate;

  @Setter
  @Column(nullable = false, length = 20)
  private ReleaseStatus status = ReleaseStatus.PLANNED;

  @Column(name = "created_at", nullable = false, updatable = false)
  private Instant createdAt;

  @Column(name = "updated_at", nullable = false)
  private Instant updatedAt;

  public Release(
      String publicId, Project project, int sequenceNumber, String displayKey, String name) {
    this.publicId = publicId;
    this.project = project;
    this.sequenceNumber = sequenceNumber;
    this.displayKey = displayKey;
    this.name = name;
    this.status = ReleaseStatus.PLANNED;
    this.createdAt = Instant.now();
    this.updatedAt = Instant.now();
  }

  @PreUpdate
  protected void onUpdate() {
    this.updatedAt = Instant.now();
  }
}
