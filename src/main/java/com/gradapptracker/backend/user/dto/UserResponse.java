package com.gradapptracker.backend.user.dto;

/**
 * Response DTO for user information.
 * <p>
 * Contains safe user data for API responses, excluding sensitive information
 * like passwords. Used after registration, login, and profile updates.
 */
public class UserResponse {
    private Integer userId;
    private String name;
    private String email;

    public UserResponse() {
    }

    public UserResponse(Integer userId, String name, String email) {
        this.userId = userId;
        this.name = name;
        this.email = email;
    }

    public Integer getUserId() {
        return userId;
    }

    public void setUserId(Integer userId) {
        this.userId = userId;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }
}
