package com.gradapptracker.ui.utils;

import javafx.application.Platform;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonBar;
import javafx.scene.control.ButtonType;

import java.util.Optional;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Simple JavaFX Alert helpers.
 * <p>
 * All methods ensure the Alert is shown on the JavaFX Application Thread. The
 * {@link #confirm(String, String)} method blocks the calling thread until the
 * user responds, making it suitable for use from non-JavaFX threads.
 */
public final class AlertUtils {

    private AlertUtils() {
        // utility
    }

    public static void info(String title, String message) {
        runOnFxThread(() -> {
            Alert alert = new Alert(Alert.AlertType.INFORMATION);
            alert.setTitle(title);
            alert.setHeaderText(null);
            alert.setContentText(message);
            alert.showAndWait();
        });
    }

    public static void error(String title, String message) {
        runOnFxThread(() -> {
            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setTitle(title);
            alert.setHeaderText(null);
            alert.setContentText(message);
            alert.showAndWait();
        });
    }

    public static void warn(String title, String message) {
        runOnFxThread(() -> {
            Alert alert = new Alert(Alert.AlertType.WARNING);
            alert.setTitle(title);
            alert.setHeaderText(null);
            alert.setContentText(message);
            alert.showAndWait();
        });
    }

    /**
     * Show a confirmation dialog and return true if the user pressed OK.
     * This method will block until the user responds if called from a non-JavaFX
     * thread.
     *
     * @param title   dialog title
     * @param message dialog message
     * @return true if OK was pressed, false otherwise
     */
    public static boolean confirm(String title, String message) {
        if (Platform.isFxApplicationThread()) {
            return showConfirmDialog(title, message);
        }

        CountDownLatch latch = new CountDownLatch(1);
        AtomicBoolean result = new AtomicBoolean(false);
        Platform.runLater(() -> {
            try {
                result.set(showConfirmDialog(title, message));
            } finally {
                latch.countDown();
            }
        });
        try {
            latch.await();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return false;
        }
        return result.get();
    }

    private static boolean showConfirmDialog(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);

        // Use explicit button types to make intent clear
        ButtonType ok = new ButtonType("OK", ButtonBar.ButtonData.OK_DONE);
        ButtonType cancel = new ButtonType("Cancel", ButtonBar.ButtonData.CANCEL_CLOSE);
        alert.getButtonTypes().setAll(ok, cancel);

        Optional<ButtonType> res = alert.showAndWait();
        return res.filter(buttonType -> buttonType == ok).isPresent();
    }

    private static void runOnFxThread(Runnable r) {
        if (Platform.isFxApplicationThread()) {
            r.run();
        } else {
            Platform.runLater(r);
        }
    }
}
