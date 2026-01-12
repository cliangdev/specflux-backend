package com.specflux.epic.domain;

import java.time.Instant;
import java.time.LocalDate;

import com.specflux.project.domain.Project;
import com.specflux.shared.domain.AggregateRoot;
import com.specflux.user.domain.User;

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

/** Epic aggregate root representing a large feature or initiative. */
@Entity
@Table(name = "epics")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Epic extends AggregateRoot<Long> {

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

  @Setter
  @Column(nullable = false, length = 20)
  private EpicStatus status = EpicStatus.PLANNING;

  @Setter
  @Column(name = "target_date")
  private LocalDate targetDate;

  @Setter
  @Column(name = "prd_id")
  private Long prdId;

  @Setter
  @Column(name = "prd_file_path", length = 500)
  private String prdFilePath;

  @Setter
  @Column(name = "epic_file_path", length = 500)
  private String epicFilePath;

  @Setter
  @Column(columnDefinition = "TEXT")
  private String notes;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "created_by_id", nullable = false)
  private User createdBy;

  @Column(name = "created_at", nullable = false, updatable = false)
  private Instant createdAt;

  @Column(name = "updated_at", nullable = false)
  private Instant updatedAt;

  public Epic(
      String publicId,
      Project project,
      int sequenceNumber,
      String displayKey,
      String title,
      User createdBy) {
    this.publicId = publicId;
    this.project = project;
    this.sequenceNumber = sequenceNumber;
    this.displayKey = displayKey;
    this.title = title;
    this.createdBy = createdBy;
    this.status = EpicStatus.PLANNING;
    this.createdAt = Instant.now();
    this.updatedAt = Instant.now();
  }

  @PreUpdate
  protected void onUpdate() {
    this.updatedAt = Instant.now();
  }
}
