package com.specflux.shared.application;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.RequestContextHolder;

import com.specflux.shared.infrastructure.security.FirebasePrincipal;
import com.specflux.user.domain.User;
import com.specflux.user.domain.UserRepository;

import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;

/**
 * Service to retrieve the current authenticated user.
 *
 * <p>Extracts the Firebase UID from the security context and loads the corresponding User entity.
 * The user is cached for the duration of the HTTP request to avoid repeated database queries.
 */
@Service
@RequiredArgsConstructor
public class CurrentUserService {

  private static final String CURRENT_USER_ATTR = "currentUser";

  private final UserRepository userRepository;

  /**
   * Gets the current authenticated user.
   *
   * <p>The user is cached in the request scope, so multiple calls within the same HTTP request will
   * only hit the database once.
   *
   * @return the current User entity
   * @throws EntityNotFoundException if user not found in database
   * @throws IllegalStateException if no authentication present
   */
  public User getCurrentUser() {
    // Check request-scoped cache first
    RequestAttributes requestAttributes = RequestContextHolder.getRequestAttributes();
    if (requestAttributes != null) {
      User cachedUser =
          (User) requestAttributes.getAttribute(CURRENT_USER_ATTR, RequestAttributes.SCOPE_REQUEST);
      if (cachedUser != null) {
        return cachedUser;
      }
    }

    // Load from database
    User user = loadCurrentUser();

    // Cache in request scope
    if (requestAttributes != null) {
      requestAttributes.setAttribute(CURRENT_USER_ATTR, user, RequestAttributes.SCOPE_REQUEST);
    }

    return user;
  }

  private User loadCurrentUser() {
    Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

    if (authentication == null || !authentication.isAuthenticated()) {
      throw new IllegalStateException("No authenticated user");
    }

    Object principal = authentication.getPrincipal();

    if (principal instanceof FirebasePrincipal firebasePrincipal) {
      return userRepository
          .findByFirebaseUid(firebasePrincipal.getFirebaseUid())
          .orElseThrow(
              () ->
                  new EntityNotFoundException(
                      "User not found for Firebase UID: " + firebasePrincipal.getFirebaseUid()));
    }

    // For test contexts with simple username
    if (principal instanceof String username) {
      return userRepository
          .findByFirebaseUid(username)
          .orElseThrow(() -> new EntityNotFoundException("User not found: " + username));
    }

    throw new IllegalStateException("Unexpected principal type: " + principal.getClass());
  }

  /**
   * Gets the current user's Firebase UID without loading the full User entity.
   *
   * @return the Firebase UID
   * @throws IllegalStateException if no authentication present
   */
  public String getCurrentFirebaseUid() {
    Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

    if (authentication == null || !authentication.isAuthenticated()) {
      throw new IllegalStateException("No authenticated user");
    }

    Object principal = authentication.getPrincipal();

    if (principal instanceof FirebasePrincipal firebasePrincipal) {
      return firebasePrincipal.getFirebaseUid();
    }

    if (principal instanceof String username) {
      return username;
    }

    throw new IllegalStateException("Unexpected principal type: " + principal.getClass());
  }
}
