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
 * <p>Test classes are automatically distributed across a fixed number of schema buckets using
 * consistent hashing. Classes in the same bucket share a Spring context and connection pool, which
 * reduces resource usage while maintaining isolation via {@code @Transactional} rollback.
 *
 * <p>Flyway automatically creates tables in each schema bucket.
 */
@SpringBootTest
@ActiveProfiles("test")
@Testcontainers
public abstract class AbstractIntegrationTest {

  /** Number of schema buckets. More buckets = more isolation but more connections. */
  private static final int SCHEMA_BUCKET_COUNT = 4;

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
   * Configure schema using automatic bucket assignment based on test class name.
   *
   * <p>Test classes are distributed across {@link #SCHEMA_BUCKET_COUNT} buckets using consistent
   * hashing. Classes that hash to the same bucket will share a Spring context and connection pool.
   *
   * <p>This provides a balance between test isolation (via @Transactional rollback) and resource
   * efficiency (fewer connection pools).
   *
   * @param registry the dynamic property registry
   * @param testClass the test class to compute bucket for
   */
  protected static void configureSchemaForClass(
      DynamicPropertyRegistry registry, Class<?> testClass) {
    int bucket = Math.abs(testClass.getSimpleName().hashCode() % SCHEMA_BUCKET_COUNT);
    String schemaName = "test_bucket_" + bucket;
    configureSchema(registry, schemaName);
  }

  /**
   * Helper method for subclasses to configure schema-specific properties.
   *
   * <p>Prefer using {@link #configureSchemaForClass} for automatic bucket assignment.
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
