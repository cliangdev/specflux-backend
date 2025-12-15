package com.specflux.shared.infrastructure.security;

import java.io.IOException;
import java.util.Collections;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseAuthException;
import com.google.firebase.auth.FirebaseToken;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

/**
 * Filter that validates Firebase ID tokens from the Authorization header.
 *
 * <p>Extracts the Bearer token, verifies it using Firebase Admin SDK, and sets the authentication
 * in the SecurityContext.
 */
public class FirebaseAuthenticationFilter extends OncePerRequestFilter {

  private static final Logger log = LoggerFactory.getLogger(FirebaseAuthenticationFilter.class);
  private static final String AUTHORIZATION_HEADER = "Authorization";
  private static final String BEARER_PREFIX = "Bearer ";

  private final FirebaseAuth firebaseAuth;

  public FirebaseAuthenticationFilter(FirebaseAuth firebaseAuth) {
    this.firebaseAuth = firebaseAuth;
  }

  @Override
  protected void doFilterInternal(
      HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
      throws ServletException, IOException {

    // Skip if already authenticated (e.g., by API key filter)
    if (SecurityContextHolder.getContext().getAuthentication() != null) {
      filterChain.doFilter(request, response);
      return;
    }

    String authHeader = request.getHeader(AUTHORIZATION_HEADER);

    if (authHeader != null && authHeader.startsWith(BEARER_PREFIX)) {
      String token = authHeader.substring(BEARER_PREFIX.length());

      try {
        FirebaseToken firebaseToken = firebaseAuth.verifyIdToken(token, true);

        FirebasePrincipal principal =
            new FirebasePrincipal(
                firebaseToken.getUid(),
                firebaseToken.getEmail(),
                firebaseToken.getName(),
                firebaseToken.getPicture());

        FirebaseAuthenticationToken authentication =
            new FirebaseAuthenticationToken(principal, token, Collections.emptyList());

        SecurityContextHolder.getContext().setAuthentication(authentication);

        log.debug("Successfully authenticated user: {}", firebaseToken.getUid());
      } catch (FirebaseAuthException e) {
        log.warn("Failed to verify Firebase token: {}", e.getMessage());
        SecurityContextHolder.clearContext();
        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        response.setContentType("application/json");
        response
            .getWriter()
            .write("{\"error\": \"Unauthorized\", \"message\": \"Invalid or revoked token\"}");
        return;
      }
    }

    filterChain.doFilter(request, response);
  }
}
