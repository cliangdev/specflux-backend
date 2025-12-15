package com.specflux.shared.infrastructure.security;

import java.util.Arrays;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import com.google.firebase.auth.FirebaseAuth;
import com.specflux.apikey.application.ApiKeyService;
import com.specflux.apikey.infrastructure.ApiKeyAuthenticationFilter;

/**
 * Security configuration for Firebase and API key authentication.
 *
 * <p>Configures stateless authentication using either Firebase tokens or API keys. API keys (sfx_
 * prefix) are validated first, then Firebase tokens. Public endpoints (health checks, OpenAPI docs)
 * are permitted without authentication.
 */
@Configuration
@EnableWebSecurity
@ConditionalOnProperty(name = "firebase.enabled", havingValue = "true", matchIfMissing = true)
public class SecurityConfig {

  private final FirebaseAuth firebaseAuth;
  private final ApiKeyService apiKeyService;

  public SecurityConfig(FirebaseAuth firebaseAuth, ApiKeyService apiKeyService) {
    this.firebaseAuth = firebaseAuth;
    this.apiKeyService = apiKeyService;
  }

  @Bean
  public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
    return http.cors(cors -> cors.configurationSource(corsConfigurationSource()))
        .csrf(csrf -> csrf.disable())
        .sessionManagement(
            session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
        .authorizeHttpRequests(
            auth ->
                auth
                    // Health and actuator endpoints
                    .requestMatchers("/actuator/**")
                    .permitAll()
                    // OpenAPI / Swagger endpoints
                    .requestMatchers(
                        "/api-docs/**", "/swagger-ui/**", "/swagger-ui.html", "/openapi/**")
                    .permitAll()
                    // All other requests require authentication
                    .anyRequest()
                    .authenticated())
        // API key filter runs first to handle sfx_ tokens
        .addFilterBefore(
            new ApiKeyAuthenticationFilter(apiKeyService),
            UsernamePasswordAuthenticationFilter.class)
        // Firebase filter runs after to handle JWT tokens
        .addFilterBefore(
            new FirebaseAuthenticationFilter(firebaseAuth), ApiKeyAuthenticationFilter.class)
        .build();
  }

  @Bean
  public CorsConfigurationSource corsConfigurationSource() {
    CorsConfiguration configuration = new CorsConfiguration();
    configuration.setAllowedOrigins(
        Arrays.asList(
            "http://localhost:5173", // Vite dev server
            "http://127.0.0.1:5173",
            "tauri://localhost", // Tauri app
            "https://tauri.localhost"));
    configuration.setAllowedMethods(
        Arrays.asList("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
    configuration.setAllowedHeaders(Arrays.asList("*"));
    configuration.setAllowCredentials(true);
    configuration.setMaxAge(3600L);

    UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
    source.registerCorsConfiguration("/**", configuration);
    return source;
  }
}
