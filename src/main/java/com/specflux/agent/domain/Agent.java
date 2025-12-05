package com.specflux.agent.domain;

import java.time.Instant;

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

/** Agent entity representing a Claude Code agent definition. */
@Entity
@Table(name = "agents")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Agent extends AggregateRoot<Long> {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(name = "public_id", nullable = false, unique = true, length = 32)
  private String publicId;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "project_id", nullable = false)
  private Project project;

  @Setter
  @Column(nullable = false)
  private String name;

  @Setter
  @Column(columnDefinition = "TEXT")
  private String description;

  @Setter
  @Column(name = "file_path", length = 1000)
  private String filePath;

  @Column(name = "created_at", nullable = false, updatable = false)
  private Instant createdAt;

  @Column(name = "updated_at", nullable = false)
  private Instant updatedAt;

  public Agent(String publicId, Project project, String name) {
    this.publicId = publicId;
    this.project = project;
    this.name = name;
    this.createdAt = Instant.now();
    this.updatedAt = Instant.now();
  }

  @PreUpdate
  protected void onUpdate() {
    this.updatedAt = Instant.now();
  }
}
