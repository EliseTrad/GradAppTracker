package com.gradapptracker.programdocument.dto;

import lombok.Data;

/**
 * DTO representing a ProgramDocument link.
 * Only contains IDs for related objects (no nested entities).
 */
@Data
public class ProgramDocumentDTO {

    private Integer programDocId;

    private Integer programId;

    private Integer documentId;

    private String usageNotes;

}
