package com.gradapptracker.exception;

/**
 * ValidationException indicates the request failed input or business
 * validation.
 *
 * Usage:
 * - Thrown by service layer when incoming data is invalid or violates business
 * rules.
 * - Results in HTTP 400 (Bad Request).
 *
 * Example messages: "email is invalid", "name is required".
 */
public class ValidationException extends RuntimeException {
    private final int code = 400;

    public ValidationException(String message) {
        super(message);
    }

    public int getCode() {
        return code;
    }
}
