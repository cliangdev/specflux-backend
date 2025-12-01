package com.specflux.shared.interfaces.rest;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import com.specflux.api.generated.model.ErrorResponse;
import com.specflux.api.generated.model.FieldError;

import jakarta.persistence.EntityNotFoundException;

/** Global exception handler for REST API endpoints. */
@RestControllerAdvice
public class GlobalExceptionHandler {

  private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

  @ExceptionHandler(EntityNotFoundException.class)
  public ResponseEntity<ErrorResponse> handleEntityNotFound(EntityNotFoundException ex) {
    ErrorResponse error = new ErrorResponse();
    error.setError(ex.getMessage());
    error.setCode("NOT_FOUND");
    return ResponseEntity.status(HttpStatus.NOT_FOUND).body(error);
  }

  @ExceptionHandler(IllegalArgumentException.class)
  public ResponseEntity<ErrorResponse> handleIllegalArgument(IllegalArgumentException ex) {
    ErrorResponse error = new ErrorResponse();
    error.setError(ex.getMessage());
    error.setCode("BAD_REQUEST");
    return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
  }

  @ExceptionHandler(MethodArgumentNotValidException.class)
  public ResponseEntity<ErrorResponse> handleValidationErrors(MethodArgumentNotValidException ex) {
    List<FieldError> fieldErrors =
        ex.getBindingResult().getFieldErrors().stream()
            .map(
                fe -> {
                  FieldError fieldError = new FieldError();
                  fieldError.setField(fe.getField());
                  fieldError.setMessage(fe.getDefaultMessage());
                  return fieldError;
                })
            .toList();

    ErrorResponse error = new ErrorResponse();
    error.setError("Validation failed");
    error.setCode("VALIDATION_ERROR");
    error.setDetails(fieldErrors);
    return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
  }

  @ExceptionHandler(ResourceConflictException.class)
  public ResponseEntity<ErrorResponse> handleConflict(ResourceConflictException ex) {
    ErrorResponse error = new ErrorResponse();
    error.setError(ex.getMessage());
    error.setCode("CONFLICT");
    return ResponseEntity.status(HttpStatus.CONFLICT).body(error);
  }

  @ExceptionHandler(AccessDeniedException.class)
  public ResponseEntity<ErrorResponse> handleAccessDenied(AccessDeniedException ex) {
    ErrorResponse error = new ErrorResponse();
    error.setError(ex.getMessage());
    error.setCode("FORBIDDEN");
    return ResponseEntity.status(HttpStatus.FORBIDDEN).body(error);
  }

  @ExceptionHandler(Exception.class)
  public ResponseEntity<ErrorResponse> handleGenericException(Exception ex) {
    log.error("Unhandled exception", ex);
    ErrorResponse error = new ErrorResponse();
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
