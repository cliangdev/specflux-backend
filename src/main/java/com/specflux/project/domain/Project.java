package com.specflux.project.domain;

import java.time.Instant;

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

/** Project aggregate root representing a software project with epics and tasks. */
@Entity
@Table(name = "projects")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Project extends AggregateRoot<Long> {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(name = "public_id", nullable = false, unique = true, length = 24)
  private String publicId;

  @Column(name = "project_key", nullable = false, unique = true, length = 10)
  private String projectKey;

  @Setter
  @Column(nullable = false)
  private String name;

  @Setter
  @Column(columnDefinition = "TEXT")
  private String description;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "owner_id", nullable = false)
  private User owner;

  @Column(name = "epic_sequence", nullable = false)
  private Integer epicSequence = 0;

  @Column(name = "task_sequence", nullable = false)
  private Integer taskSequence = 0;

  @Column(name = "release_sequence", nullable = false)
  private Integer releaseSequence = 0;

  @Column(name = "created_at", nullable = false, updatable = false)
  private Instant createdAt;

  @Column(name = "updated_at", nullable = false)
  private Instant updatedAt;

  public Project(String publicId, String projectKey, String name, User owner) {
    this.publicId = publicId;
    this.projectKey = projectKey;
    this.name = name;
    this.owner = owner;
    this.epicSequence = 0;
    this.taskSequence = 0;
    this.releaseSequence = 0;
    this.createdAt = Instant.now();
    this.updatedAt = Instant.now();
  }

  @PreUpdate
  protected void onUpdate() {
    this.updatedAt = Instant.now();
  }

  /** Increments and returns the next epic sequence number. */
  public int nextEpicSequence() {
    this.epicSequence++;
    return this.epicSequence;
  }

  /** Increments and returns the next task sequence number. */
  public int nextTaskSequence() {
    this.taskSequence++;
    return this.taskSequence;
  }

  /** Increments and returns the next release sequence number. */
  public int nextReleaseSequence() {
    this.releaseSequence++;
    return this.releaseSequence;
  }
}
