package com.gradapptracker.backend.document.dto;

import jakarta.validation.constraints.NotBlank;

/**
 * DTO used by controller for creating a Document.
 * Note: MultipartFile `file` is handled at controller layer only and not stored
 * on this DTO for service.
 */
public class DocumentCreateDTO {

    @NotBlank
    private String docType;

    private String notes;

    private String fileName;

    public DocumentCreateDTO() {
    }

    public String getDocType() {
        return docType;
    }

    public void setDocType(String docType) {
        this.docType = docType;
    }

    public String getNotes() {
        return notes;
    }

    public void setNotes(String notes) {
        this.notes = notes;
    }

    public String getFileName() {
        return fileName;
    }

    public void setFileName(String fileName) {
        this.fileName = fileName;
    }
}
