package com.specflux.shared.infrastructure.security;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

import com.google.firebase.auth.FirebaseAuth;

/**
 * Security configuration for Firebase authentication.
 *
 * <p>Configures stateless JWT-based authentication using Firebase tokens. Public endpoints (health
 * checks, OpenAPI docs) are permitted without authentication.
 */
@Configuration
@EnableWebSecurity
@ConditionalOnProperty(name = "firebase.enabled", havingValue = "true", matchIfMissing = true)
public class SecurityConfig {

  private final FirebaseAuth firebaseAuth;

  public SecurityConfig(FirebaseAuth firebaseAuth) {
    this.firebaseAuth = firebaseAuth;
  }

  @Bean
  public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
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
        .addFilterBefore(
            new FirebaseAuthenticationFilter(firebaseAuth),
            UsernamePasswordAuthenticationFilter.class)
        .build();
  }
}
