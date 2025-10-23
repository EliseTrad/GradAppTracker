package com.gradapptracker.ui.controllers;

import com.gradapptracker.backend.document.dto.DocumentCreateDTO;
import com.gradapptracker.backend.document.dto.DocumentResponseDTO;
import com.gradapptracker.backend.document.dto.DocumentUpdateDTO;
import com.gradapptracker.ui.services.DocumentServiceFx;
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
 * JavaFX controller for managing Documents.
 */
public class DocumentController {

    private final DocumentServiceFx service = new DocumentServiceFx();
    private final ObservableList<DocumentResponseDTO> items = FXCollections.observableArrayList();

    @FXML
    private TableView<DocumentResponseDTO> table;
    @FXML
    private TableColumn<DocumentResponseDTO, Integer> colId;
    @FXML
    private TableColumn<DocumentResponseDTO, Integer> colUserId;
    @FXML
    private TableColumn<DocumentResponseDTO, String> colFileName;
    @FXML
    private TableColumn<DocumentResponseDTO, String> colDocType;

    @FXML
    private TextField txtFileName;
    @FXML
    private TextField txtDocType;
    @FXML
    private TextField txtNotes;

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
    private ComboBox<String> cbFilterBy;
    @FXML
    private TextField txtFilterValue;

    @FXML
    public void initialize() {
        table.setItems(items);
        colId.setCellValueFactory(c -> new SimpleIntegerProperty(c.getValue().getDocumentId()).asObject());
        colUserId.setCellValueFactory(c -> new SimpleIntegerProperty(c.getValue().getUserId()).asObject());
        colFileName.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getFileName()));
        colDocType.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getDocType()));

        cbFilterBy.getItems().setAll("fileName", "docType", "userId");
    }

    @FXML
    public void onAdd() {
        String fileName = txtFileName.getText().trim();
        String docType = txtDocType.getText().trim();
        String notes = txtNotes.getText().trim();
        DocumentCreateDTO dto = new DocumentCreateDTO();
        dto.setFileName(fileName);
        dto.setDocType(docType);
        dto.setNotes(notes);

        new Thread(() -> {
            try {
                DocumentResponseDTO created = service.createDocument(dto);
                Platform.runLater(() -> {
                    items.add(created);
                    AlertUtils.info("Created", "Document created.");
                });
            } catch (Exception e) {
                Platform.runLater(() -> AlertUtils.error("Error", e.getMessage()));
            }
        }).start();
    }

    @FXML
    public void onEdit() {
        DocumentResponseDTO sel = table.getSelectionModel().getSelectedItem();
        if (sel == null) {
            AlertUtils.warn("No selection", "Select a document to edit.");
            return;
        }

        DocumentUpdateDTO dto = new DocumentUpdateDTO();
        dto.setFileName(txtFileName.getText().trim());
        dto.setDocType(txtDocType.getText().trim());
        dto.setNotes(txtNotes.getText().trim());

        new Thread(() -> {
            try {
                DocumentResponseDTO updated = service.updateDocument(sel.getDocumentId(), dto);
                Platform.runLater(() -> {
                    int idx = items.indexOf(sel);
                    items.set(idx, updated);
                    AlertUtils.info("Updated", "Document updated.");
                });
            } catch (Exception e) {
                Platform.runLater(() -> AlertUtils.error("Error", e.getMessage()));
            }
        }).start();
    }

    @FXML
    public void onDelete() {
        DocumentResponseDTO sel = table.getSelectionModel().getSelectedItem();
        if (sel == null) {
            AlertUtils.warn("No selection", "Select a document to delete.");
            return;
        }
        boolean ok = AlertUtils.confirm("Confirm delete", "Delete selected document?");
        if (!ok)
            return;
        new Thread(() -> {
            try {
                service.deleteDocument(sel.getDocumentId());
                Platform.runLater(() -> {
                    items.remove(sel);
                    AlertUtils.info("Deleted", "Document deleted.");
                });
            } catch (Exception e) {
                Platform.runLater(() -> AlertUtils.error("Error", e.getMessage()));
            }
        }).start();
    }

    @FXML
    public void onRefresh() {
        new Thread(() -> {
            try {
                List<DocumentResponseDTO> list = service.getAllDocuments();
                Platform.runLater(() -> items.setAll(list));
            } catch (Exception e) {
                Platform.runLater(() -> AlertUtils.error("Error", e.getMessage()));
            }
        }).start();
    }

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
                List<DocumentResponseDTO> list = service.filterDocuments(filters);
                Platform.runLater(() -> items.setAll(list));
            } catch (Exception e) {
                Platform.runLater(() -> AlertUtils.error("Error", e.getMessage()));
            }
        }).start();
    }

    @FXML
    public void onRowSelect() {
        DocumentResponseDTO sel = table.getSelectionModel().getSelectedItem();
        if (sel == null)
            return;
        txtFileName.setText(sel.getFileName());
        txtDocType.setText(sel.getDocType());
        txtNotes.setText(sel.getNotes());
    }
}
