package com.gradapptracker.backend.programdocument.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * DTO for creating a ProgramDocument link.
 */
@Data
public class ProgramDocumentCreateDTO {

    @NotNull(message = "documentId is required")
    private Integer documentId;

    private String usageNotes;

}

