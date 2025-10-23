package com.gradapptracker.backend.document.repository;

import java.util.List;
import java.util.Optional;
import org.springframework.lang.NonNull;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.gradapptracker.backend.document.entity.Document;

@Repository
public interface DocumentRepository extends JpaRepository<Document, Integer> {

    @NonNull
    Optional<Document> findById(@NonNull Integer documentId);

    List<Document> findByUserUserId(Integer userId);

    List<Document> findByFileNameContainingIgnoreCaseAndUserUserId(String fileName, Integer userId);

    List<Document> findByDocTypeContainingIgnoreCaseAndUserUserId(String docType, Integer userId);

    Optional<Document> findByDocumentIdAndUserUserId(Integer documentId, Integer userId);

    boolean existsByDocumentId(Integer documentId);

    boolean existsByDocumentIdAndUserUserId(Integer documentId, Integer userId);

}

