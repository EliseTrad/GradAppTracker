# GradAppTracker

A lightweight JavaFX + Spring Boot app to track graduate program applications,
upload and link documents, and manage your application workflow.

This repository contains both the backend (Spring Boot) and the frontend
(JavaFX) clients in the same project for local development and testing.

---

## Quick Start

This README provides cross-platform instructions to get the app running locally.
For full developer notes see the project `src/` structure and in-repo docs.

### Prerequisites

- Java 17 or higher
- Maven 3.6+
- PostgreSQL (or compatible Postgres instance)

Check versions:

```bash
java -version
mvn -version
```

### Database

1. Create the database (example uses PostgreSQL):

```sql
CREATE DATABASE gradapptracker;
```

2. Apply the schema (run from project root):

```bash
# Adjust host/port/username as needed
psql -U <db_user> -d gradapptracker -f schema.sql
```

3. Configure application properties:

**Copy the example file and customize it:**

```bash
cp src/main/resources/application.properties.example src/main/resources/application.properties
```

Then edit `src/main/resources/application.properties` with your local database
credentials:

```properties
spring.datasource.url=jdbc:postgresql://localhost:5432/gradapptracker
spring.datasource.username=postgres
spring.datasource.password=your-password-here
spring.jpa.hibernate.ddl-auto=update
spring.jpa.show-sql=true

app.jwt.secret=your-secure-secret-key-min-256-bits
app.jwt.expirationMs=3600000
app.upload.dir=./uploads
spring.profiles.active=dev
```

**Important:** `application.properties` is in `.gitignore` and will NOT be
committed to version control. This keeps your credentials private. The
`application.properties.example` file is committed as a template for other
developers.

### Build

From the repository root run:

```bash
mvn clean compile
```

Expected output: `BUILD SUCCESS`.

### Run (development)

Recommended: start backend and frontend together via the Maven launcher (JavaFX
plugin):

```bash
mvn javafx:run
```

Alternative: run backend and frontend separately from your IDE. The JavaFX main
class is `com.gradapptracker.ui.AppLauncher`.

---

## First-Time Usage (UI flow)

1. Register a new account from the Welcome screen.
2. Login with your credentials.
3. You will land on the Dashboard (Programs).
4. Use the `Add Program` dialog to create new program entries (only
   `universityName` is required).
5. Upload documents in the Documents page and link them to programs.

All user-visible validation messages are provided by the backend and shown to
the user via the GUI.

---

## Supported File Types (Documents)

- PDF, DOCX, DOC, TXT, JPG, JPEG, PNG
- Max file size: 5 MB (enforced on backend)

Documents are stored on disk under `uploads/{userId}/` and metadata in the
Postgres database. Deleting a program does not delete linked documents; a
document linked to programs cannot be deleted until unlinked.

---

## API Documentation

The backend exposes REST endpoints (JWT-auth protected) for Users, Programs,
Documents, Program-Document linking, and Dashboard statistics.

### Authentication

All API endpoints (except `/register` and `/login`) require JWT authentication:

```
Authorization: Bearer <your-jwt-token>
```

### Endpoints Summary

#### User Endpoints (`/api/users`)

| Method | Endpoint                              | Auth | Description                 |
| ------ | ------------------------------------- | ---- | --------------------------- |
| POST   | `/api/users/register`                 | No   | Register new user account   |
| POST   | `/api/users/login`                    | No   | Login and receive JWT token |
| GET    | `/api/users/{userId}`                 | Yes  | Get user profile by ID      |
| PUT    | `/api/users/{userId}`                 | Yes  | Update user profile         |
| DELETE | `/api/users/{userId}`                 | Yes  | Delete user account         |
| POST   | `/api/users/{userId}/change-password` | Yes  | Change user password        |

**Example: Register**

```bash
POST /api/users/register
Content-Type: application/json

{
  "name": "John Doe",
  "email": "john@example.com",
  "password": "SecurePass123"
}

Response (200 OK):
{
  "userId": 1,
  "name": "John Doe",
  "email": "john@example.com",
  "createdAt": "2025-12-07T10:30:00Z"
}
```

**Example: Login**

```bash
POST /api/users/login
Content-Type: application/json

{
  "email": "john@example.com",
  "password": "SecurePass123"
}

Response (200 OK):
{
  "token": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
  "userId": 1,
  "email": "john@example.com"
}
```

#### Program Endpoints (`/api/programs`)

| Method | Endpoint                    | Auth | Description                             |
| ------ | --------------------------- | ---- | --------------------------------------- |
| GET    | `/api/programs`             | Yes  | Get all programs for authenticated user |
| GET    | `/api/programs/{programId}` | Yes  | Get program by ID (ownership verified)  |
| GET    | `/api/programs/filter`      | Yes  | Filter programs by criteria             |
| POST   | `/api/programs`             | Yes  | Create new program                      |
| PUT    | `/api/programs/{programId}` | Yes  | Update existing program                 |
| DELETE | `/api/programs/{programId}` | Yes  | Delete program (cascades to links)      |

**Example: Create Program**

```bash
POST /api/programs
Authorization: Bearer <jwt-token>
Content-Type: application/json

{
  "universityName": "Stanford University",
  "fieldOfStudy": "Computer Science",
  "focusArea": "Machine Learning",
  "deadline": "2026-12-15",
  "status": "Applied",
  "portal": "ApplyWeb",
  "website": "https://cs.stanford.edu",
  "tuition": "$54,000/year",
  "requirements": "GRE, 3 LORs, SOP, Transcripts",
  "notes": "Strong research program in AI"
}

Response (201 Created):
{
  "id": 1,
  "universityName": "Stanford University",
  "fieldOfStudy": "Computer Science",
  "status": "Applied",
  "deadline": "2024-12-15",
  ...
}
```

**Example: Filter Programs**

```bash
GET /api/programs/filter?universityName=Stanford&status=Applied
Authorization: Bearer <jwt-token>

Response (200 OK):
[
  {
    "id": 1,
    "universityName": "Stanford University",
    "status": "Applied",
    ...
  }
]
```

#### Document Endpoints (`/api/users/{userId}/documents`)

| Method | Endpoint                                     | Auth | Description                               |
| ------ | -------------------------------------------- | ---- | ----------------------------------------- |
| GET    | `/api/users/{userId}/documents`              | Yes  | Get all documents for user                |
| POST   | `/api/users/{userId}/documents`              | Yes  | Upload new document (multipart/form-data) |
| DELETE | `/api/users/{userId}/documents/{documentId}` | Yes  | Delete document (must be unlinked first)  |

**Example: Upload Document**

```bash
POST /api/users/1/documents
Authorization: Bearer <jwt-token>
Content-Type: multipart/form-data

Form Data:
- file: (binary file data)
- docType: "Transcript"
- notes: "Official transcript from undergraduate university"

Response (201 Created):
{
  "documentId": 10,
  "userId": 1,
  "fileName": "transcript.pdf",
  "docType": "Transcript",
  "notes": "Official transcript...",
  "createdAt": "2025-12-07T11:00:00Z"
}
```

#### Program-Document Linking (`/api/programs/{programId}/documents`)

| Method | Endpoint                                             | Auth | Description                         |
| ------ | ---------------------------------------------------- | ---- | ----------------------------------- |
| GET    | `/api/programs/{programId}/documents`                | Yes  | Get all documents linked to program |
| POST   | `/api/programs/{programId}/documents`                | Yes  | Link existing document to program   |
| DELETE | `/api/programs/{programId}/documents/{programDocId}` | Yes  | Unlink document from program        |

**Example: Link Document to Program**

```bash
POST /api/programs/1/documents
Authorization: Bearer <jwt-token>
Content-Type: application/json

{
  "documentId": 10,
  "usageNotes": "Submitted as part of application package"
}

Response (201 Created):
{
  "programDocId": 5,
  "programId": 1,
  "documentId": 10,
  "usageNotes": "Submitted as part of application package",
  "createdAt": "2025-12-07T11:30:00Z"
}
```

#### Dashboard Endpoints (`/api/dashboard`)

| Method | Endpoint               | Auth | Description                                    |
| ------ | ---------------------- | ---- | ---------------------------------------------- |
| GET    | `/api/dashboard/stats` | Yes  | Get aggregated statistics for 3D visualization |

**Example: Get Dashboard Stats**

```bash
GET /api/dashboard/stats
Authorization: Bearer <jwt-token>

Response (200 OK):
{
  "totalPrograms": 15,
  "totalDocuments": 32,
  "statusCounts": {
    "Accepted": 3,
    "Applied": 5,
    "In Progress": 4,
    "Rejected": 2,
    "Other": 1
  }
}
```

### Error Responses

All endpoints return consistent error responses:

```json
{
  "status": 400,
  "message": "Validation failed",
  "timestamp": "2025-12-07T12:00:00Z",
  "errors": [
    {
      "field": "universityName",
      "message": "must not be blank"
    }
  ]
}
```

**Common HTTP Status Codes:**

- `200 OK` - Request succeeded
- `201 Created` - Resource created successfully
- `400 Bad Request` - Validation error or malformed request
- `401 Unauthorized` - Missing or invalid JWT token
- `403 Forbidden` - Authenticated but not authorized
- `404 Not Found` - Resource doesn't exist or doesn't belong to user
- `409 Conflict` - Duplicate resource (e.g., email already exists)
- `500 Internal Server Error` - Server error

### Validation Rules

- **Email**: Must be valid format and unique
- **Password**: Required for registration/login (hashed with BCrypt)
- **University Name**: Required for program creation
- **File Upload**: Max 5MB, allowed types: PDF, DOCX, DOC, TXT, JPG, JPEG, PNG
- **Status Values**: Strict enum (Accepted, Applied, In Progress, Rejected,
  Other)
- **Deadlines**: Must be valid ISO date format (YYYY-MM-DD)

---

## Troubleshooting

- "Connection refused" → backend not running. Start backend with:

```bash
mvn spring-boot:run
```

- Database errors → verify PostgreSQL is running and credentials in
  `application.properties` are correct.

- Date serialization errors (LocalDate) when creating resources: ensure the
  frontend and backend are using compatible Jackson configuration. The project
  includes helpers to parse backend validation messages.

If you see backend JSON or stack traces in the UI, check the backend console for
the full exception and the frontend console for the HTTP payload.

---

## Third-Party Libraries & Dependencies

### Backend (Spring Boot)

| Library                            | Version      | Purpose                                                  |
| ---------------------------------- | ------------ | -------------------------------------------------------- |
| **Spring Boot Starter Web**        | 3.1.6        | REST API framework, embedded Tomcat server               |
| **Spring Boot Starter Data JPA**   | 3.1.6        | ORM (Hibernate), database interaction                    |
| **Spring Boot Starter Security**   | 3.1.6        | Authentication, authorization, security filters          |
| **Spring Boot Starter Validation** | 3.1.6        | Bean validation (JSR-380), request validation            |
| **Spring Boot DevTools**           | 3.1.6        | Hot reload, development utilities                        |
| **JJWT API**                       | 0.11.5       | JWT token generation and parsing (API)                   |
| **JJWT Impl**                      | 0.11.5       | JWT implementation (runtime)                             |
| **JJWT Jackson**                   | 0.11.5       | JWT JSON processing with Jackson (runtime)               |
| **PostgreSQL JDBC Driver**         | 42.6.0       | Database connectivity for PostgreSQL                     |
| **Lombok**                         | 1.18.28      | Reduce boilerplate code (getters, setters, constructors) |
| **Jackson Datatype JSR310**        | (via Spring) | Java 8 Date/Time API serialization (LocalDate, etc.)     |

### Frontend (JavaFX)

| Library             | Version | Purpose                                          |
| ------------------- | ------- | ------------------------------------------------ |
| **JavaFX Controls** | 20      | UI controls (Button, TextField, TableView, etc.) |
| **JavaFX FXML**     | 20      | Declarative UI layout with FXML files            |
| **JavaFX Graphics** | 20      | 2D graphics, shapes, canvas                      |
| **JavaFX Base**     | 20      | Core JavaFX APIs, properties, collections        |

### Build Tools & Plugins

| Plugin                       | Version  | Purpose                                                         |
| ---------------------------- | -------- | --------------------------------------------------------------- |
| **Maven Compiler Plugin**    | 3.11.0   | Java compilation with annotation processing                     |
| **Spring Boot Maven Plugin** | (parent) | Package Spring Boot application, run with `mvn spring-boot:run` |
| **JavaFX Maven Plugin**      | 0.0.8    | Run JavaFX application with `mvn javafx:run`                    |
| **Exec Maven Plugin**        | 3.1.0    | Execute Java classes from Maven                                 |
| **OS Maven Plugin**          | 1.7.0    | Detect operating system for JavaFX platform-specific artifacts  |

### Why These Libraries?

- **Spring Boot**: Industry-standard framework for enterprise Java applications,
  provides dependency injection, auto-configuration, and production-ready
  features
- **Spring Security + JJWT**: Secure API with stateless JWT authentication, no
  session management needed
- **Spring Data JPA + Hibernate**: Simplified database access with ORM, reduces
  SQL boilerplate
- **PostgreSQL**: Robust, open-source relational database with excellent JSON
  support and ACID compliance
- **JavaFX**: Modern, cross-platform UI framework with FXML for declarative
  layouts and CSS styling
- **Lombok**: Reduces Java verbosity, improves code readability
- **Jackson**: Fast JSON processing, seamless integration with Spring Boot

---

## Database Schema & Entity Relationship Diagram (ERD)

### Database Design

The application uses a **normalized relational database** (3NF - Third Normal
Form) with PostgreSQL. The schema enforces referential integrity through foreign
key constraints and provides audit trails with timestamps.

### Tables & Relationships

```
┌─────────────────┐
│     users       │
│─────────────────│
│ PK: user_id     │ ←──┐
│ email (UNIQUE)  │    │
│ password        │    │
│ name            │    │
│ roles           │    │
│ created_at      │    │
│ updated_at      │    │
└─────────────────┘    │
         │             │
         │ 1           │ 1
         │             │
         ↓ M           ↓ M
┌─────────────────┐   ┌─────────────────┐
│    programs     │   │   documents     │
│─────────────────│   │─────────────────│
│ PK: program_id  │   │ PK: document_id │
│ FK: user_id     │   │ FK: user_id     │
│ university_name │   │ file_name       │
│ field_of_study  │   │ file_path       │
│ focus_area      │   │ doc_type        │
│ deadline        │   │ notes           │
│ status          │   │ created_at      │
│ portal          │   │ updated_at      │
│ website         │   │                 │
│ tuition         │   │                 │
│ requirements    │   │                 │
│ notes           │   │                 │
│ created_at      │   │                 │
│ updated_at      │   │                 │
└─────────────────┘   └─────────────────┘
         │ M                   │ M
         │                     │
         └──────┐     ┌────────┘
                │     │
                ↓ M   ↓ M
         ┌──────────────────────┐
         │  program_documents   │ (Junction Table)
         │──────────────────────│
         │ PK: program_doc_id   │
         │ FK: program_id       │
         │ FK: document_id      │
         │ usage_notes          │
         │ created_at           │
         │ UNIQUE(program_id, document_id) │
         └──────────────────────┘
```

### Relationship Types

1. **users → programs**: One-to-Many

   - One user can create multiple programs
   - `programs.user_id` references `users.user_id`
   - CASCADE DELETE: Deleting user removes all their programs

2. **users → documents**: One-to-Many

   - One user can upload multiple documents
   - `documents.user_id` references `users.user_id`
   - CASCADE DELETE: Deleting user removes all their documents

3. **programs ↔ documents**: Many-to-Many (via program_documents)
   - One program can have multiple documents
   - One document can be linked to multiple programs
   - `program_documents` junction table manages the relationship
   - CASCADE DELETE on both sides: Deleting program or document removes link
   - UNIQUE constraint prevents duplicate links

### Key Constraints

- **Primary Keys**: All tables use SERIAL (auto-incrementing) PKs
- **Foreign Keys**: All relationships enforced with ON DELETE CASCADE
- **Unique Constraints**:
  - `users.email` must be unique
  - `program_documents(program_id, document_id)` prevents duplicate links
- **NOT NULL**: Essential fields (email, password, university_name, etc.)
- **Indexes**: Created on foreign keys, email, deadline for query performance

### Audit Trail

All tables include:

- `created_at`: Timestamp when record was created (auto-set)
- `updated_at`: Timestamp when record was last modified (auto-updated via
  database triggers)

### File Storage

Documents are stored on the filesystem (not in database):

- Path format: `uploads/{userId}/{documentId}_{filename}`
- Database stores file path, metadata only
- Max file size: 5MB
- Supported types: PDF, DOCX, DOC, TXT, JPG, JPEG, PNG

### Database Migrations

Currently using direct SQL schema file (`schema.sql`). For production, consider
migration tools like Flyway or Liquibase for version-controlled schema changes.

---

## How to Run and Test the Application End-to-End

### Step-by-Step Guide

#### 1. Prerequisites Check

```bash
# Verify Java 17+
java -version

# Verify Maven 3.6+
mvn -version

# Verify PostgreSQL is running
psql --version
```

#### 2. Database Setup

```bash
# Create database
createdb gradapptracker

# OR using psql
psql -U postgres
CREATE DATABASE gradapptracker;
\q

# Apply schema
psql -U postgres -d gradapptracker -f schema.sql
```

#### 3. Configure Environment

```powershell
# Windows PowerShell
$env:DB_HOST="localhost"
$env:DB_PORT="5432"
$env:DB_NAME="gradapptracker"
$env:DB_USER="postgres"
$env:DB_PASSWORD="your-password-here"
```

#### 4. Build the Application

```bash
# Clean and compile
mvn clean compile

# Expected output: BUILD SUCCESS
```

#### 5. Run the Application

**Option A: Run Complete Application (Recommended)**

```bash
# Starts both backend and frontend together
mvn javafx:run

# Wait for Spring Boot to start (console shows "Started GradAppTrackerApplication")
# JavaFX window opens automatically
```

**Option B: Run Backend and Frontend Separately**

```bash
# Terminal 1: Start backend
mvn spring-boot:run

# Terminal 2: Start frontend
mvn javafx:run
```

#### 6. Test the Application (Manual Testing)

**Test 1: User Registration & Login**

1. Click "Register" button on welcome screen
2. Fill in: Name, Email, Password
3. Click "Create Account"
4. Verify success message
5. Login with created credentials
6. Verify redirect to Programs dashboard

**Test 2: Create Program**

1. Click "Add New Program" button
2. Enter University Name: "MIT"
3. Enter Field of Study: "Computer Science"
4. Select Status: "Applied"
5. Enter Deadline: 2026-12-31
6. Click "Save"
7. Verify program appears in table

**Test 3: Upload Document**

1. Navigate to "Documents" page
2. Click "Upload Document"
3. Select a PDF file (< 5MB)
4. Enter Doc Type: "Transcript"
5. Click "Upload"
6. Verify document appears in list

**Test 4: Link Document to Program**

1. Navigate to "Programs" page
2. Select a program from table
3. Click "Link Document"
4. Select document from available list
5. Add usage notes (optional)
6. Click "Link"
7. Verify document appears in "Linked Documents" panel

**Test 5: 3D Analytics Dashboard**

1. Navigate to "3D Analytics" page
2. Verify 3D cylinders render with program statistics
3. Drag mouse to rotate view
4. Hover over cylinder to see highlight effect
5. Click cylinder to view details panel
6. Click "Refresh Data" to reload stats

**Test 6: Filter Programs**

1. Navigate to "Programs" page
2. Enter filter criteria (e.g., University: "MIT")
3. Click "Apply Filter"
4. Verify only matching programs show
5. Click "Clear Filter" to show all

**Test 7: Update Program**

1. Select program from table
2. Click "Edit"
3. Change status to "Accepted"
4. Click "Update"
5. Verify changes saved and displayed

**Test 8: Delete Program**

1. Select program from table
2. Click "Delete"
3. Confirm deletion in dialog
4. Verify program removed from list
5. Verify linked documents are unlinked (check Documents page)

#### 7. API Testing (Optional - using cURL)

**Register User**

```bash
curl -X POST http://localhost:8080/api/users/register \
  -H "Content-Type: application/json" \
  -d "{\"name\":\"Test User\",\"email\":\"test@example.com\",\"password\":\"Pass123\"}"
```

**Login**

```bash
curl -X POST http://localhost:8080/api/users/login \
  -H "Content-Type: application/json" \
  -d "{\"email\":\"test@example.com\",\"password\":\"Pass123\"}"

# Save the returned JWT token
```

**Create Program**

```bash
curl -X POST http://localhost:8080/api/programs \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer <your-jwt-token>" \
  -d "{\"universityName\":\"Stanford\",\"fieldOfStudy\":\"CS\",\"status\":\"Applied\"}"
```

**Get Programs**

```bash
curl http://localhost:8080/api/programs \
  -H "Authorization: Bearer <your-jwt-token>"
```

#### 8. Verify Security

**Test unauthorized access (should fail with 401)**

```bash
curl http://localhost:8080/api/programs
# Expected: {"status":401,"message":"Unauthorized"}
```

**Test with invalid JWT**

```bash
curl http://localhost:8080/api/programs \
  -H "Authorization: Bearer invalid-token"
# Expected: {"status":401,"message":"Unauthorized"}
```

#### 9. Troubleshooting

**"Connection refused" error**

- Verify PostgreSQL is running: `pg_isready`
- Check credentials in `application.properties`
- Verify database exists: `psql -l | grep gradapptracker`

**"Port 8080 already in use"**

- Change port in `application.properties`: `server.port=8081`
- Or kill process using port: `lsof -ti:8080 | xargs kill -9` (Mac/Linux)

**JavaFX not rendering**

- Verify JavaFX platform classifier matches your OS in `pom.xml`
- Windows: `<javafx.platform>win</javafx.platform>`
- Mac: `<javafx.platform>mac</javafx.platform>`
- Linux: `<javafx.platform>linux</javafx.platform>`

**"BUILD FAILURE" during compile**

- Clean Maven cache: `mvn clean`
- Delete `target/` directory
- Re-run: `mvn clean compile`

### Expected Test Results

✅ All user authentication flows work  
✅ CRUD operations on programs succeed  
✅ Document upload/delete operations succeed  
✅ Program-document linking works bidirectionally  
✅ 3D visualization renders with real data  
✅ Filtering and searching return correct results  
✅ JWT authentication prevents unauthorized access  
✅ Error messages are user-friendly  
✅ UI remains responsive during API calls  
✅ Data persists across application restarts

---

## Development notes

- Project contains both backend and frontend Java code under `src/main/java`.
- JavaFX views (FXML) are under
  `src/main/resources/com/gradapptracker/ui/views`.
- Services that call the backend live in
  `src/main/java/com/gradapptracker/ui/services`.
- Note: No unit tests are currently implemented in this project.

### Common commands

Build only:

```bash
mvn -DskipTests clean package
```

Start backend (separately):

```bash
mvn spring-boot:run
```

Start GUI (separately via Maven):

```bash
mvn javafx:run
```

---

## Contributing

1. Fork the repo, create a feature branch, open a PR.
2. Keep commits small and focused; include a short description of the change and
   the testing steps.

---

## Where to look next

- Database schema: `schema.sql`
- Main JavaFX launcher: `src/main/java/com/gradapptracker/ui/AppLauncher.java`
- Frontend controllers: `src/main/java/com/gradapptracker/ui/controllers`
- Backend controllers and DTOs: `src/main/java/com/gradapptracker/backend`

---

Thank you — happy tracking!
