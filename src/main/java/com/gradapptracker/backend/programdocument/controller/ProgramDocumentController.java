package com.gradapptracker.backend.programdocument.controller;

import com.gradapptracker.backend.programdocument.dto.ProgramDocumentCreateDTO;
import com.gradapptracker.backend.programdocument.dto.ProgramDocumentDTO;
import com.gradapptracker.backend.programdocument.service.ProgramDocumentService;
import com.gradapptracker.backend.security.JwtUtils;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Thin controller for program-document links. Extracts user id from JWT and
 * delegates to the service layer. The controller enforces ownership where
 * appropriate before delegating (if needed), otherwise service performs
 * existence validation and persistence.
 */
@RestController
@RequestMapping("/api")
@Validated
public class ProgramDocumentController {

    private final ProgramDocumentService programDocumentService;
    private final JwtUtils jwtUtils;

    public ProgramDocumentController(ProgramDocumentService programDocumentService, JwtUtils jwtUtils) {
        this.programDocumentService = programDocumentService;
        this.jwtUtils = jwtUtils;
    }

    /**
     * Extract the authenticated user ID from the JWT token in the request header.
     * 
     * @param req the HTTP request containing the Authorization header
     * @return the user ID extracted from the JWT token, or null if extraction fails
     */
    private Integer extractUserId(HttpServletRequest req) {
        String auth = req.getHeader("Authorization");
        String token = (auth != null && auth.startsWith("Bearer ")) ? auth.substring(7) : auth;
        return jwtUtils.getUserIdFromToken(token);
    }

    /**
     * Link a document to a program.
     * Accepts a body with documentId and optional usageNotes.
     * 
     * @param req       the HTTP request containing the Authorization header
     * @param programId the ID of the program to link the document to
     * @param body      the request body containing documentId and optional
     *                  usageNotes
     * @return ResponseEntity with HTTP 201 Created and the created
     *         ProgramDocumentDTO
     * @throws UnauthorizedException if user doesn't own the program or document
     * @throws NotFoundException     if program or document doesn't exist
     */
    @PostMapping("/programs/{programId}/documents")
    public ResponseEntity<ProgramDocumentDTO> linkDocument(HttpServletRequest req,
            @PathVariable Integer programId,
            @RequestBody @Valid ProgramDocumentCreateDTO body) {
        Integer userId = extractUserId(req);
        ProgramDocumentDTO created = programDocumentService.linkDocument(userId, programId, body.getDocumentId(),
                body.getUsageNotes());
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    /**
     * Get all documents linked to a program.
     * 
     * @param req       the HTTP request containing the Authorization header
     * @param programId the ID of the program to retrieve linked documents for
     * @return ResponseEntity containing a list of ProgramDocumentDTOs
     * @throws UnauthorizedException if user doesn't own the program
     * @throws NotFoundException     if program doesn't exist
     */
    @GetMapping("/programs/{programId}/documents")
    public ResponseEntity<List<ProgramDocumentDTO>> getDocumentsByProgram(HttpServletRequest req,
            @PathVariable Integer programId) {
        Integer userId = extractUserId(req);
        List<ProgramDocumentDTO> list = programDocumentService.getDocumentsByProgram(userId, programId);
        return ResponseEntity.ok(list);
    }

    /**
     * Delete a program-document link.
     * 
     * @param req          the HTTP request containing the Authorization header
     * @param programDocId the ID of the program-document link to delete
     * @return ResponseEntity with HTTP 204 No Content on success
     * @throws UnauthorizedException if user doesn't own the associated program
     * @throws NotFoundException     if the link doesn't exist
     */
    @DeleteMapping("/program-docs/{programDocId}")
    public ResponseEntity<Void> deleteLink(HttpServletRequest req, @PathVariable Integer programDocId) {
        Integer userId = extractUserId(req);
        programDocumentService.deleteLink(userId, programDocId);
        return ResponseEntity.noContent().build();
    }

}
