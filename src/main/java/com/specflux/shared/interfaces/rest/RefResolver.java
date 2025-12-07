package com.specflux.shared.interfaces.rest;

import org.springframework.stereotype.Service;

import com.specflux.epic.domain.Epic;
import com.specflux.epic.domain.EpicRepository;
import com.specflux.prd.domain.Prd;
import com.specflux.prd.domain.PrdRepository;
import com.specflux.project.domain.Project;
import com.specflux.project.domain.ProjectRepository;
import com.specflux.release.domain.Release;
import com.specflux.release.domain.ReleaseRepository;
import com.specflux.shared.domain.DisplayKey;
import com.specflux.task.domain.Task;
import com.specflux.task.domain.TaskRepository;
import com.specflux.user.domain.User;
import com.specflux.user.domain.UserRepository;

import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;

/**
 * Resolves path parameters that can be either a public ID or a display key/project key.
 *
 * <p>Examples:
 *
 * <ul>
 *   <li>Project: "proj_abc123" (publicId) or "SPEC" (projectKey)
 *   <li>Epic: "epic_xyz789" (publicId) or "SPEC-E1" (displayKey)
 *   <li>Task: "task_def456" (publicId) or "SPEC-42" (displayKey)
 * </ul>
 */
@Service
@RequiredArgsConstructor
public class RefResolver {

  private final ProjectRepository projectRepository;
  private final EpicRepository epicRepository;
  private final TaskRepository taskRepository;
  private final ReleaseRepository releaseRepository;
  private final UserRepository userRepository;
  private final PrdRepository prdRepository;

  /**
   * Resolves a project reference to a Project entity.
   *
   * @param ref Project public ID (proj_xxx) or project key (SPEC)
   * @return The resolved Project
   * @throws EntityNotFoundException if project not found
   */
  public Project resolveProject(String ref) {
    if (ref == null || ref.isBlank()) {
      throw new IllegalArgumentException("Project reference is required");
    }

    // Check if it's a public ID (starts with "proj_")
    if (ref.startsWith("proj_")) {
      return projectRepository
          .findByPublicId(ref)
          .orElseThrow(() -> new EntityNotFoundException("Project not found: " + ref));
    }

    // Otherwise, treat as project key
    return projectRepository
        .findByProjectKey(ref.toUpperCase())
        .orElseThrow(() -> new EntityNotFoundException("Project not found: " + ref));
  }

  /**
   * Resolves an epic reference to an Epic entity within a project.
   *
   * @param project The parent project
   * @param ref Epic public ID (epic_xxx) or display key (PROJ-E1)
   * @return The resolved Epic
   * @throws EntityNotFoundException if epic not found
   */
  public Epic resolveEpic(Project project, String ref) {
    if (ref == null || ref.isBlank()) {
      throw new IllegalArgumentException("Epic reference is required");
    }

    // Check if it's a public ID (starts with "epic_")
    if (ref.startsWith("epic_")) {
      return epicRepository
          .findByPublicIdAndProjectId(ref, project.getId())
          .orElseThrow(() -> new EntityNotFoundException("Epic not found: " + ref));
    }

    // Treat as display key (e.g., PROJ-E1)
    // Note: Epic display keys have format PROJECT-E{number}, not PROJECT-{number}
    return epicRepository
        .findByProjectIdAndDisplayKey(project.getId(), ref)
        .orElseThrow(() -> new EntityNotFoundException("Epic not found: " + ref));
  }

  /**
   * Resolves a task reference to a Task entity within a project.
   *
   * @param project The parent project
   * @param ref Task public ID (task_xxx) or display key (PROJ-42)
   * @return The resolved Task
   * @throws EntityNotFoundException if task not found
   */
  public Task resolveTask(Project project, String ref) {
    if (ref == null || ref.isBlank()) {
      throw new IllegalArgumentException("Task reference is required");
    }

    // Check if it's a public ID (starts with "task_")
    if (ref.startsWith("task_")) {
      return taskRepository
          .findByPublicIdAndProjectId(ref, project.getId())
          .orElseThrow(() -> new EntityNotFoundException("Task not found: " + ref));
    }

    // Parse as display key (e.g., PROJ-42)
    try {
      DisplayKey displayKey = DisplayKey.parse(ref);
      return taskRepository
          .findByProjectIdAndDisplayKey(project.getId(), ref)
          .orElseThrow(() -> new EntityNotFoundException("Task not found: " + ref));
    } catch (IllegalArgumentException e) {
      throw new EntityNotFoundException("Task not found: " + ref);
    }
  }

  /**
   * Resolves a release reference to a Release entity within a project.
   *
   * @param project The parent project
   * @param ref Release public ID (rel_xxx) or display key (PROJ-R1)
   * @return The resolved Release
   * @throws EntityNotFoundException if release not found
   */
  public Release resolveRelease(Project project, String ref) {
    if (ref == null || ref.isBlank()) {
      throw new IllegalArgumentException("Release reference is required");
    }

    // Check if it's a public ID (starts with "rel_")
    if (ref.startsWith("rel_")) {
      return releaseRepository
          .findByPublicIdAndProjectId(ref, project.getId())
          .orElseThrow(() -> new EntityNotFoundException("Release not found: " + ref));
    }

    // Treat as display key (e.g., PROJ-R1)
    return releaseRepository
        .findByProjectIdAndDisplayKey(project.getId(), ref)
        .orElseThrow(() -> new EntityNotFoundException("Release not found: " + ref));
  }

  /**
   * Resolves a user reference to a User entity.
   *
   * @param ref User public ID (user_xxx)
   * @return The resolved User
   * @throws EntityNotFoundException if user not found
   */
  public User resolveUser(String ref) {
    if (ref == null || ref.isBlank()) {
      throw new IllegalArgumentException("User reference is required");
    }

    return userRepository
        .findByPublicId(ref)
        .orElseThrow(() -> new EntityNotFoundException("User not found: " + ref));
  }

  /**
   * Resolves an optional epic reference within a project.
   *
   * @param project The parent project
   * @param ref Epic reference (nullable)
   * @return The resolved Epic, or null if ref is null/blank
   */
  public Epic resolveEpicOptional(Project project, String ref) {
    if (ref == null || ref.isBlank()) {
      return null;
    }
    return resolveEpic(project, ref);
  }

  /**
   * Resolves an optional user reference.
   *
   * @param ref User reference (nullable)
   * @return The resolved User, or null if ref is null/blank
   */
  public User resolveUserOptional(String ref) {
    if (ref == null || ref.isBlank()) {
      return null;
    }
    return resolveUser(ref);
  }

  /**
   * Resolves a PRD reference to a Prd entity within a project.
   *
   * @param project The parent project
   * @param ref PRD public ID (prd_xxx) or display key (PROJ-P1)
   * @return The resolved Prd
   * @throws EntityNotFoundException if PRD not found
   */
  public Prd resolvePrd(Project project, String ref) {
    if (ref == null || ref.isBlank()) {
      throw new IllegalArgumentException("PRD reference is required");
    }

    // Check if it's a public ID (starts with "prd_")
    if (ref.startsWith("prd_")) {
      return prdRepository
          .findByPublicIdAndProjectId(ref, project.getId())
          .orElseThrow(() -> new EntityNotFoundException("PRD not found: " + ref));
    }

    // Treat as display key (e.g., PROJ-P1)
    return prdRepository
        .findByProjectIdAndDisplayKey(project.getId(), ref)
        .orElseThrow(() -> new EntityNotFoundException("PRD not found: " + ref));
  }

  /**
   * Resolves an optional PRD reference within a project.
   *
   * @param project The parent project
   * @param ref PRD reference (nullable)
   * @return The resolved Prd, or null if ref is null/blank
   */
  public Prd resolvePrdOptional(Project project, String ref) {
    if (ref == null || ref.isBlank()) {
      return null;
    }
    return resolvePrd(project, ref);
  }
}
