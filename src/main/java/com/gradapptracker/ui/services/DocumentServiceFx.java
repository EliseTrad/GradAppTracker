package com.gradapptracker.ui.services;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.gradapptracker.backend.document.dto.DocumentCreateDTO;
import com.gradapptracker.backend.document.dto.DocumentResponseDTO;
import com.gradapptracker.backend.document.dto.DocumentUpdateDTO;
import com.gradapptracker.ui.utils.UserSession;

import java.net.http.HttpResponse;
import java.util.List;
import java.util.Map;

/**
 * UI-side service for Document CRUD/filter operations.
 */
public class DocumentServiceFx extends ApiClient {

    private final ObjectMapper mapper = new ObjectMapper();
    private final String basePath = "/users";

    public DocumentServiceFx() {
        super();
    }

    private Integer getCurrentUserId() {
        Integer userId = UserSession.getInstance().getUserId();
        if (userId == null) {
            // Try to extract userId from JWT as fallback
            UserSession.getInstance().refreshUserIdFromJWT();
            userId = UserSession.getInstance().getUserId();
        }
        if (userId == null) {
            throw new RuntimeException("User not authenticated");
        }
        return userId;
    }

    public DocumentResponseDTO createDocument(DocumentCreateDTO dto) {
        try {
            Integer userId = getCurrentUserId();
            String json = mapper.writeValueAsString(dto);
            HttpResponse<String> resp = POST(basePath + "/" + userId + "/documents", json, true);
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
            Integer userId = getCurrentUserId();
            HttpResponse<String> resp = GET(basePath + "/" + userId + "/documents", true);
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
            HttpResponse<String> resp = GET("/documents/" + documentId, true);
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
            HttpResponse<String> resp = PUT("/documents/" + documentId, json, true);
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
            HttpResponse<String> resp = DELETE("/documents/" + documentId, true);
            if (resp.statusCode() / 100 == 2)
                return;
            throw new RuntimeException("Delete failed: " + resp.body());
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Search documents by type. Backend only supports filtering by docType.
     * If filters contain "type" key, uses it; otherwise returns all documents.
     */
    public List<DocumentResponseDTO> filterDocuments(Map<String, Object> filters) {
        try {
            Integer userId = getCurrentUserId();
            // Backend only supports searching by docType with GET
            // /api/users/{userId}/documents/search?docType=X
            if (filters != null && filters.containsKey("type")) {
                String docType = filters.get("type").toString();
                String path = basePath + "/" + userId + "/documents/search?docType=" +
                        java.net.URLEncoder.encode(docType, "UTF-8");
                HttpResponse<String> resp = GET(path, true);
                if (resp.statusCode() / 100 == 2) {
                    return mapper.readValue(resp.body(), new TypeReference<List<DocumentResponseDTO>>() {
                    });
                }
            }
            // If no type filter or filter failed, return all documents
            return getAllDocuments();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public DocumentResponseDTO uploadDocument(java.io.File file, String docType, String notes) {
        try {
            Integer userId = getCurrentUserId();
            HttpResponse<String> resp = POST_MULTIPART(basePath + "/" + userId + "/documents", file, docType, notes,
                    true);
            if (resp.statusCode() / 100 == 2) {
                return mapper.readValue(resp.body(), DocumentResponseDTO.class);
            }
            throw new RuntimeException("Upload failed: " + resp.body());
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Replace file for an existing document
     */
    public DocumentResponseDTO replaceFile(int documentId, java.io.File file) {
        try {
            HttpResponse<String> resp = POST_MULTIPART("/documents/" + documentId + "/replace", file, "", "", true);
            if (resp.statusCode() / 100 == 2) {
                return mapper.readValue(resp.body(), DocumentResponseDTO.class);
            }
            throw new RuntimeException("Replace file failed: " + resp.body());
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Download document file as bytes
     */
    public byte[] downloadDocument(int documentId) {
        try {
            HttpResponse<byte[]> resp = GET_BYTES("/documents/" + documentId + "/download", true);
            if (resp.statusCode() / 100 == 2) {
                return resp.body();
            }
            throw new RuntimeException("Download failed: " + resp.statusCode());
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
