# Enterprise API

The authoritative Java business API for the Autonomous Enterprise AI System. It is the intended boundary for business policy and authorization; AI-generated content is not authorization, and the Python AI service must not execute business tools or bypass this API.

This is the initial application foundation only. It currently provides Spring Boot Actuator health status and an application-context test; no business workflows or business endpoints are implemented.

## Technology

- Java 25
- Spring Boot 4.1.2
- Maven Wrapper (Maven 3.10.0 distribution)
- MariaDB JDBC driver for the primary database
- H2, test scope only, for isolated tests

Spring Initializr generated the Maven project and wrapper with the selected Spring Boot 4.1.2 dependency identifiers.

## Prerequisites

- JDK 25; ensure `JAVA_HOME` and `PATH` select the JDK 25 installation.
- MariaDB 10.11.16 for local application runs. XAMPP/phpMyAdmin are the documented local setup.
- A local MariaDB account and database connection values supplied through environment variables.

The configured default JDBC URL uses `localhost:3306`; the port is expected but has not been independently verified. Database connectivity has not been tested.

## Configure database environment variables

`application.yml` reads `DB_URL`, `DB_USERNAME`, and `DB_PASSWORD`. The JDBC URL defaults to `jdbc:mariadb://localhost:3306/autonomous_enterprise_ai`; verify the local port before relying on that default. Username and password must be supplied by the local environment and are not stored in this repository.

For Windows PowerShell, set the values in the current terminal session, replacing the username and password placeholders locally:

```powershell
$env:DB_URL = "jdbc:mariadb://localhost:3306/autonomous_enterprise_ai"
$env:DB_USERNAME = "replace-with-your-local-username"
$env:DB_PASSWORD = "replace-with-your-local-password"
```

[`.env.example`](./.env.example) contains placeholders only. Spring Boot does not load `.env` files automatically; do not commit a real `.env` file or put credentials in source control. The schema setting is `ddl-auto: none`; this application does not create or update tables.

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

Set the database environment variables above, ensure the MariaDB service is running, and run:

```powershell
.\mvnw.cmd spring-boot:run
```

The Actuator health endpoint is `http://localhost:8080/actuator/health`. Only the health endpoint is exposed; health details and components are not exposed.

## Current limitations

- Authentication, authorization policies, tenant context, business modules, API contracts, and business endpoints are not implemented.
- No tickets, approvals, audit workflows, AI integration, retrieval, or tool execution are implemented.
- There are no migrations or business tables. Hibernate schema creation and updates are disabled.
- The database name is documented, but tables/schema and application connectivity have not been verified. Port 3306 remains an unverified expected default.
- MariaDB credentials must be configured locally; no real credentials are included here.
