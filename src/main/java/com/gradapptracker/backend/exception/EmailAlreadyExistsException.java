package com.gradapptracker.backend.exception;

/**
 * EmailAlreadyExistsException is a domain-specific conflict used when a user
 * attempts to register or update an account with an email that already exists.
 *
 * Usage:
 * - Thrown by service layer when an email uniqueness check fails.
 * - Results in HTTP 409 (Conflict).
 *
 * Example message: "Email already exists: foo@example.com".
 */
public class EmailAlreadyExistsException extends RuntimeException {
    private final int code = 409;

    public EmailAlreadyExistsException(String message) {
        super(message);
    }

    public int getCode() {
        return code;
    }
}

