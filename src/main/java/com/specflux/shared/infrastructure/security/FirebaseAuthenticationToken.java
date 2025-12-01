package com.specflux.shared.infrastructure.security;

import java.util.Collection;
import java.util.Collections;

import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;

/**
 * Authentication token for Firebase-authenticated users.
 *
 * <p>This token is created after successfully verifying a Firebase ID token and contains the
 * FirebasePrincipal with user information.
 */
public class FirebaseAuthenticationToken extends AbstractAuthenticationToken {

  private final FirebasePrincipal principal;
  private final String token;

  /**
   * Creates an unauthenticated token with just the raw JWT.
   *
   * @param token the Firebase ID token (JWT)
   */
  public FirebaseAuthenticationToken(String token) {
    super(Collections.emptyList());
    this.token = token;
    this.principal = null;
    setAuthenticated(false);
  }

  /**
   * Creates an authenticated token with the verified principal.
   *
   * @param principal the FirebasePrincipal from the verified token
   * @param token the original Firebase ID token
   * @param authorities the granted authorities
   */
  public FirebaseAuthenticationToken(
      FirebasePrincipal principal,
      String token,
      Collection<? extends GrantedAuthority> authorities) {
    super(authorities);
    this.principal = principal;
    this.token = token;
    setAuthenticated(true);
  }

  @Override
  public Object getCredentials() {
    return token;
  }

  @Override
  public FirebasePrincipal getPrincipal() {
    return principal;
  }

  /**
   * Returns the original Firebase ID token.
   *
   * @return the JWT token
   */
  public String getToken() {
    return token;
  }
}
