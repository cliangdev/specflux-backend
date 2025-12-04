package com.specflux.shared.infrastructure.web;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Web MVC configuration.
 *
 * <p>Configures static resource handling for OpenAPI specification files served to Swagger UI, and
 * CORS settings for local development.
 */
@Configuration
public class WebConfig implements WebMvcConfigurer {

  @Override
  public void addResourceHandlers(ResourceHandlerRegistry registry) {
    // Serve OpenAPI spec files from classpath:openapi/ at /openapi/**
    registry
        .addResourceHandler("/openapi/**")
        .addResourceLocations("classpath:openapi/")
        .setCachePeriod(3600);
  }

  @Override
  public void addCorsMappings(CorsRegistry registry) {
    // Allow CORS from frontend dev server and Tauri app
    registry
        .addMapping("/api/**")
        .allowedOrigins(
            "http://localhost:5173", // Vite dev server
            "http://127.0.0.1:5173",
            "tauri://localhost", // Tauri app
            "https://tauri.localhost")
        .allowedMethods("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS")
        .allowedHeaders("*")
        .allowCredentials(true)
        .maxAge(3600);
  }
}
