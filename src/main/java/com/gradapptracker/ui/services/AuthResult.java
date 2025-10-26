package com.gradapptracker.ui.services;

/**
 * Simple result wrapper for authentication operations from UI services.
 */
public class AuthResult {
    private final boolean success;
    private final String message;
    private final String token;
    private final Integer userId;

    public AuthResult(boolean success, String message, String token, Integer userId) {
        this.success = success;
        this.message = message;
        this.token = token;
        this.userId = userId;
    }

    public boolean isSuccess() {
        return success;
    }

    public String getMessage() {
        return message;
    }

    public String getToken() {
        return token;
    }

    public Integer getUserId() {
        return userId;
    }
}
