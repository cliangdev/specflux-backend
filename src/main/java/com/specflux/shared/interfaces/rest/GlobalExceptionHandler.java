package com.specflux.shared.interfaces.rest;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import com.specflux.api.generated.model.ErrorResponseDto;
import com.specflux.api.generated.model.FieldErrorDto;

import jakarta.persistence.EntityNotFoundException;

/** Global exception handler for REST API endpoints. */
@RestControllerAdvice
public class GlobalExceptionHandler {

  private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

  @ExceptionHandler(EntityNotFoundException.class)
  public ResponseEntity<ErrorResponseDto> handleEntityNotFound(EntityNotFoundException ex) {
    ErrorResponseDto error = new ErrorResponseDto();
    error.setError(ex.getMessage());
    error.setCode("NOT_FOUND");
    return ResponseEntity.status(HttpStatus.NOT_FOUND).body(error);
  }

  @ExceptionHandler(IllegalArgumentException.class)
  public ResponseEntity<ErrorResponseDto> handleIllegalArgument(IllegalArgumentException ex) {
    ErrorResponseDto error = new ErrorResponseDto();
    error.setError(ex.getMessage());
    error.setCode("BAD_REQUEST");
    return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
  }

  @ExceptionHandler(MethodArgumentNotValidException.class)
  public ResponseEntity<ErrorResponseDto> handleValidationErrors(
      MethodArgumentNotValidException ex) {
    List<FieldErrorDto> fieldErrors =
        ex.getBindingResult().getFieldErrors().stream()
            .map(
                fe -> {
                  FieldErrorDto fieldError = new FieldErrorDto();
                  fieldError.setField(fe.getField());
                  fieldError.setMessage(fe.getDefaultMessage());
                  return fieldError;
                })
            .toList();

    ErrorResponseDto error = new ErrorResponseDto();
    error.setError("Validation failed");
    error.setCode("VALIDATION_ERROR");
    error.setDetails(fieldErrors);
    return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
  }

  @ExceptionHandler(ResourceConflictException.class)
  public ResponseEntity<ErrorResponseDto> handleConflict(ResourceConflictException ex) {
    ErrorResponseDto error = new ErrorResponseDto();
    error.setError(ex.getMessage());
    error.setCode("CONFLICT");
    return ResponseEntity.status(HttpStatus.CONFLICT).body(error);
  }

  @ExceptionHandler(AccessDeniedException.class)
  public ResponseEntity<ErrorResponseDto> handleAccessDenied(AccessDeniedException ex) {
    ErrorResponseDto error = new ErrorResponseDto();
    error.setError(ex.getMessage());
    error.setCode("FORBIDDEN");
    return ResponseEntity.status(HttpStatus.FORBIDDEN).body(error);
  }

  @ExceptionHandler(Exception.class)
  public ResponseEntity<ErrorResponseDto> handleGenericException(Exception ex) {
    log.error("Unhandled exception", ex);
    ErrorResponseDto error = new ErrorResponseDto();
    error.setError("An unexpected error occurred");
    error.setCode("INTERNAL_ERROR");
    return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
  }

  /** Exception thrown when a resource already exists (e.g., duplicate project key). */
  public static class ResourceConflictException extends RuntimeException {
    public ResourceConflictException(String message) {
      super(message);
    }
  }

  /** Exception thrown when user doesn't have permission to access a resource. */
  public static class AccessDeniedException extends RuntimeException {
    public AccessDeniedException(String message) {
      super(message);
    }
  }
}
