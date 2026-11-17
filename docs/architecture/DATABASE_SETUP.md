# Database Setup

## 1. Database Overview

MariaDB 10.11.16 is the selected primary relational database for the Autonomous Enterprise AI System. The database is the planned source of truth for organization-scoped application records, knowledge metadata, workflow state, approvals, simulated tool outcomes, and audit events.

This document is the central source of truth for the verified local database setup and for decisions that remain open. Local runtime and health checks were verified on 2026-11-17; no production database is configured. The logical model is documented in [DATABASE_ER_DIAGRAM.md](./DATABASE_ER_DIAGRAM.md).

## 2. Confirmed Environment

| Setting | Confirmed value | Status / note |
|---|---|---|
| Database engine/version | MariaDB 10.11.16 | Verified against the running server |
| Local installation | XAMPP | Verified local development installation |
| Management interface | phpMyAdmin | Local database management interface |
| Database name | `autonomous_enterprise_ai` | Exists and is selected by the application connection |
| Tables/schema | None | Verified: the database currently has no tables and no application schema |
| Application account | `aea_app`@`localhost` | CLI login succeeded; `CURRENT_USER()` returned `aea_app@localhost` |
| JDBC driver | MariaDB Connector/J 3.5.10 | Verified application dependency/runtime |
| Backend runtime | Java 25, Spring Boot 4.1.2 | Verified application configuration/runtime |
| Backend port | `8080` | Health endpoint verified at this port |
| Hibernate schema management | `ddl-auto: none` | Verified configuration; the application does not create or update tables |

XAMPP and phpMyAdmin describe the local development setup only. They do not select a production hosting or deployment architecture.

## 3. Database Connection Details

| Connection setting | Value | Verification status |
|---|---|---|
| Host | `localhost` | Verified application connection |
| Port | `3306` | Verified XAMPP MariaDB port |
| Database | `autonomous_enterprise_ai` | Verified by `DATABASE()` |
| Engine/version | MariaDB 10.11.16 | Verified server |
| Username | `aea_app` | Verified account; matched account is `aea_app@localhost` |
| Password | `DB_PASSWORD` environment variable | Supplied locally; its value must never be recorded here |
| JDBC URL | `DB_URL` environment variable | `jdbc:mariadb://localhost:3306/autonomous_enterprise_ai` |
| Runtime privileges | `SELECT`, `INSERT`, `UPDATE`, `DELETE` on `autonomous_enterprise_ai.*` | Verified; no `CREATE`, `ALTER`, `DROP`, or `GRANT OPTION` |

The Spring Boot datasource reads `DB_URL`, `DB_USERNAME`, and `DB_PASSWORD`. `DB_URL` defaults to the URL above; the local launch instructions set it explicitly. The application connects through HikariCP and Hibernate initializes with `MariaDBDialect`.

### Environment-variable setup

The backend's tracked `backend/enterprise-api/.env.example` contains placeholders only. Spring Boot does not load `.env` files automatically. Set the values in the same Windows PowerShell session that launches Spring Boot:

```powershell
$env:DB_URL = "jdbc:mariadb://localhost:3306/autonomous_enterprise_ai"
$env:DB_USERNAME = "aea_app"
$env:DB_PASSWORD = $env:DB_APP_PASSWORD
.\mvnw.cmd spring-boot:run
```

`DB_APP_PASSWORD` is generated locally and must match the password configured for `aea_app`. Generate it only when provisioning or rotating that account, not on every application launch. Load the existing value from a local secret store into the PowerShell session before assigning it to `DB_PASSWORD`. Never commit, print, or share `DB_APP_PASSWORD`; do not put real credentials in tracked files or a committed `.env` file. The variables set in a PowerShell session are inherited by processes started from that session and are not automatically available to separately launched IDE processes.

For local account provisioning or password rotation, a 32-byte cryptographic value can be generated without printing it:

```powershell
$bytes = New-Object byte[] 32
$rng = [System.Security.Cryptography.RandomNumberGenerator]::Create()
try {
    $rng.GetBytes($bytes)
    $env:DB_APP_PASSWORD = [Convert]::ToBase64String($bytes)
}
finally {
    [Array]::Clear($bytes, 0, $bytes.Length)
    $rng.Dispose()
}
```

The generated value must be applied to the database account through the approved local account-provisioning process and stored in a local secret store for later launches. This documentation task does not provision or change database credentials.

## 4. Database Creation and Verification

The following setup and runtime results were verified for the local environment on 2026-11-17:

| Check | Result |
|---|---|
| MariaDB CLI login as `aea_app` | Succeeded |
| `CURRENT_USER()` | `aea_app@localhost` |
| `DATABASE()` | `autonomous_enterprise_ai` |
| Database contents | No tables exist; no application schema has been created |
| Runtime grants | Only `SELECT`, `INSERT`, `UPDATE`, and `DELETE` on `autonomous_enterprise_ai.*`; no DDL or grant-option privileges |
| Spring Boot startup | Succeeded |
| HikariCP | Established a MariaDB connection |
| Hibernate | Initialized using `MariaDBDialect` |
| `GET http://localhost:8080/actuator/health` | `status: UP`; liveness and readiness groups both `UP` |

```sql
SELECT DATABASE(), VERSION();
```

```sql
SELECT CURRENT_USER();
```

```sql
SHOW TABLES;
```

The read-only queries above can be used to recheck server/database selection and table state. The current verified result of `SHOW TABLES` is empty. No schema or tables have been created.

## 5. Planned Database Entities

The following are logical entities in the planned model, not verified database tables:

| Entity | Purpose / scope |
|---|---|
| Organization | Tenant boundary and ownership root |
| User | Controlled demo identity associated with an organization |
| Role | Organization-scoped role definition |
| UserRole | User-to-role assignment within the same organization |
| Ticket | Internal IT support request and workflow outcome; not an external service-desk ticket |
| TicketComment | User, analyst, or system-authored note associated with a ticket |
| KnowledgeDocument | Curated source metadata and permission attributes |
| KnowledgeChunk | Searchable document segment and source metadata |
| AgentDefinition | Global or organization-scoped workflow/agent metadata |
| AgentRun | Bounded, request-correlated workflow execution |
| AgentEvent | Traceable event within an agent run |
| ToolDefinition | Global or organization-scoped metadata for an allow-listed simulation |
| ApprovalRequest | Proposal context and human decision lifecycle |
| ToolExecution | Attempt and outcome record for an approved simulated action |
| AuditEvent | Attributable, correlated record of material workflow/security events |
| Feedback | Optional employee evaluation; MVP-Should, not mandatory |

The logical model does not add a separate embedding entity or vector index. Feedback and organization-level configuration are conditional/optional capabilities.

## 6. Schema and Relationship Reference

Refer to [DATABASE_ER_DIAGRAM.md](./DATABASE_ER_DIAGRAM.md) for the logical entity model, attributes, and relationship details. Key requirements include:

- Opaque UUID primary keys are planned for modeled entities. UUID values are not authorization tokens.
- InnoDB is required for tables whose transaction and foreign-key enforcement is required; the actual table engine and DDL have not been verified.
- `Organization` is the tenant root. Tenant-owned records carry an `organization_id`.
- Relationships between tenant-owned records use organization-aware foreign keys, including composite `(organization_id, entity_id)` references where specified, to prevent cross-organization references.
- `AgentDefinition` and `ToolDefinition` may be global configuration or organization-scoped; the global-versus-tenant uniqueness implementation remains open.
- Human approval, simulated execution, and audit records must retain their required linkage and attribution.

This is a logical design, not a physical schema. No table definitions, indexes, constraints, or migrations are verified as present in MariaDB.

## 7. Naming Conventions

The ER document uses PascalCase logical entity names (for example, `KnowledgeDocument`) and lowercase `snake_case` attribute names (for example, `organization_id` and `created_at`). Foreign-key attribute names generally use the referenced identifier with an `_id` suffix.

These are conventions visible in the logical model; they do not establish final physical table names or a committed DDL naming standard. Singular versus plural table names, identifier quoting/case behavior, physical UUID representation, and exact column types remain to be confirmed in the schema/migration design.

## 8. Migration Strategy

No migration tool, physical schema, or migration has been selected or verified. ADR-0008 identifies Flyway and Liquibase as candidates only, subject to confirming compatible versions and drivers for the selected Spring Boot/Hibernate stack and MariaDB 10.11.16. Hibernate schema creation and updates are disabled (`spring.jpa.hibernate.ddl-auto: none`); no business tables or migrations have been created or run.

The runtime user `aea_app@localhost` is deliberately limited to DML (`SELECT`, `INSERT`, `UPDATE`, and `DELETE`) on `autonomous_enterprise_ai.*`. It has no `CREATE`, `ALTER`, `DROP`, or `GRANT OPTION` privileges. Future schema creation or changes must be performed separately through an approved, versioned migration/DDL process using a distinct, appropriately scoped migration identity; do not expand the runtime user's privileges to make migrations run. The planned schema must use MariaDB-compatible DDL and preserve the logical relationships, tenant constraints, and audit requirements. Migration application and recovery should be tested against MariaDB 10.11.16 before use. phpMyAdmin is the management interface; manual changes are not a substitute for an approved, versioned migration process.

## 9. Backup and Recovery Considerations

Backup frequency, retention, encryption, recovery objectives, ownership, and restore procedures are not selected or verified. No backup or restore test is claimed.

Before any real organizational data or shared deployment is considered, define and approve backup, restore, retention, and deletion controls under the applicable data-protection and operational review. Keep development data synthetic/demo unless the required review approves otherwise. XAMPP is not evidence of a production backup or recovery design.

## 10. Security Guidelines

- Never put passwords, API keys, tokens, signing keys, or other secrets in this document, database content, source control, prompts, or routine logs.
- Use trusted server-side identity and organization context. Do not treat an identifier or database constraint alone as authorization.
- Enforce tenant scope for reads, searches, writes, updates, and deletes. Apply organization and user-permission filters before eligible knowledge content reaches FastAPI/LangGraph or a model.
- Keep deterministic authorization in the Spring Boot business API. AI output and search ranking are not authorization decisions.
- Require authorized human approval and execution-time policy revalidation for any enabled simulated action. MVP tool execution remains simulated-only; no live enterprise side effects are authorized.
- Preserve attributable, organization-scoped audit events for material decisions and outcomes. Restrict audit access and minimize sensitive content.
- Treat XAMPP/phpMyAdmin as local-development tooling; do not infer that either is secured or approved for production exposure.
- The runtime account and its DML-only grants are verified for local development. Its password is managed locally as `DB_APP_PASSWORD`; never commit, print, or share it.
- Future migration identities/privileges, network restrictions, encryption, key management, audit retention, and operational access controls remain to be designed and verified. Do not claim these controls are implemented.

## 11. Vector Retrieval Decision Status

**Verified:** MariaDB 10.11.16 is the relational source of truth. No vector retrieval technology is implemented or selected, and the current database has no tables.

**Future decision:** Knowledge Agent retrieval technology remains **unresolved**. MariaDB is not selected as a pgvector-compatible vector store, and no vector-search replacement is approved.

ADR-0008 compares the following options without selecting one:

1. A separate self-managed vector database.
2. An external managed vector-search service.
3. Temporary non-vector retrieval, such as a measured lexical/full-text approach for a small curated corpus.

Any option must be evaluated for retrieval quality, tenant and user-permission filtering before content reaches a model, permission-change freshness, source validation, data handling, failure behavior, latency, cost, and operational burden. Full-text search is not assumed to be semantically equivalent to vector search. See [ADR-0008](./decisions/ADR-0008-mariadb-primary-database.md) and [IMPLEMENTATION_READINESS_PLAN.md](./IMPLEMENTATION_READINESS_PLAN.md), finding AR-M-07.

## 12. Change History

| Date | Change |
|---|---|
| 2026-11-13 | Initial Development Foundation reference. At that time, database creation and query execution were user-reported; their output and schema state were not verified. |
| 2026-11-13 | Recorded the datasource variable names and the then-unverified port 3306 expectation. Schema management was disabled; table/schema state was not yet verified. |
| 2026-11-13 | Historical verification state only: the XAMPP executable, configured/listening port 3306, and TCP reachability were checked, but SQL verification and Spring Boot connectivity/startup/health were not yet verified. Also recorded the isolated H2 test and workspace clean-build filesystem results from that date. |
| 2026-11-17 | Updated the verified setup: `aea_app@localhost` connects to `autonomous_enterprise_ai`; Spring Boot/Hikari/Hibernate startup and Actuator health are UP; the database has no tables; runtime grants are DML-only. Added same-session PowerShell configuration and local-only `DB_APP_PASSWORD` handling; kept schema migration privileges separate and vector retrieval undecided. |
