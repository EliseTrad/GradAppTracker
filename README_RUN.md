# Run backend and frontend (Windows PowerShell)

Open two separate PowerShell terminals.

## Terminal 1 — Backend (Spring Boot)

This runs the Spring Boot backend on the default port (8080).

```powershell
# From the repository root
mvn -DskipTests spring-boot:run
```

Wait for the backend to start and show "Started" and that it's listening on
port 8080.

## Terminal 2 — Frontend (JavaFX)

This launches the JavaFX frontend `AppLauncher` from the same Maven project.

```powershell
# From the repository root
mvn -DskipTests compile org.codehaus.mojo:exec-maven-plugin:3.1.0:java -Dexec.mainClass="com.gradapptracker.ui.AppLauncher"
```

Notes:

- Make sure you have a JDK (Java 17) available and `JAVA_HOME` set.
- The frontend's `ApiClient` expects the backend at `http://localhost:8080/api`
  by default — confirm the backend is running on port 8080 before starting the
  frontend.
- If you'd rather run the backend as a packaged jar:

```powershell
# Package backend (creates a jar under target/) then run the jar
mvn -DskipTests package
java -jar target\*.jar
```

If you want me to add these commands into your main `README.md` instead (or to
adapt them to PowerShell profiles / VS Code tasks), tell me where to put them
and I'll update the file.
