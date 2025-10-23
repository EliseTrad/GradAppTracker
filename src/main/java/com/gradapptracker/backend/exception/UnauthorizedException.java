package com.gradapptracker.backend.exception;

/**
 * UnauthorizedException indicates authentication or simple authorization
 * failed.
 *
 * Usage:
 * - Thrown by authentication logic or service layer when credentials are
 * invalid
 * or the caller is not allowed to perform an action.
 * - Results in HTTP 401 (Unauthorized).
 *
 * Example messages: "invalid credentials", "not authorized to perform this
 * action".
 */
public class UnauthorizedException extends RuntimeException {
    private final int code = 401;

    public UnauthorizedException(String message) {
        super(message);
    }

    public int getCode() {
        return code;
    }
}

