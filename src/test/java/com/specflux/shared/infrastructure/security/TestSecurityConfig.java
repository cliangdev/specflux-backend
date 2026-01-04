package com.specflux.shared.infrastructure.security;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

import com.specflux.apikey.application.ApiKeyService;
import com.specflux.apikey.infrastructure.ApiKeyAuthenticationFilter;

import lombok.RequiredArgsConstructor;

/**
 * Test security configuration that enforces authentication without requiring Firebase.
 *
 * <p>This configuration is activated when firebase.enabled=false, allowing tests to run without
 * Firebase credentials while still enforcing authentication.
 *
 * <p>Tests can use either:
 *
 * <ul>
 *   <li>{@code .with(SecurityMockMvcRequestPostProcessors.user("user"))} for mock authentication
 *   <li>API key header {@code Authorization: Bearer sfx_...} for real API key authentication
 * </ul>
 */
@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
@ConditionalOnProperty(name = "firebase.enabled", havingValue = "false")
public class TestSecurityConfig {

  private final ApiKeyService apiKeyService;

  @Bean
  public SecurityFilterChain testSecurityFilterChain(HttpSecurity http) throws Exception {
    return http.csrf(csrf -> csrf.disable())
        .sessionManagement(
            session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
        .authorizeHttpRequests(
            auth ->
                auth
                    // Health and actuator endpoints
                    .requestMatchers("/actuator/**")
                    .permitAll()
                    // OpenAPI / Swagger endpoints
                    .requestMatchers("/api-docs/**", "/swagger-ui/**", "/swagger-ui.html")
                    .permitAll()
                    // GitHub OAuth callback (receives unauthenticated redirect from GitHub)
                    .requestMatchers("/api/github/callback")
                    .permitAll()
                    // All other requests require authentication
                    .anyRequest()
                    .authenticated())
        // API key filter for sfx_ tokens (also works in tests)
        .addFilterBefore(
            new ApiKeyAuthenticationFilter(apiKeyService),
            UsernamePasswordAuthenticationFilter.class)
        .build();
  }
}
