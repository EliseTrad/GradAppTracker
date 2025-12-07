package com.gradapptracker.ui.controllers;

import com.gradapptracker.backend.shared.dto.DashboardStatsDTO;
import com.gradapptracker.ui.services.DashboardServiceFx;
import com.gradapptracker.ui.services.ServiceLocator;
import com.gradapptracker.ui.utils.AlertUtils;
import com.gradapptracker.ui.utils.AsyncUtils;

import javafx.animation.RotateTransition;
import javafx.animation.Timeline;
import javafx.fxml.FXML;
import javafx.scene.*;
import javafx.scene.control.*;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.paint.PhongMaterial;
import javafx.scene.shape.Cylinder;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.scene.text.Text;
import javafx.scene.text.TextAlignment;
import javafx.scene.transform.Rotate;
import javafx.util.Duration;

import java.util.HashMap;
import java.util.Map;

/**
 * Controller for the 3D Analytics Dashboard.
 * 
 * This controller creates an interactive 3D visualization of program statistics
 * using JavaFX 3D components. It displays application status distribution as
 * animated 3D cylinders/bars that respond to user interaction.
 * 
 * Features:
 * - PerspectiveCamera for 3D view
 * - AmbientLight + PointLight for realistic lighting
 * - Animated 3D cylinders representing status counts
 * - Mouse drag rotation
 * - Click to view details
 * - Hover effects
 * - Real-time data from backend REST API
 */
public class Dashboard3DController {

    private final DashboardServiceFx dashboardService = ServiceLocator.getInstance().getDashboardService();

    // FXML Components
    @FXML
    private StackPane subSceneContainer;
    @FXML
    private Label lblTotalPrograms;
    @FXML
    private Label lblTotalDocuments;
    @FXML
    private VBox detailsPanel;
    @FXML
    private Label lblSelectedStatus;
    @FXML
    private Label lblSelectedCount;
    @FXML
    private Label lblSelectedPercentage;
    @FXML
    private Button btnRefresh;

    // 3D Scene components
    private SubScene subScene;
    private Group root3D;
    private PerspectiveCamera camera;
    private Map<String, Cylinder> statusBars;
    private Map<String, Color> statusColors;
    private Map<String, Integer> statusCounts;
    private DashboardStatsDTO currentStats;

    // Mouse interaction state
    private double anchorX, anchorY;
    private double anchorAngleX = 0;
    private double anchorAngleY = 0;
    private final Rotate rotateX = new Rotate(0, Rotate.X_AXIS);
    private final Rotate rotateY = new Rotate(0, Rotate.Y_AXIS);

    @FXML
    public void initialize() {
        setupStatusColors();
        setup3DScene();
        loadDashboardData();
    }

    /**
     * Setup color mapping for different application statuses.
     */
    private void setupStatusColors() {
        statusColors = new HashMap<>();
        statusColors.put("Accepted", Color.rgb(76, 175, 80)); // Green
        statusColors.put("Applied", Color.rgb(33, 150, 243)); // Blue
        statusColors.put("In Progress", Color.rgb(255, 152, 0)); // Orange
        statusColors.put("Rejected", Color.rgb(244, 67, 54)); // Red
        statusColors.put("Other", Color.rgb(158, 158, 158)); // Gray
    }

    /**
     * Create and configure the 3D scene with camera, lighting, and root group.
     */
    private void setup3DScene() {
        // Create root group for all 3D objects
        root3D = new Group();
        root3D.getTransforms().addAll(rotateX, rotateY);

        // Create PerspectiveCamera
        camera = new PerspectiveCamera(true);
        camera.setTranslateZ(-800);
        camera.setNearClip(0.1);
        camera.setFarClip(2000.0);
        camera.setFieldOfView(30);

        // Add lighting
        AmbientLight ambientLight = new AmbientLight(Color.rgb(80, 80, 80));

        PointLight pointLight = new PointLight(Color.WHITE);
        pointLight.setTranslateX(200);
        pointLight.setTranslateY(-200);
        pointLight.setTranslateZ(-500);

        PointLight pointLight2 = new PointLight(Color.rgb(200, 200, 255));
        pointLight2.setTranslateX(-200);
        pointLight2.setTranslateY(200);
        pointLight2.setTranslateZ(-500);

        root3D.getChildren().addAll(ambientLight, pointLight, pointLight2);

        // Create SubScene
        subScene = new SubScene(root3D, 800, 500, true, SceneAntialiasing.BALANCED);
        subScene.setFill(Color.rgb(38, 50, 56)); // Dark background
        subScene.setCamera(camera);

        // Bind SubScene size to container
        subScene.widthProperty().bind(subSceneContainer.widthProperty());
        subScene.heightProperty().bind(subSceneContainer.heightProperty());

        // Add mouse interaction handlers
        setupMouseInteraction();

        // Add SubScene to container
        subSceneContainer.getChildren().add(subScene);
    }

    /**
     * Setup mouse event handlers for drag rotation and click interaction.
     */
    private void setupMouseInteraction() {
        subScene.setOnMousePressed(event -> {
            anchorX = event.getSceneX();
            anchorY = event.getSceneY();
            anchorAngleX = rotateX.getAngle();
            anchorAngleY = rotateY.getAngle();
        });

        subScene.setOnMouseDragged(event -> {
            double deltaX = anchorX - event.getSceneX();
            double deltaY = anchorY - event.getSceneY();

            rotateY.setAngle(anchorAngleY + deltaX * 0.5);
            rotateX.setAngle(anchorAngleX - deltaY * 0.5);
        });
    }

    /**
     * Load dashboard statistics from backend and update 3D visualization.
     */
    private void loadDashboardData() {
        AsyncUtils.run(
                () -> dashboardService.getDashboardStats(),
                stats -> {
                    currentStats = stats;
                    updateStatsSummary(stats);
                    create3DBars(stats);
                },
                ex -> {
                    AlertUtils.errorFromBackend("Failed to load dashboard data: " + ex.getMessage());
                    // Create default empty visualization
                    createDefault3DBars();
                });
    }

    /**
     * Update the summary statistics cards.
     */
    private void updateStatsSummary(DashboardStatsDTO stats) {
        lblTotalPrograms.setText(String.valueOf(stats.getTotalPrograms()));
        lblTotalDocuments.setText(String.valueOf(stats.getTotalDocuments()));
    }

    /**
     * Create 3D cylindrical bars representing status distribution.
     */
    private void create3DBars(DashboardStatsDTO stats) {
        // Remove existing bars if any
        if (statusBars != null) {
            statusBars.values().forEach(bar -> root3D.getChildren().remove(bar));
        }

        statusBars = new HashMap<>();
        statusCounts = stats.getStatusCounts();

        // Calculate max count for scaling
        int maxCount = statusCounts.values().stream().mapToInt(Integer::intValue).max().orElse(1);
        if (maxCount == 0)
            maxCount = 1; // Avoid division by zero

        // Create bars for each status
        String[] statuses = { "Accepted", "Applied", "In Progress", "Rejected", "Other" };
        int barIndex = 0;
        double spacing = 120;
        double startX = -(statuses.length - 1) * spacing / 2.0;

        for (String status : statuses) {
            int count = statusCounts.getOrDefault(status, 0);

            // Scale height based on count (no minimum, max 250)
            double height = count > 0 ? (count * 250.0 / maxCount) : 5; // Small height for zero values

            // Create cylinder (3D bar)
            Cylinder bar = new Cylinder(30, height);
            bar.setTranslateX(startX + barIndex * spacing);
            bar.setTranslateY(125 - height / 2); // Position bars from bottom (Y=250) growing upward

            // Apply material with status color
            PhongMaterial material = new PhongMaterial();
            material.setDiffuseColor(statusColors.get(status));
            material.setSpecularColor(Color.WHITE);
            bar.setMaterial(material);

            // Store original color for hover effects
            bar.setUserData(new BarData(status, count, statusColors.get(status)));

            // Add animations
            addRotationAnimation(bar);
            addInteractivity(bar, status, count);

            // Add 3D label above the bar
            Group labelGroup = create3DLabel(status + "\n" + count, bar.getTranslateX(), 125 - height - 40);

            statusBars.put(status, bar);
            root3D.getChildren().addAll(bar, labelGroup);

            barIndex++;
        }
    }

    /**
     * Create a 3D label that remains facing the camera (billboard).
     */
    private Group create3DLabel(String text, double x, double y) {
        Group labelGroup = new Group();

        // Create text without background - Text nodes work better without 3D shapes
        Text text2D = new Text(text);
        text2D.setFont(Font.font("Arial", FontWeight.BOLD, 14));
        text2D.setFill(Color.WHITE);
        text2D.setTextAlignment(TextAlignment.CENTER);
        text2D.setStyle("-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.8), 4, 0.5, 0, 0);");

        // Center the text
        text2D.setTranslateX(-text.length() * 3.5);

        labelGroup.getChildren().add(text2D);
        labelGroup.setTranslateX(x);
        labelGroup.setTranslateY(y);

        return labelGroup;
    }

    /**
     * Create default bars when no data is available.
     */
    private void createDefault3DBars() {
        statusCounts = new HashMap<>();
        statusCounts.put("Accepted", 0);
        statusCounts.put("Applied", 0);
        statusCounts.put("In Progress", 0);
        statusCounts.put("Rejected", 0);
        statusCounts.put("Other", 0);

        DashboardStatsDTO defaultStats = new DashboardStatsDTO();
        defaultStats.setTotalPrograms(0);
        defaultStats.setTotalDocuments(0);
        defaultStats.setStatusCounts(statusCounts);

        create3DBars(defaultStats);
    }

    /**
     * Add continuous rotation animation to a 3D bar.
     */
    private void addRotationAnimation(Cylinder bar) {
        RotateTransition rotateTransition = new RotateTransition(Duration.seconds(5), bar);
        rotateTransition.setAxis(Rotate.Y_AXIS);
        rotateTransition.setFromAngle(0);
        rotateTransition.setToAngle(360);
        rotateTransition.setCycleCount(Timeline.INDEFINITE);
        rotateTransition.setAutoReverse(false);
        rotateTransition.play();
    }

    /**
     * Add hover and click interactivity to a 3D bar.
     */
    private void addInteractivity(Cylinder bar, String status, int count) {
        BarData barData = (BarData) bar.getUserData();

        // Hover effect - brighten color and scale up
        bar.setOnMouseEntered(event -> {
            PhongMaterial material = (PhongMaterial) bar.getMaterial();
            Color brighterColor = barData.originalColor.brighter();
            material.setDiffuseColor(brighterColor);
            material.setSpecularColor(Color.YELLOW);
            bar.setScaleX(1.2);
            bar.setScaleZ(1.2);
        });

        bar.setOnMouseExited(event -> {
            PhongMaterial material = (PhongMaterial) bar.getMaterial();
            material.setDiffuseColor(barData.originalColor);
            material.setSpecularColor(Color.WHITE);
            bar.setScaleX(1.0);
            bar.setScaleZ(1.0);
        });

        // Click effect - show details panel
        bar.setOnMouseClicked(event -> {
            showStatusDetails(status, count);
        });
    }

    /**
     * Display detailed information about a selected status.
     */
    private void showStatusDetails(String status, int count) {
        lblSelectedStatus.setText(status);
        lblSelectedCount.setText(String.valueOf(count));

        int total = currentStats != null ? currentStats.getTotalPrograms() : 0;
        double percentage = total > 0 ? (count * 100.0 / total) : 0;
        lblSelectedPercentage.setText(String.format("%.1f%%", percentage));

        detailsPanel.setVisible(true);
        detailsPanel.setManaged(true);
    }

    /**
     * Refresh button handler - reload data from backend.
     */
    @FXML
    private void onRefresh() {
        loadDashboardData();
    }

    /**
     * Helper class to store bar metadata.
     */
    private static class BarData {
        String status;
        int count;
        Color originalColor;

        BarData(String status, int count, Color color) {
            this.status = status;
            this.count = count;
            this.originalColor = color;
        }
    }
}
