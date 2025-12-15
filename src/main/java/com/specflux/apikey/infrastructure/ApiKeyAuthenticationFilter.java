package com.specflux.apikey.infrastructure;

import java.io.IOException;
import java.util.Collections;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

import com.specflux.apikey.application.ApiKeyService;
import com.specflux.shared.infrastructure.security.FirebaseAuthenticationToken;
import com.specflux.shared.infrastructure.security.FirebasePrincipal;
import com.specflux.user.domain.User;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

/**
 * Filter that validates API keys from the Authorization header.
 *
 * <p>Extracts the Bearer token, and if it starts with "sfx_", validates it as an API key. On
 * success, sets the authentication in the SecurityContext with the associated user. If the token is
 * not an API key, this filter passes through to allow other filters (e.g., Firebase) to handle it.
 */
public class ApiKeyAuthenticationFilter extends OncePerRequestFilter {

  private static final Logger log = LoggerFactory.getLogger(ApiKeyAuthenticationFilter.class);
  private static final String AUTHORIZATION_HEADER = "Authorization";
  private static final String BEARER_PREFIX = "Bearer ";

  private final ApiKeyService apiKeyService;

  public ApiKeyAuthenticationFilter(ApiKeyService apiKeyService) {
    this.apiKeyService = apiKeyService;
  }

  @Override
  protected void doFilterInternal(
      HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
      throws ServletException, IOException {

    String authHeader = request.getHeader(AUTHORIZATION_HEADER);

    if (authHeader != null && authHeader.startsWith(BEARER_PREFIX)) {
      String token = authHeader.substring(BEARER_PREFIX.length());

      if (ApiKeyService.isApiKey(token)) {
        try {
          var userOpt = apiKeyService.validateKey(token);

          if (userOpt.isPresent()) {
            User user = userOpt.get();

            FirebasePrincipal principal =
                new FirebasePrincipal(
                    user.getFirebaseUid(), user.getEmail(), user.getDisplayName(), null);

            FirebaseAuthenticationToken authentication =
                new FirebaseAuthenticationToken(principal, token, Collections.emptyList());

            SecurityContextHolder.getContext().setAuthentication(authentication);

            log.debug("Successfully authenticated user via API key: {}", user.getPublicId());
          } else {
            log.warn("Invalid or expired API key");
            SecurityContextHolder.clearContext();
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            response.setContentType("application/json");
            response
                .getWriter()
                .write(
                    "{\"error\": \"Unauthorized\", \"message\": \"Invalid, expired, or revoked API"
                        + " key\"}");
            return;
          }
        } catch (Exception e) {
          log.error("Error validating API key", e);
          SecurityContextHolder.clearContext();
          response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
          response.setContentType("application/json");
          response
              .getWriter()
              .write("{\"error\": \"Unauthorized\", \"message\": \"API key validation failed\"}");
          return;
        }
      }
    }

    filterChain.doFilter(request, response);
  }
}
