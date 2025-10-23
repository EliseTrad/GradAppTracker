package com.gradapptracker.ui.controllers;

import com.gradapptracker.ui.services.UserServiceFx;
import com.gradapptracker.ui.utils.AlertUtils;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;

/**
 * JavaFX controller for the login view.
 * <p>
 * Calls {@link UserServiceFx#login(String,String)} off the FX thread and
 * shows success/error alerts. The {@code UserServiceFx} will store the JWT
 * into {@code UserSession} on successful login.
 */
public class LoginController {

    @FXML
    private TextField emailField;

    @FXML
    private PasswordField passwordField;

    @FXML
    private Button loginButton;

    private final UserServiceFx userService = new UserServiceFx();

    @FXML
    private void onLogin() {
        String email = emailField == null ? null : emailField.getText();
        String password = passwordField == null ? null : passwordField.getText();

        if (email == null || email.isBlank() || password == null || password.isBlank()) {
            AlertUtils.warn("Validation error", "Email and password are required.");
            return;
        }

        if (loginButton != null)
            loginButton.setDisable(true);

        // run network call off the FX thread
        new Thread(() -> {
            try {
                userService.login(email.trim(), password);
                Platform.runLater(() -> {
                    AlertUtils.info("Login successful", "You are now logged in.");
                    // UI listeners (outside this controller) can react to UserSession changes
                });
            } catch (Exception ex) {
                String msg = ex.getMessage() == null ? ex.toString() : ex.getMessage();
                Platform.runLater(() -> AlertUtils.error("Login failed", msg));
            } finally {
                if (loginButton != null)
                    Platform.runLater(() -> loginButton.setDisable(false));
            }
        }, "login-thread").start();
    }
}
