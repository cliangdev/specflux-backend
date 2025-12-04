package com.specflux.migration;

import java.time.Duration;
import java.time.Instant;

import lombok.Builder;
import lombok.Getter;

/** Result of a migration operation. */
@Getter
@Builder
public class MigrationResult {

  private final boolean success;
  private final String message;
  private final MigrationStats stats;
  private final Instant startedAt;
  private final Instant completedAt;

  /** Statistics for migrated entities. */
  @Getter
  @Builder
  public static class MigrationStats {
    private final int projects;
    private final int releases;
    private final int epics;
    private final int epicDependencies;
    private final int tasks;
    private final int acceptanceCriteria;
    private final int taskDependencies;
  }

  public Duration getDuration() {
    return Duration.between(startedAt, completedAt);
  }

  public static MigrationResult success(MigrationStats stats, Instant startedAt) {
    return MigrationResult.builder()
        .success(true)
        .message("Migration completed successfully")
        .stats(stats)
        .startedAt(startedAt)
        .completedAt(Instant.now())
        .build();
  }

  public static MigrationResult failure(String message, Instant startedAt) {
    return MigrationResult.builder()
        .success(false)
        .message(message)
        .startedAt(startedAt)
        .completedAt(Instant.now())
        .build();
  }
}
