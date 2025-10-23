package com.gradapptracker.backend.exception;

/**
 * DocumentReferencedException indicates a document (or other entity) cannot be
 * deleted because it is referenced by other entities (business rule or
 * referential integrity).
 *
 * Usage:
 * - Thrown by service layer when attempting to delete an entity that has active
 * references preventing deletion.
 * - Results in HTTP 409 (Conflict).
 *
 * Example message: "Document is referenced by program 42, cannot delete".
 */
public class DocumentReferencedException extends RuntimeException {
    private final int code = 409;

    public DocumentReferencedException(String message) {
        super(message);
    }

    public int getCode() {
        return code;
    }
}

