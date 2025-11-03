package com.gradapptracker.backend.exception;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.validation.BindException;
import org.springframework.validation.FieldError;
import jakarta.validation.ConstraintViolationException;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

@ControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger logger = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    /**
     * Centralized exception handler.
     *
     * Behavior:
     * If the thrown exception is one of the custom domain exceptions
     * (ValidationException,
     * UnauthorizedException, NotFoundException, DuplicateResourceException,
     * EmailAlreadyExistsException,
     * DocumentReferencedException), this handler uses the exception's getCode() and
     * message to form
     * the response.
     * For any other runtime exception, it logs a developer-facing stacktrace and
     * returns a generic
     * 500 Internal Server Error message to the client.
     *
     * Response JSON shape: { status: <code>, message: <message>, timestamp: <iso> }
     */
    @ExceptionHandler(RuntimeException.class)
    public ResponseEntity<Map<String, Object>> handleRuntimeException(RuntimeException ex) {
        // Specific known custom exceptions
        int code = 500;
        String message = "Internal server error";

        if (ex instanceof ValidationException) {
            code = ((ValidationException) ex).getCode();
            message = ex.getMessage();
        } else if (ex instanceof UnauthorizedException) {
            code = ((UnauthorizedException) ex).getCode();
            message = ex.getMessage();
        } else if (ex instanceof NotFoundException) {
            code = ((NotFoundException) ex).getCode();
            message = ex.getMessage();
        } else if (ex instanceof DuplicateResourceException) {
            code = ((DuplicateResourceException) ex).getCode();
            message = ex.getMessage();
        } else if (ex instanceof com.gradapptracker.backend.exception.ForbiddenException) {
            code = ((com.gradapptracker.backend.exception.ForbiddenException) ex).getCode();
            message = ex.getMessage();
        } else if (ex instanceof EmailAlreadyExistsException) {
            code = ((EmailAlreadyExistsException) ex).getCode();
            message = ex.getMessage();
        } else if (ex instanceof DocumentReferencedException) {
            code = ((DocumentReferencedException) ex).getCode();
            message = ex.getMessage();
        } else {
            // Unknown runtime exception -> log developer-facing message
            logger.error("Unhandled runtime exception in controller/service/repo: {}", ex.getMessage(), ex);
        }

        HttpStatus status = HttpStatus.resolve(code);
        if (status == null)
            status = HttpStatus.INTERNAL_SERVER_ERROR;

        Map<String, Object> body = new HashMap<>();
        body.put("status", status.value());
        body.put("message", message);
        body.put("timestamp", Instant.now().toString());

        return ResponseEntity.status(status).body(body);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String, Object>> handleException(Exception ex) {
        logger.error("Unhandled exception: {}", ex.getMessage(), ex);

        Map<String, Object> body = new HashMap<>();
        body.put("status", HttpStatus.INTERNAL_SERVER_ERROR.value());
        body.put("message", "Internal server error");
        body.put("timestamp", Instant.now().toString());

        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(body);
    }

    /**
     * Handle validation errors for @RequestBody objects.
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, Object>> handleMethodArgumentNotValid(MethodArgumentNotValidException ex) {
        Map<String, Object> body = new HashMap<>();
        body.put("status", HttpStatus.BAD_REQUEST.value());

        // Aggregate all field errors into a single readable string
        String aggregatedMessage = ex.getBindingResult().getFieldErrors().stream()
                .map(fe -> fe.getField() + ": " + fe.getDefaultMessage())
                .reduce((a, b) -> a + "; " + b)
                .orElse("Validation failed");

        body.put("message", aggregatedMessage);

        // Optional: keep the errors array for frontend debugging
        var errors = ex.getBindingResult().getFieldErrors().stream().map(fe -> {
            Map<String, Object> m = new HashMap<>();
            m.put("field", fe.getField());
            m.put("rejectedValue", fe.getRejectedValue());
            m.put("message", fe.getDefaultMessage());
            return m;
        }).toList();

        body.put("errors", errors);
        body.put("timestamp", Instant.now().toString());

        return ResponseEntity.badRequest().body(body);
    }

    /**
     * Handle validation errors for method parameters
     * (e.g., @RequestParam, @PathVariable)
     */
    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<Map<String, Object>> handleConstraintViolation(ConstraintViolationException ex) {
        Map<String, Object> body = new HashMap<>();
        body.put("status", HttpStatus.BAD_REQUEST.value());
        body.put("message", "Validation failed");

        var errors = ex.getConstraintViolations().stream().map(cv -> {
            Map<String, Object> m = new HashMap<>();
            String path = cv.getPropertyPath() == null ? null : cv.getPropertyPath().toString();
            m.put("path", path);
            m.put("invalidValue", cv.getInvalidValue());
            m.put("message", cv.getMessage());
            return m;
        }).toList();

        body.put("errors", errors);
        body.put("timestamp", Instant.now().toString());

        return ResponseEntity.badRequest().body(body);
    }

    @ExceptionHandler({ BindException.class })
    public ResponseEntity<Map<String, Object>> handleBindException(BindException ex) {
        Map<String, Object> body = new HashMap<>();
        body.put("status", HttpStatus.BAD_REQUEST.value());
        body.put("message", "Binding failed");

        var errors = ex.getFieldErrors().stream().map((FieldError fe) -> {
            Map<String, Object> m = new HashMap<>();
            m.put("field", fe.getField());
            m.put("rejectedValue", fe.getRejectedValue());
            m.put("message", fe.getDefaultMessage());
            return m;
        }).toList();

        body.put("errors", errors);
        body.put("timestamp", Instant.now().toString());
        return ResponseEntity.badRequest().body(body);
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<Map<String, Object>> handleHttpMessageNotReadable(HttpMessageNotReadableException ex) {
        Map<String, Object> body = new HashMap<>();
        body.put("status", HttpStatus.BAD_REQUEST.value());
        body.put("message", "Malformed JSON request");
        body.put("timestamp", Instant.now().toString());
        return ResponseEntity.badRequest().body(body);
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<Map<String, Object>> handleTypeMismatch(MethodArgumentTypeMismatchException ex) {
        Map<String, Object> body = new HashMap<>();
        body.put("status", HttpStatus.BAD_REQUEST.value());
        body.put("message", String.format("Parameter '%s' could not be converted to required type", ex.getName()));
        body.put("timestamp", Instant.now().toString());
        return ResponseEntity.badRequest().body(body);
    }

    @ExceptionHandler(MissingServletRequestParameterException.class)
    public ResponseEntity<Map<String, Object>> handleMissingParam(MissingServletRequestParameterException ex) {
        Map<String, Object> body = new HashMap<>();
        body.put("status", HttpStatus.BAD_REQUEST.value());
        body.put("message", String.format("Missing required parameter '%s'", ex.getParameterName()));
        body.put("timestamp", Instant.now().toString());
        return ResponseEntity.badRequest().body(body);
    }
}
