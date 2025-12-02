package com.specflux.shared.infrastructure.web;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Web MVC configuration.
 *
 * <p>Configures static resource handling for OpenAPI specification files served to Swagger UI.
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
}
