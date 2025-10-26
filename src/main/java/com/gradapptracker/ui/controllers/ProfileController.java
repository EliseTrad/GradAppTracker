package com.gradapptracker.ui.controllers;

import com.gradapptracker.backend.user.dto.UserUpdateRequest;
import com.gradapptracker.ui.services.UserServiceFx;
import com.gradapptracker.ui.utils.AlertUtils;
import com.gradapptracker.ui.utils.AsyncUtils;
import com.gradapptracker.ui.utils.UserSession;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.scene.image.ImageView;

/**
 * Controller for user profile settings. Loads current user info and allows
 * updating name/email/password.
 */
public class ProfileController {

    @FXML
    private ImageView logoImage;

    @FXML
    private Button btnBackDashboard;

    @FXML
    private TextField txtName;

    @FXML
    private TextField txtEmail;

    @FXML
    private PasswordField txtCurrentPassword;

    @FXML
    private PasswordField txtNewPassword;

    @FXML
    private Button btnUpdate;

    @FXML
    private Button btnChangePassword;

    @FXML
    private Button btnSignOut;

    @FXML
    private Button btnDeleteAccount;

    private final UserServiceFx userService = new UserServiceFx();

    @FXML
    public void initialize() {
        // Load logo if available
        try {
            logoImage.setImage(new javafx.scene.image.Image(getClass().getResourceAsStream("/logo.png")));
        } catch (Exception e) {
            // Logo not found, skip
        }
        loadUser();
    }

    /**
     * Load current user's data into the form. Uses UserSession.getUserId().
     */
    public void loadUser() {
        Integer userId = UserSession.getInstance().getUserId();
        if (userId == null) {
            AlertUtils.warn("Not authenticated", "Please log in first.");
            return;
        }

        AsyncUtils.run(() -> userService.getUser(userId), user -> {
            if (txtName != null)
                txtName.setText(user.getName());
            if (txtEmail != null)
                txtEmail.setText(user.getEmail());
        }, ex -> AlertUtils.errorFromBackend(ex.getMessage()));
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

        if (btnUpdate != null)
            btnUpdate.setDisable(true);

        UserUpdateRequest dto = new UserUpdateRequest();
        dto.setName(name != null ? name.trim() : null);
        dto.setEmail(email != null ? email.trim() : null);
        // Password not exposed in this view; no password update performed

        AsyncUtils.run(() -> userService.updateUser(userId, dto), updated -> {
            // If email changed, update session email
            if (updated != null) {
                UserSession.getInstance().login(updated.getUserId(), updated.getEmail(),
                        UserSession.getInstance().getJwt());
            }
            AlertUtils.info("Update successful", "Your profile has been updated.");
        }, ex -> AlertUtils.errorFromBackend(ex.getMessage()), () -> {
            if (btnUpdate != null)
                btnUpdate.setDisable(false);
        });
    }

    @FXML
    private void onBackDashboard() {
        MainLayoutController.getInstance().setContent("/com/gradapptracker/ui/views/ProgramView.fxml");
    }

    @FXML
    public void onChangePassword() {
        Integer userId = UserSession.getInstance().getUserId();
        if (userId == null) {
            AlertUtils.warn("Not authenticated", "Please log in first.");
            return;
        }

        String currentPassword = txtCurrentPassword.getText();
        String newPassword = txtNewPassword.getText();

        if (currentPassword.trim().isEmpty() || newPassword.trim().isEmpty()) {
            AlertUtils.warn("Invalid input", "Both passwords are required.");
            return;
        }

        if (btnChangePassword != null)
            btnChangePassword.setDisable(true);

        AsyncUtils.run(() -> {
            userService.changePassword(currentPassword.trim(), newPassword.trim());
            return null;
        }, unused -> {
            AlertUtils.info("Password changed", "Your password has been updated.");
            txtCurrentPassword.clear();
            txtNewPassword.clear();
        }, ex -> AlertUtils.errorFromBackend(ex.getMessage()), () -> {
            if (btnChangePassword != null)
                btnChangePassword.setDisable(false);
        });
    }

    @FXML
    public void onSignOut() {
        // Use the main layout's logout handler which handles everything properly
        MainLayoutController.getInstance().onLogout();
    }

    @FXML
    public void onDeleteAccount() {
        Integer userId = UserSession.getInstance().getUserId();
        if (userId == null) {
            AlertUtils.warn("Not authenticated", "Please log in first.");
            return;
        }

        if (!AlertUtils.confirm("Delete Account",
                "Are you sure you want to delete your account? This action cannot be undone.")) {
            return;
        }

        if (btnDeleteAccount != null)
            btnDeleteAccount.setDisable(true);

        AsyncUtils.run(() -> {
            userService.deleteUser(userId);
            return null;
        }, unused -> {
            UserSession.getInstance().clear();
            AlertUtils.info("Account deleted", "Your account has been deleted.");
            MainLayoutController.getInstance().setContent("/com/gradapptracker/ui/views/WelcomeView.fxml");
        }, ex -> AlertUtils.errorFromBackend(ex.getMessage()), () -> {
            if (btnDeleteAccount != null)
                btnDeleteAccount.setDisable(false);
        });
    }
}
