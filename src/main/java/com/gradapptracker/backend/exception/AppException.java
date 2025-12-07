package com.gradapptracker.backend.exception;

/**
 * Base application exception with custom HTTP status code.
 * <p>
 * Used for general application errors where a specific HTTP status code
 * needs to be returned. Subclasses provide more specific exception types.
 */
public class AppException extends RuntimeException {
    private final int code;

    public AppException(int code, String message) {
        super(message);
        this.code = code;
    }

    public int getCode() {
        return code;
    }
}
