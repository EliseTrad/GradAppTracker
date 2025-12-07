package com.gradapptracker.backend.document.service;

import com.gradapptracker.backend.programdocument.repository.ProgramDocumentRepository;
import com.gradapptracker.backend.document.dto.DocumentResponseDTO;
import com.gradapptracker.backend.document.dto.DocumentUpdateDTO;
import com.gradapptracker.backend.document.entity.Document;
import com.gradapptracker.backend.document.repository.DocumentRepository;
import com.gradapptracker.backend.user.entity.User;
import com.gradapptracker.backend.user.repository.UserRepository;
import com.gradapptracker.backend.exception.AppException;
import com.gradapptracker.backend.exception.DocumentReferencedException;
import com.gradapptracker.backend.exception.ForbiddenException;
import com.gradapptracker.backend.exception.NotFoundException;
import com.gradapptracker.backend.exception.ValidationException;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.UUID;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Service
public class DocumentService {

    private static final Logger log = LoggerFactory.getLogger(DocumentService.class);

    private final DocumentRepository documentRepository;
    private final UserRepository userRepository;
    private final ProgramDocumentRepository programDocumentRepository;

    private final Path uploadRoot;

    public DocumentService(DocumentRepository documentRepository,
            UserRepository userRepository,
            ProgramDocumentRepository programDocumentRepository,
            @Value("${upload.dir:uploads}") String uploadDir) {
        this.documentRepository = documentRepository;
        this.userRepository = userRepository;
        this.programDocumentRepository = programDocumentRepository;
        this.uploadRoot = Path.of(uploadDir).toAbsolutePath().normalize();
    }

    /**
     * Update a document's metadata and optionally replace its file on disk.
     *
     * Loads the Document by {@code documentId}; throws {@link NotFoundException}
     * if missing. Caller must be the owner or an admin (otherwise
     * {@link ForbiddenException}). If {@code dto.filePath} is provided it must
     * point to a saved replacement file; the old file is deleted when replaced.
     * Validation failures throw {@link ValidationException}; IO errors throw
     * {@link AppException}. Returns the updated
     * {@link com.gradapptracker.backend.document.dto.DocumentResponseDTO}.
     *
     * @param authenticatedUserId id of the caller (from security context)
     * @param documentId          id of the document to update
     * @param dto                 update payload; may include {@code filePath}
     * @return updated DocumentResponseDTO
     */
    @Transactional
    public DocumentResponseDTO updateDocument(Integer authenticatedUserId,
            Integer documentId,
            DocumentUpdateDTO dto) {
        Optional<Document> docTemp = documentRepository.findById(documentId);
        if (docTemp.isEmpty()) {
            throw new NotFoundException("Document not found with id: " + documentId);
        }

        Document doc = docTemp.get();

        Integer ownerId = doc.getUser() != null ? doc.getUser().getUserId() : null;
        if (ownerId == null) {
            throw new NotFoundException("Owner for document not found");
        }

        if (!authenticatedUserId.equals(ownerId)) {
            throw new ForbiddenException("not authorized to update this document");
        }

        // file replacement
        String newFilePath = dto.getFilePath();
        if (newFilePath != null && !newFilePath.isBlank()) {
            // validate extension and size by checking file on disk
            Path newPath = Path.of(newFilePath).toAbsolutePath().normalize();
            if (!Files.exists(newPath) || !Files.isRegularFile(newPath)) {
                throw new ValidationException("replacement file not found: " + newFilePath);
            }
            try {
                long size = Files.size(newPath);
                if (size > MAX_FILE_SIZE) {
                    throw new ValidationException("replacement file exceeds maximum size");
                }
                String name = newPath.getFileName().toString();
                String lower = name.toLowerCase(Locale.ROOT);
                String ext = "";
                int idx = lower.lastIndexOf('.');
                if (idx >= 0 && idx < lower.length() - 1) {
                    ext = lower.substring(idx + 1);
                }
                if (!ALLOWED_EXT.contains(ext)) {
                    throw new ValidationException("replacement file type not allowed: " + ext);
                }
            } catch (IOException e) {
                throw new AppException(500, "failed to access replacement file: " + e.getMessage());
            }

            // attempt to delete old file (best-effort; if fails throw AppException)
            String oldPath = doc.getFilePath();
            if (oldPath != null && !oldPath.isBlank()) {
                try {
                    Files.deleteIfExists(Path.of(oldPath));
                } catch (IOException e) {
                    throw new AppException(500, "failed to delete old file: " + e.getMessage());
                }
            }

            doc.setFilePath(newFilePath);
            // optionally update stored fileName to new file name
            String newName = Path.of(newFilePath).getFileName().toString();
            doc.setFileName(newName);
        }

        // update optional fields
        if (dto.getFileName() != null) {
            if (dto.getFileName().isBlank()) {
                throw new ValidationException("fileName must not be blank");
            }
            doc.setFileName(dto.getFileName());
        }

        if (dto.getDocType() != null) {
            doc.setDocType(dto.getDocType());
        }

        if (dto.getNotes() != null) {
            doc.setNotes(dto.getNotes());
        }

        Document saved = documentRepository.save(doc);

        com.gradapptracker.backend.document.dto.DocumentResponseDTO resp = new com.gradapptracker.backend.document.dto.DocumentResponseDTO();
        resp.setDocumentId(saved.getDocumentId());
        resp.setUserId(saved.getUser() != null ? saved.getUser().getUserId() : null);
        resp.setFileName(saved.getFileName());
        resp.setFilePath(saved.getFilePath());
        resp.setDocType(saved.getDocType());
        resp.setNotes(saved.getNotes());

        return resp;
    }

    private static final long MAX_FILE_SIZE = 5L * 1024 * 1024; // 5MB

    private static final List<String> ALLOWED_EXT = List.of("pdf", "docx", "doc", "txt", "jpg", "jpeg", "png");

    /**
     * Uploads a document for a user.
     *
     * Caller must be the same user (or an admin). Validates the target user,
     * file presence, size (<=5MB) and extension. The file is saved under
     * {@code upload.dir} in a per-user folder using the pattern
     * {upload.dir}/{userId}/{UUID}_{originalFilename}. IO errors throw
     * {@link com.gradapptracker.backend.exception.AppException}. On success a
     * {@link com.gradapptracker.backend.document.dto.DocumentResponseDTO} is
     * returned.
     *
     * @param authenticatedUserId id of the caller
     * @param userId              id of the user to own the document
     * @param file                multipart file to save
     * @param docType             document type/category
     * @param notes               optional notes
     * @return DocumentResponseDTO for the saved document
     */
    @Transactional
    public DocumentResponseDTO upload(Integer authenticatedUserId, Integer userId, MultipartFile file, String docType,
            String notes) {
        // Auth check: allow if same user or admin (admin check omitted here â€” project
        // typically encodes roles in security layer)
        if (!authenticatedUserId.equals(userId)) {
            // no admin role inspection here â€” if needed, caller should allow or pass a
            // flag
            throw new ForbiddenException("not authorized to upload for this user");
        }

        // user exists
        Optional<User> uOpt = userRepository.findById(userId);
        if (uOpt.isEmpty()) {
            throw new NotFoundException("User not found with id: " + userId);
        }
        User user = uOpt.get();

        if (file == null || file.isEmpty()) {
            throw new ValidationException("file is required");
        }

        if (file.getSize() > MAX_FILE_SIZE) {
            throw new ValidationException("file size exceeds maximum of 5MB");
        }

        String original = file.getOriginalFilename();
        if (original == null)
            original = "file";
        String lower = original.toLowerCase(Locale.ROOT);
        String ext = "";
        int idx = lower.lastIndexOf('.');
        if (idx >= 0 && idx < lower.length() - 1) {
            ext = lower.substring(idx + 1);
        }

        if (!ALLOWED_EXT.contains(ext)) {
            throw new ValidationException("file type not allowed: " + ext);
        }

        // build paths:
        String uuid = UUID.randomUUID().toString();
        String safeFileName = uuid + "_" + original.replaceAll("[\\/:*?\"<>|]", "_");
        Path userDir = uploadRoot.resolve(String.valueOf(userId));
        Path tmpDir = userDir.resolve("tmp");
        Path tmpFile;
        try {
            Files.createDirectories(tmpDir);
            tmpFile = Files.createTempFile(tmpDir, uuid + "_", ".tmp");
            // copy to temp
            Files.copy(file.getInputStream(), tmpFile, StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException e) {
            throw new AppException(500, "failed to write temp file: " + e.getMessage());
        }

        // final target path
        Path finalTarget = userDir.resolve(safeFileName).normalize();

        // persist entity with final path (file will be moved after commit)
        Document doc = new Document();
        doc.setUser(user);
        doc.setFileName(original);
        doc.setFilePath(finalTarget.toString());
        doc.setDocType(docType);
        doc.setNotes(notes);

        Document saved;
        try {
            saved = documentRepository.save(doc);
        } catch (RuntimeException e) {
            // cleanup temp
            try {
                Files.deleteIfExists(tmpFile);
            } catch (IOException ex) {
                log.warn("failed to delete temp file after db save failure: {}", tmpFile, ex);
            }
            throw e;
        }

        // Register transaction synchronization: move temp -> final after commit;
        // if rollback, delete temp
        final Path tmpToDelete = tmpFile;
        final Path finalToMove = finalTarget;
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                try {
                    Files.createDirectories(finalToMove.getParent());
                    Files.move(tmpToDelete, finalToMove, StandardCopyOption.REPLACE_EXISTING);
                } catch (IOException ex) {
                    log.error("failed to move uploaded file to final location {}", finalToMove, ex);
                    throw new AppException(500, "failed to move uploaded file: " + ex.getMessage());
                }
            }

            @Override
            public void afterCompletion(int status) {
                if (status != TransactionSynchronization.STATUS_COMMITTED) {
                    // cleanup temp on rollback or unknown status
                    try {
                        Files.deleteIfExists(tmpToDelete);
                    } catch (IOException ex) {
                        log.warn("failed to delete temp file after rollback: {}", tmpToDelete, ex);
                    }
                }
            }
        });

        DocumentResponseDTO resp = new DocumentResponseDTO();
        resp.setDocumentId(saved.getDocumentId());
        resp.setUserId(user.getUserId());
        resp.setFileName(saved.getFileName());
        resp.setFilePath(saved.getFilePath());
        resp.setDocType(saved.getDocType());
        resp.setNotes(saved.getNotes());

        return resp;
    }

    /**
     * Retrieve all documents for a user. Caller must be the owner or an admin.
     * Throws {@link NotFoundException} when the user doesn't exist. Results are
     * mapped to
     * {@link com.gradapptracker.backend.document.dto.DocumentResponseDTO}.
     *
     * @param authenticatedUserId id of the caller
     * @param userId              id of the user whose documents to fetch
     * @return list of DocumentResponseDTO
     */
    @Transactional(readOnly = true)
    public List<DocumentResponseDTO> getAllByUser(Integer authenticatedUserId,
            Integer userId) {
        if (!authenticatedUserId.equals(userId)) {
            throw new ForbiddenException("not authorized to view documents for this user");
        }

        Optional<User> uOpt = userRepository.findById(userId);
        if (uOpt.isEmpty()) {
            throw new NotFoundException("User not found with id: " + userId);
        }

        List<Document> docs = documentRepository.findByUserUserId(userId);

        List<DocumentResponseDTO> dtoList = new ArrayList<>();

        for (Document d : docs) {
            DocumentResponseDTO dto = new DocumentResponseDTO();
            dto.setDocumentId(d.getDocumentId());
            dto.setUserId(d.getUser() != null ? d.getUser().getUserId() : null);
            dto.setFileName(d.getFileName());
            dto.setFilePath(d.getFilePath());
            dto.setDocType(d.getDocType());
            dto.setNotes(d.getNotes());

            dtoList.add(dto);
        }

        return dtoList;
    }

    /**
     * Retrieve documents for a user with pagination support.
     *
     * @param authenticatedUserId id of the caller
     * @param userId              id of the user whose documents to fetch
     * @param pageable            pagination parameters (page number, size, sorting)
     * @return Page of DocumentResponseDTO
     */
    @Transactional(readOnly = true)
    public Page<DocumentResponseDTO> getAllByUser(Integer authenticatedUserId,
            Integer userId, Pageable pageable) {
        if (!authenticatedUserId.equals(userId)) {
            throw new ForbiddenException("not authorized to view documents for this user");
        }

        Optional<User> uOpt = userRepository.findById(userId);
        if (uOpt.isEmpty()) {
            throw new NotFoundException("User not found with id: " + userId);
        }

        Page<Document> page = documentRepository.findByUserUserId(userId, pageable);

        return page.map(d -> {
            DocumentResponseDTO dto = new DocumentResponseDTO();
            dto.setDocumentId(d.getDocumentId());
            dto.setUserId(d.getUser() != null ? d.getUser().getUserId() : null);
            dto.setFileName(d.getFileName());
            dto.setFilePath(d.getFilePath());
            dto.setDocType(d.getDocType());
            dto.setNotes(d.getNotes());
            return dto;
        });
    }

    /**
     * Retrieve documents by type for a user (case-insensitive, contains).
     * Validates {@code docType} is not blank and that caller is authorized.
     * Results are mapped to DocumentResponseDTO.
     *
     * @param authenticatedUserId caller id
     * @param userId              target user id
     * @param docType             search string for document type
     * @return list of matching DocumentResponseDTO
     */
    @Transactional(readOnly = true)
    public List<DocumentResponseDTO> getByType(Integer authenticatedUserId,
            Integer userId, String docType) {
        if (docType == null || docType.isBlank()) {
            throw new ValidationException("docType must not be blank");
        }

        if (!authenticatedUserId.equals(userId)) {
            throw new ForbiddenException("not authorized to view documents for this user");
        }

        Optional<User> uOpt = userRepository.findById(userId);
        if (uOpt.isEmpty()) {
            throw new NotFoundException("User not found with id: " + userId);
        }

        List<Document> docs = documentRepository.findByDocTypeContainingIgnoreCaseAndUserUserId(docType, userId);

        List<DocumentResponseDTO> dtoList = new ArrayList<>();

        for (Document d : docs) {
            DocumentResponseDTO dto = new DocumentResponseDTO();
            dto.setDocumentId(d.getDocumentId());
            dto.setUserId(d.getUser() != null ? d.getUser().getUserId() : null);
            dto.setFileName(d.getFileName());
            dto.setFilePath(d.getFilePath());
            dto.setDocType(d.getDocType());
            dto.setNotes(d.getNotes());

            dtoList.add(dto);
        }

        return dtoList;
    }

    /**
     * Delete a document and its file. Caller must be the owner or an admin.
     * If the document is referenced by programs a
     * DocumentReferencedException is thrown. IO errors when deleting the file
     * result in {@link AppException}. The DB row is removed on success.
     *
     * @param authenticatedUserId caller id
     * @param documentId          id of the document to delete
     */
    @Transactional
    public void deleteDocument(Integer authenticatedUserId, Integer documentId) {
        Optional<Document> dOpt = documentRepository.findById(documentId);
        if (dOpt.isEmpty()) {
            throw new NotFoundException("Document not found with id: " + documentId);
        }

        Document doc = dOpt.get();
        Integer ownerId = doc.getUser() != null ? doc.getUser().getUserId() : null;
        if (ownerId == null) {
            throw new NotFoundException("Owner for document not found");
        }

        if (!authenticatedUserId.equals(ownerId)) {
            throw new ForbiddenException("not authorized to delete this document");
        }

        // Check references in program documents
        if (programDocumentRepository.existsByDocumentDocumentId(documentId)) {
            throw new DocumentReferencedException(
                    "Document linked to programs; unlink first");
        }

        // Delete file from disk
        String filePath = doc.getFilePath();
        if (filePath != null && !filePath.isBlank()) {
            try {
                Files.delete(Path.of(filePath));
            } catch (IOException e) {
                throw new AppException(500, "failed to delete file: " + e.getMessage());
            }
        }
        // Delete DB row
        documentRepository.deleteById(documentId);
    }

    /**
     * Retrieve a single document by id. Caller must be the owner or an admin.
     * Returns a DocumentResponseDTO.
     *
     * @param authenticatedUserId caller id
     * @param documentId          document id
     * @return DocumentResponseDTO
     */
    @Transactional(readOnly = true)
    public DocumentResponseDTO getById(Integer authenticatedUserId, Integer documentId) {
        Optional<Document> dOpt = documentRepository.findById(documentId);
        if (dOpt.isEmpty()) {
            throw new NotFoundException("Document not found with id: " + documentId);
        }

        Document doc = dOpt.get();
        if (doc.getUser() == null) {
            throw new NotFoundException("Owner for document not found");
        }

        Integer ownerId = doc.getUser().getUserId();
        if (!authenticatedUserId.equals(ownerId)) {
            throw new ForbiddenException("not authorized to view this document");
        }

        DocumentResponseDTO dto = new DocumentResponseDTO();
        dto.setDocumentId(doc.getDocumentId());
        dto.setUserId(ownerId);
        dto.setFileName(doc.getFileName());
        dto.setFilePath(doc.getFilePath());
        dto.setDocType(doc.getDocType());
        dto.setNotes(doc.getNotes());
        return dto;
    }

    /**
     * Return the on-disk file path for a document after authorization checks.
     * Throws NotFoundException or ForbiddenException as appropriate.
     */
    @Transactional(readOnly = true)
    public String getFilePathForDocument(Integer authenticatedUserId, Integer documentId) {
        Optional<Document> dOpt = documentRepository.findById(documentId);
        if (dOpt.isEmpty()) {
            throw new NotFoundException("Document not found with id: " + documentId);
        }
        Document doc = dOpt.get();
        if (doc.getUser() == null) {
            throw new NotFoundException("Owner for document not found");
        }
        Integer ownerId = doc.getUser().getUserId();
        if (!authenticatedUserId.equals(ownerId)) {
            throw new ForbiddenException("not authorized to view this document");
        }
        if (doc.getFilePath() == null || doc.getFilePath().isBlank()) {
            throw new NotFoundException("document has no file path");
        }
        return doc.getFilePath();
    }

    /**
     * Replace the file for an existing document. Saves the uploaded file to the
     * same user folder and updates the Document record. Uses a temp file +
     * transaction synchronization like upload.
     */
    @Transactional
    public DocumentResponseDTO replaceFile(Integer authenticatedUserId, Integer documentId, MultipartFile file) {
        Optional<Document> dOpt = documentRepository.findById(documentId);
        if (dOpt.isEmpty()) {
            throw new NotFoundException("Document not found with id: " + documentId);
        }
        Document doc = dOpt.get();
        Integer ownerId = doc.getUser() != null ? doc.getUser().getUserId() : null;
        if (ownerId == null) {
            throw new NotFoundException("Owner for document not found");
        }
        if (!authenticatedUserId.equals(ownerId)) {
            throw new ForbiddenException("not authorized to update this document");
        }

        if (file == null || file.isEmpty()) {
            throw new ValidationException("file is required");
        }

        if (file.getSize() > MAX_FILE_SIZE) {
            throw new ValidationException("file size exceeds maximum of 5MB");
        }

        String original = file.getOriginalFilename();
        if (original == null)
            original = "file";
        String lower = original.toLowerCase(Locale.ROOT);
        String ext = "";
        int idx = lower.lastIndexOf('.');
        if (idx >= 0 && idx < lower.length() - 1) {
            ext = lower.substring(idx + 1);
        }
        if (!ALLOWED_EXT.contains(ext)) {
            throw new ValidationException("file type not allowed: " + ext);
        }

        // build paths
        String uuid = UUID.randomUUID().toString();
        String safeFileName = uuid + "_" + original.replaceAll("[\\/:*?\"<>|]", "_");
        Path userDir = uploadRoot.resolve(String.valueOf(ownerId));
        Path tmpDir = userDir.resolve("tmp");
        Path tmpFile;
        try {
            Files.createDirectories(tmpDir);
            tmpFile = Files.createTempFile(tmpDir, uuid + "_", ".tmp");
            Files.copy(file.getInputStream(), tmpFile, StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException e) {
            throw new AppException(500, "failed to write temp file: " + e.getMessage());
        }

        Path finalTarget = userDir.resolve(safeFileName).normalize();

        // delete old file after commit and move new file into place
        final Path tmpToDelete = tmpFile;
        final Path finalToMove = finalTarget;
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                try {
                    Files.createDirectories(finalToMove.getParent());
                    Files.move(tmpToDelete, finalToMove, StandardCopyOption.REPLACE_EXISTING);
                } catch (IOException ex) {
                    log.error("failed to move replacement file to final location {}", finalToMove, ex);
                    throw new AppException(500, "failed to move replacement file: " + ex.getMessage());
                }
                // attempt to delete the old file now that replacement is in place
                String oldPath = doc.getFilePath();
                if (oldPath != null && !oldPath.isBlank()) {
                    try {
                        Files.deleteIfExists(Path.of(oldPath));
                    } catch (IOException ex) {
                        log.warn("failed to delete old file after replacement: {}", oldPath, ex);
                    }
                }
            }

            @Override
            public void afterCompletion(int status) {
                if (status != TransactionSynchronization.STATUS_COMMITTED) {
                    try {
                        Files.deleteIfExists(tmpToDelete);
                    } catch (IOException ex) {
                        log.warn("failed to delete temp file after rollback: {}", tmpToDelete, ex);
                    }
                }
            }
        });

        // update DB record to point to the new final path and filename
        doc.setFilePath(finalTarget.toString());
        doc.setFileName(original);
        Document saved = documentRepository.save(doc);

        DocumentResponseDTO resp = new DocumentResponseDTO();
        resp.setDocumentId(saved.getDocumentId());
        resp.setUserId(saved.getUser() != null ? saved.getUser().getUserId() : null);
        resp.setFileName(saved.getFileName());
        resp.setFilePath(saved.getFilePath());
        resp.setDocType(saved.getDocType());
        resp.setNotes(saved.getNotes());
        return resp;
    }

}
