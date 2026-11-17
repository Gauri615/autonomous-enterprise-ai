# Enterprise API

The authoritative Java business API for the Autonomous Enterprise AI System. It is the intended boundary for business policy and authorization; AI-generated content is not authorization, and the Python AI service must not execute business tools or bypass this API.

This is the initial application foundation only. It currently provides Spring Boot Actuator health status and an application-context test; no business workflows or business endpoints are implemented.

## Technology

- Java 25
- Spring Boot 4.1.2
- Maven Wrapper (Maven 3.10.0 distribution)
- MariaDB Connector/J 3.5.10 for the primary database
- H2, test scope only, for isolated tests

Spring Initializr generated the Maven project and wrapper with the selected Spring Boot 4.1.2 dependency identifiers.

## Prerequisites

- JDK 25; ensure `JAVA_HOME` and `PATH` select the JDK 25 installation.
- MariaDB 10.11.16 from the local XAMPP installation, managed with phpMyAdmin.
- The `autonomous_enterprise_ai` database and least-privilege `aea_app@localhost` runtime account.

The verified local connection is `localhost:3306/autonomous_enterprise_ai`. MariaDB CLI login as `aea_app` and Spring Boot connectivity both succeeded. See the [database setup reference](../../docs/architecture/DATABASE_SETUP.md) for verified grants, environment setup, and future migration requirements.

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

[`.env.example`](./.env.example) contains placeholders only. Spring Boot does not load `.env` files automatically; do not commit a real `.env` file or put credentials in source control. The runtime account has only `SELECT`, `INSERT`, `UPDATE`, and `DELETE` on `autonomous_enterprise_ai.*`. JPA uses `ddl-auto: none`; the application cannot create or modify the schema. Keep future DDL/migrations separate from the runtime identity; details are in the [database setup reference](../../docs/architecture/DATABASE_SETUP.md).

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

The context test activates the `test` profile and uses an in-memory H2 database. It does not connect to MariaDB.

## Start the application

Set the database environment variables above in this PowerShell session, ensure the MariaDB service is running, and run:

```powershell
.\mvnw.cmd spring-boot:run
```

The Actuator health endpoint is `http://localhost:8080/actuator/health`. The verified local application started successfully, HikariCP established a MariaDB connection, Hibernate initialized with `MariaDBDialect`, and the health endpoint returned `UP` with liveness and readiness both `UP`. Only the health endpoint is exposed; health details and components are not exposed.

## Current limitations

- Authentication, authorization policies, tenant context, business modules, API contracts, and business endpoints are not implemented.
- No tickets, approvals, audit workflows, AI integration, retrieval, or tool execution are implemented.
- The database currently has no tables or application schema. There are no migrations; Hibernate schema creation and updates are disabled.
- Runtime permissions are DML-only. Future schema creation and migration privileges/processes are not yet selected or configured.
- The Knowledge Agent's vector retrieval technology remains undecided; no vector store or retrieval implementation is configured.
- MariaDB credentials must be supplied locally. No real credentials are included here.
