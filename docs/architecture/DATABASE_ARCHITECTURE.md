# Enterprise Database Architecture

## 1. Status, Scope, and Authority

| Property | Value |
|---|---|
| Logical model authority | [DATABASE_MODEL_RECONCILIATION.md](./DATABASE_MODEL_RECONCILIATION.md) |
| Relational target | MariaDB 10.11.16 |
| Local development target | XAMPP, database `autonomous_enterprise_ai`, phpMyAdmin |
| Backend | Java 25, Spring Boot 4.1.2, Spring Data JPA/Hibernate |
| Persistence implementation | Organization, IdentityPrincipal, ExternalIdentity, OrganizationMembership only |
| Migration status | V1 file is present and committed; application to MariaDB is not verified and was not performed in this task |

This architecture describes the canonical logical model and its implementation boundary. The canonical reconciliation controls entity definitions and approved first-slice choices. This document does not claim that every logical entity, constraint, grant, or workflow is implemented or exists in the database. The current XAMPP server version, page size, SQL modes, database contents, and connection health have not been verified in this task.

Spring Boot remains authoritative for trusted identity, authorization, tenant scope, workflow state, approvals, and simulated actions. Keep `spring.jpa.hibernate.ddl-auto: none`. Flyway is configured only as an explicitly invoked Maven plugin; it is absent from application runtime dependencies and does not migrate at application startup.

## 2. Logical Model and Implemented Foundation

The logical inventory comprises:

- **Organization, identity, and RBAC:** `Organization`, `IdentityPrincipal`, `ExternalIdentity`, `OrganizationMembership`, `Role`, `PermissionCatalog`, `MembershipRole`, and `RolePermission`.
- **Support:** `SupportRequest` and `RequestMessage`.
- **Knowledge:** `KnowledgeDocument`, `KnowledgeChunk`, `KnowledgeDocumentRoleGrant`, `KnowledgeDocumentUserGrant`, and `MessageSource`.
- **Agent workflows:** `AgentDefinition`, `OrganizationAgentConfig`, `AgentRun`, and `AgentEvent`.
- **Tools and approval:** `ToolDefinition`, `OrganizationToolPolicy`, `ApprovalRequest`, `ToolExecution`, and `ToolExecutionAttempt`.
- **Audit:** `AuditEvent`.
- **Optional:** `ResponseFeedback`, `EvaluationRun`, and `EvaluationResult`; these are outside required MVP persistence.

Only the first four organization/identity entities have persistence code and repositories. Their H2-backed tests and opt-in MariaDB integration tests are present. The single V1 migration creates the four tables `organizations`, `identity_principals`, `external_identities`, and `organization_memberships`. The migration file is not proof of an applied migration or live schema. No second identity-migration sequence is intended.

The previous `User`/`AppUser`, `UserRole`, `Ticket`/`TicketComment`, and generic `Feedback` names are superseded logical-model terminology, not authoritative entities. Their historical appearance in source documents does not change the canonical model.

## 3. Identity, Membership, and Tenant Actors

`IdentityPrincipal` is a global platform identity, independent of organizations and identity providers. A principal may link multiple `ExternalIdentity` rows and have membership episodes in multiple organizations. `ExternalIdentity` binds a verified provider issuer/subject pair to one immutable principal; it is not an organization-local login row.

`OrganizationMembership` connects one principal to one organization as a retained lifecycle episode. Tenant actors in business data are memberships, not global principal IDs. This includes requesters, human message authors, curators, grant recipients/grantors/revokers, configuration actors, approval requesters/reviewers, execution actors, and optional feedback/evaluation actors. Every such reference pairs `organization_id` with `membership_id`.

The first-slice lifecycle rules are selected:

| Entity | States and transitions |
|---|---|
| Organization | `ACTIVE`, `SUSPENDED`, `CLOSED`; ACTIVE -> SUSPENDED/CLOSED; SUSPENDED -> ACTIVE/CLOSED; CLOSED terminal. |
| IdentityPrincipal | `ACTIVE`, `DISABLED`; transitions between the two states are allowed. |
| ExternalIdentity | LINKED while `unlinked_at` is null; UNLINKED otherwise. Re-link only after fresh provider verification and to the same principal. |
| OrganizationMembership | `PENDING`, `ACTIVE`, `SUSPENDED`, `REVOKED`; PENDING -> ACTIVE/REVOKED; ACTIVE -> SUSPENDED/REVOKED; SUSPENDED -> ACTIVE/REVOKED; REVOKED terminal. Rejoin creates a new UUID. |

Creation in the initial state is permitted. Application logic enforces transitions; status checks only constrain tokens. No hard delete or cascading deletion of these four records is allowed. Access requires an active principal, active organization, linked verified external identity for authentication, and active membership.

## 4. Exact Identifier Mapping

- Every UUID PK and FK uses `BINARY(16)` in canonical UUID octet order: most-significant 64-bit half first, each half big-endian, no byte swapping. IDs use Java `UUID.randomUUID()` version 4. UUID attributes have explicit Hibernate `@JdbcTypeCode(SqlTypes.BINARY)` mappings.
- Organization `organization_key` stores canonical lowercase ASCII slug bytes in `VARBINARY(100)`, matching `[a-z0-9]+(?:-[a-z0-9]+)*`. Invalid values are rejected, never silently normalized or truncated.
- External identity `issuer` is strict UTF-8 bytes in `VARBINARY(480)`; `subject` is exact ASCII bytes in `VARBINARY(255)`. Reject empty, non-ASCII, malformed, or oversized values as applicable. Never trim, lowercase, normalize, or truncate. The 480-byte issuer limit is application policy, not an OIDC limit.
- Enforce full-value unique `(issuer, subject)` independently of link state. Exact case and trailing bytes matter; email is never an identity key.
- The identity index has 735 declared key bytes plus the model's 16-byte primary-key suffix budget (751 total). This is within the documented 4-KB InnoDB page limit but has not been checked on the current XAMPP server.
- Use UTC `DATETIME(6)` for instants where the application maintains that contract. MariaDB `JSON` is text-backed, not PostgreSQL JSONB; bound and validate JSON at the application boundary.

The UUID and exact-byte decisions are selected and recorded in the canonical model and migration strategy. H2/proxy JDBC tests do not establish MariaDB runtime compatibility.

## 5. Membership and Tenant-Safe Relationships

Every tenant-owned row has a required `organization_id`. Tenant parents expose alternate `(organization_id, entity_id)` keys where required, and child relationships use both values in composite FKs. Optional composite FK tuples must be entirely null or entirely present; MariaDB/InnoDB does not check a composite FK when any child component is null.

`OrganizationMembership` has alternate `(organization_id, membership_id)` for tenant actor references and the selected generated nullable live-principal key:

```sql
generated_live_principal_id BINARY(16)
    GENERATED ALWAYS AS (
        CASE WHEN status = 'REVOKED' THEN NULL ELSE identity_principal_id END
    ) STORED
```

The unique key `(organization_id, generated_live_principal_id)` allows multiple historical REVOKED episodes while limiting a principal to one non-revoked episode per organization. This expression and uniqueness key are in V1; MariaDB generated-column and concurrent-insert behavior remains unverified.

Global `PermissionCatalog`, `AgentDefinition`, and `ToolDefinition` remain separate from organization-scoped grants, configuration, and policy. Do not mix global and tenant definitions in one nullable-`organization_id` catalog entity. The recommended versioned-catalog/configuration behavior and exact constraints for entities beyond V1 remain logical design, not implemented schema.

## 6. Canonical Business Relationships

- **RBAC:** `Role` belongs to an organization. `MembershipRole` links an organization membership to a same-organization role as a grant episode. `RolePermission` links a same-organization role to a global permission key. Grant/revoke history uses stable IDs, actors, and timestamps; re-grant creates a new episode.
- **Support:** `SupportRequest` is the tenant-owned internal IT request and authoritative outcome. It references a requester membership in the same organization. `RequestMessage` belongs to the same-organization request and has an optional same-organization membership author for system/AI messages.
- **Knowledge:** Tenant-owned `KnowledgeDocument` references optional same-organization curator membership. `KnowledgeChunk` references its document within the same tenant. Role and membership access use separate historical grant entities. `MessageSource` relates a message to eligible chunks with tenant-aware keys.
- **Agents:** A platform `AgentDefinition` is distinct from each organization's `OrganizationAgentConfig`. A tenant `AgentRun` references the exact `(organization_id, agent_definition_id, config_version)` configuration, request, and requesting membership. `AgentEvent` is an ordered child of the run.
- **Tools and approvals:** A platform `ToolDefinition` is distinct from `OrganizationToolPolicy`. `ApprovalRequest` references an exact tenant policy version, same-tenant request and requester/reviewer memberships, and optional exact run provenance. `ToolExecution` binds to the same approval, request, tool and policy version; unique `(organization_id, approval_id)` permits at most one logical execution. `ToolExecutionAttempt` records each attempt separately.
- **Audit:** `AuditEvent` is tenant- or platform-scoped, with actor-kind-specific identity references and optional typed resource/grant references. The full actor matrix is in the canonical reconciliation and ER document.
- **Optional evaluation:** Feedback attaches to a response message. Evaluation runs/results are tenant-scoped and results must match exact agent definition/configuration provenance. These entities are not required MVP persistence.

## 7. Data Authority and Isolation

- Derive the organization only from trusted server-side identity context; do not accept organization scope from request bodies, prompts, model output, or retrieved documents.
- Include organization scope in every tenant query, write, retrieval candidate selection, audit review, and workflow operation.
- Same-organization composite FKs prevent cross-tenant references but do not replace authorization. UUID possession or an active FK does not grant access.
- A principal's membership/role in one organization grants nothing in another. A disabled principal or non-active membership cannot authorize tenant access.
- Apply organization, document lifecycle, membership, role, grant, and permission checks before returning knowledge content to the AI service/model. Recheck permissions before displaying stored citations.
- Keep tenant configuration/policy distinct from global catalogs. A catalog definition, grant, approval row, or model output does not independently authorize an action.
- If caching is later introduced, include tenant and permission context and design invalidation for membership, grant, source, and policy changes. Redis is not selected or required.

## 8. Knowledge Retrieval

The retrieval choice remains unresolved. ADR-0008 considers a separate vector database, an external managed vector-search service, and a temporary non-vector approach. This architecture selects no vector store, embedding schema, or search index.

Preserve relational document/chunk identifiers, organization, lifecycle, access rules, and citations irrespective of retrieval implementation. Filter by current organization and user permissions before returning any candidate text to the AI service/model. Validate each citation against eligible relational rows. Similarity or lexical rank is ranking only, not authorization or factual proof.

## 9. Simulated Action and Approval Invariants

No live enterprise action is authorized. Every enabled MVP action must remain a bounded simulation requiring human approval. Before approval and again immediately before each simulated effect, the backend must revalidate current actor and organization, membership/permission, exact request/run/policy/tool, proposal binding, approval state/expiry, and tool schema/status.

The logical model recommends one-time, expiring, proposal-bound approvals and immutable versioned tenant tool policy. Expiry duration, self-approval policy, fingerprint canonicalization, exact tool catalog, invalidation, idempotency, and ambiguous-outcome recovery remain unresolved. Never report success for an unconfirmed outcome or required audit failure; never blindly retry an ambiguous attempt.

## 10. Audit and Actor Scope

`AuditEvent` distinguishes tenant and platform scope and uses exactly one actor reference:

| Scope | Actor kind | Required | Must be null |
|---|---|---|---|
| TENANT | MEMBERSHIP | `organization_id`, same-organization `actor_membership_id` | Principal and service key |
| PLATFORM | IDENTITY_PRINCIPAL | Global `actor_identity_principal_id` | Organization, membership, service key |
| TENANT or PLATFORM | SERVICE | Controlled non-secret `actor_service_key`; organization only for tenant activity | Membership and principal |

Implement the matrix in deterministic application validation; do not imply that V1 implements audit. Audit, agent events, and tool attempts are append-oriented logical records, not proven database-immutable. The local runtime account's broad DML grants are historical and unverified here; no privilege change or append-only control is claimed. Minimize metadata and do not routinely store full prompts, source content, credentials, or unnecessary personal information.

## 11. Transactions and Consistency

These are future workflow requirements, not implemented transaction boundaries:

1. Commit authoritative request creation, initial message, and required audit evidence consistently before reporting success.
2. Pin each run to the request and exact tenant agent-configuration revision; keep provider calls outside long-running database transactions.
3. Persist response and validated source links consistently; revalidate access on later reads.
4. Bind approval decision to the exact proposal and policy revision; use tested locking/versioning for races.
5. Commit execution/attempt-start state before the simulated call, then persist outcome and required audit. Report unknown when outcome is ambiguous; do not automatically retry.
6. Select idempotency scope, transaction isolation, failure recovery, and audit-failure behavior before implementing these entities.

## 12. Indexing and Integrity

V1 contains the keys and indexes for the four implemented entities. The rest are logical requirements or candidates, not a physical index specification:

- Preserve tenant alternate keys and composite FK support indexes.
- Tenant access indexes should generally lead with `organization_id`; validate additional status/time, reverse-lookup, and correlation indexes using representative queries.
- Keep full unique identity values without prefixes or hashes. The issuer/subject calculation fits documented page-size limits; verify on the actual test target.
- Do not assume partial-index behavior. An at-most-one-active historical grant strategy outside membership remains to be selected and tested.
- Verify nullable composite-FK checks and key/index compatibility against MariaDB 10.11.16.
- No checks, indexes, or constraints beyond V1 should be described as implemented until a migration and its runtime tests exist.

## 13. Retention and Deletion

No general retention periods are selected. First-slice records use lifecycle state and restrictive/no-action foreign keys; hard deletion and cascading deletion of Organization, IdentityPrincipal, ExternalIdentity, and OrganizationMembership are prohibited. Broader retention, archival, deletion, anonymization, and legal-hold policies remain unresolved for other records.

Before real organizational data is used, define retention and deletion for requests/messages, knowledge/chunks and any future retrieval index, agent metadata, grants, approvals, executions/attempts, audit, and optional feedback/evaluation. Provider-side retention and backup/restore are separate operational decisions.

## 14. Security and Operations

- Use least-privilege runtime access; do not grant runtime DDL privileges to run migrations.
- Flyway remains an unbound Maven plugin with plugin-only modules and dependencies. Its configured filesystem location is `src/main/resources/db/migration`; V1 is `V1__create_identity_foundation.sql`.
- Migration application must use a separately authorized process and identity. No credentials, account changes, or grants are established by this architecture.
- Preserve `ddl-auto: none`; normal application startup must not create or alter schema.
- Do not store authentication secrets or provider credentials in database content or tracked files.
- XAMPP/phpMyAdmin are local-development tools, not evidence of production security, backup, or recovery readiness.
- The current XAMPP server version, `innodb_page_size`, SQL modes, database contents, runtime grants, and connection health were not verified in this task.

## 15. Implementation Status and Next Work

The identity foundation has already been implemented; do not repeat it or create a second V1. The next logical entities, if approved for implementation, begin with RBAC and support/authorization foundations that depend on the existing membership model, followed by knowledge and citation relations, versioned agent configuration/runs, and governed simulated actions. Optional feedback/evaluation remain separate and optional.

Before adding each new persistence slice, define its physical mapping and migration, preserve composite tenant keys and actor scope, and test cross-tenant, lifecycle, concurrency, and failure behavior against an isolated MariaDB 10.11.16 target. Do not use the development XAMPP database for integration testing. This is sequencing guidance, not authorization to apply DDL.

## 16. Open Decisions

- Identity recovery, provider migration, membership invitation/recovery, attribution, and audit procedures.
- Permission-code vocabulary, role mappings, administration, and enforcement of unique live grant episodes.
- Knowledge visibility/grant semantics, curator capability, revocation freshness, and retrieval technology.
- Request/workflow state graphs, sync/async behavior, idempotency, and error/unknown outcomes.
- Catalog governance and exact agent/tool version lifecycle.
- Approval expiry, self-approval, proposal fingerprint/canonicalization, policy invalidation, retry, and recovery.
- Audit service-key governance, retention, access, and any separate writer/reader or append-only database privilege design.
- Physical schemas and indexes beyond V1; MariaDB integration evidence for V1; current server version/page size/SQL modes.
- Data retention, archival, deletion, backup/restore, migration operations, encryption, key management, and production controls.
- Whether and how to enable optional feedback or evaluation.

## 17. Requirements and References

This architecture preserves the existing product boundaries: tenant and permission filtering before AI input; internal support requests rather than external ticketing; deterministic backend authorization; approved simulation only; minimized attributable audit; and optional feedback/evaluation. It does not replace PRD, SRS, or security requirements.

- [Canonical database model reconciliation](./DATABASE_MODEL_RECONCILIATION.md)
- [Canonical logical ER model](./DATABASE_ER_DIAGRAM.md)
- [Physical database and migration strategy](./DATABASE_MIGRATION_STRATEGY.md)
- [Database setup and verification status](./DATABASE_SETUP.md)
- [ADR-0008: MariaDB primary database](./decisions/ADR-0008-mariadb-primary-database.md)
- [Implementation readiness plan](./IMPLEMENTATION_READINESS_PLAN.md)
- [PRD](../product/PRD.md) and [SRS](../product/SRS.md)
- [API contracts](../api/API_CONTRACTS.md) and [threat model](../security/THREAT_MODEL.md)
