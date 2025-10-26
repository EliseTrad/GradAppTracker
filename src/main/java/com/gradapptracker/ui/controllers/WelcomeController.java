package com.gradapptracker.ui.controllers;

import com.gradapptracker.ui.services.UserServiceFx;
import com.gradapptracker.ui.services.AuthResult;
import com.gradapptracker.ui.utils.AlertUtils;
import com.gradapptracker.ui.utils.UserSession;
import javafx.fxml.FXML;
import javafx.scene.control.*;

public class WelcomeController {

    private final UserServiceFx userService = new UserServiceFx();

    @FXML private TabPane authTabPane;
    @FXML private Tab loginTab;
    @FXML private Tab registerTab;
    @FXML private TextField txtLoginEmail;
    @FXML private PasswordField txtLoginPassword;
    @FXML private TextField txtRegisterName;
    @FXML private TextField txtRegisterEmail;
    @FXML private PasswordField txtRegisterPassword;

    @FXML
    public void initialize() {
        authTabPane.getSelectionModel().select(loginTab);
        setupEnterKeyHandlers();
    }

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

    @FXML
    private void onSwitchToRegister() {
        authTabPane.getSelectionModel().select(registerTab);
    }

    @FXML
    private void onSwitchToLogin() {
        authTabPane.getSelectionModel().select(loginTab);
    }

    private void setupEnterKeyHandlers() {
        if (txtLoginPassword != null) txtLoginPassword.setOnAction(e -> onLogin());
        if (txtRegisterPassword != null) txtRegisterPassword.setOnAction(e -> onRegister());
    }
}
