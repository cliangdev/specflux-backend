package com.specflux.acceptancecriteria.domain;

import java.time.Instant;

import com.specflux.epic.domain.Epic;
import com.specflux.task.domain.Task;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** Acceptance criteria entity that can belong to either a Task or an Epic. */
@Entity
@Table(name = "acceptance_criteria")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class AcceptanceCriteria {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "task_id")
  private Task task;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "epic_id")
  private Epic epic;

  @Setter
  @Column(nullable = false, columnDefinition = "TEXT")
  private String criteria;

  @Setter
  @Column(name = "is_met", nullable = false)
  private Boolean isMet = false;

  @Setter
  @Column(name = "order_index", nullable = false)
  private Integer orderIndex = 0;

  @Column(name = "created_at", nullable = false, updatable = false)
  private Instant createdAt;

  /**
   * Creates acceptance criteria for a task.
   *
   * @param task the task this criteria belongs to
   * @param criteria the criteria text
   * @param orderIndex the display order
   */
  public AcceptanceCriteria(Task task, String criteria, int orderIndex) {
    if (task == null) {
      throw new IllegalArgumentException(
          "Task cannot be null when creating task acceptance criteria");
    }
    this.task = task;
    this.criteria = criteria;
    this.orderIndex = orderIndex;
    this.isMet = false;
    this.createdAt = Instant.now();
  }

  /**
   * Creates acceptance criteria for an epic.
   *
   * @param epic the epic this criteria belongs to
   * @param criteria the criteria text
   * @param orderIndex the display order
   */
  public AcceptanceCriteria(Epic epic, String criteria, int orderIndex) {
    if (epic == null) {
      throw new IllegalArgumentException(
          "Epic cannot be null when creating epic acceptance criteria");
    }
    this.epic = epic;
    this.criteria = criteria;
    this.orderIndex = orderIndex;
    this.isMet = false;
    this.createdAt = Instant.now();
  }
}
