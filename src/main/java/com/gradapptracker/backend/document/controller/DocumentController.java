package com.gradapptracker.backend.document.controller;

import com.gradapptracker.backend.document.dto.DocumentResponseDTO;
import com.gradapptracker.backend.document.dto.DocumentUpdateDTO;
import com.gradapptracker.backend.document.service.DocumentService;
import com.gradapptracker.backend.exception.UnauthorizedException;
import com.gradapptracker.backend.security.JwtUtils;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.core.io.InputStreamResource;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

/**
 * Controller for document CRUD and upload operations.
 */
@RestController
@Validated
public class DocumentController {

    private final DocumentService documentService;
    private final JwtUtils jwtUtils;

    public DocumentController(DocumentService documentService, JwtUtils jwtUtils) {
        this.documentService = documentService;
        this.jwtUtils = jwtUtils;
    }

    private Integer getAuthenticatedUserId(HttpServletRequest req) {
        String auth = req.getHeader("Authorization");
        if (auth == null || !auth.startsWith("Bearer ")) {
            throw new UnauthorizedException("missing or invalid Authorization header");
        }
        String token = auth.substring(7);
        Integer userId = jwtUtils.getUserIdFromToken(token);
        if (userId == null) {
            throw new UnauthorizedException("invalid token");
        }
        return userId;
    }

    /**
     * Upload a document for a given user.
     */
    @PostMapping(value = "/api/users/{userId}/documents", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public DocumentResponseDTO uploadDocument(HttpServletRequest req,
            @PathVariable Integer userId,
            @RequestPart("file") MultipartFile file,
            @RequestParam("docType") String docType,
            @RequestParam(value = "notes", required = false) String notes) {
        Integer authUser = getAuthenticatedUserId(req);
        return documentService.upload(authUser, userId, file, docType, notes);
    }

    /**
     * Replace the file for an existing document. Accepts a multipart file and
     * replaces the on-disk file and updates the DB record.
     */
    @PostMapping(value = "/api/documents/{documentId}/replace", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public DocumentResponseDTO replaceFile(HttpServletRequest req,
            @PathVariable Integer documentId,
            @RequestPart("file") MultipartFile file) {
        Integer authUser = getAuthenticatedUserId(req);
        return documentService.replaceFile(authUser, documentId, file);
    }

    /**
     * List all documents for a user.
     */
    @GetMapping("/api/users/{userId}/documents")
    public List<DocumentResponseDTO> listDocuments(HttpServletRequest req, @PathVariable Integer userId) {
        Integer authUser = getAuthenticatedUserId(req);
        return documentService.getAllByUser(authUser, userId);
    }

    /**
     * Search documents by type for a user.
     */
    @GetMapping("/api/users/{userId}/documents/search")
    public List<DocumentResponseDTO> searchByType(HttpServletRequest req,
            @PathVariable Integer userId,
            @RequestParam("docType") String docType) {
        Integer authUser = getAuthenticatedUserId(req);
        return documentService.getByType(authUser, userId, docType);
    }

    /**
     * Get a single document by id.
     */
    @GetMapping("/api/documents/{documentId}")
    public DocumentResponseDTO getById(HttpServletRequest req, @PathVariable Integer documentId) {
        Integer authUser = getAuthenticatedUserId(req);
        return documentService.getById(authUser, documentId);
    }

    /**
     * Download the document file. Streams the file with proper content type.
     */
    @GetMapping("/api/documents/{documentId}/download")
    public ResponseEntity<InputStreamResource> downloadDocument(HttpServletRequest req,
            @PathVariable Integer documentId) {
        Integer authUser = getAuthenticatedUserId(req);
        String path = documentService.getFilePathForDocument(authUser, documentId);
        java.nio.file.Path p = java.nio.file.Path.of(path);
        try {
            InputStreamResource resource = new InputStreamResource(java.nio.file.Files.newInputStream(p));
            String contentType = java.nio.file.Files.probeContentType(p);
            if (contentType == null)
                contentType = "application/octet-stream";
            return ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_DISPOSITION,
                            "attachment; filename=\"" + p.getFileName().toString() + "\"")
                    .header(HttpHeaders.CONTENT_TYPE, contentType)
                    .body(resource);
        } catch (java.io.IOException e) {
            throw new com.gradapptracker.backend.exception.AppException(500, "failed to read file: " + e.getMessage());
        }
    }

    /**
     * Update document metadata. File replacement must be handled separately by
     * uploading a new file and providing its path in the DTO.
     */
    @PutMapping("/api/documents/{documentId}")
    public DocumentResponseDTO updateDocument(HttpServletRequest req,
            @PathVariable Integer documentId,
            @RequestBody @Valid DocumentUpdateDTO dto) {
        Integer authUser = getAuthenticatedUserId(req);
        return documentService.updateDocument(authUser, documentId, dto);
    }

    /**
     * Delete a document.
     */
    @DeleteMapping("/api/documents/{documentId}")
    public void deleteDocument(HttpServletRequest req, @PathVariable Integer documentId) {
        Integer authUser = getAuthenticatedUserId(req);
        documentService.deleteDocument(authUser, documentId);
    }

}

