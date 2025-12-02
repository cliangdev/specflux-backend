package com.specflux.migration;

import java.io.File;
import java.util.Arrays;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import com.specflux.user.domain.User;
import com.specflux.user.domain.UserRepository;

import lombok.RequiredArgsConstructor;

/**
 * Command line runner for migrating data from v1 SQLite to v2 PostgreSQL.
 *
 * <p>Usage:
 *
 * <pre>
 * java -jar specflux-backend.jar --migrate \
 *   --migration.sqlite.path=/path/to/specflux.db \
 *   --migration.target.user=user@example.com
 * </pre>
 */
@Component
@RequiredArgsConstructor
public class MigrationCommand implements CommandLineRunner {

  private static final Logger log = LoggerFactory.getLogger(MigrationCommand.class);

  private final MigrationService migrationService;
  private final UserRepository userRepository;

  @Value("${migration.sqlite.path:}")
  private String sqlitePath;

  @Value("${migration.target.user:}")
  private String targetUserEmail;

  @Override
  public void run(String... args) throws Exception {
    if (!Arrays.asList(args).contains("--migrate")) {
      return; // Not running migration
    }

    log.info("=== SpecFlux v1 → v2 Migration ===");

    // Validate SQLite path
    if (sqlitePath == null || sqlitePath.isEmpty()) {
      log.error("SQLite path not specified. Use --migration.sqlite.path=/path/to/specflux.db");
      System.exit(1);
      return;
    }

    File sqliteFile = new File(sqlitePath);
    if (!sqliteFile.exists()) {
      log.error("SQLite file not found: {}", sqlitePath);
      System.exit(1);
      return;
    }

    // Validate target user
    if (targetUserEmail == null || targetUserEmail.isEmpty()) {
      log.error("Target user email not specified. Use --migration.target.user=user@example.com");
      System.exit(1);
      return;
    }

    User targetUser =
        userRepository
            .findByEmail(targetUserEmail)
            .orElseThrow(
                () -> {
                  log.error("User not found: {}", targetUserEmail);
                  return new RuntimeException("User not found: " + targetUserEmail);
                });

    log.info("SQLite path: {}", sqlitePath);
    log.info("Target user: {} ({})", targetUser.getDisplayName(), targetUser.getEmail());

    // Run migration
    MigrationResult result = migrationService.migrate(sqlitePath, targetUser);

    // Print results
    log.info("=== Migration Results ===");
    log.info("Success: {}", result.isSuccess());
    log.info("Duration: {} seconds", result.getDuration().toSeconds());

    if (result.isSuccess()) {
      MigrationResult.MigrationStats stats = result.getStats();
      log.info("Projects migrated: {}", stats.getProjects());
      log.info("Releases migrated: {}", stats.getReleases());
      log.info("Epics migrated: {}", stats.getEpics());
      log.info("Tasks migrated: {}", stats.getTasks());
      log.info("Acceptance criteria migrated: {}", stats.getAcceptanceCriteria());
      log.info("Task dependencies migrated: {}", stats.getTaskDependencies());
    } else {
      log.error("Migration failed: {}", result.getMessage());
      System.exit(1);
    }

    log.info("=== Migration Complete ===");
    System.exit(0);
  }
}
