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

    /**
     * Extract and validate the authenticated user ID from the JWT token.
     * 
     * @param req the HTTP request containing the Authorization header
     * @return the authenticated user ID
     * @throws UnauthorizedException if Authorization header is missing, invalid, or
     *                               token is invalid
     */
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
     * 
     * @param req     the HTTP request containing the Authorization header
     * @param userId  the ID of the user who will own the document
     * @param file    the multipart file to upload (PDF, DOCX, TXT, etc.)
     * @param docType the type of document (e.g., "Resume", "Transcript", "Letter of
     *                Recommendation")
     * @param notes   optional notes about the document
     * @return DocumentResponseDTO containing the uploaded document's metadata
     * @throws UnauthorizedException if the authenticated user doesn't match the
     *                               userId
     * @throws ValidationException   if file is invalid (wrong type, too large,
     *                               etc.)
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
     * Replace the file for an existing document.
     * Accepts a multipart file and replaces the on-disk file and updates the DB
     * record.
     * 
     * @param req        the HTTP request containing the Authorization header
     * @param documentId the ID of the document whose file should be replaced
     * @param file       the new multipart file to replace the existing one
     * @return DocumentResponseDTO containing the updated document's metadata
     * @throws UnauthorizedException if user is not the document owner
     * @throws NotFoundException     if document doesn't exist
     * @throws ValidationException   if file is invalid
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
     * 
     * @param req    the HTTP request containing the Authorization header
     * @param userId the ID of the user whose documents to retrieve
     * @return List of DocumentResponseDTO containing all documents owned by the
     *         user
     * @throws UnauthorizedException if authenticated user doesn't match userId
     */
    @GetMapping("/api/users/{userId}/documents")
    public List<DocumentResponseDTO> listDocuments(HttpServletRequest req, @PathVariable Integer userId) {
        Integer authUser = getAuthenticatedUserId(req);
        return documentService.getAllByUser(authUser, userId);
    }

    /**
     * Search documents by type for a user.
     * 
     * @param req     the HTTP request containing the Authorization header
     * @param userId  the ID of the user whose documents to search
     * @param docType the document type to filter by (e.g., "Resume", "Transcript")
     * @return List of DocumentResponseDTO matching the specified type
     * @throws UnauthorizedException if authenticated user doesn't match userId
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
     * 
     * @param req        the HTTP request containing the Authorization header
     * @param documentId the ID of the document to retrieve
     * @return DocumentResponseDTO containing the document's metadata
     * @throws UnauthorizedException if user is not the document owner
     * @throws NotFoundException     if document doesn't exist
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
