package com.specflux.migration;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.Instant;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.specflux.acceptancecriteria.domain.AcceptanceCriteria;
import com.specflux.acceptancecriteria.domain.AcceptanceCriteriaRepository;
import com.specflux.epic.domain.Epic;
import com.specflux.epic.domain.EpicDependency;
import com.specflux.epic.domain.EpicDependencyRepository;
import com.specflux.epic.domain.EpicRepository;
import com.specflux.project.domain.Project;
import com.specflux.project.domain.ProjectMember;
import com.specflux.project.domain.ProjectMemberRepository;
import com.specflux.project.domain.ProjectRepository;
import com.specflux.task.domain.Task;
import com.specflux.task.domain.TaskDependency;
import com.specflux.task.domain.TaskDependencyRepository;
import com.specflux.task.domain.TaskRepository;
import com.specflux.user.domain.User;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import lombok.RequiredArgsConstructor;

/** Service for migrating data from v1 SQLite to v2 PostgreSQL. */
@Service
@RequiredArgsConstructor
public class MigrationService {

  private static final Logger log = LoggerFactory.getLogger(MigrationService.class);

  private final ProjectRepository projectRepository;
  private final ProjectMemberRepository projectMemberRepository;
  private final EpicRepository epicRepository;
  private final EpicDependencyRepository epicDependencyRepository;
  private final TaskRepository taskRepository;
  private final AcceptanceCriteriaRepository acceptanceCriteriaRepository;
  private final TaskDependencyRepository taskDependencyRepository;

  @PersistenceContext private EntityManager entityManager;

  // ID mappings: v1 ID -> v2 entity
  private final Map<Long, Project> projectMap = new HashMap<>();
  private final Map<Long, Epic> epicMap = new HashMap<>();
  private final Map<Long, Task> taskMap = new HashMap<>();

  /**
   * Migrates all data from SQLite database to PostgreSQL.
   *
   * @param sqlitePath path to the SQLite database file
   * @param targetUser the Firebase user to assign all data to
   * @return migration result with statistics
   */
  @Transactional
  public MigrationResult migrate(String sqlitePath, User targetUser) {
    Instant startedAt = Instant.now();
    log.info("Starting migration from SQLite: {}", sqlitePath);

    try {
      // Clear existing data first (clean slate)
      clearExistingData();

      // Clear ID mappings
      projectMap.clear();
      epicMap.clear();
      taskMap.clear();

      int projectCount = 0;
      int epicCount = 0;
      int epicDepCount = 0;
      int taskCount = 0;
      int acCount = 0;
      int depCount = 0;

      try (Connection conn = DriverManager.getConnection("jdbc:sqlite:" + sqlitePath)) {
        // Migrate in FK dependency order
        projectCount = migrateProjects(conn, targetUser);
        log.info("Migrated {} projects", projectCount);

        epicCount = migrateEpics(conn, targetUser);
        log.info("Migrated {} epics", epicCount);

        epicDepCount = migrateEpicDependencies(conn);
        log.info("Migrated {} epic dependencies", epicDepCount);

        taskCount = migrateTasks(conn, targetUser);
        log.info("Migrated {} tasks", taskCount);

        acCount = migrateAcceptanceCriteria(conn);
        log.info("Migrated {} acceptance criteria", acCount);

        depCount = migrateTaskDependencies(conn);
        log.info("Migrated {} task dependencies", depCount);
      }

      MigrationResult.MigrationStats stats =
          MigrationResult.MigrationStats.builder()
              .projects(projectCount)
              .releases(0)
              .epics(epicCount)
              .epicDependencies(epicDepCount)
              .tasks(taskCount)
              .acceptanceCriteria(acCount)
              .taskDependencies(depCount)
              .build();

      log.info("Migration completed successfully");
      return MigrationResult.success(stats, startedAt);

    } catch (Exception e) {
      log.error("Migration failed", e);
      return MigrationResult.failure(e.getMessage(), startedAt);
    }
  }

  private void clearExistingData() {
    log.info("Clearing existing data (clean slate)...");
    // Use TRUNCATE CASCADE for PostgreSQL to handle all foreign key constraints
    entityManager.createNativeQuery("TRUNCATE TABLE task_dependencies CASCADE").executeUpdate();
    entityManager.createNativeQuery("TRUNCATE TABLE acceptance_criteria CASCADE").executeUpdate();
    entityManager.createNativeQuery("TRUNCATE TABLE tasks CASCADE").executeUpdate();
    entityManager.createNativeQuery("TRUNCATE TABLE epic_dependencies CASCADE").executeUpdate();
    entityManager.createNativeQuery("TRUNCATE TABLE epics CASCADE").executeUpdate();
    entityManager.createNativeQuery("TRUNCATE TABLE project_members CASCADE").executeUpdate();
    entityManager.createNativeQuery("TRUNCATE TABLE projects CASCADE").executeUpdate();
    log.info("Existing data cleared");
  }

  private int migrateProjects(Connection conn, User targetUser) throws SQLException {
    int count = 0;
    String sql = "SELECT id, project_id, name, created_at, updated_at FROM projects";

    try (Statement stmt = conn.createStatement();
        ResultSet rs = stmt.executeQuery(sql)) {

      while (rs.next()) {
        long v1Id = rs.getLong("id");
        String projectKey = rs.getString("project_id");
        String name = rs.getString("name");

        // Truncate projectKey to 10 chars if needed and uppercase
        if (projectKey != null) {
          if (projectKey.length() > 10) {
            projectKey = projectKey.substring(0, 10);
          }
          projectKey = projectKey.toUpperCase();
        }

        String publicId = generatePublicId("proj");
        Project project = new Project(publicId, projectKey, name, targetUser);
        project.setDescription(name); // Use name as initial description

        Project saved = projectRepository.save(project);

        // Add owner as member
        ProjectMember ownerMember = ProjectMember.createOwner(saved, targetUser);
        projectMemberRepository.save(ownerMember);

        projectMap.put(v1Id, saved);
        count++;
      }
    }

    return count;
  }

  private int migrateEpics(Connection conn, User targetUser) throws SQLException {
    int count = 0;
    String sql =
        "SELECT id, project_id, title, description, status, target_date,"
            + " prd_file_path, epic_file_path, created_at FROM epics";

    try (Statement stmt = conn.createStatement();
        ResultSet rs = stmt.executeQuery(sql)) {

      while (rs.next()) {
        long v1Id = rs.getLong("id");
        long v1ProjectId = rs.getLong("project_id");
        String title = rs.getString("title");
        String description = rs.getString("description");
        String status = rs.getString("status");
        String targetDateStr = rs.getString("target_date");
        String prdFilePath = rs.getString("prd_file_path");
        String epicFilePath = rs.getString("epic_file_path");

        Project project = projectMap.get(v1ProjectId);
        if (project == null) {
          log.warn("Skipping epic {} - project {} not found", v1Id, v1ProjectId);
          continue;
        }

        int seqNum = project.nextEpicSequence();
        String displayKey = project.getProjectKey() + "-E" + seqNum;
        String publicId = generatePublicId("epic");

        Epic epic = new Epic(publicId, project, seqNum, displayKey, title, targetUser);
        epic.setDescription(description);
        epic.setStatus(StatusMapper.mapEpicStatus(status));
        epic.setPrdFilePath(prdFilePath);
        epic.setEpicFilePath(epicFilePath);

        if (targetDateStr != null && !targetDateStr.isEmpty()) {
          try {
            epic.setTargetDate(LocalDate.parse(targetDateStr.substring(0, 10)));
          } catch (Exception e) {
            log.warn("Could not parse target date: {}", targetDateStr);
          }
        }

        Epic saved = epicRepository.save(epic);
        projectRepository.save(project); // Save updated sequence

        epicMap.put(v1Id, saved);
        count++;
      }
    }

    return count;
  }

  private int migrateTasks(Connection conn, User targetUser) throws SQLException {
    int count = 0;
    String sql =
        "SELECT id, project_id, epic_id, title, description, status, requires_approval,"
            + " estimated_duration, actual_duration, github_pr_url, created_at FROM tasks";

    try (Statement stmt = conn.createStatement();
        ResultSet rs = stmt.executeQuery(sql)) {

      while (rs.next()) {
        long v1Id = rs.getLong("id");
        long v1ProjectId = rs.getLong("project_id");
        long v1EpicId = rs.getLong("epic_id");
        String title = rs.getString("title");
        String description = rs.getString("description");
        String status = rs.getString("status");
        int requiresApproval = rs.getInt("requires_approval");
        Integer estimatedDuration = getIntOrNull(rs, "estimated_duration");
        Integer actualDuration = getIntOrNull(rs, "actual_duration");
        String githubPrUrl = rs.getString("github_pr_url");

        Project project = projectMap.get(v1ProjectId);
        if (project == null) {
          log.warn("Skipping task {} - project {} not found", v1Id, v1ProjectId);
          continue;
        }

        int seqNum = project.nextTaskSequence();
        String displayKey = project.getProjectKey() + "-T" + seqNum;
        String publicId = generatePublicId("task");

        Task task = new Task(publicId, project, seqNum, displayKey, title, targetUser);
        task.setDescription(description);
        task.setStatus(StatusMapper.mapTaskStatus(status));
        task.setRequiresApproval(requiresApproval == 1);
        task.setEstimatedDuration(estimatedDuration);
        task.setActualDuration(actualDuration);
        task.setGithubPrUrl(githubPrUrl);

        // Map epic ID
        if (v1EpicId > 0) {
          Epic epic = epicMap.get(v1EpicId);
          if (epic != null) {
            task.setEpic(epic);
          }
        }

        Task saved = taskRepository.save(task);
        projectRepository.save(project); // Save updated sequence

        taskMap.put(v1Id, saved);
        count++;
      }
    }

    return count;
  }

  private int migrateAcceptanceCriteria(Connection conn) throws SQLException {
    int count = 0;
    String sql =
        "SELECT id, entity_type, entity_id, text, checked, position FROM acceptance_criteria";

    try (Statement stmt = conn.createStatement();
        ResultSet rs = stmt.executeQuery(sql)) {

      while (rs.next()) {
        String entityType = rs.getString("entity_type");
        long entityId = rs.getLong("entity_id");
        String text = rs.getString("text");
        int checked = rs.getInt("checked");
        int position = rs.getInt("position");

        AcceptanceCriteria ac;

        if ("task".equals(entityType)) {
          Task task = taskMap.get(entityId);
          if (task == null) {
            log.warn("Skipping AC - task {} not found", entityId);
            continue;
          }
          ac = new AcceptanceCriteria(task, text, position);
        } else if ("epic".equals(entityType)) {
          Epic epic = epicMap.get(entityId);
          if (epic == null) {
            log.warn("Skipping AC - epic {} not found", entityId);
            continue;
          }
          ac = new AcceptanceCriteria(epic, text, position);
        } else {
          log.warn("Unknown entity type: {}", entityType);
          continue;
        }

        ac.setIsMet(checked == 1);
        acceptanceCriteriaRepository.save(ac);
        count++;
      }
    }

    return count;
  }

  private int migrateTaskDependencies(Connection conn) throws SQLException {
    int count = 0;
    String sql = "SELECT task_id, depends_on_task_id FROM task_dependencies";

    try (Statement stmt = conn.createStatement();
        ResultSet rs = stmt.executeQuery(sql)) {

      while (rs.next()) {
        long taskId = rs.getLong("task_id");
        long dependsOnTaskId = rs.getLong("depends_on_task_id");

        Task task = taskMap.get(taskId);
        Task dependsOnTask = taskMap.get(dependsOnTaskId);

        if (task == null || dependsOnTask == null) {
          log.warn(
              "Skipping dependency - task {} or depends_on {} not found", taskId, dependsOnTaskId);
          continue;
        }

        TaskDependency dep = new TaskDependency(task, dependsOnTask);
        taskDependencyRepository.save(dep);
        count++;
      }
    }

    return count;
  }

  private int migrateEpicDependencies(Connection conn) throws SQLException {
    int count = 0;
    // v1 stores dependencies as JSON array in depends_on column
    String sql =
        "SELECT id, depends_on FROM epics WHERE depends_on IS NOT NULL AND depends_on != ''";

    try (Statement stmt = conn.createStatement();
        ResultSet rs = stmt.executeQuery(sql)) {

      while (rs.next()) {
        long epicId = rs.getLong("id");
        String dependsOnJson = rs.getString("depends_on");

        Epic epic = epicMap.get(epicId);
        if (epic == null) {
          log.warn("Skipping epic dependency - epic {} not found in map", epicId);
          continue;
        }

        // Parse JSON array of epic IDs (e.g., "[1, 2, 3]" or "1,2,3")
        if (dependsOnJson != null && !dependsOnJson.isBlank()) {
          // Remove brackets and whitespace
          String cleaned = dependsOnJson.trim().replaceAll("[\\[\\]]", "");
          if (!cleaned.isEmpty()) {
            String[] depIds = cleaned.split(",");
            for (String depIdStr : depIds) {
              try {
                long depId = Long.parseLong(depIdStr.trim());
                Epic dependsOnEpic = epicMap.get(depId);
                if (dependsOnEpic != null && !epic.getId().equals(dependsOnEpic.getId())) {
                  EpicDependency dependency = new EpicDependency(epic, dependsOnEpic);
                  epicDependencyRepository.save(dependency);
                  count++;
                } else if (dependsOnEpic == null) {
                  log.warn(
                      "Skipping dependency - depends_on epic {} not found for epic {}",
                      depId,
                      epicId);
                }
              } catch (NumberFormatException e) {
                log.warn("Could not parse epic dependency ID: {}", depIdStr);
              }
            }
          }
        }
      }
    }

    return count;
  }

  private String generatePublicId(String prefix) {
    return prefix + "_" + UUID.randomUUID().toString().replace("-", "").substring(0, 16);
  }

  private Integer getIntOrNull(ResultSet rs, String column) throws SQLException {
    int value = rs.getInt(column);
    return rs.wasNull() ? null : value;
  }
}
