package com.gradapptracker.ui.services;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.gradapptracker.ui.utils.UserSession;
import com.gradapptracker.backend.user.dto.UserResponse;
import com.gradapptracker.backend.user.dto.UserUpdateRequest;

import java.net.http.HttpResponse;

/**
 * UI-facing service for user operations (login/register) that talks to the
 * backend API and stores authentication into {@link UserSession} on success.
 */
public class UserServiceFx extends ApiClient {

    private final ObjectMapper mapper = new ObjectMapper();

    /**
     * Login with email and password. On success stores token and userId in
     * {@link UserSession}.
     *
     * @param email    user's email
     * @param password user's password
     */
    public void login(String email, String password) {
        try {
            String json = String.format("{\"email\":\"%s\",\"password\":\"%s\"}",
                    escape(email), escape(password));
            HttpResponse<String> resp = POST("/auth/login", json, false);
            int status = resp.statusCode();
            String body = resp.body();
            if (status == 200) {
                JsonNode node = mapper.readTree(body);
                String token = node.path("token").asText(null);
                Integer userId = node.path("userId").isMissingNode() ? null : node.path("userId").asInt();
                if (token == null) {
                    throw new RuntimeException("Login failed: missing token in response");
                }
                UserSession.getInstance().login(userId, email, token);
                return;
            } else if (status == 401) {
                throw new RuntimeException("Invalid credentials");
            } else {
                throw new RuntimeException("Login failed: " + body);
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Register a new user. Throws RuntimeException on failure.
     *
     * @param fullName user's full name
     * @param email    user's email
     * @param password user's password
     */
    public void register(String fullName, String email, String password) {
        try {
            String json = String.format("{\"fullName\":\"%s\",\"email\":\"%s\",\"password\":\"%s\"}",
                    escape(fullName), escape(email), escape(password));
            HttpResponse<String> resp = POST("/auth/register", json, false);
            int status = resp.statusCode();
            String body = resp.body();
            if (status == 201) {
                return;
            } else {
                throw new RuntimeException("Register failed: " + body);
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Fetch a user by id using authenticated request.
     *
     * @param id user id
     * @return UserResponse on success
     */
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
     * Update a user by id. Requires authentication.
     *
     * @param id  user id
     * @param dto update payload
     * @return updated UserResponse
     */
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

    private static String escape(String s) {
        if (s == null)
            return "";
        return s.replace("\\", "\\\\").replace("\"", "\\\"");
    }
}
