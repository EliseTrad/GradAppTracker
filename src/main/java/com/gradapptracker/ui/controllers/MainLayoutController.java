package com.gradapptracker.ui.controllers;

import com.gradapptracker.ui.utils.AlertUtils;
import com.gradapptracker.ui.utils.UserSession;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;

import java.io.IOException;

/**
 * MainLayoutController manages the main application layout including:
 * - Header with logo and user info
 * - Navigation panel (shown after login)
 * - Content area for page switching
 * - Footer
 */
public class MainLayoutController {

    private static MainLayoutController instance;

    // Header components
    @FXML
    private ImageView logoImage;

    @FXML
    private Label logoPlaceholder;

    @FXML
    private HBox userInfoBox;

    @FXML
    private Label lblUserName;

    @FXML
    private Button btnHeaderLogout;

    // Navigation panel
    @FXML
    private VBox navigationPanel;

    @FXML
    private Button btnNavDashboard;

    @FXML
    private Button btnNav3DDashboard;

    @FXML
    private Button btnNavDocuments;

    @FXML
    private Button btnNavProfile;

    @FXML
    private Button btnNavLogout;

    // Content area
    @FXML
    private StackPane contentPane;

    // Current active navigation button
    private Button activeNavButton = null;

    public MainLayoutController() {
        instance = this;
    }

    public static MainLayoutController getInstance() {
        return instance;
    }

    @FXML
    private void initialize() {
        loadLogo();
    }

    /**
     * Load the logo image from resources.
     * If logo.png is not found, the placeholder text "LOGO" will be displayed.
     * 
     * To add your logo:
     * 1. Create folder: src/main/resources/images/
     * 2. Place your logo as: logo.png
     * 3. Recommended size: 150x150px PNG with transparent background
     */
    private void loadLogo() {
        try {
            String logoPath = "/images/logo.png";
            var url = getClass().getResource(logoPath);
            if (url != null) {
                Image logo = new Image(url.toExternalForm());
                logoImage.setImage(logo);
                logoPlaceholder.setVisible(false);
            } else {
                System.out.println("Logo not found at " + logoPath + ", using placeholder text");
            }
        } catch (Exception e) {
            System.out.println("Error loading logo: " + e.getMessage());
            // Placeholder text will remain visible
        }
    }

    /**
     * Load content into the center pane.
     * 
     * @param fxmlPath Path to FXML file (e.g.,
     *                 "/com/gradapptracker/ui/views/ProgramView.fxml")
     */
    public void setContent(String fxmlPath) {
        try {
            Node page = FXMLLoader.load(getClass().getResource(fxmlPath));
            contentPane.getChildren().setAll(page);
        } catch (IOException e) {
            System.err.println("Error loading page: " + fxmlPath);
            e.printStackTrace();
            AlertUtils.showError("Error", "Could not load page. Please try again.");
        }
    }

    /**
     * Show navigation panel and user info after successful login.
     * 
     * @param userName Name of logged-in user to display in header
     */
    public void showNavigation(String userName) {
        navigationPanel.setVisible(true);
        navigationPanel.setManaged(true);
        userInfoBox.setVisible(true);
        userInfoBox.setManaged(true);
        lblUserName.setText("Welcome, " + userName);
    }

    /**
     * Hide navigation panel and user info (used on logout).
     */
    public void hideNavigation() {
        navigationPanel.setVisible(false);
        navigationPanel.setManaged(false);
        userInfoBox.setVisible(false);
        userInfoBox.setManaged(false);
        lblUserName.setText("");
    }

    /**
     * Set active navigation button styling.
     * Removes the "nav-button-active" style class from the previously active button
     * and applies it to the newly selected button for visual feedback.
     * 
     * @param button the navigation button to mark as active
     */
    private void setActiveNavButton(Button button) {
        // Remove active class from previous button
        if (activeNavButton != null) {
            activeNavButton.getStyleClass().remove("nav-button-active");
        }
        // Add active class to new button
        if (button != null && !button.getStyleClass().contains("nav-button-active")) {
            button.getStyleClass().add("nav-button-active");
        }
        activeNavButton = button;
    }

    // ==================== Navigation Actions ====================

    /**
     * Navigate to the Programs dashboard view.
     * Sets the Programs view as the active content and highlights the dashboard
     * navigation button.
     */
    @FXML
    private void onNavigateDashboard() {
        setContent("/com/gradapptracker/ui/views/ProgramView.fxml");
        setActiveNavButton(btnNavDashboard);
    }

    /**
     * Navigate to the 3D Analytics dashboard view.
     * Requires authentication. Shows error dialog if user is not logged in.
     * Sets the 3D visualization as the active content.
     */
    @FXML
    private void onNavigate3DDashboard() {
        // Check authentication before navigating to 3D dashboard
        if (!UserSession.getInstance().isAuthenticated()) {
            AlertUtils.error("Authentication Required", "Please log in to access 3D Analytics.");
            return;
        }
        setContent("/com/gradapptracker/ui/views/Dashboard3DView.fxml");
        setActiveNavButton(btnNav3DDashboard);
    }

    /**
     * Navigate to the Documents management view.
     * Requires authentication. Shows error dialog if user is not logged in.
     * Sets the Documents view as the active content.
     */
    @FXML
    private void onNavigateDocuments() {
        // Check authentication before navigating to documents
        if (!UserSession.getInstance().isAuthenticated()) {
            AlertUtils.error("Authentication Required", "Please log in to access documents.");
            return;
        }
        setContent("/com/gradapptracker/ui/views/DocumentView.fxml");
        setActiveNavButton(btnNavDocuments);
    }

    /**
     * Navigate to the Profile management view.
     * Sets the Profile view as the active content and highlights the profile
     * navigation button.
     */
    @FXML
    private void onNavigateProfile() {
        setContent("/com/gradapptracker/ui/views/ProfileView.fxml");
        setActiveNavButton(btnNavProfile);
    }

    /**
     * Handle user logout action.
     * Prompts for confirmation, then clears the user session, hides navigation
     * panel,
     * and returns to the welcome/login screen.
     */
    @FXML
    public void onLogout() {
        boolean confirmed = AlertUtils.showConfirmation(
                "Confirm Logout",
                "Are you sure you want to sign out?");

        if (confirmed) {
            // Clear user session
            UserSession.getInstance().clear();

            // Hide navigation
            hideNavigation();

            // Return to welcome page
            setContent("/com/gradapptracker/ui/views/WelcomeView.fxml");

            // Reset active button
            activeNavButton = null;

            AlertUtils.showInfo("Signed Out", "You have been signed out successfully.");
        }
    }

    // ==================== Public Helper Methods ====================

    /**
     * Navigate to dashboard and set it as active.
     * Called after successful login.
     */
    public void navigateToDashboard() {
        onNavigateDashboard();
    }

    /**
     * Navigate to a specific view programmatically.
     * 
     * @param view View name: "dashboard", "programs", "documents", or "profile"
     */
    public void navigateTo(String view) {
        switch (view.toLowerCase()) {
            case "dashboard":
            case "programs": // Redirect programs to dashboard since dashboard shows programs
                onNavigateDashboard();
                break;
            case "documents":
                onNavigateDocuments();
                break;
            case "profile":
                onNavigateProfile();
                break;
            default:
                System.err.println("Unknown view: " + view);
        }
    }
}
