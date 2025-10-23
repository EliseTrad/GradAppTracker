package com.gradapptracker.exception;

/**
 * DuplicateResourceException represents a conflict when creating/updating a
 * resource
 * that violates uniqueness constraints (e.g., duplicate email).
 *
 * Usage:
 * - Thrown by service layer when a uniqueness check fails, or repository
 * detects
 * a unique constraint violation and the service decides to rethrow domain-level
 * conflict.
 * - Results in HTTP 409 (Conflict).
 *
 * Example message: "Resource already exists".
 */
public class DuplicateResourceException extends RuntimeException {
    private final int code = 409;

    public DuplicateResourceException(String message) {
        super(message);
    }

    public int getCode() {
        return code;
    }
}
