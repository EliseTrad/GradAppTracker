package com.gradapptracker.exception;

/**
 * ForbiddenException indicates the current principal is not allowed to perform
 * the action.
 * Results in HTTP 403.
 */
public class ForbiddenException extends RuntimeException {
    private final int code = 403;

    public ForbiddenException(String message) {
        super(message);
    }

    public int getCode() {
        return code;
    }
}
