# Database Setup

## 1. Database Overview

MariaDB 10.11.16 is the selected primary relational database target for the Autonomous Enterprise AI System. The database is the planned source of truth for organization-scoped application records, knowledge metadata, workflow state, approvals, simulated tool outcomes, and audit events.

This document is the central source of truth for reported local database setup details and decisions that remain open. Local runtime and health checks were reported as verified on 2026-11-17, but were not re-verified in this implementation task. The current XAMPP server version, `innodb_page_size`, and SQL modes remain unverified because a database connection could not be established. No production database is configured. The logical model is documented in [DATABASE_ER_DIAGRAM.md](./DATABASE_ER_DIAGRAM.md).

## 2. Confirmed Environment

| Setting | Selected or previously reported value | Status / note |
|---|---|---|
| Database engine/version | MariaDB 10.11.16 | Selected target; current XAMPP server version was not verified in this task |
| Local installation | XAMPP | Previously reported local development installation; not re-verified |
| Management interface | phpMyAdmin | Previously reported local management interface; not re-verified |
| Database name | `autonomous_enterprise_ai` | Previously reported; not re-verified |
| Tables/schema | No migration was applied by this task | Current database contents were not checked |
| Application account | Previously reported local runtime account | Account and grants were not re-verified |
| JDBC driver | MariaDB Connector/J 3.5.10 | Configured application dependency; no database connection was made |
| Backend runtime | Java 25, Spring Boot 4.1.2 | Configured project versions |
| Backend port | `8080` | Previously reported health endpoint port; not re-verified |
| Hibernate schema management | `ddl-auto: none` | Configured; the application does not create or update tables |

The 2026-11-17 connection, server-version, grant, empty-schema, and health results below are historical reports, not current runtime evidence. This task did not connect to XAMPP or execute SQL/DDL. In particular, do not treat MariaDB runtime compatibility as proven.

XAMPP and phpMyAdmin describe the local development setup only. They do not select a production hosting or deployment architecture.

## 3. Database Connection Details

| Connection setting | Value | Verification status |
|---|---|---|
| Host | `localhost` | Previously reported; not re-verified |
| Port | `3306` | Previously reported XAMPP MariaDB port; not re-verified |
| Database | `autonomous_enterprise_ai` | Previously reported by `DATABASE()`; not re-verified |
| Engine/version | MariaDB 10.11.16 | Selected target; current server version not verified |
| Username | Previously reported local application account | Not re-verified in this task |
| Password | `DB_PASSWORD` environment variable | Supplied locally; its value must never be recorded here |
| JDBC URL | `DB_URL` environment variable | `jdbc:mariadb://localhost:3306/autonomous_enterprise_ai` |
| Runtime privileges | Previously reported as DML-only on the application database | Not re-verified in this task |

The Spring Boot datasource reads `DB_URL`, `DB_USERNAME`, and `DB_PASSWORD`. `DB_URL` defaults to the URL above; the local launch instructions set it explicitly. The application is configured to use HikariCP and `MariaDBDialect`; no current database initialization was observed in this task.

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

The following are historical setup and runtime results reported for 2026-11-17. They were not re-verified during this task, which did not connect to the database:

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

The read-only queries above can be used to recheck server/database selection and table state. An empty table list was reported on 2026-11-17; the current result is unknown. The V1 migration file now exists but was not applied in this task, so do not infer current schema contents from the historical check.

## 5. Planned Database Entities

The following inventory is the canonical logical model in [DATABASE_MODEL_RECONCILIATION.md](./DATABASE_MODEL_RECONCILIATION.md). Most of these entities are not implemented as persistence; only the four first-slice tables described in Section 8 have entity/repository code and a migration file.

| Logical group | Canonical entities and scope |
|---|---|
| Identity and RBAC | `Organization` (tenant root); global `IdentityPrincipal` and `ExternalIdentity`; tenant `OrganizationMembership`, `Role`, `MembershipRole`, and `RolePermission`; global `PermissionCatalog`. |
| Support | `SupportRequest` (internal IT request, not an external service-desk ticket) and `RequestMessage`. |
| Knowledge | `KnowledgeDocument`, `KnowledgeChunk`, `KnowledgeDocumentRoleGrant`, `KnowledgeDocumentUserGrant`, and `MessageSource`. |
| Agents | Global `AgentDefinition`; tenant `OrganizationAgentConfig`; tenant `AgentRun` and `AgentEvent`. |
| Tools and approval | Global `ToolDefinition`; tenant `OrganizationToolPolicy`, `ApprovalRequest`, `ToolExecution`, and `ToolExecutionAttempt`. |
| Audit | `AuditEvent`, which supports tenant and platform scope with the canonical actor-kind rules. |
| Optional evaluation | `ResponseFeedback`, `EvaluationRun`, and `EvaluationResult`; all remain optional and outside the required MVP persistence foundation. |

Global catalogs are separate from tenant configuration and policy; do not model global and tenant definitions with nullable `organization_id` on one catalog row. The logical model does not add a separate embedding entity or vector index. Database schema presence remains unverified.

## 6. Schema and Relationship Reference

Refer to [DATABASE_ER_DIAGRAM.md](./DATABASE_ER_DIAGRAM.md) for the logical entity model, attributes, and relationship details. Key requirements include:

- Opaque UUID primary keys are planned for modeled entities. UUID values are not authorization tokens.
- InnoDB is required for tables whose transaction and foreign-key enforcement is required; the actual table engine and DDL have not been verified.
- `Organization` is the tenant root. Tenant-owned records carry an `organization_id`.
- Relationships between tenant-owned records use organization-aware foreign keys, including composite `(organization_id, entity_id)` references where specified, to prevent cross-organization references.
- Global `AgentDefinition` and `ToolDefinition` catalogs are separate from tenant `OrganizationAgentConfig` and `OrganizationToolPolicy`; their recommended lifecycle and implementation status are described in the canonical model.
- Human approval, simulated execution, and audit records must retain their required linkage and attribution.

The first persistence slice has entity/repository code and a versioned migration file defining only `Organization`, `IdentityPrincipal`, `ExternalIdentity`, and `OrganizationMembership`; V1 was not applied in this task. No table definitions, indexes, or constraints are verified as present in MariaDB.

## 7. Naming Conventions

The ER document uses PascalCase logical entity names (for example, `KnowledgeDocument`) and lowercase `snake_case` attribute names (for example, `organization_id` and `created_at`). Foreign-key attribute names generally use the referenced identifier with an `_id` suffix.

The first-slice physical names and representations are selected in the reconciliation and migration strategy documents and encoded in V1; other entities remain subject to the canonical model and later physical design. No database-level behavior has been verified by applying the migration.

## 8. Migration Strategy

Flyway is selected as an explicitly invoked Maven plugin, with its Flyway/MariaDB modules isolated under the plugin and absent from the application runtime dependencies. The configured migration location is `backend/enterprise-api/src/main/resources/db/migration`; the committed V1 file creates only `organizations`, `identity_principals`, `external_identities`, and `organization_memberships`. V1 has not been applied, and no tables, constraints, or MariaDB runtime behavior are claimed as verified. Hibernate schema creation and updates remain disabled (`spring.jpa.hibernate.ddl-auto: none`). See [DATABASE_MIGRATION_STRATEGY.md](./DATABASE_MIGRATION_STRATEGY.md) for the plugin configuration, implemented test scope, and remaining verification gates.

The runtime account was previously reported as DML-only (`SELECT`, `INSERT`, `UPDATE`, and `DELETE`) on the application database, without DDL or grant-option privileges; those grants were not re-verified in this task. Future schema creation or changes must be performed separately through an approved, versioned migration/DDL process using a distinct, appropriately scoped migration identity; do not expand the runtime account's privileges to make migrations run. The planned schema must use MariaDB-compatible DDL and preserve the logical relationships, tenant constraints, and audit requirements. Migration application and recovery should be tested against MariaDB 10.11.16 before use. phpMyAdmin is the previously reported management interface; manual changes are not a substitute for an approved, versioned migration process.

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
- The runtime account and its DML-only grants were previously reported for local development but were not re-verified in this task. Any local password remains secret and must never be committed, printed, or shared.
- Future migration identities/privileges, network restrictions, encryption, key management, audit retention, and operational access controls remain to be designed and verified. Do not claim these controls are implemented.

## 11. Vector Retrieval Decision Status

**Selected:** MariaDB 10.11.16 is the relational database target. The current XAMPP server version and current database contents were not verified in this task. No vector retrieval technology is implemented or selected.

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
| 2026-11-21 | Added the first unapplied V1 identity-foundation migration and recorded its Flyway plugin-only configuration. This task made no database connection; the current XAMPP server version, `innodb_page_size`, SQL modes, and database contents remain unverified. |
