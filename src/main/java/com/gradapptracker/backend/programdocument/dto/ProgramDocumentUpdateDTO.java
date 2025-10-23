package com.gradapptracker.backend.programdocument.dto;

import lombok.Data;

/**
 * DTO for updating an existing ProgramDocument link.
 */
@Data
public class ProgramDocumentUpdateDTO {

    private String usageNotes;

    public String getUsageNotes() {
        return usageNotes;
    }

    public void setUsageNotes(String usageNotes) {
        this.usageNotes = usageNotes;
    }

}
