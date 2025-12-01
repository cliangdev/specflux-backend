package com.specflux.testcontainers;

import org.testcontainers.containers.PostgreSQLContainer;

/**
 * Shared PostgreSQL container for all integration tests.
 *
 * <p>Uses singleton pattern to ensure only one container is created across all test classes. This
 * prevents port conflicts and speeds up test execution.
 *
 * <p>Each test class will use a unique schema within this container for isolation.
 */
public final class SharedPostgreSQLContainer
    extends PostgreSQLContainer<SharedPostgreSQLContainer> {
  private static final String IMAGE_VERSION = "postgres:18";
  private static SharedPostgreSQLContainer container;

  private SharedPostgreSQLContainer() {
    super(IMAGE_VERSION);
  }

  public static SharedPostgreSQLContainer getInstance() {
    if (container == null) {
      container =
          new SharedPostgreSQLContainer()
              .withDatabaseName("specflux_test")
              .withUsername("test")
              .withPassword("test")
              .withReuse(true);
    }
    return container;
  }

  @Override
  public void start() {
    super.start();
    // Set system properties for Spring to use
    System.setProperty("DB_URL", container.getJdbcUrl());
    System.setProperty("DB_USERNAME", container.getUsername());
    System.setProperty("DB_PASSWORD", container.getPassword());
  }

  @Override
  public void stop() {
    // Do nothing - container stops when JVM shuts down
  }
}
