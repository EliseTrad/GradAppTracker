package com.gradapptracker.ui.utils;

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
}
