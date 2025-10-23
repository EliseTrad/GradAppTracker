package com.gradapptracker.ui.services;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.gradapptracker.backend.programdocument.dto.ProgramDocumentCreateDTO;
import com.gradapptracker.backend.programdocument.dto.ProgramDocumentDTO;
import com.gradapptracker.backend.programdocument.dto.ProgramDocumentUpdateDTO;

import java.net.http.HttpResponse;
import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * JavaFX frontend service for ProgramDocument operations.
 * <p>
 * Uses {@link ApiClient} for HTTP and Jackson's {@link ObjectMapper} for JSON.
 * All requests are made against the backend path "/programdocuments".
 * Methods throw RuntimeException for non-success responses (except filter which
 * returns an empty list on failure). All exceptions are wrapped in
 * RuntimeException.
 */
public class ProgramDocumentServiceFx extends ApiClient {

    private final ObjectMapper mapper = new ObjectMapper();
    private final String basePath = "/programdocuments";

    public ProgramDocumentServiceFx() {
        super();
    }

    /**
     * Create a new ProgramDocument linkage.
     */
    public ProgramDocumentDTO createProgramDocument(ProgramDocumentCreateDTO dto) {
        try {
            String json = mapper.writeValueAsString(dto);
            HttpResponse<String> resp = POST(basePath, json, true);
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
            HttpResponse<String> resp = GET(basePath + "?programId=" + programId, true);
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
     * Get a single ProgramDocument by id.
     */
    public ProgramDocumentDTO getProgramDocumentById(int programDocumentId) {
        try {
            HttpResponse<String> resp = GET(basePath + "/" + programDocumentId, true);
            if (resp.statusCode() / 100 == 2) {
                return mapper.readValue(resp.body(), ProgramDocumentDTO.class);
            }
            throw new RuntimeException("Get failed: " + resp.body());
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Update an existing ProgramDocument.
     */
    public ProgramDocumentDTO updateProgramDocument(int programDocumentId, ProgramDocumentUpdateDTO dto) {
        try {
            String json = mapper.writeValueAsString(dto);
            HttpResponse<String> resp = PUT(basePath + "/" + programDocumentId, json, true);
            if (resp.statusCode() / 100 == 2) {
                return mapper.readValue(resp.body(), ProgramDocumentDTO.class);
            }
            throw new RuntimeException("Update failed: " + resp.body());
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Delete a ProgramDocument by id.
     */
    public void deleteProgramDocument(int programDocumentId) {
        try {
            HttpResponse<String> resp = DELETE(basePath + "/" + programDocumentId, true);
            if (resp.statusCode() / 100 == 2) {
                return;
            }
            throw new RuntimeException("Delete failed: " + resp.body());
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Filter program documents. The backend supports POST /programdocuments/filter.
     * On failure this method returns an empty list rather than throwing.
     */
    public List<ProgramDocumentDTO> filterProgramDocuments(Map<String, Object> filters) {
        try {
            String json = mapper.writeValueAsString(filters);
            HttpResponse<String> resp = POST(basePath + "/filter", json, true);
            if (resp.statusCode() / 100 == 2) {
                return mapper.readValue(resp.body(), new TypeReference<List<ProgramDocumentDTO>>() {
                });
            }
            return Collections.emptyList();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
