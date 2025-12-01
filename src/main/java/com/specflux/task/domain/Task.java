package com.specflux.task.domain;

import java.time.Instant;

import com.specflux.epic.domain.Epic;
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

/** Task aggregate root representing a unit of work. */
@Entity
@Table(name = "tasks")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Task extends AggregateRoot<Long> {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(name = "public_id", nullable = false, unique = true, length = 24)
  private String publicId;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "project_id", nullable = false)
  private Project project;

  @Setter
  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "epic_id")
  private Epic epic;

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
  private TaskStatus status = TaskStatus.BACKLOG;

  @Setter
  @Column(length = 10)
  private TaskPriority priority = TaskPriority.MEDIUM;

  @Setter
  @Column(name = "requires_approval", nullable = false)
  private Boolean requiresApproval = true;

  @Setter
  @Column(name = "estimated_duration")
  private Integer estimatedDuration;

  @Setter
  @Column(name = "actual_duration")
  private Integer actualDuration;

  @Setter
  @Column(name = "github_pr_url", length = 500)
  private String githubPrUrl;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "created_by_id", nullable = false)
  private User createdBy;

  @Setter
  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "assigned_to_id")
  private User assignedTo;

  @Column(name = "created_at", nullable = false, updatable = false)
  private Instant createdAt;

  @Column(name = "updated_at", nullable = false)
  private Instant updatedAt;

  public Task(
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
    this.status = TaskStatus.BACKLOG;
    this.priority = TaskPriority.MEDIUM;
    this.requiresApproval = true;
    this.createdAt = Instant.now();
    this.updatedAt = Instant.now();
  }

  @PreUpdate
  protected void onUpdate() {
    this.updatedAt = Instant.now();
  }
}
