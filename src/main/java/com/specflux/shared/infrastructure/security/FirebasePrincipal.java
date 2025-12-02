package com.specflux.shared.infrastructure.security;

import java.security.Principal;
import java.util.Objects;

import lombok.Getter;

/**
 * Principal implementation that holds Firebase user information.
 *
 * <p>Extracted from a verified Firebase ID token and contains the user's Firebase UID, email, and
 * display name.
 */
@Getter
public final class FirebasePrincipal implements Principal {

  private final String firebaseUid;
  private final String email;
  private final String displayName;
  private final String pictureUrl;

  /**
   * Creates a new FirebasePrincipal.
   *
   * @param firebaseUid the Firebase UID (required)
   * @param email the user's email (may be null)
   * @param displayName the user's display name (may be null)
   * @param pictureUrl the user's profile picture URL (may be null)
   */
  public FirebasePrincipal(
      String firebaseUid, String email, String displayName, String pictureUrl) {
    this.firebaseUid = Objects.requireNonNull(firebaseUid, "firebaseUid must not be null");
    this.email = email;
    this.displayName = displayName;
    this.pictureUrl = pictureUrl;
  }

  /**
   * Returns the Firebase UID as the principal name.
   *
   * @return the Firebase UID
   */
  @Override
  public String getName() {
    return firebaseUid;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;
    FirebasePrincipal that = (FirebasePrincipal) o;
    return Objects.equals(firebaseUid, that.firebaseUid);
  }

  @Override
  public int hashCode() {
    return Objects.hash(firebaseUid);
  }

  @Override
  public String toString() {
    return "FirebasePrincipal{firebaseUid='" + firebaseUid + "', email='" + email + "'}";
  }
}
