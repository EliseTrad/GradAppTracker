# GradAppTracker

A lightweight JavaFX + Spring Boot app to track graduate program applications,
upload and link documents, and manage your application workflow.

This repository contains both the backend (Spring Boot) and the frontend
(JavaFX) clients in the same project for local development and testing.

---

## Quick Start (README)

This README provides a concise, copy-pastable set of steps to get the app
running locally on Windows (PowerShell). For full developer notes see the
project `src/` structure and in-repo docs.

### Prerequisites

- Java 17 or higher
- Maven 3.6+
- PostgreSQL (or compatible Postgres instance)

Check versions:

```powershell
java -version
mvn -version
```

### Database

1. Create the database (example uses PostgreSQL):

```sql
CREATE DATABASE gradapptracker;
```

2. Apply the schema (run from project root):

```powershell
# runs psql against local Postgres; adjust host/port/username as needed
psql -U postgres -d gradapptracker -f schema.sql
```

3. Default connection properties are set in
   `src/main/resources/application.properties`. The defaults used in this
   project are:

```properties
spring.datasource.url=jdbc:postgresql://localhost:3001/gradapptracker
spring.datasource.username=postgres
spring.datasource.password=elise
upload.dir=uploads
spring.profiles.active=dev
```

Adjust those values for your environment before running the backend.

### Build

From the repository root run:

```powershell
mvn clean compile
```

Expected output: `BUILD SUCCESS`.

### Run (development)

Recommended: start backend and frontend together via the Maven launcher (JavaFX
plugin):

```powershell
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

## API (high level)

The backend exposes REST endpoints (JWT-auth protected) for Users, Programs,
Documents and Program-Document linking. Common endpoints:

- `POST /api/users/register` - register
- `POST /api/users/login` - login (receives JWT)
- `GET /api/programs` - list programs
- `POST /api/programs` - create program
- `POST /api/users/{userId}/documents` - upload document (multipart)
- `POST /api/programs/{programId}/documents` - link document to program

All endpoints require `Authorization: Bearer <JWT>` except login/register.

---

## Troubleshooting

- "Connection refused" → backend not running. Start backend with:

```powershell
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

## Development notes

- Project contains both backend and frontend Java code under `src/main/java`.
- JavaFX views (FXML) are under
  `src/main/resources/com/gradapptracker/ui/views`.
- Services that call the backend live in
  `src/main/java/com/gradapptracker/ui/services`.
- Tests (if present) are under `src/test` and run with `mvn test`.

### Common commands

Build only:

```powershell
mvn -DskipTests clean package
```

Run tests:

```powershell
mvn test
```

Start backend (separately):

```powershell
mvn spring-boot:run
```

Start GUI (separately via Maven):

```powershell
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

If you'd like, I can also:

- create a dedicated `README.md` from this content (so GitHub displays it),
- or add a quick troubleshooting checklist for common runtime errors.

---

Thank you — happy tracking!
