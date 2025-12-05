package com.specflux.repository.domain;

import java.time.Instant;

import com.specflux.project.domain.Project;
import com.specflux.shared.domain.AggregateRoot;

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

/** Repository entity representing a Git repository associated with a project. */
@Entity
@Table(name = "repositories")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Repository extends AggregateRoot<Long> {

  public enum Status {
    READY,
    SYNCING,
    ERROR
  }

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

  @Column(nullable = false, length = 1000)
  private String path;

  @Setter
  @Column(name = "git_url", length = 500)
  private String gitUrl;

  @Setter
  @Column(name = "default_branch", length = 100)
  private String defaultBranch = "main";

  @Setter
  @Enumerated(EnumType.STRING)
  @Column(length = 50)
  private Status status = Status.READY;

  @Column(name = "created_at", nullable = false, updatable = false)
  private Instant createdAt;

  @Column(name = "updated_at", nullable = false)
  private Instant updatedAt;

  public Repository(String publicId, Project project, String name, String path) {
    this.publicId = publicId;
    this.project = project;
    this.name = name;
    this.path = path;
    this.status = Status.READY;
    this.defaultBranch = "main";
    this.createdAt = Instant.now();
    this.updatedAt = Instant.now();
  }

  @PreUpdate
  protected void onUpdate() {
    this.updatedAt = Instant.now();
  }
}
