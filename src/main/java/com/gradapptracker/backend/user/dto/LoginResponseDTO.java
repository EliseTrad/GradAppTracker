package com.gradapptracker.backend.user.dto;

/**
 * Response DTO for user login.
 * <p>
 * Contains JWT token for authentication and user profile information.
 */
public class LoginResponseDTO {
    private String token;
    private UserResponse user;

    public LoginResponseDTO() {
    }

    public LoginResponseDTO(String token, UserResponse user) {
        this.token = token;
        this.user = user;
    }

    public String getToken() {
        return token;
    }

    public void setToken(String token) {
        this.token = token;
    }

    public UserResponse getUser() {
        return user;
    }

    public void setUser(UserResponse user) {
        this.user = user;
    }
}
