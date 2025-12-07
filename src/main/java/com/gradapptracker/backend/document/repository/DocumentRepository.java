package com.gradapptracker.backend.document.repository;

import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.lang.NonNull;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.gradapptracker.backend.document.entity.Document;

/**
 * Spring Data JPA repository for Document entity.
 * <p>
 * Provides CRUD operations and custom query methods for document management
 * including search by filename, document type, and user ownership.
 * All queries enforce user-level data isolation.
 */
@Repository
public interface DocumentRepository extends JpaRepository<Document, Integer> {

    @NonNull
    Optional<Document> findById(@NonNull Integer documentId);

    List<Document> findByUserUserId(Integer userId);

    Page<Document> findByUserUserId(Integer userId, Pageable pageable);

    List<Document> findByFileNameContainingIgnoreCaseAndUserUserId(String fileName, Integer userId);

    List<Document> findByDocTypeContainingIgnoreCaseAndUserUserId(String docType, Integer userId);

    Optional<Document> findByDocumentIdAndUserUserId(Integer documentId, Integer userId);

    boolean existsByDocumentId(Integer documentId);

    boolean existsByDocumentIdAndUserUserId(Integer documentId, Integer userId);

}
