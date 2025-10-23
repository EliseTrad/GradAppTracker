package com.gradapptracker.ui.services;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.gradapptracker.backend.document.dto.DocumentCreateDTO;
import com.gradapptracker.backend.document.dto.DocumentResponseDTO;
import com.gradapptracker.backend.document.dto.DocumentUpdateDTO;

import java.net.http.HttpResponse;
import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * UI-side service for Document CRUD/filter operations.
 */
public class DocumentServiceFx extends ApiClient {

    private final ObjectMapper mapper = new ObjectMapper();
    private final String basePath = "/documents";

    public DocumentServiceFx() {
        super();
    }

    public DocumentResponseDTO createDocument(DocumentCreateDTO dto) {
        try {
            String json = mapper.writeValueAsString(dto);
            HttpResponse<String> resp = POST(basePath, json, true);
            if (resp.statusCode() / 100 == 2) {
                return mapper.readValue(resp.body(), DocumentResponseDTO.class);
            }
            throw new RuntimeException("Create failed: " + resp.body());
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public List<DocumentResponseDTO> getAllDocuments() {
        try {
            HttpResponse<String> resp = GET(basePath, true);
            if (resp.statusCode() / 100 == 2) {
                return mapper.readValue(resp.body(), new TypeReference<List<DocumentResponseDTO>>() {
                });
            }
            throw new RuntimeException("Get all failed: " + resp.body());
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public DocumentResponseDTO getDocumentById(int documentId) {
        try {
            HttpResponse<String> resp = GET(basePath + "/" + documentId, true);
            if (resp.statusCode() / 100 == 2) {
                return mapper.readValue(resp.body(), DocumentResponseDTO.class);
            }
            throw new RuntimeException("Get failed: " + resp.body());
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public DocumentResponseDTO updateDocument(int documentId, DocumentUpdateDTO dto) {
        try {
            String json = mapper.writeValueAsString(dto);
            HttpResponse<String> resp = PUT(basePath + "/" + documentId, json, true);
            if (resp.statusCode() / 100 == 2) {
                return mapper.readValue(resp.body(), DocumentResponseDTO.class);
            }
            throw new RuntimeException("Update failed: " + resp.body());
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public void deleteDocument(int documentId) {
        try {
            HttpResponse<String> resp = DELETE(basePath + "/" + documentId, true);
            if (resp.statusCode() / 100 == 2)
                return;
            throw new RuntimeException("Delete failed: " + resp.body());
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public List<DocumentResponseDTO> filterDocuments(Map<String, Object> filters) {
        try {
            String json = mapper.writeValueAsString(filters);
            HttpResponse<String> resp = POST(basePath + "/filter", json, true);
            if (resp.statusCode() / 100 == 2) {
                return mapper.readValue(resp.body(), new TypeReference<List<DocumentResponseDTO>>() {
                });
            }
            return Collections.emptyList();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
