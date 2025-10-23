package com.gradapptracker.backend.exception;

/**
 * NotFoundException indicates that a requested resource could not be located.
 *
 * Usage:
 * - Thrown by service layer when an entity (user, document, program, etc.)
 * cannot be found for a given identifier.
 * - Results in HTTP 404 (Not Found).
 *
 * Example message: "User not found with id: 123".
 */
public class NotFoundException extends RuntimeException {
    private final int code = 404;

    public NotFoundException(String message) {
        super(message);
    }

    public int getCode() {
        return code;
    }
}

