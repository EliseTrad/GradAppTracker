package com.gradapptracker.backend.programdocument.repository;

import com.gradapptracker.backend.programdocument.entity.ProgramDocument;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ProgramDocumentRepository extends JpaRepository<ProgramDocument, Integer> {

    List<ProgramDocument> findAllByProgramProgramId(Integer programId);

    Optional<ProgramDocument> findByProgramProgramIdAndDocumentDocumentId(Integer programId, Integer documentId);

    /**
     * Returns true when a program-document link exists for the given documentId.
     */
    boolean existsByDocumentDocumentId(Integer documentId);

}

