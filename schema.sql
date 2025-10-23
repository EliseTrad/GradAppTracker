-- GradAppTracker Database Schema
-- This file defines all tables and relationships for the application.
-- Documentation: Each table is described above its definition.

-- Table: users
-- Stores user accounts for the application.
CREATE TABLE users (
    user_id SERIAL PRIMARY KEY, -- Unique user identifier
    name VARCHAR(150) NOT NULL, -- User's full name
    email VARCHAR(255) NOT NULL UNIQUE, -- User's email address (must be unique)
    password VARCHAR(255) NOT NULL -- Hashed password
);

-- Table: programs
-- Stores graduate programs tracked by users.
CREATE TABLE programs (
    program_id SERIAL PRIMARY KEY, -- Unique program identifier
    user_id INT NOT NULL REFERENCES users(user_id) ON DELETE CASCADE, -- Owner user
    university_name VARCHAR(255) NOT NULL, -- University name
    field_of_study VARCHAR(255), -- Field of study
    focus_area TEXT, -- Specific focus area
    portal VARCHAR(255), -- Application portal URL
    website VARCHAR(255), -- Program website URL
    deadline DATE, -- Application deadline
    status VARCHAR(50), -- Application status
    tuition TEXT, -- Tuition information
    requirements TEXT, -- Admission requirements
    notes TEXT -- Additional notes
);

-- Table: documents
-- Stores documents uploaded by users (e.g., transcripts, SOPs).
CREATE TABLE documents (
    document_id SERIAL PRIMARY KEY, -- Unique document identifier
    user_id INT NOT NULL REFERENCES users(user_id) ON DELETE CASCADE, -- Owner user
    file_name VARCHAR(255) NOT NULL, -- Name of the file
    file_path VARCHAR(500) NOT NULL, -- Path to the file on disk
    doc_type VARCHAR(100), -- Type of document (e.g., transcript, SOP)
    notes TEXT -- Additional notes
);

-- Table: program_documents
-- Links documents to specific programs (e.g., which document was used for which application).
CREATE TABLE program_documents (
    program_doc_id SERIAL PRIMARY KEY, -- Unique link identifier
    program_id INT NOT NULL REFERENCES programs(program_id) ON DELETE CASCADE, -- Linked program
    document_id INT NOT NULL REFERENCES documents(document_id) ON DELETE CASCADE, -- Linked document
    usage_notes TEXT, -- Notes about document usage
    UNIQUE(program_id, document_id) -- Prevent duplicate links
);
