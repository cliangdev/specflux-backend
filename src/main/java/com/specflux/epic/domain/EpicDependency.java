package com.specflux.epic.domain;

import java.time.Instant;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/** Represents a dependency relationship between two epics. */
@Entity
@Table(
    name = "epic_dependencies",
    uniqueConstraints = @UniqueConstraint(columnNames = {"epic_id", "depends_on_epic_id"}))
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class EpicDependency {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "epic_id", nullable = false)
  private Epic epic;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "depends_on_epic_id", nullable = false)
  private Epic dependsOnEpic;

  @Column(name = "created_at", nullable = false, updatable = false)
  private Instant createdAt;

  public EpicDependency(Epic epic, Epic dependsOnEpic) {
    this.epic = epic;
    this.dependsOnEpic = dependsOnEpic;
    this.createdAt = Instant.now();
  }
}
