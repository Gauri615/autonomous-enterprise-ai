# ADR-0008: MariaDB Primary Database

| Field | Value |
|---|---|
| ADR Number and Title | ADR-0008 — MariaDB Primary Database |
| Status | Accepted |
| Date | 2026-11-13 |

## Context

The approved database change selects MariaDB 10.11.16 as the primary relational database, XAMPP as the local database environment, phpMyAdmin as the management interface, and `autonomous_enterprise_ai` as the database name. This ADR supersedes the prior PostgreSQL/pgvector decision recorded in ADR-0003.

The logical model already defines tenant-owned entities, UUID primary keys, organization-aware foreign keys, proposal/approval/execution integrity, and attributable audit events. These requirements remain unchanged. The Knowledge Agent still needs retrieval over a small curated corpus, but MariaDB 10.11.16 is not pgvector-compatible and no vector-search replacement is approved by this ADR.

## Problem Statement

Select the relational system of record without silently selecting an unrelated retrieval product or weakening tenant isolation, transaction integrity, permission-filtered retrieval, or audit requirements.

## Decision

- Use MariaDB 10.11.16 as the primary relational system of record for application and knowledge metadata.
- Use the database name `autonomous_enterprise_ai`. The local environment is XAMPP and phpMyAdmin is the management interface; neither implies a production deployment model.
- Preserve the existing logical entities, primary keys, foreign keys, same-tenant relationship constraints, deterministic application authorization, proposal/approval/execution integrity, and audit requirements.
- Use InnoDB for tables whose transactions and foreign-key enforcement are required. The exact physical DDL and transaction strategy remain implementation work and must be tested with MariaDB 10.11.16.
- MariaDB JSON columns are supported, but in MariaDB 10.11 the `JSON` type is an alias for `LONGTEXT` with JSON validation, not PostgreSQL JSONB. Keep JSON documents bounded and application-validated; index only measured access patterns using supported MariaDB mechanisms.
- MariaDB 10.11 supports relational constraints and indexes, composite foreign keys, and InnoDB full-text indexes on supported text columns. Their exact use and behavior must be validated against the chosen collation, query patterns, and XAMPP-bundled configuration.
- Spring Data JPA/Hibernate is retained. Hibernate ORM 6.6 documents `MariaDBDialect` with a minimum database version of 10.4; verify the actual Spring Boot/Hibernate/JDBC-driver combination against MariaDB 10.11.16 before implementation.
- Use a MariaDB-compatible migration tool (for example, Flyway or Liquibase) only after selecting and verifying a compatible version/driver. Migration tooling, schema, and rollout details are not selected here.
- Keep tenant scope and permission filtering authoritative in Spring Boot. A database constraint or search ranking does not replace deterministic backend authorization.
- Keep Knowledge Agent vector/retrieval technology undecided pending the evaluation below. Do not treat MariaDB 10.11.16 as providing pgvector compatibility or native vector-index equivalence.

## Knowledge Agent Retrieval Alternatives (Unresolved)

No alternative is finalized. Evaluate each against retrieval quality on the curated corpus, organization and document permission filtering before any candidate content is returned, consistent permission changes/revocation, source-reference validation, data minimization, failure behavior, latency, operational burden, cost, backups/deletion, and tenant-isolation testing.

| Alternative | Potential fit | Material trade-offs / questions |
|---|---|---|
| Separate self-managed vector database | Supports vector-native similarity and can scale independently from transactional records. | Adds a service, identity/network boundary, operations and backup/deletion responsibility, duplicated metadata, consistency and permission-filter enforcement risks. Must prove pre-retrieval or candidate-stage tenant/permission filtering and handle stale metadata safely. |
| External managed vector-search service | May reduce self-hosting and scaling work while providing vector-search capabilities. | Requires provider/security/data-processing review, an additional external data boundary, cost/availability assessment, data residency/retention/deletion terms, secret management, network controls, and tenant-filter verification. No service/provider is selected. |
| Temporary non-vector retrieval | Uses MariaDB relational filtering and potentially InnoDB full-text search or bounded lexical matching for a small curated corpus; avoids a new vector service during early validation. | Lexical search is not semantically equivalent to vector similarity; tokenization, stopwords, short words, language support, relevance/recall, ranking, and corpus growth need evaluation. It may be a temporary retrieval strategy, not an accepted long-term semantic-search replacement. |

Until evaluation is complete, document Knowledge Agent retrieval as unresolved. Any prototype retrieval must remain permission-filtered before model input, return only eligible content, validate source references, and fail safely if its index/search component is unavailable. A lexical approach may be used only after an explicit scope/implementation decision; this ADR does not approve it.

## Alternatives Considered

- Retain PostgreSQL/pgvector as the primary database: superseded by the approved MariaDB decision; recorded historically in ADR-0003.
- Assume MariaDB 10.11.16 is pgvector-compatible or provides equivalent vector storage: rejected; it does not.
- Select a vector database, managed vector service, or lexical-only retrieval without comparative tests and data/security review: deferred; insufficient evidence exists to select one.
- Introduce Redis as a required source of truth or authorization dependency: rejected; Redis remains optional and cannot be required for correctness.

## Rationale

The relational database choice is explicitly approved. MariaDB 10.11.16 provides the required relational building blocks, including InnoDB transactions and foreign keys, JSON validation/storage, constraints and indexing, and full-text search. Its use with Spring Data JPA/Hibernate and migration tooling is supported in principle, subject to version-pinned integration verification. Vector search is a distinct capability; separating that decision avoids implying compatibility or committing to an unevaluated additional service.

## Consequences

- Database documentation and schema candidates must use MariaDB-compatible types and syntax, while preserving all logical identifiers and relationships.
- Map UUID and timestamp fields to MariaDB-compatible physical representations only after Hibernate/JDBC integration tests; logical UUID primary keys and UTC timestamp semantics remain required.
- Replace PostgreSQL-only JSONB/GIN/partial-index assumptions with supported alternatives only after precise schema and query design. Nullable global-versus-tenant uniqueness requires a MariaDB-compatible design and tests.
- Plan migrations against the MariaDB target, not PostgreSQL DDL. Test migration application and rollback/recovery using MariaDB 10.11.16; do not rely on phpMyAdmin manual changes as a migration process.
- Vector provider/model/dimensions/metric/index and final retrieval mode remain unresolved. Do not store incompatible embeddings or send unfiltered retrieval results to the AI service.
- Database roles, encryption/key management, backups/restore, retention/deletion, production deployment, and operational access remain open under AR-H-03.

## Security Implications

- Every protected read, write, retrieval, approval, and audit operation remains under deterministic Spring Boot authorization using trusted organization context.
- Preserve organization-aware foreign keys where supported by MariaDB and verify referenced columns, matching types/signs/collations, indexes, and same-engine InnoDB requirements.
- Transactional persistence and database constraints support, but do not alone guarantee, exact approval binding, duplicate-safe execution, or audit completeness; exercise concurrency and persistence-failure tests.
- For any external retrieval service, include the service in the trust model and enforce organization/permission constraints before results can reach FastAPI/LangGraph or the LLM.
- Use synthetic/demo data until AR-H-03 is resolved. No production readiness, compliance, or security certification is implied.

## Related Documents

- [PRD.md](../../product/PRD.md)
- [SRS.md](../../product/SRS.md)
- [SYSTEM_ARCHITECTURE.md](../SYSTEM_ARCHITECTURE.md)
- [DATABASE_ER_DIAGRAM.md](../DATABASE_ER_DIAGRAM.md)
- [THREAT_MODEL.md](../../security/THREAT_MODEL.md)
- [ARCHITECTURE_REVIEW.md](../ARCHITECTURE_REVIEW.md)
- [IMPLEMENTATION_READINESS_PLAN.md](../IMPLEMENTATION_READINESS_PLAN.md)
- [ADR-0003](./ADR-0003-database-and-vector-storage.md) (superseded)

## Technical References

- [MariaDB JSON data type](https://mariadb.com/docs/server/reference/data-types/string-data-types/json)
- [MariaDB foreign keys](https://mariadb.com/docs/server/ha-and-performance/optimization-and-tuning/optimization-and-indexes/foreign-keys)
- [MariaDB full-text index overview](https://mariadb.com/docs/server/ha-and-performance/optimization-and-tuning/optimization-and-indexes/full-text-indexes/full-text-index-overview)
- [MariaDB transactions](https://mariadb.com/docs/server/reference/sql-statements/transactions)
- [Hibernate ORM 6.6 supported dialects](https://docs.hibernate.org/orm/6.6/dialect/)
