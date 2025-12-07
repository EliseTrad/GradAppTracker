package com.gradapptracker.ui.controllers;

import com.gradapptracker.backend.document.dto.DocumentResponseDTO;
import com.gradapptracker.backend.document.dto.DocumentUpdateDTO;
import com.gradapptracker.ui.services.DocumentServiceFx;
import com.gradapptracker.ui.services.ServiceLocator;
import com.gradapptracker.ui.utils.AlertUtils;
import com.gradapptracker.ui.utils.AsyncUtils;
import com.gradapptracker.ui.utils.UserSession;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.VBox;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import javafx.scene.Scene;

import java.io.File;
import java.util.HashMap;
import java.util.Map;
import java.util.List;

/**
 * Controller for document management view.
 * 
 * Handles all document CRUD operations by calling the backend API:
 * - Upload new documents (with file)
 * - Update document metadata (type, notes) - partial updates allowed
 * - Delete documents
 * - List all documents
 * - Filter documents by type
 * 
 * All validations and error messages come from the backend.
 * Frontend displays backend responses only.
 */
public class DocumentController {

    private final DocumentServiceFx documentService = ServiceLocator.getInstance().getDocumentService();

    private final ObservableList<DocumentResponseDTO> documents = FXCollections.observableArrayList();

    // Table and columns
    @FXML
    private TableView<DocumentResponseDTO> tblDocuments;

    @FXML
    private TableColumn<DocumentResponseDTO, String> colPreview;

    @FXML
    private TableColumn<DocumentResponseDTO, String> colDocType;

    @FXML
    private TableColumn<DocumentResponseDTO, String> colFileName;

    @FXML
    private TableColumn<DocumentResponseDTO, String> colNotes;

    @FXML
    private TableColumn<DocumentResponseDTO, String> colFilePath;

    @FXML
    private Label lblDocumentCount;

    // Search/Filter controls
    @FXML
    private TextField txtSearch;

    @FXML
    private TextField txtFilterDocType;

    @FXML
    private TextField txtFilterFileName;

    @FXML
    private TextField txtFilterNotes;

    @FXML
    private Button btnFilter;

    @FXML
    private Button btnClearFilter;

    @FXML
    private Button btnRefresh;

    // Form controls
    @FXML
    private Label lblFormMode;

    @FXML
    private TextField txtDocType;

    @FXML
    private TextField txtFilePath;

    @FXML
    private Button btnBrowse;

    @FXML
    private TextField txtFileName;

    @FXML
    private TextArea txtNotes;

    @FXML
    private Button btnUpload;

    @FXML
    private Button btnUpdate;

    @FXML
    private Button btnDelete;

    @FXML
    private Button btnClear;

    @FXML
    private Button btnOpenFile;

    // Programs using this document section
    @FXML
    private VBox programsUsingDocSection;

    @FXML
    private ListView<String> lstProgramsUsingDoc;

    private File selectedFile;

    @FXML
    public void initialize() {
        // Setup table columns
        tblDocuments.setItems(documents);

        // Setup preview column with thumbnail and click handler
        colPreview.setCellValueFactory(c -> new javafx.beans.property.SimpleStringProperty("📄"));
        colPreview.setCellFactory(col -> new TableCell<DocumentResponseDTO, String>() {
            private final Button previewBtn = new Button();

            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || getTableRow().getItem() == null) {
                    setGraphic(null);
                } else {
                    DocumentResponseDTO doc = getTableRow().getItem();
                    String fileName = doc.getFileName().toLowerCase();

                    // Set appropriate icon based on file type
                    if (fileName.endsWith(".pdf")) {
                        previewBtn.setText("📋");
                    } else if (fileName.endsWith(".jpg") || fileName.endsWith(".jpeg") ||
                            fileName.endsWith(".png") || fileName.endsWith(".gif")) {
                        previewBtn.setText("View");
                    } else if (fileName.endsWith(".doc") || fileName.endsWith(".docx")) {
                        previewBtn.setText("📝");
                    } else {
                        previewBtn.setText("View");
                    }

                    previewBtn.setStyle("-fx-background-color: transparent; -fx-border: none; -fx-font-size: 16px;");
                    previewBtn.setOnAction(e -> DocumentController.this.openFilePreview(doc));
                    setGraphic(previewBtn);
                }
            }
        });

        colDocType.setCellValueFactory(c -> new javafx.beans.property.SimpleStringProperty(c.getValue().getDocType()));
        colFileName
                .setCellValueFactory(c -> new javafx.beans.property.SimpleStringProperty(c.getValue().getFileName()));
        colNotes.setCellValueFactory(
                c -> new javafx.beans.property.SimpleStringProperty(nullSafe(c.getValue().getNotes())));
        colFilePath
                .setCellValueFactory(c -> new javafx.beans.property.SimpleStringProperty(c.getValue().getFilePath()));

        // DocType is free text - no predefined list needed

        // Hide programs section initially
        if (programsUsingDocSection != null) {
            programsUsingDocSection.setVisible(false);
            programsUsingDocSection.setManaged(false);
        }

        // Check authentication before loading documents
        UserSession session = UserSession.getInstance();
        boolean isAuth = session.isAuthenticated();
        Integer userId = session.getUserId();
        String jwt = session.getJwt();

        System.out.println("DocumentController.initialize() - Authentication Check:");
        System.out.println("  isAuthenticated(): " + isAuth);
        System.out.println("  userId: " + userId);
        System.out.println("  jwt present: " + (jwt != null ? "yes (length: " + jwt.length() + ")" : "no"));

        if (userId == null && jwt != null) {
            System.out.println("  WARNING: userId is null but JWT is present - attempting to extract from JWT");
            session.refreshUserIdFromJWT();
            userId = session.getUserId();
            System.out.println("  After JWT extraction - userId: " + userId);
        }

        if (!isAuth) {
            AlertUtils.error("Authentication Required", "Please log in to access documents.");
            return;
        }

        if (userId == null) {
            AlertUtils.error("Session Error", "User ID not available. Please log out and log in again.");
            return;
        }

        // Load all documents on startup
        loadDocuments();
    }

    /**
     * Load documents from backend and update the table.
     * Called on initialization and after create/update/delete operations.
     */
    private void loadDocuments() {
        // Check authentication before making API call
        if (!UserSession.getInstance().isAuthenticated()) {
            AlertUtils.handleAuthError(new RuntimeException("User not authenticated"));
            return;
        }

        AsyncUtils.run(
                () -> documentService.getAllDocuments(),
                list -> {
                    documents.setAll(list);
                    updateDocumentCount(list.size());
                },
                ex -> AlertUtils.handleGenericError(ex, "load", "documents"));
    }

    /**
     * Update the document count label.
     */
    private void updateDocumentCount(int count) {
        if (lblDocumentCount != null) {
            lblDocumentCount.setText("Total: " + count + " document" + (count == 1 ? "" : "s"));
        }
    }

    /**
     * Handle file browse button click.
     * Opens file chooser for user to select a document file.
     */
    @FXML
    private void onBrowse() {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Select Document File");
        selectedFile = fileChooser.showOpenDialog(btnBrowse.getScene().getWindow());
        if (selectedFile != null) {
            txtFilePath.setText(selectedFile.getAbsolutePath());
        }
    }

    /**
     * Handle upload button click.
     * Upload a new document with file to backend.
     * Backend validates: file must be present, docType is required.
     */
    @FXML
    private void onUpload() {
        // No frontend validation - let backend handle it
        String docType = txtDocType.getText();
        String notes = txtNotes.getText();

        btnUpload.setDisable(true);
        AsyncUtils.run(
                () -> documentService.uploadDocument(selectedFile, docType, notes),
                result -> {
                    AlertUtils.showSuccess("Success", "Document uploaded successfully.");
                    loadDocuments();
                    clearForm();
                },
                ex -> {
                    // Hide raw backend errors from user
                    String message = ex.getMessage();
                    if (message != null && message.contains("User not authenticated")) {
                        AlertUtils.error("Authentication Error", "Please log in again to upload documents.");
                    } else if (message != null && message.contains("userId could not be converted")) {
                        AlertUtils.error("Upload Error", "Session expired. Please log in again.");
                    } else {
                        // For any other error, show generic message
                        AlertUtils.error("Upload Error", "Upload failed. Please try again.");
                    }
                },
                () -> btnUpload.setDisable(false));
    }

    /**
     * Handle update button click.
     * Update document metadata (docType, fileName, notes).
     * Partial updates are allowed - only non-empty fields are sent.
     * File replacement is not supported in this operation.
     */
    @FXML
    private void onUpdate() {
        DocumentResponseDTO selected = tblDocuments.getSelectionModel().getSelectedItem();
        if (selected == null) {
            AlertUtils.warn("Selection Error", "Please select a document to update.");
            return;
        }

        // Build update DTO with partial updates support
        DocumentUpdateDTO dto = new DocumentUpdateDTO();

        // Only set fields that have values (partial update)
        String docType = txtDocType.getText();
        if (docType != null && !docType.trim().isEmpty()) {
            dto.setDocType(docType.trim());
        }

        String fileName = txtFileName.getText();
        if (fileName != null && !fileName.trim().isEmpty()) {
            dto.setFileName(fileName.trim());
        }

        String notes = txtNotes.getText();
        if (notes != null && !notes.trim().isEmpty()) {
            dto.setNotes(notes.trim());
        }

        btnUpdate.setDisable(true);
        AsyncUtils.run(
                () -> documentService.updateDocument(selected.getDocumentId(), dto),
                result -> {
                    AlertUtils.showSuccess("Success", "Document updated successfully.");
                    loadDocuments();
                    clearForm();
                },
                ex -> AlertUtils.handleGenericError(ex, "update", "document"),
                () -> btnUpdate.setDisable(false));
    }

    /**
     * Handle delete button click.
     * Delete the selected document from backend.
     */
    @FXML
    private void onDelete() {
        DocumentResponseDTO selected = tblDocuments.getSelectionModel().getSelectedItem();
        if (selected == null) {
            AlertUtils.warn("Selection Error", "Please select a document to delete.");
            return;
        }

        if (!AlertUtils.confirm("Delete Document",
                "Are you sure you want to delete this document? This action cannot be undone.")) {
            return;
        }

        btnDelete.setDisable(true);
        AsyncUtils.run(
                () -> {
                    documentService.deleteDocument(selected.getDocumentId());
                    return null;
                },
                result -> {
                    AlertUtils.showSuccess("Success", "Document deleted successfully.");
                    loadDocuments();
                    clearForm();
                },
                ex -> AlertUtils.handleGenericError(ex, "delete", "document"),
                () -> btnDelete.setDisable(false));
    }

    /**
     * Handle refresh button click.
     * Reload all documents from backend.
     */
    @FXML
    private void onRefresh() {
        loadDocuments();
        clearForm();
    }

    /**
     * Handle filter button click.
     * Filter documents by any combination of document type, file name, and notes.
     * Backend only supports docType filtering, so fileName and notes are filtered
     * client-side.
     */
    @FXML
    private void onFilter() {
        String docType = txtFilterDocType.getText();
        String fileName = txtFilterFileName.getText();
        String notes = txtFilterNotes.getText();

        // If no filters entered, load all documents
        if ((docType == null || docType.isBlank()) &&
                (fileName == null || fileName.isBlank()) &&
                (notes == null || notes.isBlank())) {
            loadDocuments();
            return;
        }

        // Start with backend filtering by docType if provided
        AsyncUtils.run(
                () -> {
                    List<DocumentResponseDTO> results;
                    if (docType != null && !docType.isBlank()) {
                        // Use backend filtering for docType
                        Map<String, Object> filters = new HashMap<>();
                        filters.put("type", docType);
                        results = documentService.filterDocuments(filters);
                    } else {
                        // Get all documents if no docType filter
                        results = documentService.getAllDocuments();
                    }

                    // Apply client-side filtering for fileName and notes
                    return filterDocumentsClientSide(results, fileName, notes);
                },
                (List<DocumentResponseDTO> list) -> {
                    documents.setAll(list);
                    updateDocumentCount(list.size());
                    updateFilterPlaceholder(list);
                },
                ex -> AlertUtils.handleGenericError(ex, "filter", "documents"));
    }

    /**
     * Apply client-side filtering for fileName and notes.
     */
    private List<DocumentResponseDTO> filterDocumentsClientSide(List<DocumentResponseDTO> docs, String fileName,
            String notes) {
        if ((fileName == null || fileName.isBlank()) && (notes == null || notes.isBlank())) {
            return docs;
        }

        return docs.stream()
                .filter(doc -> {
                    boolean fileNameMatch = true;
                    boolean notesMatch = true;

                    if (fileName != null && !fileName.isBlank()) {
                        fileNameMatch = doc.getFileName() != null &&
                                doc.getFileName().toLowerCase().contains(fileName.toLowerCase());
                    }

                    if (notes != null && !notes.isBlank()) {
                        notesMatch = doc.getNotes() != null &&
                                doc.getNotes().toLowerCase().contains(notes.toLowerCase());
                    }

                    return fileNameMatch && notesMatch;
                })
                .collect(java.util.stream.Collectors.toList());
    }

    /**
     * Update the placeholder message based on filter state and results.
     */
    private void updateFilterPlaceholder(List<DocumentResponseDTO> documents) {
        if (tblDocuments == null)
            return;

        // Check if any filters are active
        boolean hasActiveFilters = (txtFilterDocType != null && !txtFilterDocType.getText().trim().isEmpty()) ||
                (txtFilterFileName != null && !txtFilterFileName.getText().trim().isEmpty()) ||
                (txtFilterNotes != null && !txtFilterNotes.getText().trim().isEmpty());

        if (documents.isEmpty()) {
            if (hasActiveFilters) {
                Label placeholderLabel = new Label("No results found");
                placeholderLabel.setStyle("-fx-text-fill: #9e9e9e; -fx-font-size: 14px;");
                tblDocuments.setPlaceholder(placeholderLabel);
            } else {
                Label placeholderLabel = new Label("No documents uploaded yet. Upload your first document below!");
                placeholderLabel.setStyle("-fx-text-fill: #9e9e9e; -fx-font-size: 14px;");
                tblDocuments.setPlaceholder(placeholderLabel);
            }
        }
    }

    /**
     * Handle clear filter button click.
     * Clear all filter fields and reload all documents.
     */
    @FXML
    private void onClearFilter() {
        if (txtFilterDocType != null)
            txtFilterDocType.clear();
        if (txtFilterFileName != null)
            txtFilterFileName.clear();
        if (txtFilterNotes != null)
            txtFilterNotes.clear();
        loadDocuments();
    }

    /**
     * Handle table row selection.
     * Populate form fields with selected document data for editing.
     */
    @FXML
    private void onRowSelect() {
        DocumentResponseDTO selected = tblDocuments.getSelectionModel().getSelectedItem();
        if (selected != null) {
            // Populate form fields
            txtDocType.setText(selected.getDocType());
            txtFileName.setText(selected.getFileName());
            txtNotes.setText(nullSafe(selected.getNotes()));
            txtFilePath.clear();
            selectedFile = null;

            // Update form mode label
            if (lblFormMode != null) {
                lblFormMode.setText("Edit Document #" + selected.getDocumentId());
            }

            // Load programs using this document
            loadProgramsUsingDocument(selected.getDocumentId());
        }
    }

    /**
     * Load programs that are using this document.
     */
    private void loadProgramsUsingDocument(int documentId) {
        if (programsUsingDocSection == null || lstProgramsUsingDoc == null) {
            return;
        }

        AsyncUtils.run(
                () -> {
                    // For each program, check if it has this document linked
                    // Note: This requires fetching all programs and checking their linked documents
                    // A more efficient backend endpoint would be: GET /documents/{id}/programs
                    // For now, we'll show the section but with a message
                    return null;
                },
                result -> {
                    programsUsingDocSection.setVisible(true);
                    programsUsingDocSection.setManaged(true);
                    lstProgramsUsingDoc.getItems().clear();
                    lstProgramsUsingDoc.getItems().add("Link this document to programs in the Programs page");
                },
                ex -> {
                    // Silently fail - this is not critical functionality
                    programsUsingDocSection.setVisible(false);
                    programsUsingDocSection.setManaged(false);
                });
    }

    /**
     * Clear all form fields and reset to "add new" mode.
     */
    @FXML
    private void onClear() {
        clearForm();
    }

    /**
     * Open file location in system file explorer.
     */
    @FXML
    private void onOpenFile() {
        DocumentResponseDTO selected = tblDocuments.getSelectionModel().getSelectedItem();
        if (selected == null) {
            AlertUtils.warn("Selection Error", "Please select a document to open its location.");
            return;
        }

        try {
            String filePath = selected.getFilePath();
            if (filePath != null && !filePath.isEmpty()) {
                File file = new File(filePath);
                if (file.exists()) {
                    // Open containing folder using the shared helper so the method is used
                    openFileWithDefaultApp(file.getParentFile());
                } else {
                    AlertUtils.warn("File Not Found", "The document file could not be found on disk.");
                }
            } else {
                AlertUtils.warn("No File Path", "This document does not have a file path.");
            }
        } catch (Exception e) {
            AlertUtils.error("Error", "Could not open file location: " + e.getMessage());
        }
    }

    /**
     * Clear all form fields and reset state.
     */
    private void clearForm() {
        txtDocType.clear();
        txtFileName.clear();
        txtNotes.clear();
        txtFilePath.clear();
        selectedFile = null;
        tblDocuments.getSelectionModel().clearSelection();

        if (lblFormMode != null) {
            lblFormMode.setText("Upload New Document");
        }

        if (programsUsingDocSection != null) {
            programsUsingDocSection.setVisible(false);
            programsUsingDocSection.setManaged(false);
        }
    }

    /**
     * Try to preview any file. No assumptions about file type.
     * For images, attempt preview. For any file, fall back to download if preview
     * fails.
     */
    private void openFilePreview(DocumentResponseDTO document) {
        try {
            showImagePreview(document);
        } catch (Exception e) {
            // If any error occurs, fall back to download
            showDownloadDialog(document);
        }
    }

    /**
     * Show image preview in a new window.
     * If image loading fails, silently fall back to download button.
     */
    private void showImagePreview(DocumentResponseDTO document) {
        try {
            // Download file from backend
            byte[] fileData = documentService.downloadDocument(document.getDocumentId().intValue());

            // Try to create an image from the byte data
            java.io.ByteArrayInputStream inputStream = new java.io.ByteArrayInputStream(fileData);
            Image image = new Image(inputStream);

            // If image loads successfully, show preview
            if (!image.isError() && image.getWidth() > 0 && image.getHeight() > 0) {
                ImageView imageView = new ImageView(image);
                imageView.setPreserveRatio(true);
                imageView.setFitWidth(800);
                imageView.setFitHeight(600);

                ScrollPane scrollPane = new ScrollPane(imageView);
                scrollPane.setFitToWidth(true);
                scrollPane.setFitToHeight(true);

                Scene scene = new Scene(scrollPane, 850, 650);
                Stage stage = new Stage();
                stage.setTitle("Preview: " + document.getFileName());
                stage.setScene(scene);
                stage.show();
                return;
            }
        } catch (Exception e) {
            // Silently ignore preview errors
        }

        // If preview fails for any reason, show download option instead
        showDownloadDialog(document);
    }

    /**
     * Show a simple download dialog when image preview is not possible.
     */
    private void showDownloadDialog(DocumentResponseDTO document) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Document: " + document.getFileName());
        alert.setHeaderText("Preview not available");
        alert.setContentText("Click Download to save the file to your computer.");

        ButtonType downloadButton = new ButtonType("Download");
        ButtonType cancelButton = new ButtonType("Cancel", ButtonBar.ButtonData.CANCEL_CLOSE);
        alert.getButtonTypes().setAll(downloadButton, cancelButton);

        alert.showAndWait().ifPresent(response -> {
            if (response == downloadButton) {
                downloadDocument(document);
            }
        });
    }

    /**
     * Download/save the document file to user's chosen location.
     */
    private void downloadDocument(DocumentResponseDTO document) {
        try {
            FileChooser fileChooser = new FileChooser();
            fileChooser.setTitle("Save Document");
            fileChooser.setInitialFileName(document.getFileName());

            // Set file extension filter if possible
            String fileName = document.getFileName();
            if (fileName.contains(".")) {
                String extension = fileName.substring(fileName.lastIndexOf('.') + 1).toLowerCase();
                FileChooser.ExtensionFilter filter = new FileChooser.ExtensionFilter(
                        extension.toUpperCase() + " files", "*." + extension);
                fileChooser.getExtensionFilters().add(filter);
            }

            java.io.File targetFile = fileChooser.showSaveDialog(null);
            if (targetFile != null) {
                // Download from backend and save to chosen location
                byte[] fileData = documentService.downloadDocument(document.getDocumentId().intValue());
                java.nio.file.Files.write(targetFile.toPath(), fileData,
                        java.nio.file.StandardOpenOption.CREATE,
                        java.nio.file.StandardOpenOption.TRUNCATE_EXISTING);

                AlertUtils.showInfo("Download Complete", "File saved to: " + targetFile.getAbsolutePath());
            }
        } catch (Exception e) {
            AlertUtils.showError("Download Error", "Failed to download file: " + e.getMessage());
        }
    }

    /**
     * Open file with default system application.
     */
    private void openFileWithDefaultApp(java.io.File file) {
        try {
            if (java.awt.Desktop.isDesktopSupported()) {
                java.awt.Desktop.getDesktop().open(file);
            } else {
                // Fallback for systems without Desktop support
                String os = System.getProperty("os.name").toLowerCase();
                ProcessBuilder pb;
                if (os.contains("win")) {
                    pb = new ProcessBuilder("rundll32", "url.dll,FileProtocolHandler", file.getAbsolutePath());
                } else if (os.contains("mac")) {
                    pb = new ProcessBuilder("open", file.getAbsolutePath());
                } else {
                    pb = new ProcessBuilder("xdg-open", file.getAbsolutePath());
                }
                pb.start();
            }
        } catch (Exception e) {
            AlertUtils.showError("Open File Error", "Failed to open file: " + e.getMessage());
        }
    }

    /**
     * Utility to safely handle null strings.
     */
    private static String nullSafe(String s) {
        return s == null ? "" : s;
    }
}
