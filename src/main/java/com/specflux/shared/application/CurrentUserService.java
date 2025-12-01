package com.specflux.shared.application;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import com.specflux.shared.infrastructure.security.FirebasePrincipal;
import com.specflux.user.domain.User;
import com.specflux.user.domain.UserRepository;

import jakarta.persistence.EntityNotFoundException;

/**
 * Service to retrieve the current authenticated user.
 *
 * <p>Extracts the Firebase UID from the security context and loads the corresponding User entity.
 */
@Service
public class CurrentUserService {

  private final UserRepository userRepository;

  public CurrentUserService(UserRepository userRepository) {
    this.userRepository = userRepository;
  }

  /**
   * Gets the current authenticated user.
   *
   * @return the current User entity
   * @throws EntityNotFoundException if user not found in database
   * @throws IllegalStateException if no authentication present
   */
  public User getCurrentUser() {
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
