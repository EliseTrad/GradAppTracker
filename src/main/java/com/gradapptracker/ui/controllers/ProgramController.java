package com.gradapptracker.ui.controllers;

import com.gradapptracker.backend.program.dto.ProgramCreateDTO;
import com.gradapptracker.backend.program.dto.ProgramDTO;
import com.gradapptracker.backend.program.dto.ProgramUpdateDTO;
import com.gradapptracker.ui.services.ProgramServiceFx;
import com.gradapptracker.ui.utils.AlertUtils;
import javafx.application.Platform;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.*;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * JavaFX controller for listing and editing Programs.
 */
public class ProgramController {

    private final ProgramServiceFx service = new ProgramServiceFx();
    private final ObservableList<ProgramDTO> items = FXCollections.observableArrayList();

    @FXML
    private TableView<ProgramDTO> table;
    @FXML
    private TableColumn<ProgramDTO, Integer> colId;
    @FXML
    private TableColumn<ProgramDTO, String> colName;
    @FXML
    private TableColumn<ProgramDTO, String> colDeadline;
    @FXML
    private TableColumn<ProgramDTO, String> colStatus;

    @FXML
    private TextField txtName;
    @FXML
    private TextField txtDeadline; // yyyy-MM-dd
    @FXML
    private TextField txtStatus;

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
        colId.setCellValueFactory(c -> new SimpleIntegerProperty(c.getValue().getProgramId()).asObject());
        colName.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getUniversityName()));
        colDeadline.setCellValueFactory(c -> new SimpleStringProperty(
                c.getValue().getDeadline() == null ? "" : c.getValue().getDeadline().toString()));
        colStatus.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getStatus()));

        cbFilterBy.getItems().setAll("name", "deadline", "status");
    }

    @FXML
    public void onAdd() {
        try {
            String name = txtName.getText().trim();
            LocalDate deadline = txtDeadline.getText().isBlank() ? null : LocalDate.parse(txtDeadline.getText().trim());
            String status = txtStatus.getText().trim();

            ProgramCreateDTO dto = new ProgramCreateDTO();
            dto.setUniversityName(name);
            dto.setDeadline(deadline);
            dto.setStatus(status);

            new Thread(() -> {
                try {
                    ProgramDTO created = service.createProgram(dto);
                    Platform.runLater(() -> {
                        items.add(created);
                        AlertUtils.info("Created", "Program created.");
                    });
                } catch (Exception e) {
                    Platform.runLater(() -> AlertUtils.error("Error", e.getMessage()));
                }
            }).start();
        } catch (Exception e) {
            AlertUtils.error("Invalid input", e.getMessage());
        }
    }

    @FXML
    public void onEdit() {
        ProgramDTO sel = table.getSelectionModel().getSelectedItem();
        if (sel == null) {
            AlertUtils.warn("No selection", "Select a program to edit.");
            return;
        }
        try {
            String name = txtName.getText().trim();
            LocalDate deadline = txtDeadline.getText().isBlank() ? null : LocalDate.parse(txtDeadline.getText().trim());
            String status = txtStatus.getText().trim();

            ProgramUpdateDTO dto = new ProgramUpdateDTO();
            dto.setUniversityName(name);
            dto.setDeadline(deadline);
            dto.setStatus(status);

            new Thread(() -> {
                try {
                    ProgramDTO updated = service.updateProgram(sel.getProgramId(), dto);
                    Platform.runLater(() -> {
                        int idx = items.indexOf(sel);
                        items.set(idx, updated);
                        AlertUtils.info("Updated", "Program updated.");
                    });
                } catch (Exception e) {
                    Platform.runLater(() -> AlertUtils.error("Error", e.getMessage()));
                }
            }).start();
        } catch (Exception e) {
            AlertUtils.error("Invalid input", e.getMessage());
        }
    }

    @FXML
    public void onDelete() {
        ProgramDTO sel = table.getSelectionModel().getSelectedItem();
        if (sel == null) {
            AlertUtils.warn("No selection", "Select a program to delete.");
            return;
        }
        boolean ok = AlertUtils.confirm("Confirm delete", "Delete selected program?");
        if (!ok)
            return;
        new Thread(() -> {
            try {
                service.deleteProgram(sel.getProgramId());
                Platform.runLater(() -> {
                    items.remove(sel);
                    AlertUtils.info("Deleted", "Program deleted.");
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
                List<ProgramDTO> list = service.getAllPrograms();
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
                List<ProgramDTO> list = service.filterPrograms(filters);
                Platform.runLater(() -> items.setAll(list));
            } catch (Exception e) {
                Platform.runLater(() -> AlertUtils.error("Error", e.getMessage()));
            }
        }).start();
    }

    @FXML
    public void onRowSelect() {
        ProgramDTO sel = table.getSelectionModel().getSelectedItem();
        if (sel == null)
            return;
        txtName.setText(sel.getUniversityName());
        txtDeadline.setText(sel.getDeadline() == null ? "" : sel.getDeadline().toString());
        txtStatus.setText(sel.getStatus());
    }
}
