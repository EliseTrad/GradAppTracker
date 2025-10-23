package com.gradapptracker.ui.controllers;

import com.gradapptracker.backend.user.dto.UserResponse;
import com.gradapptracker.backend.user.dto.UserUpdateRequest;
import com.gradapptracker.ui.services.UserServiceFx;
import com.gradapptracker.ui.utils.AlertUtils;
import com.gradapptracker.ui.utils.UserSession;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.TextField;

/**
 * Controller for user profile settings. Loads current user info and allows
 * updating name/email/password.
 */
public class ProfileController {

    @FXML
    private TextField txtName;

    @FXML
    private TextField txtEmail;

    @FXML
    private Button btnUpdate;

    private final UserServiceFx userService = new UserServiceFx();

    /**
     * Load current user's data into the form. Uses UserSession.getUserId().
     */
    public void loadUser() {
        Integer userId = UserSession.getInstance().getUserId();
        if (userId == null) {
            AlertUtils.warn("Not authenticated", "Please log in first.");
            return;
        }

        new Thread(() -> {
            try {
                UserResponse user = userService.getUser(userId);
                Platform.runLater(() -> {
                    if (txtName != null)
                        txtName.setText(user.getName());
                    if (txtEmail != null)
                        txtEmail.setText(user.getEmail());
                });
            } catch (Exception ex) {
                String msg = ex.getMessage() == null ? ex.toString() : ex.getMessage();
                Platform.runLater(() -> AlertUtils.error("Load user failed", msg));
            }
        }, "profile-load-thread").start();
    }

    /**
     * Update current user's profile with values from the form.
     */
    @FXML
    public void onUpdate() {
        Integer userId = UserSession.getInstance().getUserId();
        if (userId == null) {
            AlertUtils.warn("Not authenticated", "Please log in first.");
            return;
        }

        String name = txtName == null ? null : txtName.getText();
        String email = txtEmail == null ? null : txtEmail.getText();

        if (name == null || name.isBlank()) {
            AlertUtils.warn("Validation error", "Name is required.");
            return;
        }
        if (email == null || email.isBlank()) {
            AlertUtils.warn("Validation error", "Email is required.");
            return;
        }

        if (btnUpdate != null)
            btnUpdate.setDisable(true);

        UserUpdateRequest dto = new UserUpdateRequest();
        dto.setName(name.trim());
        dto.setEmail(email.trim());
        // Password not exposed in this view; no password update performed

        new Thread(() -> {
            try {
                UserResponse updated = userService.updateUser(userId, dto);
                // If email changed, update session email
                if (updated != null) {
                    UserSession.getInstance().login(updated.getUserId(), updated.getEmail(),
                            UserSession.getInstance().getJwt());
                }
                Platform.runLater(() -> AlertUtils.info("Update successful", "Your profile has been updated."));
            } catch (Exception ex) {
                String msg = ex.getMessage() == null ? ex.toString() : ex.getMessage();
                Platform.runLater(() -> AlertUtils.error("Update failed", msg));
            } finally {
                if (btnUpdate != null)
                    Platform.runLater(() -> btnUpdate.setDisable(false));
            }
        }, "profile-update-thread").start();
    }
}
