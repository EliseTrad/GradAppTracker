package com.gradapptracker.ui.controllers;

import com.gradapptracker.ui.services.UserServiceFx;
import com.gradapptracker.ui.utils.AlertUtils;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;

/**
 * JavaFX controller for user registration.
 * <p>
 * Expected FXML ids: txtName, txtEmail, txtPassword, btnRegister, btnGoToLogin
 */
public class RegisterController {

    @FXML
    private TextField txtName;

    @FXML
    private TextField txtEmail;

    @FXML
    private PasswordField txtPassword;

    @FXML
    private Button btnRegister;

    @FXML
    private Button btnGoToLogin;

    private final UserServiceFx userService = new UserServiceFx();

    @FXML
    private void onRegister() {
        String fullName = txtName == null ? null : txtName.getText();
        String email = txtEmail == null ? null : txtEmail.getText();
        String password = txtPassword == null ? null : txtPassword.getText();

        if (fullName == null || fullName.isBlank()) {
            AlertUtils.warn("Validation error", "Full name is required.");
            return;
        }
        if (email == null || email.isBlank()) {
            AlertUtils.warn("Validation error", "Email is required.");
            return;
        }
        if (password == null || password.isBlank()) {
            AlertUtils.warn("Validation error", "Password is required.");
            return;
        }

        if (btnRegister != null)
            btnRegister.setDisable(true);

        new Thread(() -> {
            try {
                userService.register(fullName.trim(), email.trim(), password);
                Platform.runLater(
                        () -> AlertUtils.info("Registration successful", "You can now log in with your credentials."));
            } catch (Exception ex) {
                String msg = ex.getMessage() == null ? ex.toString() : ex.getMessage();
                Platform.runLater(() -> AlertUtils.error("Registration failed", msg));
            } finally {
                if (btnRegister != null)
                    Platform.runLater(() -> btnRegister.setDisable(false));
            }
        }, "register-thread").start();
    }
}
