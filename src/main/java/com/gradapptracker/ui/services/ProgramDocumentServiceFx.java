package com.gradapptracker.ui.services;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.gradapptracker.backend.programdocument.dto.ProgramDocumentCreateDTO;
import com.gradapptracker.backend.programdocument.dto.ProgramDocumentDTO;

import java.net.http.HttpResponse;
import java.util.List;

/**
 * JavaFX frontend service for ProgramDocument operations.
 * <p>
 * Uses {@link ApiClient} for HTTP and Jackson's {@link ObjectMapper} for JSON.
 * Backend endpoints are under /api/programs/{programId}/documents and
 * /api/program-docs/{id}.
 * All exceptions are wrapped in RuntimeException.
 */
public class ProgramDocumentServiceFx extends ApiClient {

    private final ObjectMapper mapper = new ObjectMapper();

    public ProgramDocumentServiceFx() {
        super();
    }

    /**
     * Create a new ProgramDocument linkage.
     */
    public ProgramDocumentDTO createProgramDocument(ProgramDocumentCreateDTO dto) {
        try {
            String json = mapper.writeValueAsString(dto);
            HttpResponse<String> resp = POST("/programs/" + dto.getProgramId() + "/documents", json, true);
            if (resp.statusCode() / 100 == 2) {
                return mapper.readValue(resp.body(), ProgramDocumentDTO.class);
            }
            throw new RuntimeException("Create failed: " + resp.body());
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Get all ProgramDocument entries for a given program id.
     */
    public List<ProgramDocumentDTO> getAllProgramDocuments(int programId) {
        try {
            HttpResponse<String> resp = GET("/programs/" + programId + "/documents", true);
            if (resp.statusCode() / 100 == 2) {
                return mapper.readValue(resp.body(), new TypeReference<List<ProgramDocumentDTO>>() {
                });
            }
            throw new RuntimeException("Get all failed: " + resp.body());
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Delete a ProgramDocument by id.
     * Backend endpoint: DELETE /api/program-docs/{programDocId}
     */
    public void deleteProgramDocument(int programDocumentId) {
        try {
            HttpResponse<String> resp = DELETE("/program-docs/" + programDocumentId, true);
            if (resp.statusCode() / 100 == 2) {
                return;
            }
            throw new RuntimeException("Delete failed: " + resp.body());
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    // NOTE: Backend does NOT support the following operations:
    // - getProgramDocumentById (no such endpoint)
    // - updateProgramDocument (no such endpoint)
    // - filterProgramDocuments (no such endpoint)
    // If these are needed, they must be added to the backend first.
}
