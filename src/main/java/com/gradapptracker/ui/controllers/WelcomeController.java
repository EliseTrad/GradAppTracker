package com.gradapptracker.ui.controllers;

import com.gradapptracker.ui.services.UserServiceFx;
import com.gradapptracker.ui.services.ServiceLocator;
import com.gradapptracker.ui.services.AuthResult;
import com.gradapptracker.ui.utils.AlertUtils;
import com.gradapptracker.ui.utils.UserSession;
import javafx.fxml.FXML;
import javafx.scene.control.*;

/**
 * Controller for the welcome/authentication view.
 * <p>
 * Handles user login and registration through a tabbed interface.
 * Successful authentication stores JWT token in UserSession and navigates
 * to the main programs dashboard.
 */
public class WelcomeController {

    private final UserServiceFx userService = ServiceLocator.getInstance().getUserService();

    @FXML
    private TabPane authTabPane;
    @FXML
    private Tab loginTab;
    @FXML
    private Tab registerTab;
    @FXML
    private TextField txtLoginEmail;
    @FXML
    private PasswordField txtLoginPassword;
    @FXML
    private TextField txtRegisterName;
    @FXML
    private TextField txtRegisterEmail;
    @FXML
    private PasswordField txtRegisterPassword;

    /**
     * Initialize the controller after FXML components are loaded.
     * Sets the login tab as the default active tab and configures Enter key
     * handlers.
     */
    @FXML
    public void initialize() {
        authTabPane.getSelectionModel().select(loginTab);
        setupEnterKeyHandlers();
    }

    /**
     * Handle user login action.
     * Validates credentials via backend API, stores session on success,
     * shows navigation panel, and navigates to the main dashboard.
     * Displays error message if login fails.
     */
    @FXML
    private void onLogin() {
        // Trim input to avoid blank/whitespace errors
        String email = txtLoginEmail.getText().trim();
        String password = txtLoginPassword.getText().trim();

        AuthResult result = userService.login(email, password);

        if (result.isSuccess()) {
            UserSession.getInstance().login(result.getUserId(), email, result.getToken());
            MainLayoutController.getInstance().showNavigation(email.contains("@") ? email.split("@")[0] : email);
            MainLayoutController.getInstance().navigateToDashboard();
        } else {
            AlertUtils.errorFromBackend(result.getMessage());
        }
    }

    /**
     * Handle user registration action.
     * Creates a new user account via backend API, then automatically logs in
     * the new user on success. Navigates to the main dashboard after successful
     * registration and login. Displays error message if registration fails.
     */
    @FXML
    private void onRegister() {
        // Trim input to avoid blank/whitespace errors
        String name = txtRegisterName.getText().trim();
        String email = txtRegisterEmail.getText().trim();
        String password = txtRegisterPassword.getText().trim();

        AuthResult result = userService.register(name, email, password);

        if (result.isSuccess()) {
            // Auto-login after register
            AuthResult loginResult = userService.login(email, password);
            if (loginResult.isSuccess()) {
                UserSession.getInstance().login(loginResult.getUserId(), email, loginResult.getToken());
                MainLayoutController.getInstance().showNavigation(email.contains("@") ? email.split("@")[0] : email);
                MainLayoutController.getInstance().navigateToDashboard();

                // Clear register fields
                txtRegisterName.clear();
                txtRegisterEmail.clear();
                txtRegisterPassword.clear();
            } else {
                AlertUtils.errorFromBackend(loginResult.getMessage());
                authTabPane.getSelectionModel().select(loginTab);
                txtLoginEmail.setText(email);
            }
        } else {
            AlertUtils.errorFromBackend(result.getMessage());
        }
    }

    /**
     * Switch to the registration tab.
     * Called when user clicks the "Create Account" link from the login tab.
     */
    @FXML
    private void onSwitchToRegister() {
        authTabPane.getSelectionModel().select(registerTab);
    }

    /**
     * Switch to the login tab.
     * Called when user clicks the "Sign In" link from the registration tab.
     */
    @FXML
    private void onSwitchToLogin() {
        authTabPane.getSelectionModel().select(loginTab);
    }

    /**
     * Configure Enter key press handlers for password fields.
     * Pressing Enter in the login password field triggers login,
     * and pressing Enter in the registration password field triggers registration.
     */
    private void setupEnterKeyHandlers() {
        if (txtLoginPassword != null)
            txtLoginPassword.setOnAction(e -> onLogin());
        if (txtRegisterPassword != null)
            txtRegisterPassword.setOnAction(e -> onRegister());
    }
}
