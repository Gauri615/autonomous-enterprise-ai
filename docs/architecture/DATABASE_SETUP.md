# Database Setup

## 1. Database Overview

MariaDB 10.11.16 is the selected primary relational database for the Autonomous Enterprise AI System. The database is the planned source of truth for organization-scoped application records, knowledge metadata, workflow state, approvals, simulated tool outcomes, and audit events.

This document records the approved local environment and the current verification state. It is not evidence that tables, migrations, application connectivity, or a production database have been configured. The logical model is documented in [DATABASE_ER_DIAGRAM.md](./DATABASE_ER_DIAGRAM.md).

## 2. Confirmed Environment

| Setting | Confirmed value | Status / note |
|---|---|---|
| Database engine | MariaDB | Selected primary relational database |
| Database version | 10.11.16 | Approved target version |
| Local environment | XAMPP | Selected local database environment; installed XAMPP version is not recorded here |
| Management interface | phpMyAdmin | Selected management interface |
| Database name | `autonomous_enterprise_ai` | Approved database name |
| Database creation | Completed by the user | User-reported; not independently verified in this document |

XAMPP and phpMyAdmin describe the local development setup only. They do not select a production hosting or deployment architecture.

## 3. Database Connection Details

| Connection setting | Value | Verification status |
|---|---|---|
| Host | `localhost` | Provided for the local environment; application connectivity has not been independently tested |
| Port | `3306` | Default/expected MariaDB port; actual configured port is not yet verified |
| Database | `autonomous_enterprise_ai` | Approved name; current selected database has not been confirmed from query output |
| Engine/version | MariaDB 10.11.16 | Approved target; observed server output is not available |
| Username | `DB_USERNAME` environment variable | Must be set from the local setup; the value is not documented |
| Password | `DB_PASSWORD` environment variable | Must be set locally; never record a password, token, or other secret here |
| JDBC URL | `DB_URL` environment variable | Application default is `jdbc:mariadb://localhost:3306/autonomous_enterprise_ai`; port 3306 is expected but unverified |

The application uses the MariaDB JDBC driver. The configured connection-property names are `DB_URL`, `DB_USERNAME`, and `DB_PASSWORD`; the username/password values and live connectivity have not been verified. The JDBC URL default records the expected local host, port, and database name, not a verified connection.

### Environment-variable setup

The backend's tracked `backend/enterprise-api/.env.example` contains placeholders only. Spring Boot does not load `.env` files automatically. In Windows PowerShell, supply the values in the terminal session before starting the application:

```powershell
$env:DB_URL = "jdbc:mariadb://localhost:3306/autonomous_enterprise_ai"
$env:DB_USERNAME = "your-local-mariadb-username"
$env:DB_PASSWORD = "your-local-mariadb-password"
```

Confirm the actual MariaDB port before using the example URL. Keep real credentials out of tracked files and do not commit a real `.env` file. Hibernate schema management is configured as `ddl-auto: none`; the application will not automatically create or update database tables.

## 4. Database Creation and Verification

The user reports that database creation is complete. No database creation command or verification query was run by this task.

The following read-only verification commands are documented for manual use:

```sql
SELECT DATABASE(), VERSION();
```

The user reports executing the verification query, but its output was not provided. This task did not execute a database verification query or inspect database tables. Therefore, the selected database and observed server version cannot be confirmed from the output available here.

```sql
SHOW TABLES;
```

No output for `SHOW TABLES;` is recorded here. Table count, table existence, schema correctness, and migration status are not verified. These commands are documented only; no SQL was executed while preparing this file. No tables or schema were verified or created by this task.

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

No migration tool, physical schema, or migration has been selected or verified. ADR-0008 identifies Flyway and Liquibase as candidates only, subject to confirming compatible versions and drivers for the selected Spring Boot/Hibernate stack and MariaDB 10.11.16. The initial application explicitly disables Hibernate schema creation and updates (`spring.jpa.hibernate.ddl-auto: none`); no business tables or migrations were created or run by this task.

The planned schema must use MariaDB-compatible DDL and preserve the logical relationships, tenant constraints, and audit requirements. Migration application and recovery should be tested against MariaDB 10.11.16 before use. phpMyAdmin is the management interface; manual phpMyAdmin changes are not a substitute for an approved, versioned migration process. No migration was created or run as part of this document task.

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
- Database users/roles, network restrictions, encryption, key management, audit retention, and operational access controls remain to be designed and verified. Do not claim these controls are implemented.

## 11. Vector Retrieval Decision Status

Knowledge Agent retrieval technology remains **unresolved**. MariaDB 10.11.16 is the relational source of truth; it is not selected as a pgvector-compatible vector store, and no vector-search replacement is approved.

ADR-0008 compares the following options without selecting one:

1. A separate self-managed vector database.
2. An external managed vector-search service.
3. Temporary non-vector retrieval, such as a measured lexical/full-text approach for a small curated corpus.

Any option must be evaluated for retrieval quality, tenant and user-permission filtering before content reaches a model, permission-change freshness, source validation, data handling, failure behavior, latency, cost, and operational burden. Full-text search is not assumed to be semantically equivalent to vector search. See [ADR-0008](./decisions/ADR-0008-mariadb-primary-database.md) and [IMPLEMENTATION_READINESS_PLAN.md](./IMPLEMENTATION_READINESS_PLAN.md), finding AR-M-07.

## 12. Change History

| Date | Change |
|---|---|
| 2026-11-13 | Created this setup reference for the Development Foundation phase. Recorded the approved MariaDB environment and user-reported database creation/query execution without inventing query output or schema verification. |
| 2026-11-13 | Documented the Spring Boot datasource environment variables (`DB_URL`, `DB_USERNAME`, `DB_PASSWORD`) and expected-but-unverified port 3306 default. Confirmed schema management is disabled and no tables/schema were verified or created by the backend foundation task. |
