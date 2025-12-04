package com.specflux.project.domain;

import java.time.Instant;

import com.specflux.user.domain.User;

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
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/** Entity representing a user's membership in a project. */
@Entity
@Table(
    name = "project_members",
    uniqueConstraints = @UniqueConstraint(columnNames = {"project_id", "user_id"}))
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ProjectMember {

  public enum Role {
    owner,
    admin,
    member
  }

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "project_id", nullable = false)
  private Project project;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "user_id", nullable = false)
  private User user;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false, length = 20)
  private Role role;

  @Column(name = "joined_at", nullable = false)
  private Instant joinedAt;

  public ProjectMember(Project project, User user, Role role) {
    this.project = project;
    this.user = user;
    this.role = role;
    this.joinedAt = Instant.now();
  }

  public static ProjectMember createOwner(Project project, User user) {
    return new ProjectMember(project, user, Role.owner);
  }

  public static ProjectMember createAdmin(Project project, User user) {
    return new ProjectMember(project, user, Role.admin);
  }

  public static ProjectMember createMember(Project project, User user) {
    return new ProjectMember(project, user, Role.member);
  }
}
