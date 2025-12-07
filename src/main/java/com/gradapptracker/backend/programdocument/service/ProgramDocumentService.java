package com.gradapptracker.backend.programdocument.service;

import com.gradapptracker.backend.document.entity.Document;
import com.gradapptracker.backend.document.repository.DocumentRepository;
import com.gradapptracker.backend.program.entity.Program;
import com.gradapptracker.backend.program.repository.ProgramRepository;
import com.gradapptracker.backend.exception.ForbiddenException;
import com.gradapptracker.backend.exception.NotFoundException;
import com.gradapptracker.backend.programdocument.dto.ProgramDocumentDTO;
import com.gradapptracker.backend.programdocument.entity.ProgramDocument;
import com.gradapptracker.backend.programdocument.repository.ProgramDocumentRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Service for managing ProgramDocument links (program_documents table).
 *
 * Note: this service validates existence of referenced entities and performs
 * persistence.
 * Authorization / ownership checks should be performed by callers (controllers)
 * and are
 * intentionally omitted here.
 */
@Service
public class ProgramDocumentService {

    private final ProgramDocumentRepository programDocumentRepository;
    private final ProgramRepository programRepository;
    private final DocumentRepository documentRepository;

    public ProgramDocumentService(ProgramDocumentRepository programDocumentRepository,
            ProgramRepository programRepository,
            DocumentRepository documentRepository) {
        this.programDocumentRepository = programDocumentRepository;
        this.programRepository = programRepository;
        this.documentRepository = documentRepository;
    }

    /**
     * Create a link between a program and a document.
     *
     * @param userId     caller's user id (passed for callers' context; not used for
     *                   authorization here)
     * @param programId  id of the program to link
     * @param documentId id of the document to link
     * @param usageNotes optional notes about the usage
     * @return the saved link as a DTO
     * @throws NotFoundException if the program or document does not exist
     */
    @Transactional
    public ProgramDocumentDTO linkDocument(Integer userId, Integer programId, Integer documentId, String usageNotes) {
        // Ensure program exists
        Program program = programRepository.findById(programId)
                .orElseThrow(() -> new NotFoundException("Program not found with id: " + programId));

        // Ensure caller owns the program
        boolean programOwnedByUser = programRepository.findByProgramIdAndUserUserId(programId, userId).isPresent();
        if (!programOwnedByUser) {
            throw new ForbiddenException("not authorized to link documents to this program");
        }

        // Ensure document exists
        Document document = documentRepository.findById(documentId)
                .orElseThrow(() -> new NotFoundException("Document not found with id: " + documentId));

        // Ensure caller owns the document
        boolean documentOwnedByUser = documentRepository.findByDocumentIdAndUserUserId(documentId, userId).isPresent();
        if (!documentOwnedByUser) {
            throw new ForbiddenException("not authorized to link this document");
        }

        ProgramDocument link = new ProgramDocument();
        link.setProgram(program);
        link.setDocument(document);
        link.setUsageNotes(usageNotes);

        ProgramDocument saved = programDocumentRepository.save(link);
        return toDto(saved);
    }

    /**
     * Retrieve all ProgramDocument links for a program.
     *
     * @param userId    caller's user id (passed for callers' context; not used
     *                  here)
     * @param programId id of the program to query
     * @return list of ProgramDocumentDTO; empty list when no links exist
     * @throws NotFoundException when the program does not exist
     */
    @Transactional(readOnly = true)
    public List<ProgramDocumentDTO> getDocumentsByProgram(Integer userId, Integer programId) {
        if (!programRepository.existsById(programId)) {
            throw new NotFoundException("Program not found with id: " + programId);
        }

        // Ensure caller owns the program
        boolean programOwnedByUser = programRepository.findByProgramIdAndUserUserId(programId, userId).isPresent();
        if (!programOwnedByUser) {
            throw new ForbiddenException("not authorized to view documents for this program");
        }

        List<ProgramDocument> links = programDocumentRepository.findAllByProgramProgramId(programId);
        return links.stream().map(this::toDto).collect(Collectors.toList());
    }

    /**
     * Delete an existing ProgramDocument link.
     *
     * @param userId       caller's user id (passed for callers' context; not used
     *                     here)
     * @param programDocId id of the program-document link to delete
     * @throws NotFoundException when the link does not exist
     */
    @Transactional
    public void deleteLink(Integer userId, Integer programDocId) {
        ProgramDocument existing = programDocumentRepository.findById(programDocId)
                .orElseThrow(() -> new NotFoundException("ProgramDocument not found with id: " + programDocId));

        // Verify the caller owns the program associated with the link
        Integer programId = existing.getProgram() != null ? existing.getProgram().getProgramId() : null;
        if (programId == null) {
            throw new NotFoundException("ProgramDocument has no associated program: " + programDocId);
        }
        boolean programOwnedByUser = programRepository.findByProgramIdAndUserUserId(programId, userId).isPresent();
        if (!programOwnedByUser) {
            throw new ForbiddenException("not authorized to delete this program-document link");
        }

        programDocumentRepository.delete(existing);
    }

    /**
     * Map a ProgramDocument entity to a ProgramDocumentDTO.
     *
     * @param pd entity to map
     * @return mapped DTO containing ids only for nested objects
     */
    private ProgramDocumentDTO toDto(ProgramDocument pd) {
        ProgramDocumentDTO dto = new ProgramDocumentDTO();
        dto.setProgramDocId(pd.getProgramDocId());
        dto.setProgramId(pd.getProgram() != null ? pd.getProgram().getProgramId() : null);
        dto.setDocumentId(pd.getDocument() != null ? pd.getDocument().getDocumentId() : null);
        dto.setUsageNotes(pd.getUsageNotes());
        return dto;
    }

}
