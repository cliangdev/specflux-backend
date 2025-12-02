package com.specflux.task.domain;

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

/** Entity representing a dependency relationship between two tasks. */
@Entity
@Table(
    name = "task_dependencies",
    uniqueConstraints = @UniqueConstraint(columnNames = {"task_id", "depends_on_task_id"}))
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class TaskDependency {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "task_id", nullable = false)
  private Task task;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "depends_on_task_id", nullable = false)
  private Task dependsOnTask;

  @Column(name = "created_at", nullable = false, updatable = false)
  private Instant createdAt;

  public TaskDependency(Task task, Task dependsOnTask) {
    if (task.getId().equals(dependsOnTask.getId())) {
      throw new IllegalArgumentException("A task cannot depend on itself");
    }
    this.task = task;
    this.dependsOnTask = dependsOnTask;
    this.createdAt = Instant.now();
  }
}
