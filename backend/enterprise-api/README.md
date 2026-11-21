# Enterprise API

The authoritative Java business API for the Autonomous Enterprise AI System. It is the intended boundary for business policy and authorization; AI-generated content is not authorization, and the Python AI service must not execute business tools or bypass this API.

The application foundation provides Spring Boot Actuator health status and the first persistence slice for Organization, IdentityPrincipal, ExternalIdentity, and OrganizationMembership. Business workflows, authentication, and business endpoints are not implemented.

## Technology

- Java 25
- Spring Boot 4.1.2
- Maven Wrapper (Maven 3.10.0 distribution)
- MariaDB Connector/J 3.5.10 for the primary database
- H2, test scope only, for isolated tests

Spring Initializr generated the Maven project and wrapper with the selected Spring Boot 4.1.2 dependency identifiers.

## Prerequisites

- JDK 25; ensure `JAVA_HOME` and `PATH` select the JDK 25 installation.
- MariaDB 10.11.16 is the selected target; the current XAMPP server version was not re-verified in this task.
- The previously reported `autonomous_enterprise_ai` database and least-privilege runtime account; neither was re-verified in this task.

The setup document records a previously verified local connection, but this task did not re-verify the current XAMPP server or database. See the [database setup reference](../../docs/architecture/DATABASE_SETUP.md) for verification status and configuration.

## Configure database environment variables

`application.yml` reads `DB_URL`, `DB_USERNAME`, and `DB_PASSWORD`. Set them in the same PowerShell session that starts Spring Boot. Load the existing locally managed `DB_APP_PASSWORD` into that session without printing it:

```powershell
$env:DB_URL = "jdbc:mariadb://localhost:3306/autonomous_enterprise_ai"
$env:DB_USERNAME = "aea_app"
$env:DB_PASSWORD = $env:DB_APP_PASSWORD
```

`DB_APP_PASSWORD` is generated and managed locally. Never commit, print, or share it. Generate it only when provisioning or rotating the database account; do not generate a different value on every app launch. The application consumes it as `DB_PASSWORD`. Start the application from that same session:

```powershell
.\mvnw.cmd spring-boot:run
```

[`.env.example`](./.env.example) contains placeholders only. Spring Boot does not load `.env` files automatically; do not commit a real `.env` file or put credentials in source control. DML-only runtime privileges were previously reported but were not re-verified in this task. JPA uses `ddl-auto: none`; the application cannot create or modify the schema. Keep DDL/migrations separate from the runtime identity; details are in the [database setup reference](../../docs/architecture/DATABASE_SETUP.md).

## Maven Wrapper (Windows PowerShell)

From `backend/enterprise-api`:

```powershell
$env:JAVA_HOME = "C:\Path\To\jdk-25"
$env:Path = "$env:JAVA_HOME\bin;$env:Path"
.\mvnw.cmd -version
.\mvnw.cmd clean verify
```

The wrapper downloads its pinned Maven distribution on first use; a separate Maven installation is not required.

## Run tests

```powershell
.\mvnw.cmd test
```

Repository tests use an isolated in-memory H2 schema and do not connect to MariaDB. MariaDB integration tests are disabled unless explicitly enabled with test-specific connection settings; they were not run as part of this task.

## Start the application

Set the database environment variables above in this PowerShell session, ensure the MariaDB service is running, and run:

```powershell
.\mvnw.cmd spring-boot:run
```

The Actuator health endpoint is `http://localhost:8080/actuator/health`. A successful local startup and health check were previously reported, but were not re-verified in this task. Only the health endpoint is exposed; health details and components are not exposed.

## Current limitations

- Authentication, authorization policies, tenant context, business modules, API contracts, and business endpoints are not implemented.
- No tickets, approvals, audit workflows, AI integration, retrieval, or tool execution are implemented.
- The V1 identity-foundation migration exists at `src/main/resources/db/migration` but was not applied; the current database schema state is unverified. Hibernate schema creation and updates are disabled.
- Flyway is configured only as an explicitly invoked Maven plugin; it is absent from application runtime dependencies and does not run at application startup. Migration account provisioning and privileges remain unverified.
- The Knowledge Agent's vector retrieval technology remains undecided; no vector store or retrieval implementation is configured.
- MariaDB credentials must be supplied locally. No real credentials are included here.
