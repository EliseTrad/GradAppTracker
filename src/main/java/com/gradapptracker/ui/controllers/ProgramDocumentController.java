package com.gradapptracker.ui.controllers;

import com.gradapptracker.backend.programdocument.dto.ProgramDocumentCreateDTO;
import com.gradapptracker.backend.programdocument.dto.ProgramDocumentDTO;
import com.gradapptracker.ui.services.ProgramDocumentServiceFx;
import com.gradapptracker.ui.utils.AlertUtils;
import com.gradapptracker.ui.utils.AsyncUtils;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.image.ImageView;

/**
 * JavaFX controller for managing ProgramDocument links in the UI.
 * <p>
 * FXML should bind controls named in the fields below. Methods are concise
 * and forward calls to {@link ProgramDocumentServiceFx} while showing alerts
 * for errors and confirmations.
 */
public class ProgramDocumentController {

    private final ProgramDocumentServiceFx service = new ProgramDocumentServiceFx();
    private final ObservableList<ProgramDocumentDTO> items = FXCollections.observableArrayList();

    @FXML
    private TableView<ProgramDocumentDTO> table;
    @FXML
    private TableColumn<ProgramDocumentDTO, Integer> colId;
    @FXML
    private TableColumn<ProgramDocumentDTO, Integer> colProgramId;
    @FXML
    private TableColumn<ProgramDocumentDTO, Integer> colDocumentId;
    @FXML
    private TableColumn<ProgramDocumentDTO, String> colUsageNotes;

    @FXML
    private TextField txtProgramId;
    @FXML
    private TextField txtDocumentId;
    @FXML
    private TextField txtUsageNotes;

    @FXML
    private Button btnAdd;
    @FXML
    private Button btnEdit;
    @FXML
    private Button btnDelete;
    @FXML
    private Button btnRefresh;
    @FXML
    private Button btnFilter;

    @FXML
    private ComboBox<String> cbFilterBy; // example control for choosing filter key
    @FXML
    private TextField txtFilterValue;

    @FXML
    private ImageView logoImage;

    @FXML
    private Button btnBackPrograms;

    @FXML
    public void initialize() {
        try {
            logoImage.setImage(new javafx.scene.image.Image(getClass().getResourceAsStream("/logo.png")));
        } catch (Exception e) {
            // Logo not found, skip
        }

        table.setItems(items);
        // Basic column wiring if not done in FXML; use proper JavaFX properties
        colId.setCellValueFactory(cell -> new SimpleIntegerProperty(cell.getValue().getProgramDocId()).asObject());
        colProgramId.setCellValueFactory(cell -> new SimpleIntegerProperty(cell.getValue().getProgramId()).asObject());
        colDocumentId
                .setCellValueFactory(cell -> new SimpleIntegerProperty(cell.getValue().getDocumentId()).asObject());
        colUsageNotes.setCellValueFactory(cell -> new SimpleStringProperty(cell.getValue().getUsageNotes()));

        cbFilterBy.getItems().setAll("usageNotes", "documentId", "programId");
    }

    /**
     * Add a new program-document link using values from UI fields.
     */
    @FXML
    public void onAdd() {
        int programId = Integer.parseInt(txtProgramId.getText().trim());
        int documentId = Integer.parseInt(txtDocumentId.getText().trim());
        String usage = txtUsageNotes.getText();
        ProgramDocumentCreateDTO dto = new ProgramDocumentCreateDTO();
        dto.setProgramId(programId);
        dto.setDocumentId(documentId);
        dto.setUsageNotes(usage);

        AsyncUtils.run(() -> service.createProgramDocument(dto), created -> {
            items.add(created);
            AlertUtils.info("Created", "ProgramDocument created.");
        }, ex -> AlertUtils.error("Error", ex == null ? "Unknown error" : ex.getMessage()));
    }

    /**
     * Edit not supported - backend has no update endpoint for program-document
     * links.
     * To change a link, delete and recreate it.
     */
    @FXML
    public void onEdit() {
        AlertUtils.warn("Not Supported", "Editing links is not supported. Delete and recreate the link instead.");
    }

    /**
     * Delete the selected program-document link with confirmation.
     */
    @FXML
    public void onDelete() {
        ProgramDocumentDTO sel = table.getSelectionModel().getSelectedItem();
        if (sel == null) {
            AlertUtils.warn("No selection", "Please select a row to delete.");
            return;
        }
        boolean ok = AlertUtils.confirm("Confirm delete", "Delete selected link? This cannot be undone.");
        if (!ok)
            return;
        AsyncUtils.run(() -> {
            service.deleteProgramDocument(sel.getProgramDocId());
            return null;
        }, unused -> {
            items.remove(sel);
            AlertUtils.info("Deleted", "ProgramDocument deleted.");
        }, ex -> AlertUtils.error("Error", ex == null ? "Unknown error" : ex.getMessage()));
    }

    /**
     * Refresh the list for the programId entered in txtProgramId.
     */
    @FXML
    public void onRefresh() {
        int programId = Integer.parseInt(txtProgramId.getText().trim());
        AsyncUtils.run(() -> service.getAllProgramDocuments(programId), list -> items.setAll(list),
                ex -> AlertUtils.error("Error", ex == null ? "Unknown error" : ex.getMessage()));
    }

    /**
     * Filter not supported - backend has no filter endpoint for program-document
     * links.
     */
    @FXML
    public void onFilter() {
        AlertUtils.warn("Not Supported", "Filtering is not supported. Use Refresh to load links for a program.");
    }

    // Optional helper to pre-fill fields when a table row is selected
    @FXML
    public void onRowSelect() {
        ProgramDocumentDTO sel = table.getSelectionModel().getSelectedItem();
        if (sel == null)
            return;
        txtProgramId.setText(String.valueOf(sel.getProgramId()));
        txtDocumentId.setText(String.valueOf(sel.getDocumentId()));
        txtUsageNotes.setText(sel.getUsageNotes());
    }

    @FXML
    private void onBackPrograms() {
        MainLayoutController.getInstance().setContent("/com/gradapptracker/ui/views/ProgramView.fxml");
    }
}
