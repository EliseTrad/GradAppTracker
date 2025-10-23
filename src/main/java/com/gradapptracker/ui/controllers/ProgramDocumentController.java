package com.gradapptracker.ui.controllers;

import com.gradapptracker.backend.programdocument.dto.ProgramDocumentCreateDTO;
import com.gradapptracker.backend.programdocument.dto.ProgramDocumentDTO;
import com.gradapptracker.backend.programdocument.dto.ProgramDocumentUpdateDTO;
import com.gradapptracker.ui.services.ProgramDocumentServiceFx;
import com.gradapptracker.ui.utils.AlertUtils;
import javafx.application.Platform;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

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
    public void initialize() {
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
        try {
            int programId = Integer.parseInt(txtProgramId.getText().trim());
            int documentId = Integer.parseInt(txtDocumentId.getText().trim());
            String usage = txtUsageNotes.getText();
            ProgramDocumentCreateDTO dto = new ProgramDocumentCreateDTO();
            dto.setProgramId(programId);
            dto.setDocumentId(documentId);
            dto.setUsageNotes(usage);

            new Thread(() -> {
                try {
                    ProgramDocumentDTO created = service.createProgramDocument(dto);
                    Platform.runLater(() -> {
                        items.add(created);
                        AlertUtils.info("Created", "ProgramDocument created.");
                    });
                } catch (Exception e) {
                    Platform.runLater(() -> AlertUtils.error("Error", e.getMessage()));
                }
            }).start();
        } catch (NumberFormatException nfe) {
            AlertUtils.error("Invalid input", "Program ID and Document ID must be integers.");
        }
    }

    /**
     * Edit the selected program-document link using UI fields.
     */
    @FXML
    public void onEdit() {
        ProgramDocumentDTO sel = table.getSelectionModel().getSelectedItem();
        if (sel == null) {
            AlertUtils.warn("No selection", "Please select a row to edit.");
            return;
        }
        try {
            String usage = txtUsageNotes.getText();
            ProgramDocumentUpdateDTO dto = new ProgramDocumentUpdateDTO();
            dto.setUsageNotes(usage);

            new Thread(() -> {
                try {
                    ProgramDocumentDTO updated = service.updateProgramDocument(sel.getProgramDocId(), dto);
                    Platform.runLater(() -> {
                        int idx = items.indexOf(sel);
                        items.set(idx, updated);
                        AlertUtils.info("Updated", "ProgramDocument updated.");
                    });
                } catch (Exception e) {
                    Platform.runLater(() -> AlertUtils.error("Error", e.getMessage()));
                }
            }).start();
        } catch (Exception e) {
            AlertUtils.error("Error", e.getMessage());
        }
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
        new Thread(() -> {
            try {
                service.deleteProgramDocument(sel.getProgramDocId());
                Platform.runLater(() -> {
                    items.remove(sel);
                    AlertUtils.info("Deleted", "ProgramDocument deleted.");
                });
            } catch (Exception e) {
                Platform.runLater(() -> AlertUtils.error("Error", e.getMessage()));
            }
        }).start();
    }

    /**
     * Refresh the list for the programId entered in txtProgramId.
     */
    @FXML
    public void onRefresh() {
        try {
            int programId = Integer.parseInt(txtProgramId.getText().trim());
            new Thread(() -> {
                try {
                    List<ProgramDocumentDTO> list = service.getAllProgramDocuments(programId);
                    Platform.runLater(() -> items.setAll(list));
                } catch (Exception e) {
                    Platform.runLater(() -> AlertUtils.error("Error", e.getMessage()));
                }
            }).start();
        } catch (NumberFormatException nfe) {
            AlertUtils.error("Invalid input", "Program ID must be an integer.");
        } catch (Exception e) {
            AlertUtils.error("Error", e.getMessage());
        }
    }

    /**
     * Apply a simple filter using the selected key and filter value.
     */
    @FXML
    public void onFilter() {
        String key = cbFilterBy.getValue();
        String val = txtFilterValue.getText();
        if (key == null || key.isBlank()) {
            AlertUtils.warn("No filter", "Select a filter key.");
            return;
        }
        Map<String, Object> filters = new HashMap<>();
        filters.put(key, val);
        new Thread(() -> {
            try {
                List<ProgramDocumentDTO> list = service.filterProgramDocuments(filters);
                Platform.runLater(() -> items.setAll(list));
            } catch (Exception e) {
                Platform.runLater(() -> AlertUtils.error("Error", e.getMessage()));
            }
        }).start();
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
}
