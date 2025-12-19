package com.specflux.shared.infrastructure.web;

import java.util.List;

import org.openapitools.jackson.nullable.JsonNullableModule;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

/**
 * Jackson configuration for JSON Merge Patch (RFC 7396) support.
 *
 * <p>This configuration registers the JsonNullableModule which allows distinguishing between:
 *
 * <ul>
 *   <li>Field absent in JSON: {@code JsonNullable.undefined()} - don't modify
 *   <li>Field explicitly null in JSON: {@code JsonNullable.of(null)} - clear the value
 *   <li>Field has value in JSON: {@code JsonNullable.of(value)} - set the value
 * </ul>
 *
 * <p><strong>Note:</strong> This uses deprecated Jackson 2 APIs
 * (MappingJackson2HttpMessageConverter, configureMessageConverters) because
 * jackson-databind-nullable only supports Jackson 2. When/if jackson-databind-nullable adds Jackson
 * 3 support, this should be migrated to use JacksonJsonHttpMessageConverter and the new
 * configureMessageConverters(ServerBuilder) API.
 */
@Configuration
@SuppressWarnings("removal") // Using Jackson 2 APIs for jackson-databind-nullable compatibility
public class JacksonConfig implements WebMvcConfigurer {

  /** Primary ObjectMapper with JsonNullableModule for HTTP message conversion. */
  @Bean
  @Primary
  public ObjectMapper objectMapper() {
    ObjectMapper mapper = new ObjectMapper();
    mapper.registerModule(new JsonNullableModule());
    mapper.registerModule(new JavaTimeModule());
    mapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
    return mapper;
  }

  /** Register JsonNullableModule as a bean. */
  @Bean
  public JsonNullableModule jsonNullableModule() {
    return new JsonNullableModule();
  }

  /**
   * Configure HTTP message converters to use our ObjectMapper with JsonNullableModule.
   *
   * <p>Uses deprecated Jackson 2 APIs for jackson-databind-nullable compatibility.
   */
  @Override
  public void configureMessageConverters(List<HttpMessageConverter<?>> converters) {
    // Add our custom Jackson converter first, so it takes precedence
    MappingJackson2HttpMessageConverter converter =
        new MappingJackson2HttpMessageConverter(objectMapper());
    converters.add(0, converter);
  }
}
