-- GradAppTracker Database Schema
-- PostgreSQL Database Schema for Graduate Application Tracker
-- This file defines all tables, relationships, and constraints for the application.
-- Database: PostgreSQL (main and only database)
-- Design: Normalized, well-structured with clear relationships

-- ============================================================================
-- TABLE: users
-- Purpose: Stores user accounts for the application
-- Relationships: One-to-Many with programs and documents
-- ============================================================================
CREATE TABLE users (
    user_id SERIAL PRIMARY KEY,                    -- PK: Unique user identifier
    name VARCHAR(150) NOT NULL,                    -- User's full name (NOT NULL)
    email VARCHAR(255) NOT NULL UNIQUE,            -- Email (UNIQUE, NOT NULL) - business key
    password VARCHAR(255) NOT NULL,                -- Hashed password (NOT NULL)
    roles VARCHAR(255),                            -- User roles (comma-separated, e.g., ROLE_USER,ROLE_ADMIN)
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,-- Audit: record creation timestamp
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP -- Audit: record update timestamp
);

-- Index for faster email lookups during authentication
CREATE INDEX idx_users_email ON users(email);

-- ============================================================================
-- TABLE: programs
-- Purpose: Stores graduate programs tracked by users
-- Relationships: Many-to-One with users, One-to-Many with program_documents
-- ============================================================================
CREATE TABLE programs (
    program_id SERIAL PRIMARY KEY,                 -- PK: Unique program identifier
    user_id INT NOT NULL,                          -- FK: Owner user (NOT NULL)
    university_name VARCHAR(255) NOT NULL,         -- University name (NOT NULL) - required field
    field_of_study VARCHAR(255),                   -- Field of study
    focus_area TEXT,                               -- Specific focus area
    portal VARCHAR(255),                           -- Application portal URL
    website VARCHAR(255),                          -- Program website URL
    deadline DATE,                                 -- Application deadline
    status VARCHAR(50),                            -- Application status (e.g., applied, accepted, rejected)
    tuition TEXT,                                  -- Tuition information
    requirements TEXT,                             -- Admission requirements
    notes TEXT,                                    -- Additional notes
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,-- Audit: record creation timestamp
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,-- Audit: record update timestamp
    CONSTRAINT fk_programs_user                    -- FK constraint with CASCADE delete
        FOREIGN KEY (user_id) 
        REFERENCES users(user_id) 
        ON DELETE CASCADE                          -- Delete programs when user is deleted
);

-- Index for faster user-based queries
CREATE INDEX idx_programs_user_id ON programs(user_id);

-- Index for deadline-based queries and sorting
CREATE INDEX idx_programs_deadline ON programs(deadline);

-- ============================================================================
-- TABLE: documents
-- Purpose: Stores documents uploaded by users (e.g., transcripts, SOPs, CVs)
-- Relationships: Many-to-One with users, One-to-Many with program_documents
-- ============================================================================
CREATE TABLE documents (
    document_id SERIAL PRIMARY KEY,                -- PK: Unique document identifier
    user_id INT NOT NULL,                          -- FK: Owner user (NOT NULL)
    file_name VARCHAR(255) NOT NULL,               -- Name of the file (NOT NULL)
    file_path VARCHAR(500) NOT NULL,               -- Path to file on disk (NOT NULL)
    doc_type VARCHAR(100),                         -- Type of document (e.g., transcript, SOP, CV, LOR)
    notes TEXT,                                    -- Additional notes about the document
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,-- Audit: record creation timestamp
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,-- Audit: record update timestamp
    CONSTRAINT fk_documents_user                   -- FK constraint with CASCADE delete
        FOREIGN KEY (user_id) 
        REFERENCES users(user_id) 
        ON DELETE CASCADE                          -- Delete documents when user is deleted
);

-- Index for faster user-based queries
CREATE INDEX idx_documents_user_id ON documents(user_id);

-- Index for document type searches
CREATE INDEX idx_documents_doc_type ON documents(doc_type);

-- ============================================================================
-- TABLE: program_documents
-- Purpose: Junction table linking documents to programs (Many-to-Many relationship)
-- Relationships: Many-to-One with programs and documents
-- Business Logic: A document can be linked to multiple programs, 
--                 and a program can have multiple documents
-- ============================================================================
CREATE TABLE program_documents (
    program_doc_id SERIAL PRIMARY KEY,             -- PK: Unique link identifier
    program_id INT NOT NULL,                       -- FK: Linked program (NOT NULL)
    document_id INT NOT NULL,                      -- FK: Linked document (NOT NULL)
    usage_notes TEXT,                              -- Notes about how document is used for this program
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,-- Audit: when link was created
    CONSTRAINT fk_program_documents_program        -- FK constraint with CASCADE delete
        FOREIGN KEY (program_id) 
        REFERENCES programs(program_id) 
        ON DELETE CASCADE,                         -- Delete link when program is deleted
    CONSTRAINT fk_program_documents_document       -- FK constraint with CASCADE delete
        FOREIGN KEY (document_id) 
        REFERENCES documents(document_id) 
        ON DELETE CASCADE,                         -- Delete link when document is deleted
    CONSTRAINT uq_program_document                 -- UNIQUE constraint: prevent duplicate links
        UNIQUE(program_id, document_id)            -- A document can only be linked to a program once
);

-- Index for faster program-based queries
CREATE INDEX idx_program_documents_program_id ON program_documents(program_id);

-- Index for faster document-based queries
CREATE INDEX idx_program_documents_document_id ON program_documents(document_id);

-- ============================================================================
-- TRIGGERS: Auto-update updated_at timestamp
-- ============================================================================

-- Function to update the updated_at timestamp
CREATE OR REPLACE FUNCTION update_updated_at_column()
RETURNS TRIGGER AS $$
BEGIN
    NEW.updated_at = CURRENT_TIMESTAMP;
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

-- Trigger for users table
CREATE TRIGGER update_users_updated_at
    BEFORE UPDATE ON users
    FOR EACH ROW
    EXECUTE FUNCTION update_updated_at_column();

-- Trigger for programs table
CREATE TRIGGER update_programs_updated_at
    BEFORE UPDATE ON programs
    FOR EACH ROW
    EXECUTE FUNCTION update_updated_at_column();

-- Trigger for documents table
CREATE TRIGGER update_documents_updated_at
    BEFORE UPDATE ON documents
    FOR EACH ROW
    EXECUTE FUNCTION update_updated_at_column();

-- ============================================================================
-- SUMMARY OF DATABASE DESIGN
-- ============================================================================
-- 
-- NORMALIZATION: 3NF (Third Normal Form)
-- - No repeating groups
-- - All non-key attributes depend on the primary key
-- - No transitive dependencies
--
-- RELATIONSHIPS:
-- - users (1) ──< (M) programs      [One-to-Many]
-- - users (1) ──< (M) documents     [One-to-Many]
-- - programs (M) ──< (M) documents  [Many-to-Many via program_documents]
--
-- CONSTRAINTS ENFORCED:
-- - PRIMARY KEYS: All tables have SERIAL primary keys
-- - FOREIGN KEYS: All relationships enforced with CASCADE deletes
-- - UNIQUE: email (users), program_id+document_id (program_documents)
-- - NOT NULL: Essential fields marked as NOT NULL
--
-- INDEXES:
-- - Email lookups for authentication
-- - User-based queries for data isolation
-- - Deadline sorting for program management
-- - Document type filtering
-- - Junction table lookups
--
-- AUDIT TRAILS:
-- - created_at: Timestamp when record was created
-- - updated_at: Timestamp when record was last modified (auto-updated via triggers)
--
-- ============================================================================
