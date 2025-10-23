package com.gradapptracker.backend.document.dto;

/**
 * DTO for updating a document. All fields optional.
 * Note: file upload is handled at controller level as MultipartFile; service
 * takes filePath/fileName when replacing.
 */
public class DocumentUpdateDTO {

    private String fileName;

    /**
     * Optional filePath for service-level replacement. Controller should save the
     * uploaded MultipartFile to disk and provide its path here when replacing the
     * document file.
     */
    private String filePath;

    private String docType;

    private String notes;

    public DocumentUpdateDTO() {
    }

    public String getFileName() {
        return fileName;
    }

    public void setFileName(String fileName) {
        this.fileName = fileName;
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

    public String getFilePath() {
        return filePath;
    }

    public void setFilePath(String filePath) {
        this.filePath = filePath;
    }
}

