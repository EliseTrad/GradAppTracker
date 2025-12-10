package com.gradapptracker.ui.services;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.gradapptracker.ui.utils.UserSession;
import com.gradapptracker.backend.user.dto.UserResponse;
import com.gradapptracker.backend.user.dto.UserUpdateRequest;

import java.net.http.HttpResponse;

public class UserServiceFx extends ApiClient {

    private final ObjectMapper mapper = new ObjectMapper();

    /**
     * Authenticate a user with email and password.
     * 
     * @param email    the user's email address
     * @param password the user's plain-text password
     * @return AuthResult containing success status, JWT token, userId, and message
     */
    // ----------------- LOGIN -----------------
    public AuthResult login(String email, String password) {
        try {
            String json = String.format("{\"email\":\"%s\",\"password\":\"%s\"}",
                    escape(email), escape(password));
            HttpResponse<String> resp = POST("/users/login", json, false);
            int status = resp.statusCode();
            String body = resp.body();

            if (status / 100 == 2) {
                JsonNode node = mapper.readTree(body);
                String token = node.path("token").asText(null);
                int userId = node.path("user").path("userId").asInt();
                return new AuthResult(true, "Login successful", token, userId);
            } else {
                String message = extractBackendErrorMessage(body);
                return new AuthResult(false, message, null, null);
            }
        } catch (Exception e) {
            return new AuthResult(false, extractBackendErrorMessage(e.getMessage()), null, null);
        }
    }

    /**
     * Register a new user account and automatically log them in.
     * 
     * @param fullName the user's full name
     * @param email    the user's email address
     * @param password the user's password
     * @return AuthResult containing success status, JWT token, userId, and message
     */
    // ----------------- REGISTER -----------------
    public AuthResult register(String fullName, String email, String password) {
        try {
            String json = String.format("{\"name\":\"%s\",\"email\":\"%s\",\"password\":\"%s\"}",
                    escape(fullName), escape(email), escape(password));
            HttpResponse<String> resp = POST("/users/register", json, false);
            int status = resp.statusCode();
            String body = resp.body();

            if (status / 100 == 2) {
                // Auto-login immediately
                return login(email, password);
            } else {
                String message = extractBackendErrorMessage(body);
                return new AuthResult(false, message, null, null);
            }
        } catch (Exception e) {
            return new AuthResult(false, extractBackendErrorMessage(e.getMessage()), null, null);
        }
    }

    /**
     * Retrieve user information by ID.
     * 
     * @param id the user ID to retrieve
     * @return UserResponse containing the user's information
     * @throws RuntimeException if the request fails or user doesn't exist
     */
    // ----------------- GET USER -----------------
    public UserResponse getUser(Integer id) {
        try {
            HttpResponse<String> resp = GET("/users/" + id, true);
            int status = resp.statusCode();
            String body = resp.body();
            if (status / 100 == 2) {
                return mapper.readValue(body, UserResponse.class);
            }
            throw new RuntimeException("Get user failed: " + body);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Update user information (name and/or email).
     * 
     * @param id  the user ID to update
     * @param dto the update payload containing fields to modify
     * @return UserResponse containing the updated user information
     * @throws RuntimeException if the update fails
     */
    // ----------------- UPDATE USER -----------------
    public UserResponse updateUser(Integer id, UserUpdateRequest dto) {
        try {
            String json = mapper.writeValueAsString(dto);
            HttpResponse<String> resp = PUT("/users/" + id, json, true);
            int status = resp.statusCode();
            String body = resp.body();
            if (status / 100 == 2) {
                return mapper.readValue(body, UserResponse.class);
            }
            throw new RuntimeException("Update user failed: " + body);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Change the current user's password.
     * Requires the user to be logged in (valid UserSession).
     * 
     * @param currentPassword the user's current password for verification
     * @param newPassword     the new password to set
     * @throws RuntimeException if user is not logged in or password change fails
     */
    // ----------------- CHANGE PASSWORD -----------------
    public void changePassword(String currentPassword, String newPassword) {
        try {
            Integer userId = UserSession.getInstance().getUserId();
            if (userId == null)
                throw new RuntimeException("User not logged in");

            String path = String.format("/users/%d/password?old=%s&new=%s",
                    userId,
                    java.net.URLEncoder.encode(currentPassword, "UTF-8"),
                    java.net.URLEncoder.encode(newPassword, "UTF-8"));
            HttpResponse<String> resp = POST(path, "", true);
            if (resp.statusCode() / 100 != 2) {
                throw new RuntimeException("Change password failed: " + resp.body());
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Delete a user account.
     * 
     * @param id the user ID to delete
     * @throws RuntimeException if the deletion fails
     */
    // ----------------- DELETE USER -----------------
    public void deleteUser(Integer id) {
        try {
            HttpResponse<String> resp = DELETE("/users/" + id, true);
            if (resp.statusCode() / 100 != 2) {
                throw new RuntimeException("Delete user failed: " + resp.body());
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Escape special characters in strings for JSON formatting.
     * 
     * @param s the string to escape
     * @return escaped string safe for JSON
     */
    // ----------------- HELPERS -----------------
    private static String escape(String s) {
        return s == null ? "" : s.replace("\\", "\\\\").replace("\"", "\\\"");
    }

    /**
     * Extract user-friendly error message from backend JSON response.
     * Attempts to parse JSON error format from backend and extract the message
     * field
     * or validation errors array.
     * 
     * @param fullMessage the raw error message or JSON from backend
     * @return user-friendly error message string
     */
    private static String extractBackendErrorMessage(String fullMessage) {
        if (fullMessage == null || fullMessage.isBlank()) {
            return "An error occurred";
        }

        try {
            ObjectMapper mapper = new ObjectMapper();
            JsonNode node = mapper.readTree(fullMessage);

            // Case 1: single message field (old style)
            if (node.has("message")) {
                String msg = node.get("message").asText();
                if (msg != null && !msg.isBlank())
                    return msg;
            }

            // Case 2: validation errors array
            if (node.has("errors") && node.get("errors").isArray()) {
                StringBuilder sb = new StringBuilder();
                for (JsonNode err : node.get("errors")) {
                    String field = err.path("field").asText();
                    String message = err.path("defaultMessage").asText();
                    if (!field.isBlank() && !message.isBlank()) {
                        sb.append(field).append(": ").append(message).append("\n");
                    }
                }
                if (sb.length() > 0)
                    return sb.toString().trim();
            }

        } catch (Exception e) {
            // Fall back to raw message
        }

        return fullMessage;
    }

}
