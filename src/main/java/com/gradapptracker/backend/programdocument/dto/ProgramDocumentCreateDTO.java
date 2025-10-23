package com.gradapptracker.backend.programdocument.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * DTO for creating a ProgramDocument link.
 */
@Data
public class ProgramDocumentCreateDTO {

    @NotNull(message = "programId is required")
    private Integer programId;

    @NotNull(message = "documentId is required")
    private Integer documentId;

    private String usageNotes;

    // explicit getters/setters for clarity
    public Integer getProgramId() {
        return programId;
    }

    public void setProgramId(Integer programId) {
        this.programId = programId;
    }

    public Integer getDocumentId() {
        return documentId;
    }

    public void setDocumentId(Integer documentId) {
        this.documentId = documentId;
    }

    public String getUsageNotes() {
        return usageNotes;
    }

    public void setUsageNotes(String usageNotes) {
        this.usageNotes = usageNotes;
    }
}
