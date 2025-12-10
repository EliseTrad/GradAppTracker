package com.gradapptracker.ui.controllers;

import com.gradapptracker.backend.document.dto.DocumentResponseDTO;
import com.gradapptracker.backend.program.dto.ProgramDTO;
import com.gradapptracker.backend.program.dto.ProgramCreateDTO;
import com.gradapptracker.backend.program.dto.ProgramUpdateDTO;
import com.gradapptracker.backend.programdocument.dto.ProgramDocumentCreateDTO;
import com.gradapptracker.backend.programdocument.dto.ProgramDocumentDTO;
import com.gradapptracker.ui.services.ProgramServiceFx;
import com.gradapptracker.ui.services.DocumentServiceFx;
import com.gradapptracker.ui.services.ProgramDocumentServiceFx;
import com.gradapptracker.ui.services.ServiceLocator;
import com.gradapptracker.ui.utils.AlertUtils;
import com.gradapptracker.ui.utils.AsyncUtils;

import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.VBox;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Controller for program management view.
 * 
 * Handles all program CRUD operations and document linking by calling backend
 * API:
 * - Create new programs
 * - Update program details (partial updates allowed)
 * - Delete programs
 * - List and filter programs by any field
 * - Link multiple documents to a program
 * - Unlink documents from a program
 * 
 * All validations and error messages come from the backend.
 * Frontend displays backend responses only.
 */
public class ProgramController {

    private final ProgramServiceFx programService = ServiceLocator.getInstance().getProgramService();
    private final DocumentServiceFx documentService = ServiceLocator.getInstance().getDocumentService();
    private final ProgramDocumentServiceFx programDocumentService = ServiceLocator.getInstance()
            .getProgramDocumentService();

    private final ObservableList<ProgramDTO> programs = FXCollections.observableArrayList();
    private final ObservableList<DocumentResponseDTO> availableDocuments = FXCollections.observableArrayList();
    private final ObservableList<ProgramDocumentDTO> linkedDocuments = FXCollections.observableArrayList();

    // Table and columns
    @FXML
    private TableView<ProgramDTO> tblPrograms;

    @FXML
    private TableColumn<ProgramDTO, String> colUniversityName, colFieldOfStudy, colFocusArea,
            colDeadline, colStatus, colTuition, colPortal, colWebsite;

    @FXML
    private Label lblProgramCount;

    // Search and filter controls
    @FXML
    private TextField txtFilterUniversity;

    @FXML
    private TextField txtFilterFieldOfStudy;

    @FXML
    private TextField txtFilterStatus;

    @FXML
    private TextField txtFilterFocusArea;

    @FXML
    private TextField txtFilterTuition;

    @FXML
    private TextField txtFilterPortal;

    @FXML
    private TextField txtFilterWebsite;

    @FXML
    private Button btnFilter;

    @FXML
    private Button btnClearFilter;

    @FXML
    private Button btnRefresh;

    // Form fields
    @FXML
    private Label lblFormMode;

    @FXML
    private TextField txtUniversityName, txtFieldOfStudy, txtFocusArea, txtPortal, txtWebsite, txtTuition;

    @FXML
    private DatePicker dpDeadline;

    @FXML
    private ComboBox<String> cmbStatus;

    @FXML
    private TextArea txtRequirements, txtNotes;

    // Action buttons
    @FXML
    private Button btnAdd, btnEdit, btnDelete, btnClear, btnManageDocuments, btnShowAddForm;

    // Form section
    @FXML
    private VBox programFormSection;

    // Linked documents section
    @FXML
    private VBox linkedDocsSection;

    @FXML
    private ComboBox<DocumentResponseDTO> cbAvailableDocuments;

    @FXML
    private TextField txtUsageNotes;

    @FXML
    private Button btnLinkDocument;

    @FXML
    private ListView<String> lstLinkedDocuments;

    @FXML
    private Button btnUnlinkDocument;

    // Internal state
    private ProgramDTO selectedProgram;

    /**
     * Initialize the controller after FXML components are loaded.
     * Sets up table columns, status dropdown, document combo box,
     * and loads initial data from backend.
     */
    @FXML
    public void initialize() {
        // Setup table columns
        tblPrograms.setItems(programs);
        colUniversityName
                .setCellValueFactory(c -> new SimpleStringProperty(nullSafe(c.getValue().getUniversityName())));
        colFieldOfStudy.setCellValueFactory(c -> new SimpleStringProperty(nullSafe(c.getValue().getFieldOfStudy())));
        colFocusArea.setCellValueFactory(c -> new SimpleStringProperty(nullSafe(c.getValue().getFocusArea())));
        colDeadline.setCellValueFactory(c -> new SimpleStringProperty(dateToString(c.getValue().getDeadline())));
        colStatus.setCellValueFactory(c -> new SimpleStringProperty(nullSafe(c.getValue().getStatus())));
        colTuition.setCellValueFactory(c -> new SimpleStringProperty(nullSafe(c.getValue().getTuition())));
        colPortal.setCellValueFactory(c -> new SimpleStringProperty(nullSafe(c.getValue().getPortal())));
        colWebsite.setCellValueFactory(c -> new SimpleStringProperty(nullSafe(c.getValue().getWebsite())));

        // Initialize status dropdown with predefined values
        cmbStatus.setItems(FXCollections.observableArrayList(
                "Accepted", "Applied", "In Progress", "Rejected", "Other"));

        // Program details and linked docs sections are now always visible
        // They show "Select a program" message when no program is selected

        // Setup document combo box display
        if (cbAvailableDocuments != null) {
            cbAvailableDocuments.setItems(FXCollections.observableArrayList(availableDocuments));
            cbAvailableDocuments.setButtonCell(new ListCell<DocumentResponseDTO>() {
                @Override
                protected void updateItem(DocumentResponseDTO item, boolean empty) {
                    super.updateItem(item, empty);
                    if (empty || item == null) {
                        setText(null);
                    } else {
                        setText(item.getDocType() + " - " + item.getFileName());
                    }
                }
            });
            cbAvailableDocuments.setCellFactory(param -> new ListCell<DocumentResponseDTO>() {
                @Override
                protected void updateItem(DocumentResponseDTO item, boolean empty) {
                    super.updateItem(item, empty);
                    if (empty || item == null) {
                        setText(null);
                    } else {
                        setText(item.getDocType() + " - " + item.getFileName());
                    }
                }
            });
        }

        loadPrograms();
        loadAvailableDocuments(); // Load available documents for linking

        // Initialize form state
        clearForm(); // This will set proper initial state
    }

    /**
     * Load all programs from backend.
     */
    private void loadPrograms() {
        AsyncUtils.run(
                () -> programService.getAllPrograms(),
                list -> {
                    programs.setAll(list);
                    updateProgramCount(list.size());
                    updateFilterPlaceholder(list);
                },
                ex -> AlertUtils.handleGenericError(ex, "load", "programs"));
    }

    /**
     * Update program count label.
     */
    private void updateProgramCount(int count) {
        if (lblProgramCount != null) {
            lblProgramCount.setText("Total: " + count + " program" + (count == 1 ? "" : "s"));
        }
    }

    /**
     * Refresh programs list.
     */
    @FXML
    private void onRefresh() {
        loadPrograms();
        clearForm();
    }

    /**
     * Search/filter programs using backend filter capability.
     * Supports multiple field filtering.
     */
    /**
     * Handle filter button click.
     * Filter programs by any combination of university, field of study, status, and
     * focus area.
     */
    @FXML
    private void onFilter() {
        String university = txtFilterUniversity != null ? txtFilterUniversity.getText() : null;
        String fieldOfStudy = txtFilterFieldOfStudy != null ? txtFilterFieldOfStudy.getText() : null;
        String status = txtFilterStatus != null ? txtFilterStatus.getText() : null;
        String focusArea = txtFilterFocusArea != null ? txtFilterFocusArea.getText() : null;
        String tuition = txtFilterTuition != null ? txtFilterTuition.getText() : null;
        String portal = txtFilterPortal != null ? txtFilterPortal.getText() : null;
        String website = txtFilterWebsite != null ? txtFilterWebsite.getText() : null;

        // Build filters map for backend
        Map<String, Object> filters = new HashMap<>();

        if (university != null && !university.isBlank()) {
            filters.put("universityName", university.trim());
        }
        if (fieldOfStudy != null && !fieldOfStudy.isBlank()) {
            filters.put("fieldOfStudy", fieldOfStudy.trim());
        }
        if (status != null && !status.isBlank()) {
            filters.put("status", status.trim());
        }
        if (focusArea != null && !focusArea.isBlank()) {
            filters.put("focusArea", focusArea.trim());
        }
        if (tuition != null && !tuition.isBlank()) {
            filters.put("tuition", tuition.trim());
        }
        if (portal != null && !portal.isBlank()) {
            filters.put("portal", portal.trim());
        }
        if (website != null && !website.isBlank()) {
            filters.put("website", website.trim());
        }

        // If no filters specified, load all programs
        if (filters.isEmpty()) {
            loadPrograms();
            return;
        }

        AsyncUtils.run(
                () -> programService.filterPrograms(filters),
                list -> {
                    programs.setAll(list);
                    updateProgramCount(list.size());
                    updateFilterPlaceholder(list);
                },
                ex -> AlertUtils.handleGenericError(ex, "filter", "programs"));
    }

    /**
     * Handle clear filter button click.
     * Clear all filter fields and reload all programs.
     */
    @FXML
    private void onClearFilter() {
        if (txtFilterUniversity != null)
            txtFilterUniversity.clear();
        if (txtFilterFieldOfStudy != null)
            txtFilterFieldOfStudy.clear();
        if (txtFilterStatus != null)
            txtFilterStatus.clear();
        if (txtFilterFocusArea != null)
            txtFilterFocusArea.clear();
        if (txtFilterTuition != null)
            txtFilterTuition.clear();
        if (txtFilterPortal != null)
            txtFilterPortal.clear();
        if (txtFilterWebsite != null)
            txtFilterWebsite.clear();
        loadPrograms();
    }

    /**
     * Update the placeholder message based on filter state and results.
     */
    private void updateFilterPlaceholder(List<ProgramDTO> programs) {
        if (tblPrograms == null)
            return;

        // Check if any filters are active
        boolean hasActiveFilters = (txtFilterUniversity != null && !txtFilterUniversity.getText().trim().isEmpty()) ||
                (txtFilterFieldOfStudy != null && !txtFilterFieldOfStudy.getText().trim().isEmpty()) ||
                (txtFilterStatus != null && !txtFilterStatus.getText().trim().isEmpty()) ||
                (txtFilterFocusArea != null && !txtFilterFocusArea.getText().trim().isEmpty()) ||
                (txtFilterTuition != null && !txtFilterTuition.getText().trim().isEmpty()) ||
                (txtFilterPortal != null && !txtFilterPortal.getText().trim().isEmpty()) ||
                (txtFilterWebsite != null && !txtFilterWebsite.getText().trim().isEmpty());

        if (programs.isEmpty()) {
            if (hasActiveFilters) {
                Label placeholderLabel = new Label("No results found");
                placeholderLabel.setStyle("-fx-text-fill: #9e9e9e; -fx-font-size: 14px;");
                tblPrograms.setPlaceholder(placeholderLabel);
            } else {
                Label placeholderLabel = new Label("No programs found. Add your first program below!");
                placeholderLabel.setStyle("-fx-text-fill: #9e9e9e; -fx-font-size: 14px;");
                tblPrograms.setPlaceholder(placeholderLabel);
            }
        }
    }

    /**
     * Advanced filter using form fields - filter by any non-empty field.
     */
    @FXML
    private void onAdvancedFilter() {
        Map<String, Object> filters = new HashMap<>();

        // Add all non-empty form fields as filters
        addFilterIfNotEmpty(filters, "universityName", txtUniversityName.getText());
        addFilterIfNotEmpty(filters, "fieldOfStudy", txtFieldOfStudy.getText());
        addFilterIfNotEmpty(filters, "focusArea", txtFocusArea.getText());
        addFilterIfNotEmpty(filters, "portal", txtPortal.getText());
        addFilterIfNotEmpty(filters, "website", txtWebsite.getText());
        addFilterIfNotEmpty(filters, "status", cmbStatus.getValue());
        addFilterIfNotEmpty(filters, "tuition", txtTuition.getText());
        addFilterIfNotEmpty(filters, "requirements", txtRequirements.getText());
        addFilterIfNotEmpty(filters, "notes", txtNotes.getText());

        // Handle deadline separately
        if (dpDeadline.getValue() != null) {
            filters.put("deadline", dpDeadline.getValue().toString());
        }

        if (filters.isEmpty()) {
            AlertUtils.showInfo("Filter Required", "Please fill in at least one field to filter by.");
            return;
        }

        AsyncUtils.run(
                () -> programService.filterPrograms(filters),
                list -> {
                    programs.setAll(list);
                    updateProgramCount(list.size());
                    AlertUtils.showInfo("Filter Applied",
                            "Found " + list.size() + " programs matching your criteria.");
                },
                ex -> AlertUtils.handleGenericError(ex, "filter", "programs"));
    }

    /**
     * Helper method to add non-empty values to filter map.
     */
    private void addFilterIfNotEmpty(Map<String, Object> filters, String key, String value) {
        if (value != null && !value.trim().isEmpty()) {
            filters.put(key, value.trim());
        }
    }

    /**
     * Add a new program.
     * Backend validates required fields.
     */
    @FXML
    private void onAdd() {
        // No frontend validation - always trust backend
        ProgramCreateDTO dto = new ProgramCreateDTO();
        dto.setUniversityName(emptyToNull(txtUniversityName.getText()));
        dto.setFieldOfStudy(emptyToNull(txtFieldOfStudy.getText()));
        dto.setFocusArea(emptyToNull(txtFocusArea.getText()));
        dto.setPortal(emptyToNull(txtPortal.getText()));
        dto.setWebsite(emptyToNull(txtWebsite.getText()));
        dto.setDeadline(dpDeadline.getValue());
        dto.setStatus(cmbStatus.getValue());
        dto.setTuition(emptyToNull(txtTuition.getText()));
        dto.setRequirements(emptyToNull(txtRequirements.getText()));
        dto.setNotes(emptyToNull(txtNotes.getText()));

        btnAdd.setDisable(true);
        AsyncUtils.run(
                () -> programService.createProgram(dto),
                created -> {
                    AlertUtils.showSuccess("Success", "Program added successfully.");
                    loadPrograms();
                    clearForm(); // Clear the form for next use
                },
                ex -> {
                    // Extract and show exact backend error message
                    String errorMessage = extractBackendErrorMessage(ex.getMessage());
                    AlertUtils.error("Error", errorMessage);
                },
                () -> btnAdd.setDisable(false));
    }

    /**
     * Toggle between view and edit modes for the selected program.
     */
    @FXML
    private void onEdit() {
        ProgramDTO sel = tblPrograms.getSelectionModel().getSelectedItem();
        if (sel == null) {
            AlertUtils.warn("Selection Error", "Please select a program to edit.");
            return;
        }

        // Check if form is currently editable
        if (txtUniversityName.isEditable()) {
            // Currently in edit mode - save changes
            saveCurrentProgram();
        } else {
            // Currently in view mode - switch to edit mode
            setFormEditable(true);
            btnEdit.setText("Save Changes");
            btnAdd.setVisible(false); // Keep Add hidden
            if (lblFormMode != null) {
                lblFormMode.setText("Editing: " + sel.getUniversityName() + " - " + sel.getFieldOfStudy());
            }
        }
    }

    /**
     * Save the currently edited program.
     */
    private void saveCurrentProgram() {
        ProgramDTO sel = tblPrograms.getSelectionModel().getSelectedItem();
        if (sel == null)
            return;

        // Build update DTO - partial updates supported
        ProgramUpdateDTO dto = new ProgramUpdateDTO();
        dto.setUniversityName(emptyToNull(txtUniversityName.getText()));
        dto.setFieldOfStudy(emptyToNull(txtFieldOfStudy.getText()));
        dto.setFocusArea(emptyToNull(txtFocusArea.getText()));
        dto.setPortal(emptyToNull(txtPortal.getText()));
        dto.setWebsite(emptyToNull(txtWebsite.getText()));
        dto.setDeadline(dpDeadline.getValue());
        dto.setStatus(cmbStatus.getValue());
        dto.setTuition(emptyToNull(txtTuition.getText()));
        dto.setRequirements(emptyToNull(txtRequirements.getText()));
        dto.setNotes(emptyToNull(txtNotes.getText()));

        btnEdit.setDisable(true);
        AsyncUtils.run(
                () -> programService.updateProgram(sel.getProgramId(), dto),
                updated -> {
                    AlertUtils.showSuccess("Success", "Program updated successfully.");
                    loadPrograms();
                    // Return to view mode
                    setFormEditable(false);
                    btnEdit.setText("Edit");
                    if (lblFormMode != null) {
                        lblFormMode
                                .setText("Viewing: " + updated.getUniversityName() + " - " + updated.getFieldOfStudy());
                    }
                },
                ex -> {
                    // Extract and show exact backend error message
                    String errorMessage = extractBackendErrorMessage(ex.getMessage());
                    AlertUtils.error("Error", errorMessage);
                },
                () -> btnEdit.setDisable(false));
    }

    /**
     * Delete selected program.
     */
    @FXML
    private void onDelete() {
        ProgramDTO sel = tblPrograms.getSelectionModel().getSelectedItem();
        if (sel == null) {
            AlertUtils.warn("Selection Error", "Please select a program to delete.");
            return;
        }

        if (!AlertUtils.confirm("Delete Program",
                "Are you sure you want to delete this program? This action cannot be undone.")) {
            return;
        }

        btnDelete.setDisable(true);
        AsyncUtils.run(
                () -> {
                    programService.deleteProgram(sel.getProgramId());
                    return null;
                },
                result -> {
                    AlertUtils.showSuccess("Success", "Program deleted successfully.");
                    loadPrograms();
                    clearForm();
                },
                ex -> AlertUtils.errorFromBackend(ex.getMessage()),
                () -> btnDelete.setDisable(false));
    }

    /**
     * Handle table row selection.
     * Populate form with selected program data.
     */
    @FXML
    private void onRowSelect() {
        ProgramDTO sel = tblPrograms.getSelectionModel().getSelectedItem();
        if (sel == null)
            return;

        selectedProgram = sel;
        txtUniversityName.setText(nullSafe(sel.getUniversityName()));
        txtFieldOfStudy.setText(nullSafe(sel.getFieldOfStudy()));
        txtFocusArea.setText(nullSafe(sel.getFocusArea()));
        txtPortal.setText(nullSafe(sel.getPortal()));
        txtWebsite.setText(nullSafe(sel.getWebsite()));
        dpDeadline.setValue(sel.getDeadline());
        cmbStatus.setValue(sel.getStatus());
        txtTuition.setText(nullSafe(sel.getTuition()));
        txtRequirements.setText(nullSafe(sel.getRequirements()));
        txtNotes.setText(nullSafe(sel.getNotes()));

        if (lblFormMode != null) {
            lblFormMode.setText("Viewing: " + sel.getUniversityName() + " - " + sel.getFieldOfStudy());
        }

        // Load linked documents for this program
        loadLinkedDocuments();

        // Set form to view-only mode
        setFormEditable(false);
    }

    /**
     * Set form fields to editable or read-only mode.
     */
    private void setFormEditable(boolean editable) {
        txtUniversityName.setEditable(editable);
        txtFieldOfStudy.setEditable(editable);
        txtFocusArea.setEditable(editable);
        txtPortal.setEditable(editable);
        txtWebsite.setEditable(editable);
        dpDeadline.setDisable(!editable);
        cmbStatus.setDisable(!editable);
        txtTuition.setEditable(editable);
        txtRequirements.setEditable(editable);
        txtNotes.setEditable(editable);

        // Update button visibility based on mode
        btnAdd.setVisible(false); // Never show Add when viewing existing program
        btnEdit.setVisible(selectedProgram != null); // Always show Edit/Save when program selected
        if (selectedProgram != null) {
            btnEdit.setText(editable ? "Save Changes" : "Edit");
        }
        btnDelete.setVisible(!editable && selectedProgram != null); // Show Delete when viewing
        btnClear.setText(editable ? "Cancel" : "Clear Selection");
    }

    /**
     * Show document management section for selected program.
     */
    @FXML
    private void onManageDocuments() {
        if (selectedProgram == null) {
            AlertUtils.warn("Selection Error", "Please select a program first.");
            return;
        }

        // Documents section is now always visible
        loadAvailableDocuments();
        loadLinkedDocuments();
    }

    /**
     * Load all available documents for linking.
     */
    private void loadAvailableDocuments() {
        System.out.println("=== LOADING AVAILABLE DOCUMENTS ===");
        AsyncUtils.run(
                () -> {
                    List<DocumentResponseDTO> docs = documentService.getAllDocuments();
                    System.out.println("Retrieved " + docs.size() + " documents from backend");
                    return docs;
                },
                docs -> {
                    System.out.println("Setting " + docs.size() + " documents in availableDocuments list");
                    availableDocuments.setAll(docs);
                    if (cbAvailableDocuments != null) {
                        cbAvailableDocuments.setItems(FXCollections.observableArrayList(availableDocuments));
                        System.out.println("Updated combo box with " + availableDocuments.size() + " documents");
                    }
                },
                ex -> {
                    System.out.println("ERROR loading documents: " + ex.getMessage());
                    AlertUtils.errorFromBackend(ex.getMessage());
                });
    }

    /**
     * Load documents already linked to the selected program.
     */
    private void loadLinkedDocuments() {
        if (selectedProgram == null)
            return;

        AsyncUtils.run(
                () -> programDocumentService.getAllProgramDocuments(selectedProgram.getProgramId()),
                links -> {
                    linkedDocuments.setAll(links);
                    updateLinkedDocumentsDisplay(links);
                },
                ex -> AlertUtils.errorFromBackend(ex.getMessage()));
    }

    /**
     * Update the linked documents ListView display.
     */
    private void updateLinkedDocumentsDisplay(List<ProgramDocumentDTO> links) {
        if (lstLinkedDocuments == null)
            return;

        lstLinkedDocuments.getItems().clear();

        if (links.isEmpty()) {
            lstLinkedDocuments.getItems().add("No documents linked yet");
            return;
        }

        // For each link, fetch document details and display
        for (ProgramDocumentDTO link : links) {
            AsyncUtils.run(
                    () -> documentService.getDocumentById(link.getDocumentId()),
                    doc -> {
                        String display = String.format("[ID:%d] %s - %s%s",
                                link.getProgramDocId(),
                                doc.getDocType(),
                                doc.getFileName(),
                                link.getUsageNotes() != null ? " (" + link.getUsageNotes() + ")" : "");
                        lstLinkedDocuments.getItems().add(display);
                    },
                    ex -> {
                        // Silently fail for individual document fetch
                        lstLinkedDocuments.getItems()
                                .add("[ID:" + link.getProgramDocId() + "] Document #" + link.getDocumentId());
                    });
        }
    }

    /**
     * Link selected document to the program.
     */
    @FXML
    private void onLinkDocument() {
        if (selectedProgram == null) {
            AlertUtils.warn("Selection Error", "Please select a program first.");
            return;
        }

        DocumentResponseDTO doc = cbAvailableDocuments.getValue();
        if (doc == null) {
            AlertUtils.warn("Selection Error", "Please select a document to link.");
            return;
        }

        String usageNotes = emptyToNull(txtUsageNotes.getText());

        ProgramDocumentCreateDTO dto = new ProgramDocumentCreateDTO();
        dto.setProgramId(selectedProgram.getProgramId());
        dto.setDocumentId(doc.getDocumentId());
        dto.setUsageNotes(usageNotes);

        btnLinkDocument.setDisable(true);
        AsyncUtils.run(
                () -> programDocumentService.createProgramDocument(dto),
                created -> {
                    AlertUtils.showSuccess("Success", "Document linked successfully.");
                    loadLinkedDocuments();
                    cbAvailableDocuments.setValue(null);
                    txtUsageNotes.clear();
                },
                ex -> AlertUtils.errorFromBackend(ex.getMessage()),
                () -> btnLinkDocument.setDisable(false));
    }

    /**
     * Unlink selected document from the program.
     */
    @FXML
    private void onUnlinkDocument() {
        String selected = lstLinkedDocuments.getSelectionModel().getSelectedItem();
        if (selected == null || selected.equals("No documents linked yet")) {
            AlertUtils.warn("Selection Error", "Please select a linked document to unlink.");
            return;
        }

        // Extract program document ID from the display string [ID:X]
        try {
            int startIdx = selected.indexOf("[ID:") + 4;
            int endIdx = selected.indexOf("]");
            int programDocId = Integer.parseInt(selected.substring(startIdx, endIdx));

            if (!AlertUtils.confirm("Unlink Document", "Are you sure you want to unlink this document?")) {
                return;
            }

            btnUnlinkDocument.setDisable(true);
            AsyncUtils.run(
                    () -> {
                        programDocumentService.deleteProgramDocument(programDocId);
                        return null;
                    },
                    result -> {
                        AlertUtils.showSuccess("Success", "Document unlinked successfully.");
                        loadLinkedDocuments();
                    },
                    ex -> AlertUtils.errorFromBackend(ex.getMessage()),
                    () -> btnUnlinkDocument.setDisable(false));
        } catch (Exception e) {
            AlertUtils.error("Error", "Could not parse document ID from selection.");
        }
    }

    /**
     * Refresh the documents list (both available and linked).
     */
    @FXML
    private void onRefreshDocuments() {
        loadAvailableDocuments();
        loadLinkedDocuments();
    }

    /**
     * Clear form and reset to "add new" mode.
     */
    @FXML
    private void onClear() {
        // If currently editing, ask for confirmation
        if (selectedProgram != null && txtUniversityName.isEditable()) {
            boolean confirmed = AlertUtils.showConfirmation(
                    "Cancel Changes",
                    "Are you sure you want to cancel your changes?");
            if (!confirmed) {
                return;
            }
            // Reload the selected program to restore original values
            onRowSelect();
        } else {
            // Just clear the form
            clearForm();
        }
    }

    /**
     * Show popup dialog to add a new program.
     */
    @FXML
    private void onShowAddForm() {
        showAddProgramDialog();
    }

    /**
     * Show a dialog popup for adding a new program.
     */
    private void showAddProgramDialog() {
        // Create a new dialog
        Dialog<ButtonType> dialog = new Dialog<>();
        dialog.setTitle("Add New Program");
        dialog.setHeaderText("Enter details for the new program");

        // Create form fields
        GridPane form = new GridPane();
        form.setHgap(15);
        form.setVgap(15);
        form.setPadding(new javafx.geometry.Insets(20));

        TextField txtUniversity = new TextField();
        txtUniversity.setPromptText("e.g., Stanford University");
        TextField txtField = new TextField();
        txtField.setPromptText("e.g., Computer Science");
        TextField txtFocus = new TextField();
        txtFocus.setPromptText("e.g., Machine Learning, AI");
        TextField txtPortalDialog = new TextField();
        txtPortalDialog.setPromptText("e.g., ApplyWeb, Slate");
        TextField txtWebsiteDialog = new TextField();
        txtWebsiteDialog.setPromptText("https://...");
        DatePicker dpDeadlineDialog = new DatePicker();
        dpDeadlineDialog.setPromptText("Select date");
        ComboBox<String> cmbStatusDialog = new ComboBox<>();
        cmbStatusDialog
                .setItems(FXCollections.observableArrayList("Accepted", "Applied", "In Progress", "Rejected", "Other"));
        cmbStatusDialog.setPromptText("Select status...");
        TextField txtTuitionDialog = new TextField();
        txtTuitionDialog.setPromptText("e.g., Full scholarship, $50,000/year");
        TextArea txtRequirementsDialog = new TextArea();
        txtRequirementsDialog.setPromptText("List required documents, tests, etc.");
        txtRequirementsDialog.setPrefRowCount(3);
        txtRequirementsDialog.setWrapText(true);
        TextArea txtNotesDialog = new TextArea();
        txtNotesDialog.setPromptText("Additional notes about this program...");
        txtNotesDialog.setPrefRowCount(3);
        txtNotesDialog.setWrapText(true);

        // Add fields to form
        form.add(new Label("University Name:"), 0, 0);
        form.add(txtUniversity, 1, 0);
        form.add(new Label("Field of Study:"), 0, 1);
        form.add(txtField, 1, 1);
        form.add(new Label("Focus Area:"), 0, 2);
        form.add(txtFocus, 1, 2);
        form.add(new Label("Application Portal:"), 0, 3);
        form.add(txtPortalDialog, 1, 3);
        form.add(new Label("Program Website:"), 0, 4);
        form.add(txtWebsiteDialog, 1, 4);
        form.add(new Label("Deadline:"), 0, 5);
        form.add(dpDeadlineDialog, 1, 5);
        form.add(new Label("Status:"), 0, 6);
        form.add(cmbStatusDialog, 1, 6);
        form.add(new Label("Tuition Info:"), 0, 7);
        form.add(txtTuitionDialog, 1, 7);
        form.add(new Label("Requirements:"), 0, 8);
        form.add(txtRequirementsDialog, 1, 8);
        form.add(new Label("Notes:"), 0, 9);
        form.add(txtNotesDialog, 1, 9);

        dialog.getDialogPane().setContent(form);
        dialog.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);

        // Handle OK button click
        dialog.setResultConverter(buttonType -> {
            if (buttonType == ButtonType.OK) {
                // Create the program
                ProgramCreateDTO dto = new ProgramCreateDTO();
                dto.setUniversityName(emptyToNull(txtUniversity.getText()));
                dto.setFieldOfStudy(emptyToNull(txtField.getText()));
                dto.setFocusArea(emptyToNull(txtFocus.getText()));
                dto.setPortal(emptyToNull(txtPortalDialog.getText()));
                dto.setWebsite(emptyToNull(txtWebsiteDialog.getText()));
                dto.setDeadline(dpDeadlineDialog.getValue());
                dto.setStatus(emptyToNull(cmbStatusDialog.getValue()));
                dto.setTuition(emptyToNull(txtTuitionDialog.getText()));
                dto.setRequirements(emptyToNull(txtRequirementsDialog.getText()));
                dto.setNotes(emptyToNull(txtNotesDialog.getText()));

                // Create program via API
                AsyncUtils.run(
                        () -> programService.createProgram(dto),
                        created -> {
                            AlertUtils.showSuccess("Success", "Program added successfully.");
                            loadPrograms(); // Refresh the list
                        },
                        ex -> {
                            // Extract and show exact backend error message
                            String errorMessage = extractBackendErrorMessage(ex.getMessage());
                            AlertUtils.error("Error", errorMessage);
                        });
            }
            return null;
        });

        dialog.showAndWait();
    }

    /**
     * Clear all form fields and reset state.
     */
    private void clearForm() {
        txtUniversityName.clear();
        txtFieldOfStudy.clear();
        txtFocusArea.clear();
        txtPortal.clear();
        txtWebsite.clear();
        dpDeadline.setValue(null);
        cmbStatus.setValue(null);
        txtTuition.clear();
        txtRequirements.clear();
        txtNotes.clear();

        if (lblFormMode != null) {
            lblFormMode.setText("Select a program to view details");
        }

        selectedProgram = null;
        tblPrograms.getSelectionModel().clearSelection();

        // Clear linked documents since no program is selected
        linkedDocuments.clear();

        // Reset form to default state (read-only and empty)
        setFormEditable(false);
        btnAdd.setVisible(false); // Hide Add button in main view
        btnEdit.setVisible(false);
        btnDelete.setVisible(false);
    }

    // Utility methods

    /**
     * Extract clean backend error message from exception message.
     * Handles JSON error responses and wrapper text.
     */
    private static String extractBackendErrorMessage(String fullMessage) {
        if (fullMessage == null) {
            return "An error occurred";
        }

        // Remove wrapper text like "Create failed: " or "Update failed: "
        String message = fullMessage;
        if (message.startsWith("Create failed: ")) {
            message = message.substring("Create failed: ".length());
        } else if (message.startsWith("Update failed: ")) {
            message = message.substring("Update failed: ".length());
        }

        // Try to parse JSON error response
        try {
            if (message.startsWith("{") && message.contains("message")) {
                com.fasterxml.jackson.databind.ObjectMapper mapper = new com.fasterxml.jackson.databind.ObjectMapper();
                com.fasterxml.jackson.databind.JsonNode node = mapper.readTree(message);
                if (node.has("message")) {
                    return node.get("message").asText();
                }
            }
        } catch (Exception e) {
            // If JSON parsing fails, fall back to raw message
        }

        return message;
    }

    private static String nullSafe(String s) {
        return s == null ? "" : s;
    }

    private static String emptyToNull(String s) {
        return (s == null || s.isBlank()) ? null : s.trim();
    }

    private static String dateToString(LocalDate d) {
        return d == null ? "" : d.toString();
    }
}
