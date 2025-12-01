package com.specflux.common;

import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import com.specflux.testcontainers.SharedPostgreSQLContainer;

/**
 * Base class for integration tests with schema isolation.
 *
 * <p>Each test class extending this gets its own database schema, enabling parallel test execution
 * without data conflicts.
 *
 * <p>Schema name is defined by each subclass via the @DynamicPropertySource method.
 *
 * <p>Flyway automatically creates tables in the schema for each test class.
 */
@SpringBootTest
@ActiveProfiles("test")
@Testcontainers
public abstract class AbstractIntegrationTest {

  @Container
  protected static final PostgreSQLContainer<?> POSTGRES = SharedPostgreSQLContainer.getInstance();

  /**
   * Configure Spring datasource to use the shared PostgreSQL container.
   *
   * <p>Subclasses must add their own @DynamicPropertySource to configure schema-specific
   * properties.
   *
   * @param registry the dynamic property registry
   */
  @DynamicPropertySource
  static void configureDataSource(DynamicPropertyRegistry registry) {
    registry.add("spring.datasource.url", POSTGRES::getJdbcUrl);
    registry.add("spring.datasource.username", POSTGRES::getUsername);
    registry.add("spring.datasource.password", POSTGRES::getPassword);

    // Enable schema creation by Flyway
    registry.add("spring.flyway.create-schemas", () -> "true");
  }

  /**
   * Helper method for subclasses to configure schema-specific properties.
   *
   * @param registry the dynamic property registry
   * @param schemaName the unique schema name for this test class
   */
  protected static void configureSchema(DynamicPropertyRegistry registry, String schemaName) {
    registry.add("spring.flyway.schemas", () -> schemaName);
    registry.add("spring.flyway.default-schema", () -> schemaName);
    registry.add("spring.jpa.properties.hibernate.default_schema", () -> schemaName);
    registry.add(
        "spring.datasource.hikari.connection-init-sql", () -> "SET search_path TO " + schemaName);
  }
}
