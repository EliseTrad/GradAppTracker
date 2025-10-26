package com.gradapptracker.ui.utils;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import javafx.application.Platform;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonBar;
import javafx.scene.control.ButtonType;

import java.util.List;
import java.util.Map;
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

    private static final ObjectMapper mapper = new ObjectMapper();

    private AlertUtils() {
        // utility
    }

    public static Alert info(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        return alert;
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

    public static void errorFromBackend(String rawMessage) {
        String friendlyMessage = parseBackendError(rawMessage);
        error("Error", friendlyMessage);
    }

    private static String parseBackendError(String raw) {
        if (raw == null || raw.trim().isEmpty()) {
            return "Unknown error";
        }
        // Extract JSON part if present
        String jsonPart = raw;
        int colonIndex = raw.lastIndexOf(": ");
        if (colonIndex != -1) {
            jsonPart = raw.substring(colonIndex + 2);
        }
        try {
            // Try to parse as JSON
            Map<String, Object> json = mapper.readValue(jsonPart, new TypeReference<Map<String, Object>>() {
            });
            String message = (String) json.get("message");
            @SuppressWarnings("unchecked")
            List<Map<String, String>> errors = (List<Map<String, String>>) json.get("errors");
            if (errors != null && !errors.isEmpty()) {
                StringBuilder sb = new StringBuilder(message != null ? message : "Validation failed");
                sb.append(": ");
                for (Map<String, String> error : errors) {
                    String field = error.get("field");
                    String msg = error.get("message");
                    if (field != null && msg != null) {
                        sb.append(field).append(" ").append(msg).append("; ");
                    }
                }
                return sb.toString().replaceAll("; $", "");
            } else if (message != null) {
                return message;
            }
        } catch (Exception e) {
            // Not JSON, return as is
        }
        return raw;
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

    // ==================== Convenience Methods ====================

    /**
     * Show an error alert dialog.
     * 
     * @param title   dialog title
     * @param message error message
     */
    public static void showError(String title, String message) {
        error(title, message);
    }

    /**
     * Show an information alert dialog.
     * 
     * @param title   dialog title
     * @param message info message
     */
    public static void showInfo(String title, String message) {
        runOnFxThread(() -> {
            Alert alert = info(title, message);
            alert.showAndWait();
        });
    }

    /**
     * Show a success alert dialog (info type with success styling).
     * 
     * @param title   dialog title
     * @param message success message
     */
    public static void showSuccess(String title, String message) {
        runOnFxThread(() -> {
            Alert alert = new Alert(Alert.AlertType.INFORMATION);
            alert.setTitle(title);
            alert.setHeaderText(null);
            alert.setContentText(message);
            alert.showAndWait();
        });
    }

    /**
     * Show a warning alert dialog.
     * 
     * @param title   dialog title
     * @param message warning message
     */
    public static void showWarning(String title, String message) {
        warn(title, message);
    }

    /**
     * Show a confirmation dialog.
     * 
     * @param title   dialog title
     * @param message confirmation message
     * @return true if user clicked OK, false otherwise
     */
    public static boolean showConfirmation(String title, String message) {
        return confirm(title, message);
    }

    // ==================== User-Friendly Error Handlers ====================

    /**
     * Handle authentication-related errors with user-friendly messages.
     * 
     * @param ex the exception that occurred
     */
    public static void handleAuthError(Throwable ex) {
        String message = ex != null ? ex.getMessage() : "";
        if (message.contains("User not authenticated") || message.contains("not logged in")) {
            error("Authentication Required", "Please log in to continue.");
        } else if (message.contains("Session expired") || message.contains("Token expired")) {
            error("Session Expired", "Your session has expired. Please log in again.");
        } else if (message.contains("Unauthorized") || message.contains("403")) {
            error("Access Denied", "You don't have permission to perform this action.");
        } else {
            error("Authentication Error", "Please log in and try again.");
        }
    }

    /**
     * Handle operation errors with generic user-friendly messages.
     * 
     * @param operation the operation that failed (e.g., "create", "update",
     *                  "delete")
     * @param itemType  the type of item (e.g., "program", "document")
     */
    public static void handleOperationError(String operation, String itemType) {
        String message = String.format("Failed to %s %s. Please try again.", operation, itemType);
        error("Operation Failed", message);
    }

    /**
     * Handle load/fetch errors with user-friendly messages.
     * 
     * @param itemType the type of item being loaded (e.g., "programs", "documents")
     */
    public static void handleLoadError(String itemType) {
        error("Load Error", String.format("Failed to load %s. Please refresh and try again.", itemType));
    }

    /**
     * Handle network/connection errors with user-friendly messages.
     */
    public static void handleConnectionError() {
        error("Connection Error", "Unable to connect to the server. Please check your connection and try again.");
    }

    /**
     * Handle general errors by checking for common patterns and showing appropriate
     * messages.
     * 
     * @param ex        the exception that occurred
     * @param operation the operation that was being performed
     * @param itemType  the type of item being operated on
     */
    public static void handleGenericError(Throwable ex, String operation, String itemType) {
        if (ex == null) {
            handleOperationError(operation, itemType);
            return;
        }

        String message = ex.getMessage();
        if (message == null) {
            handleOperationError(operation, itemType);
            return;
        }

        // Check for authentication issues
        if (message.contains("User not authenticated") ||
                message.contains("not logged in") ||
                message.contains("Session expired") ||
                message.contains("Token expired") ||
                message.contains("Unauthorized") ||
                message.contains("userId could not be converted")) {
            handleAuthError(ex);
            return;
        }

        // Check for connection issues
        if (message.contains("Connection") ||
                message.contains("timeout") ||
                message.contains("refused") ||
                message.contains("unreachable")) {
            handleConnectionError();
            return;
        }

        // For validation errors, try to use errorFromBackend which can parse structured
        // errors
        if (message.contains("validation") || message.contains("invalid") || message.contains("{")) {
            errorFromBackend(message);
            return;
        }

        // Default generic message
        handleOperationError(operation, itemType);
    }
}
