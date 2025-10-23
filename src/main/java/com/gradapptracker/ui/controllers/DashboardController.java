package com.gradapptracker.ui.controllers;

import com.gradapptracker.backend.program.dto.ProgramDTO;
import com.gradapptracker.backend.programdocument.dto.ProgramDocumentDTO;
import com.gradapptracker.ui.services.ProgramDocumentServiceFx;
import com.gradapptracker.ui.services.ProgramServiceFx;
import com.gradapptracker.ui.utils.AlertUtils;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.TableView;

import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Dashboard controller: loads programs and program-document stats for UI
 * overview.
 */
public class DashboardController {

    private final ProgramServiceFx programService = new ProgramServiceFx();
    private final ProgramDocumentServiceFx pdService = new ProgramDocumentServiceFx();

    public final ObservableList<ProgramDTO> programs = FXCollections.observableArrayList();
    public final ObservableList<ProgramDocumentDTO> programDocuments = FXCollections.observableArrayList();

    @FXML
    private TableView<ProgramDTO> tblPrograms;
    @FXML
    private TableView<ProgramDocumentDTO> tblProgramDocuments;

    @FXML
    private Label lblProgramCount;
    @FXML
    private Label lblUpcomingDeadlines; // simple text summary
    @FXML
    private Label lblDocumentLinks;

    @FXML
    public void initialize() {
        tblPrograms.setItems(programs);
        tblProgramDocuments.setItems(programDocuments);
        loadPrograms();
        loadDocumentStats();
    }

    public void loadPrograms() {
        new Thread(() -> {
            try {
                List<ProgramDTO> list = programService.getAllPrograms();
                Platform.runLater(() -> {
                    programs.setAll(list);
                    lblProgramCount.setText(String.valueOf(list.size()));

                    // upcoming deadlines: next 5
                    String upcoming = list.stream()
                            .filter(p -> p.getDeadline() != null && p.getDeadline().isAfter(LocalDate.now()))
                            .sorted((a, b) -> a.getDeadline().compareTo(b.getDeadline()))
                            .limit(5)
                            .map(p -> p.getUniversityName() + ": " + p.getDeadline())
                            .collect(Collectors.joining("\n"));
                    lblUpcomingDeadlines.setText(upcoming.isEmpty() ? "None" : upcoming);
                });
            } catch (Exception e) {
                Platform.runLater(() -> AlertUtils.error("Error loading programs", e.getMessage()));
            }
        }).start();
    }

    public void loadDocumentStats() {
        new Thread(() -> {
            try {
                List<ProgramDocumentDTO> list = pdService.getAllProgramDocuments(0); // get all or modify API to support
                                                                                     // no-program
                Platform.runLater(() -> {
                    programDocuments.setAll(list);
                    lblDocumentLinks.setText(String.valueOf(list.size()));
                });
            } catch (Exception e) {
                Platform.runLater(() -> AlertUtils.error("Error loading document stats", e.getMessage()));
            }
        }).start();
    }
}
