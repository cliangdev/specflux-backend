package com.specflux.shared.infrastructure.web;

import java.io.IOException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

/**
 * Filter that logs all incoming HTTP requests for debugging purposes.
 *
 * <p>Logs the request method, URI, query string, and response status code. This is useful for
 * debugging 404 errors and understanding request routing.
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class RequestLoggingFilter extends OncePerRequestFilter {

  private static final Logger log = LoggerFactory.getLogger(RequestLoggingFilter.class);

  @Override
  protected void doFilterInternal(
      HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
      throws ServletException, IOException {

    String method = request.getMethod();
    String uri = request.getRequestURI();
    String queryString = request.getQueryString();
    String contextPath = request.getContextPath();
    String servletPath = request.getServletPath();

    log.info(
        "[REQUEST] {} {} | contextPath={} | servletPath={} | queryString={}",
        method,
        uri,
        contextPath,
        servletPath,
        queryString);

    try {
      filterChain.doFilter(request, response);
    } finally {
      int status = response.getStatus();
      log.info("[RESPONSE] {} {} -> status={}", method, uri, status);

      // Log additional debug info for 404 errors
      if (status == 404) {
        log.warn(
            "[DEBUG 404] Request URI: {} | Context Path: {} | Servlet Path: {} | "
                + "Check if the path includes the context-path prefix (e.g., /api)",
            uri,
            contextPath,
            servletPath);
      }
    }
  }
}
