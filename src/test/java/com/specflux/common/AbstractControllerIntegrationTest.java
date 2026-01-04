package com.specflux.common;

import static org.mockito.Mockito.when;

import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.specflux.shared.application.CurrentUserService;
import com.specflux.user.domain.User;
import com.specflux.user.domain.UserRepository;

/**
 * Base class for controller integration tests.
 *
 * <p>Provides common test infrastructure including:
 *
 * <ul>
 *   <li>MockMvc for HTTP request testing
 *   <li>ObjectMapper for JSON serialization
 *   <li>CurrentUserService mock for authentication simulation
 *   <li>Test user setup and management
 * </ul>
 *
 * <p>Subclasses must:
 *
 * <ul>
 *   <li>Define their own SCHEMA_NAME constant
 *   <li>Add @DynamicPropertySource method calling configureSchema()
 *   <li>Override createTestUser() if custom user attributes are needed
 * </ul>
 *
 * <p>Authentication in tests:
 *
 * <ul>
 *   <li>Use {@code .with(user("user"))} for authenticated requests
 *   <li>Omit the post-processor for unauthenticated requests (expect 403)
 * </ul>
 */
@AutoConfigureMockMvc
@Transactional
public abstract class AbstractControllerIntegrationTest extends AbstractIntegrationTest {

  @Autowired protected MockMvc mockMvc;
  @Autowired protected UserRepository userRepository;
  @MockitoBean protected CurrentUserService currentUserService;

  protected final ObjectMapper objectMapper = new ObjectMapper();

  protected User testUser;

  /**
   * Sets up the test user before each test.
   *
   * <p>Creates a test user and configures the CurrentUserService mock to return it.
   */
  @BeforeEach
  void setUpTestUser() {
    testUser = userRepository.save(createTestUser());
    when(currentUserService.getCurrentUser()).thenReturn(testUser);
    when(currentUserService.getOrCreateCurrentUser()).thenReturn(testUser);
  }

  /**
   * Creates the test user for this test class.
   *
   * <p>Subclasses can override this to customize user attributes. The default implementation
   * creates a user with a unique publicId based on the test class name (truncated to fit column
   * limits).
   *
   * @return the test user to use for tests
   */
  protected User createTestUser() {
    // Use short prefix to stay within varchar(24) limit for publicId
    String shortName = getClass().getSimpleName().replace("ControllerTest", "").toLowerCase();
    if (shortName.length() > 16) {
      shortName = shortName.substring(0, 16);
    }
    return new User("user_" + shortName, "fb_" + shortName, shortName + "@test.com", "Test User");
  }
}
