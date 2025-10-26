package com.gradapptracker.ui;

import com.gradapptracker.ui.controllers.MainLayoutController;
import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

/**
 * Simple JavaFX application launcher. Loads the MainLayout.fxml and sets
 * WelcomeView as initial content.
 */
public class AppLauncher extends Application {

    @Override
    public void start(Stage stage) throws Exception {
        FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/gradapptracker/ui/views/MainLayout.fxml"));
        Parent root = loader.load();
        MainLayoutController controller = loader.getController();
        controller.setContent("/com/gradapptracker/ui/views/WelcomeView.fxml");

        Scene scene = new Scene(root);
        stage.setTitle("GradAppTracker");
        stage.setMaximized(true); // Maximize the window
        stage.setScene(scene);
        stage.show();
    }

    public static void main(String[] args) {
        launch(args);
    }
}
