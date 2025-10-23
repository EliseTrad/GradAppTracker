package com.gradapptracker.ui;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

/**
 * Simple JavaFX application launcher. Loads the LoginView.fxml from
 * resources at /com/gradapptracker/ui/views/LoginView.fxml.
 */
public class AppLauncher extends Application {

    @Override
    public void start(Stage stage) throws Exception {
        Parent root = FXMLLoader.load(getClass().getResource("/com/gradapptracker/ui/views/LoginView.fxml"));
        Scene scene = new Scene(root);
        stage.setTitle("GradAppTracker");
        stage.setScene(scene);
        stage.show();
    }

    public static void main(String[] args) {
        launch(args);
    }
}
