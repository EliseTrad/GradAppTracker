package com.gradapptracker.ui.services;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.gradapptracker.ui.utils.UserSession;
import com.gradapptracker.backend.user.dto.UserResponse;
import com.gradapptracker.backend.user.dto.UserUpdateRequest;

import java.net.http.HttpResponse;

public class UserServiceFx extends ApiClient {

    private final ObjectMapper mapper = new ObjectMapper();

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

    // ----------------- HELPERS -----------------
    private static String escape(String s) {
        return s == null ? "" : s.replace("\\", "\\\\").replace("\"", "\\\"");
    }

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
