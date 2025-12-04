package com.specflux.migration;

import com.specflux.epic.domain.EpicStatus;
import com.specflux.release.domain.ReleaseStatus;
import com.specflux.task.domain.TaskStatus;

/** Maps v1 status strings to v2 enum values. */
public final class StatusMapper {

  private StatusMapper() {
    // Utility class
  }

  /**
   * Maps v1 epic status to v2 EpicStatus.
   *
   * <p>v1 values: planning, active, completed
   *
   * @param v1Status the v1 status string
   * @return the corresponding EpicStatus
   */
  public static EpicStatus mapEpicStatus(String v1Status) {
    if (v1Status == null) {
      return EpicStatus.PLANNING;
    }
    return switch (v1Status.toLowerCase()) {
      case "planning" -> EpicStatus.PLANNING;
      case "active" -> EpicStatus.IN_PROGRESS;
      case "completed" -> EpicStatus.COMPLETED;
      default -> EpicStatus.PLANNING;
    };
  }

  /**
   * Maps v1 task status to v2 TaskStatus.
   *
   * <p>v1 values: backlog, ready, in_progress, pending_review, approved, done
   *
   * @param v1Status the v1 status string
   * @return the corresponding TaskStatus
   */
  public static TaskStatus mapTaskStatus(String v1Status) {
    if (v1Status == null) {
      return TaskStatus.BACKLOG;
    }
    return switch (v1Status.toLowerCase()) {
      case "backlog" -> TaskStatus.BACKLOG;
      case "ready" -> TaskStatus.READY;
      case "in_progress" -> TaskStatus.IN_PROGRESS;
      case "pending_review" -> TaskStatus.IN_REVIEW;
      case "approved", "done" -> TaskStatus.COMPLETED;
      default -> TaskStatus.BACKLOG;
    };
  }

  /**
   * Maps v1 release status to v2 ReleaseStatus.
   *
   * <p>v1 values: planned, in_progress, released
   *
   * @param v1Status the v1 status string
   * @return the corresponding ReleaseStatus
   */
  public static ReleaseStatus mapReleaseStatus(String v1Status) {
    if (v1Status == null) {
      return ReleaseStatus.PLANNED;
    }
    return switch (v1Status.toLowerCase()) {
      case "planned" -> ReleaseStatus.PLANNED;
      case "in_progress" -> ReleaseStatus.IN_PROGRESS;
      case "released" -> ReleaseStatus.RELEASED;
      default -> ReleaseStatus.PLANNED;
    };
  }
}
