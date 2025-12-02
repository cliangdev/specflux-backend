package com.specflux.shared.infrastructure.security;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;

/**
 * Test security configuration that enforces authentication without requiring Firebase.
 *
 * <p>This configuration is activated when firebase.enabled=false, allowing tests to run without
 * Firebase credentials while still enforcing authentication.
 *
 * <p>Tests should use {@code .with(SecurityMockMvcRequestPostProcessors.user("user"))} to simulate
 * authenticated requests. This works reliably with Spring Security 7.0, unlike @WithMockUser which
 * has issues with the SecurityContextHolderFilter overwriting the mock context.
 */
@Configuration
@EnableWebSecurity
@ConditionalOnProperty(name = "firebase.enabled", havingValue = "false")
public class TestSecurityConfig {

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
                    // All other requests require authentication
                    .anyRequest()
                    .authenticated())
        .build();
  }
}
