package com.specflux.infrastructure;

import static org.assertj.core.api.Assertions.assertThat;

import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashSet;
import java.util.Set;

import javax.sql.DataSource;

import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;

@SpringBootTest
@ActiveProfiles("test")
@Testcontainers
class FlywayMigrationTest {

  @Container
  static PostgreSQLContainer postgres =
      new PostgreSQLContainer("postgres:18").withDatabaseName("specflux_test");

  @DynamicPropertySource
  static void configureProperties(DynamicPropertyRegistry registry) {
    registry.add("spring.datasource.url", postgres::getJdbcUrl);
    registry.add("spring.datasource.username", postgres::getUsername);
    registry.add("spring.datasource.password", postgres::getPassword);
  }

  @Autowired private DataSource dataSource;

  @Autowired private Flyway flyway;

  @Test
  void flywayMigrationsApplySuccessfully() throws SQLException {
    // Verify Flyway ran migrations
    assertThat(flyway.info().current()).isNotNull();

    try (Connection conn = dataSource.getConnection()) {
      DatabaseMetaData meta = conn.getMetaData();
      // Use public schema for PostgreSQL
      ResultSet tables = meta.getTables(null, "public", "%", new String[] {"TABLE"});

      Set<String> tableNames = new HashSet<>();
      while (tables.next()) {
        tableNames.add(tables.getString("TABLE_NAME"));
      }

      // Verify all expected tables exist
      assertThat(tableNames)
          .contains(
              "users",
              "projects",
              "project_members",
              "epics",
              "tasks",
              "task_dependencies",
              "releases",
              "acceptance_criteria",
              "notifications",
              "flyway_schema_history");
    }
  }
}
