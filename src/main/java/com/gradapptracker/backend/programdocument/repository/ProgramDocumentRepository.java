package com.gradapptracker.backend.programdocument.repository;

import com.gradapptracker.backend.programdocument.entity.ProgramDocument;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Spring Data JPA repository for ProgramDocument junction entity.
 * <p>
 * Manages the many-to-many relationship between programs and documents.
 * Provides methods to find links by program, check for document usage,
 * and prevent duplicate associations.
 */
@Repository
public interface ProgramDocumentRepository extends JpaRepository<ProgramDocument, Integer> {

    List<ProgramDocument> findAllByProgramProgramId(Integer programId);

    Optional<ProgramDocument> findByProgramProgramIdAndDocumentDocumentId(Integer programId, Integer documentId);

    /**
     * Returns true when a program-document link exists for the given documentId.
     */
    boolean existsByDocumentDocumentId(Integer documentId);

}
