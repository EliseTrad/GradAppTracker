package com.gradapptracker.ui.utils;

import java.util.Base64;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * Eager, thread-safe singleton that holds the current user's session
 * information for the JavaFX UI.
 * <p>
 * This implementation uses eager initialization which is inherently
 * thread-safe and simple.
 */
public final class UserSession {

    private static final UserSession INSTANCE = new UserSession();

    private Integer userId;
    private String email;
    private String jwt;

    private UserSession() {
        // prevent external instantiation
    }

    /**
     * Return the singleton instance.
     *
     * @return the single UserSession instance
     */
    public static UserSession getInstance() {
        return INSTANCE;
    }

    /**
     * Login and store the authenticated user's information.
     *
     * @param userId the user's id
     * @param email  the user's email
     * @param jwt    the JWT token
     */
    public synchronized void login(Integer userId, String email, String jwt) {
        this.userId = userId;
        this.email = email;
        this.jwt = jwt;

        // If userId is null, try to extract it from JWT token
        if (this.userId == null && jwt != null) {
            this.userId = extractUserIdFromJWT(jwt);
        }
    }

    /**
     * Extract userId from JWT token payload.
     * JWT format: header.payload.signature (all base64 encoded)
     */
    private Integer extractUserIdFromJWT(String jwt) {
        try {
            // Split JWT token into parts
            String[] parts = jwt.split("\\.");
            if (parts.length != 3) {
                return null;
            }

            // Decode the payload (second part)
            String payload = new String(Base64.getDecoder().decode(parts[1]));

            // Parse JSON payload
            ObjectMapper mapper = new ObjectMapper();
            JsonNode node = mapper.readTree(payload);

            // Try different possible field names for userId
            if (node.has("userId")) {
                return node.get("userId").asInt();
            } else if (node.has("sub")) {
                // 'sub' (subject) is a standard JWT claim that often contains user ID
                String sub = node.get("sub").asText();
                try {
                    return Integer.parseInt(sub);
                } catch (NumberFormatException e) {
                    // If sub is not a number, return null
                    return null;
                }
            } else if (node.has("id")) {
                return node.get("id").asInt();
            } else if (node.has("user_id")) {
                return node.get("user_id").asInt();
            }

            return null;
        } catch (Exception e) {
            System.err.println("Failed to extract userId from JWT: " + e.getMessage());
            return null;
        }
    }

    /**
     * Clear the stored session information.
     */
    public synchronized void clear() {
        this.userId = null;
        this.email = null;
        this.jwt = null;
    }

    /**
     * True if a user is currently authenticated.
     *
     * @return true if jwt is present, false otherwise
     */
    public synchronized boolean isAuthenticated() {
        return jwt != null && !jwt.isEmpty();
    }

    /**
     * Get the authenticated user's id.
     *
     * @return user id or null if not authenticated
     */
    public synchronized Integer getUserId() {
        return userId;
    }

    /**
     * Get the authenticated user's email.
     *
     * @return email or null if not authenticated
     */
    public synchronized String getEmail() {
        return email;
    }

    /**
     * Get the stored JWT token.
     *
     * @return jwt token or null if not set
     */
    public synchronized String getJwt() {
        return jwt;
    }

    /**
     * Set the JWT token for authenticated requests.
     *
     * @param token the JWT token
     */
    public synchronized void setToken(String token) {
        this.jwt = token;
    }

    /**
     * Set the user ID.
     *
     * @param userId the user's id
     */
    public synchronized void setUserId(Integer userId) {
        this.userId = userId;
    }

    /**
     * Set the user's email.
     *
     * @param email the user's email
     */
    public synchronized void setEmail(String email) {
        this.email = email;
    }

    /**
     * Manually trigger userId extraction from JWT if userId is null.
     * This can be used as a fallback if login didn't properly set userId.
     */
    public synchronized void refreshUserIdFromJWT() {
        if (this.userId == null && this.jwt != null) {
            this.userId = extractUserIdFromJWT(this.jwt);
            System.out.println("UserSession.refreshUserIdFromJWT() - extracted userId: " + this.userId);
        }
    }

}
