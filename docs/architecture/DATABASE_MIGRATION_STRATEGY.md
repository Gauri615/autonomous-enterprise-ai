# Physical Database and Migration Strategy

## 1. Purpose and status

This document records the migration approach and selected first-slice physical mappings from the canonical logical model. The existing V1 migration and entity/repository code cover only `Organization`, `IdentityPrincipal`, `ExternalIdentity`, and `OrganizationMembership`; V1 has not been applied to MariaDB. No database identity is provisioned and no DDL application is authorized by this document.

The selected local development target remains MariaDB 10.11.16 under XAMPP, managed locally with phpMyAdmin; the current XAMPP server version was not verified in this task. Do not move development to Docker, PostgreSQL, MySQL Server, or a cloud database as part of this strategy. Keep Hibernate schema generation disabled with `spring.jpa.hibernate.ddl-auto: none`.

Flyway is selected for version-controlled migrations and is configured exclusively as an explicitly invoked Maven plugin. No Flyway dependency is added to the application runtime. This tooling selection does not approve physical DDL, create migrations, provision a migration account, or authorize applying migrations.

## 2. Reviewed documents and verified environment

The strategy was prepared against:

- `docs/architecture/DATABASE_MODEL_RECONCILIATION.md` — proposed canonical logical model and tenant-safe relationships.
- `docs/architecture/DATABASE_SETUP.md` — reported local environment, account and migration/backup status, including verification limits.
- `docs/architecture/DATABASE_ARCHITECTURE.md` and `docs/architecture/DATABASE_ER_DIAGRAM.md` — existing logical proposals and MariaDB conventions.
- `docs/architecture/IMPLEMENTATION_READINESS_PLAN.md` — readiness gates and database checklist.
- `docs/architecture/decisions/ADR-0008-mariadb-primary-database.md` — accepted MariaDB choice; physical mapping remains open. This document records the selected Flyway runner for this backend.
- `backend/enterprise-api/pom.xml` and `backend/enterprise-api/src/main/resources/application.yml` — current backend dependency and configuration state.

| Setting | Selected or previously reported value |
|---|---|
| Database platform | MariaDB 10.11.16 target; current XAMPP server version not verified in this task |
| Local environment | XAMPP, previously reported and not re-verified |
| Management interface | phpMyAdmin, previously reported and not re-verified |
| Host and port | `localhost:3306`, previously reported and not re-verified |
| Database name | `autonomous_enterprise_ai`, previously reported and not re-verified |
| Runtime account | Previously reported local account; not re-verified |
| Runtime privileges | Previously reported DML-only; not re-verified |
| Schema generation | `spring.jpa.hibernate.ddl-auto: none` |
| Backend versions recorded in setup | Java 25, Spring Boot 4.1.2, MariaDB Connector/J 3.5.10 |
| Current schema state | An empty schema was reported on 2026-11-17; current database contents were not checked |

The setup document contains historical account and grant verification from 2026-11-17; it was not re-verified in this task. A separate migration account remains a recommendation, not an existing or provisioned account. No credentials are included here.

## 3. Implementation status and design recommendations

Flyway's plugin-only Maven integration is configured as described in Section 3.1. The first-slice mappings selected in `DATABASE_MODEL_RECONCILIATION.md` are encoded in V1 but have not been applied or runtime-verified. The remaining canonical entities, including RBAC, support, knowledge, agent, tool, approval, audit, and optional evaluation entities, are not part of V1 and have no persistence implementation in this slice. Other physical mapping recommendations below are not approved DDL.

### 3.1 Migration tool and execution boundary

- The backend configures `org.flywaydb:flyway-maven-plugin:12.4.0`. Its plugin-only dependencies are `org.flywaydb:flyway-mysql:12.4.0` and `org.mariadb.jdbc:mariadb-java-client:3.5.10`.
- The Flyway plugin and its dependencies are declared under Maven `<build><plugins>`, not application `<dependencies>`. Do not add `flyway-core` or a Spring Boot Flyway starter to the application dependency set.
- The plugin has no lifecycle execution binding. Run a specific Flyway goal only as an explicit operator action. Ordinary Spring Boot startup and the application runtime classpath do not execute migrations.
- Keep `spring.jpa.hibernate.ddl-auto: none` in normal application configuration. Hibernate must not create, update, or drop the schema.
- Place future SQL migrations under `backend/enterprise-api/src/main/resources/db/migration`. Use ordered, descriptive versioned names, for example `V2__add_next_schema_change.sql`.
- V1 is located at `backend/enterprise-api/src/main/resources/db/migration/V1__create_identity_foundation.sql`, matching the configured migration location. It has not been executed.
- Treat an applied migration as immutable. Correct a deployed schema with a new forward migration rather than editing an applied script.
- Review every migration for tenant integrity, data preservation, privilege boundaries, and recoverability before application.
- phpMyAdmin may remain the local management interface, but manual schema edits are not a substitute for reviewed, versioned migrations.

The plugin is not bound to `validate`, `test`, `package`, `verify`, or another Maven lifecycle phase. The commands below demonstrate deliberate invocation; `migrate` must only be run after the migration, target, credentials, approvals, and recovery plan have been reviewed.

From `backend/enterprise-api`, set the non-secret connection settings and obtain the password from the approved local secret store without printing it:

```powershell
$env:FLYWAY_URL = "jdbc:mariadb://localhost:3306/autonomous_enterprise_ai"
$env:FLYWAY_USER = "<dedicated migration identity>"
# Securely inject FLYWAY_PASSWORD from the approved secret store; never echo it.

.\mvnw.cmd org.flywaydb:flyway-maven-plugin:12.4.0:info
.\mvnw.cmd org.flywaydb:flyway-maven-plugin:12.4.0:validate
.\mvnw.cmd org.flywaydb:flyway-maven-plugin:12.4.0:migrate
```

Flyway reads `FLYWAY_URL`, `FLYWAY_USER`, and `FLYWAY_PASSWORD` from the process environment. Never pass a password as a Maven `-D` argument or put it in the POM, a tracked configuration file, migration SQL, logs, or shell history. Do not use Maven debug logging (`-X`) for a credentialed invocation. Use an approved secret manager or protected local secret mechanism. A real migration identity is not verified or provisioned by this integration.

### 3.2 Separate database identities

- Keep the runtime account `aea_app` separate from the migration identity.
- The runtime identity must remain least-privileged and must not receive `CREATE`, `ALTER`, `DROP`, or grant-option privileges to make migrations work.
- Use a dedicated migration account only in the separately invoked migration process. Grant only the DDL and metadata permissions needed for the approved migration scope; do not reuse the runtime credentials.
- Keep migration credentials out of source control, migration files, application logs, and this document.
- Account creation, password handling, and grant changes require explicit authorization and must use the approved local secret/account-provisioning process.

The previously reported aea_app grants were DML-only and appeared consistent with the runtime separation principle, but they were not re-verified in this task. The migration account and its privileges remain pending; this document does not provision them or alter existing grants.

### 3.3 Physical mapping candidates

The following recommendations align with the canonical logical model and existing MariaDB architecture documents. They are not executable DDL.

| Logical concept | Recommended candidate | Required caution |
|---|---|---|
| UUID identifiers | `BINARY(16)` using canonical UUID octet order: most-significant 64 bits then least-significant 64 bits, each big-endian (equivalent to the standard UUID text with hyphens removed) | Generate with Java 25 `UUID.randomUUID()` (v4). Map every UUID attribute explicitly with Hibernate `@JdbcTypeCode(SqlTypes.BINARY)` and length 16. Use identical bytes and SQL types for all PK/FK columns; do not apply time-byte swaps or rely on Hibernate's dialect-selected UUID default. The no-database Hibernate binding/extraction test passed; verify the complete storage round trip on MariaDB before applying DDL. |
| Organization key | `VARBINARY(100)` containing canonical lowercase ASCII slug bytes | Accept only `[a-z0-9]+(?:-[a-z0-9]+)*`, 1-100 ASCII bytes; reject rather than lowercase, trim, or otherwise transform. A full unique index is exact, collation-independent, and distinguishes trailing bytes. |
| OIDC issuer and subject | `issuer VARBINARY(480)` containing strict UTF-8 bytes; `subject VARBINARY(255)` containing exact ASCII bytes | The 480-byte issuer bound is an application policy, not an OIDC specification limit. Validate the configured issuer as an exact case-sensitive HTTPS issuer URL (no query or fragment); do not normalize it. Require a verified subject of 1-255 ASCII bytes; reject non-ASCII, empty, or oversized values. Full unique `(issuer, subject)`; no prefix index, hash, collation, trimming, lowercasing, normalization, or truncation. |
| Instants | `DATETIME(6)` stored under a strict UTC application contract | MariaDB `DATETIME` has no timezone. Preserve the existing Hibernate JDBC UTC setting and test read/write behavior. |
| JSON documents | MariaDB-compatible `JSON` representation with application schema and size validation | MariaDB 10.11 JSON is text-backed, not PostgreSQL JSONB. Keep values bounded and non-authoritative; do not store secrets or relational authorization state in JSON. |
| Transactional and relational tables | InnoDB | Verify all participating tables use an engine that supports the required transactions and foreign keys. |
| Human-readable text and other machine keys | `utf8mb4` for human text; choose a bounded representation for each key | Do not infer exact identity equality from a binary character collation; verify comparison and index behavior on the target MariaDB build. |

The Hibernate mapping recommendation is field-explicit `@JdbcTypeCode(SqlTypes.BINARY)`, which selects Hibernate's JDBC `BINARY` binder/extractor and its UUID-to-byte conversion. Do not leave `UUID` attributes to the dialect default: Hibernate 7.4 defaults the preferred UUID JDBC type to `SqlTypes.UUID`, while its MariaDB dialect registers MariaDB's native `uuid` type for server versions 10.7 and later and a `VarcharUUIDJdbcType`. That is not the selected `BINARY(16)` mapping. `@Column(length = 16)` should accompany the explicit binary code where a column length is declared; migration DDL must independently declare the chosen physical type.

The canonical representation is byte-for-byte standard UUID text order, with no `UUID_TO_BIN`-style field permutation. Hibernate 7.4.13.Final converts `UUID.getMostSignificantBits()` to bytes 0–7 and `getLeastSignificantBits()` to bytes 8–15 using big-endian helpers; its binary JDBC type calls `PreparedStatement.setBytes()` and `ResultSet.getBytes()`. MariaDB Connector/J 3.5.10's `setBytes()` stores the supplied byte array in its byte-array codec, whose binary encoder writes those bytes and binary decoder returns the bytes. This source-level behavior shows no UUID-specific byte swapping by either layer; it does not replace a server-backed round-trip test.

Java 25's `UUID.randomUUID()` produces version-4 UUIDs and is the selected built-in generator. UUIDv7 does not have a built-in Java 25 factory. Hibernate's byte conversion preserves a UUIDv7's bits, and canonical bytewise order keeps its leading timestamp fields in order, but no v7 generator is selected. UUIDv4's random insertion pattern has poorer B-tree locality; a future approved UUIDv7 generator could improve locality without changing this storage format. `BINARY(16)` uses 16 key bytes and bytewise comparison without collation; `CHAR(36)` is a larger textual key with character/collation and padding considerations. `CHAR(36)` remains an unnecessary fallback unless binary verification fails.

#### First-slice exact identifier keys

The organization key is stored as its canonical ASCII bytes in `VARBINARY(100)`, with a full unique index. The application accepts only the lowercase slug grammar `[a-z0-9]+(?:-[a-z0-9]+)*`, 1-100 bytes; uppercase, whitespace, non-ASCII, empty segments, and leading/trailing hyphens are rejected. Do not silently normalize input. The database column and unique index compare the exact bytes, independent of collation. Persist the encoded value as `byte[]` with explicit `@JdbcTypeCode(SqlTypes.VARBINARY)` or use a tested String-to-byte converter.

OIDC `issuer` is a configured, validated case-sensitive issuer identifier, and `subject` is the exact opaque, case-sensitive provider value. Store both as UTF-8 bytes in `VARBINARY`, not character columns. MariaDB documents that `VARBINARY` comparisons are bytewise and values are not right-padded; this avoids the trailing-space equivalence of many `VARCHAR` collations. A binary collation by itself is not sufficient for this key. `BINARY` is also unsuitable for variable identifiers because it zero-pads. Use strict UTF-8 encoder/decoder behavior (report malformed input; never replacement-encode), and map persistence bytes as Java `byte[]` with explicit Hibernate `@JdbcTypeCode(SqlTypes.VARBINARY)` and matching column lengths. A String-facing persistence converter is acceptable only if it is tested to preserve all bytes exactly.

The selected input bounds are `issuer` at 480 UTF-8 bytes and `subject` at 1-255 ASCII bytes. The issuer byte limit is an application storage policy, not an OIDC protocol limit; reject an oversized configured issuer rather than truncate it. Validate issuer syntax as an absolute HTTPS OIDC issuer URL with a host and without query or fragment; preserve its original spelling and case. Reject whitespace in issuer syntax. A subject must come only from a successfully verified provider assertion and must contain only ASCII bytes; reject empty, non-ASCII, or oversized values. Do not trim, lowercase, normalize, or truncate a subject. Email is never part of identity uniqueness.

Create a full unique index on `(issuer, subject)` for all external-identity rows, including unlinked rows. The pair and its `identity_principal_id` binding are immutable. Unlinking retains the row; re-linking requires fresh provider verification and may only restore the same principal binding. Its declared maximum key-part size is `480 + 255 = 735` bytes; with the 16-byte UUID primary key carried by a secondary-index record, budget up to 751 bytes. MariaDB documents InnoDB key limits of 768 bytes at 4-KB pages, 1,536 bytes at 8-KB pages, and 3,072 bytes at 16-KB pages; its default page size is 16 KB. The 751-byte key fits even the documented 4-KB-page limit, so no 8-KB minimum is required. The existing setup document does not record the XAMPP instance's page size; inspect it in the opt-in integration environment, but the selected key size is compatible with every documented page size. Strict SQL mode must also be enabled so oversized binary values fail rather than truncate; enforce the bounds before JDBC binding as well.

For membership episodes, preserve the composite unique target `(organization_id, membership_id)`. Define `generated_live_principal_id BINARY(16) GENERATED ALWAYS AS (CASE WHEN status = 'REVOKED' THEN NULL ELSE identity_principal_id END) STORED` and a unique index on `(organization_id, generated_live_principal_id)`. MariaDB unique indexes permit multiple `NULL` values, so historical revoked episodes coexist while the index permits at most one `PENDING`, `ACTIVE`, or `SUSPENDED` episode per principal and organization. Declared unique-index key-part sizes, excluding any implicit primary-key suffix, are: organization slug 100 bytes; external issuer/subject 735 bytes; membership live-principal key 32 bytes; and `(organization_id, membership_id)` 32 bytes. The external secondary-index primary-key suffix brings the maximum to 751 bytes. Verify generated-column/index behavior against MariaDB 10.11.16. Use restrictive/no-action foreign keys; do not hard-delete or cascade-delete organizations, principals, external identities, or memberships.

The first-slice lifecycle vocabularies and transitions are:

| Entity | Allowed transitions |
|---|---|
| `Organization` | `ACTIVE` -> `SUSPENDED` or `CLOSED`; `SUSPENDED` -> `ACTIVE` or `CLOSED`; `CLOSED` is terminal. |
| `IdentityPrincipal` | `ACTIVE` -> `DISABLED`; `DISABLED` -> `ACTIVE`. |
| `ExternalIdentity` | `LINKED` -> `UNLINKED`; `UNLINKED` -> `LINKED` only after fresh provider verification and only for the same immutable principal. |
| `OrganizationMembership` | `PENDING` -> `ACTIVE` or `REVOKED`; `ACTIVE` -> `SUSPENDED` or `REVOKED`; `SUSPENDED` -> `ACTIVE` or `REVOKED`; `REVOKED` is terminal. A post-revocation join creates a new UUID episode. |

Creation in the initial state is permitted; all other transitions are rejected by the application. Persist status tokens in bounded ASCII columns with database checks for the exact vocabulary; do not treat a valid status value as proof that a transition is legal. An organization must be `ACTIVE`, its principal `ACTIVE`, and its membership `ACTIVE` for tenant access. No hard deletion is allowed for these four entities; lifecycle state changes preserve references and history.

### 3.4 Tenant-safe relationships

- Preserve the canonical model's required `organization_id` on every tenant-owned row.
- Parent rows referenced within a tenant should expose a unique `(organization_id, entity_id)` key; tenant children should reference both values in composite foreign keys.
- Every membership actor reference in a tenant-owned record must pair the membership ID with that record's organization ID. A global principal ID does not replace the same-organization membership relationship.
- Keep global catalog references distinct from tenant configuration and policy references. Runs and approvals must retain the exact organization and configuration/policy version relationships in the canonical model.
- For each optional composite foreign key, require either the entire FK tuple to be null or the entire tuple to be present. MariaDB/InnoDB does not check a composite foreign key when any child component is null; verify the selected database constraint mechanism and retain application-level validation.
- Do not assume PostgreSQL partial-index behavior. Any "at most one active grant" invariant must use a MariaDB-compatible enforcement mechanism, with its exact logical key and behavior documented and tested before DDL is approved.

The canonical logical requirements are in `DATABASE_MODEL_RECONCILIATION.md`. This strategy does not replace or weaken them.

## 4. Compatibility evidence and remaining verification

The selected tooling versions align with the Spring Boot 4.1.2 dependency-management metadata and the verified project dependency resolution:

1. Spring Boot 4.1.2 resolves Hibernate ORM to 7.4.13.Final and MariaDB Connector/J to 3.5.10. The Maven wrapper is pinned to Maven 3.10.0 and the project targets Java 25.
2. Hibernate 7.4.13.Final's [`UUIDJavaType`](https://github.com/hibernate/hibernate-orm/blob/7.4.13/hibernate-core/src/main/java/org/hibernate/type/descriptor/java/UUIDJavaType.java) converts a UUID to 16 bytes by writing the most-significant then least-significant long through [`BytesHelper`](https://github.com/hibernate/hibernate-orm/blob/7.4.13/hibernate-core/src/main/java/org/hibernate/internal/util/BytesHelper.java), which writes each long big-endian. Its [`BinaryJdbcType`](https://github.com/hibernate/hibernate-orm/blob/7.4.13/hibernate-core/src/main/java/org/hibernate/type/descriptor/jdbc/BinaryJdbcType.java) uses [`VarbinaryJdbcType`](https://github.com/hibernate/hibernate-orm/blob/7.4.13/hibernate-core/src/main/java/org/hibernate/type/descriptor/jdbc/VarbinaryJdbcType.java)'s `setBytes()`/`getBytes()` binder/extractor. Hibernate's [`@JdbcTypeCode`](https://docs.hibernate.org/orm/7.4/javadocs/org/hibernate/annotations/JdbcTypeCode.html) selects a JDBC type code explicitly.
3. Hibernate's versioned [`MappingSettings`](https://github.com/hibernate/hibernate-orm/blob/7.4.13/hibernate-core/src/main/java/org/hibernate/cfg/MappingSettings.java) sets the default preferred UUID JDBC type to `SqlTypes.UUID`. Its [`MariaDBDialect`](https://github.com/hibernate/hibernate-orm/blob/7.4.13/hibernate-core/src/main/java/org/hibernate/dialect/MariaDBDialect.java) maps UUID natively for MariaDB 10.7+ and registers `VarcharUUIDJdbcType`; explicit `SqlTypes.BINARY` is therefore required for the selected binary schema.
4. MariaDB Connector/J 3.5.10's [`BasePreparedStatement`](https://github.com/mariadb-corporation/mariadb-connector-j/blob/3.5.10/src/main/java/org/mariadb/jdbc/BasePreparedStatement.java) passes `setBytes()` values to [`ByteArrayCodec`](https://github.com/mariadb-corporation/mariadb-connector-j/blob/3.5.10/src/main/java/org/mariadb/jdbc/plugin/codec/ByteArrayCodec.java); binary encoding writes the supplied byte array and decoding returns bytes. MariaDB documents `BINARY` as fixed-length, bytewise-comparing data ([BINARY type](https://mariadb.com/docs/server/reference/data-types/string-data-types/binary.md)).
5. Isolated executable probes against the resolved Hibernate 7.4.13.Final artifact confirmed `Types.BINARY` resolves to `BinaryJdbcType` and used proxy JDBC objects (no connection) to verify exact bytes through its binder/extractor. The canonical byte vector, an RFC 9562 UUIDv7 example, and a JDK-generated UUIDv4 all passed.
6. Oracle's [Java 25 UUID API](https://docs.oracle.com/en/java/javase/25/docs/api/java.base/java/util/UUID.html) documents `randomUUID()` as version 4 and provides no built-in UUIDv7 factory. RFC 9562 describes UUIDv7's time-ordered leading fields and the database-index locality benefit over random UUIDv4 values ([RFC 9562](https://www.rfc-editor.org/rfc/rfc9562.html)).
7. Flyway 12.4.0 is configured as an unbound Maven plugin with plugin-only MariaDB dependencies. It remains absent from the application runtime; normal startup does not invoke migrations.
8. MariaDB documents that [`VARBINARY`](https://mariadb.com/docs/server/reference/data-types/string-data-types/varbinary) compares byte values and is not right-padded, while [`VARCHAR`](https://mariadb.com/docs/server/reference/data-types/string-data-types/varchar) comparisons may ignore trailing pad characters. A binary character collation alone therefore does not meet the exact external-identity key requirement. MariaDB documents InnoDB index limits of 768, 1,536, and 3,072 bytes for 4-KB, 8-KB, and 16-KB pages respectively, with 16 KB as the default ([InnoDB limitations](https://mariadb.com/docs/server/server-usage/storage-engines/innodb/innodb-limitations)). OpenID Connect Core defines case-sensitive issuer and subject values and a 255-ASCII-character subject limit ([OIDC Core, ID Token](https://openid.net/specs/openid-connect-core-1_0.html#IDToken)).

These source and documentation checks do not prove MariaDB runtime behavior. Before applying migrations, run the opt-in test suite against an isolated, disposable MariaDB 10.11.16 target and record its `innodb_page_size` and SQL mode. The 751-byte maximum index fits MariaDB's documented 768-byte limit at 4-KB pages, so no page-size floor above the documented minimum is required. Strict SQL mode is still required; the current XAMPP server version, page size, and SQL mode have not been checked.

Required first-slice compatibility tests:

1. Persist and reload a fixed UUID through Hibernate; compare the raw Connector/J primary-key bytes with the canonical 16 octets.
2. Verify a foreign key points to the exact same UUID bytes.
3. Verify case-distinct issuer/subject values and valid trailing-byte-distinct subject values remain distinct; an exact duplicate pair must violate the unique key regardless of `LINKED`/`UNLINKED` status. Verify whitespace-invalid issuer input and empty, non-ASCII, or >255-byte subjects are rejected; confirm values are never trimmed, lowercased, normalized, or truncated, and enforce the 480-byte issuer boundary.
4. Verify the complete unique `(issuer, subject)` key is indexed without prefix or hash and fits the active InnoDB page size.
5. Verify the generated membership key permits multiple `REVOKED` episodes but rejects two `PENDING`, `ACTIVE`, or `SUSPENDED` episodes for the same organization/principal, including concurrent insert attempts.
6. Verify all allowed and forbidden lifecycle transitions, restrictive foreign-key deletion behavior, and no cascade deletion.

The persistence implementation includes H2 tests for the four entities and an opt-in MariaDB integration test class with four test methods. Those MariaDB tests are disabled by default and require `-Dmariadb.integration.enabled=true` plus `MARIADB_IT_URL`, `MARIADB_IT_USER`, and `MARIADB_IT_PASSWORD`; the URL must omit embedded credentials/query parameters and target a database name ending in `_it` other than `autonomous_enterprise_ai`. The test class rejects missing or invalid settings rather than falling back to application datasource defaults. This task did not enable or run them. Do not use the XAMPP development database as a test target. No MariaDB connection was made.

Other physical compatibility checks remain:

1. Test UTC `DATETIME(6)` precision and round trips across the application, JDBC driver, Hibernate, and MariaDB session settings.
2. Test MariaDB JSON validation, application-side shape/size limits, and the behavior of any indexes that are later justified by measured queries.
3. Test InnoDB composite foreign keys, alternate keys, null-pair checks, the exact bytewise keys and lengths, generated membership index, strict-mode truncation rejection, and the required page-size floor against MariaDB 10.11.16.
4. Test migration transaction and failure behavior for each DDL operation used. Do not assume DDL is transactionally reversible or that a failed migration left no changes.
5. Verify migration-account grants are sufficient for the reviewed migrations but do not grant the runtime account DDL privileges.
6. Run post-migration integration tests with the application configured to use `aea_app` and `ddl-auto: none`, including tenant-crossing rejection, nullable tuple validation, identity/RBAC lifecycle, and approval/configuration version consistency.

Use an isolated, disposable MariaDB test database or environment for compatibility and migration tests. Do not use the development database containing user data as a test target. No test environment or database-level migration compatibility result is created or claimed by this document.

## 5. Recommended controlled migration procedure

This is a future procedure recommendation, not an instruction to run migrations now.

1. **Approve scope and review:** Approve the logical entities, physical mapping decisions, DDL, migration identity and grants, data handling, and execution window. Ensure unresolved product/security decisions that affect the schema are closed.
2. **Prepare recovery:** Confirm a current backup appropriate to the data and migration risk, protect it under the approved access/retention policy, and verify restoration to an isolated environment. No backup or restore process is claimed as verified by the reviewed setup documents.
3. **Preflight:** Confirm the intended MariaDB 10.11.16 XAMPP instance, `localhost:3306`, database name, migration history, expected prior schema version, migration account, and available recovery path. Ensure the application is not relying on startup schema generation.
4. **Apply separately:** Invoke `.\mvnw.cmd org.flywaydb:flyway-maven-plugin:12.4.0:migrate` as a deliberate operator-controlled action with the migration identity. Do not expose the migration credential to the normal `aea_app` runtime or run the migration automatically on application startup.
5. **Validate:** Confirm the migration history matches the reviewed release, expected tables and constraints exist, and integrity checks and migration-specific tests pass. Start or test the application with `ddl-auto: none` and the DML-only runtime identity; verify the application does not attempt DDL.
6. **Handle failure safely:** Stop further migration attempts, preserve logs and observed state, and determine whether any DDL took effect. Do not edit an applied migration or run an automatic destructive rollback. Select a reviewed forward-fix or a separately authorized restore/recovery action based on tested evidence.
7. **Record evidence:** Record the migration version, target environment/version, review and operator approvals, validation results, backup/restore reference, and any deviations without recording secrets.

## 6. Backup, recovery, and validation recommendations

- Define backup frequency, retention, encryption, access, recovery objectives, ownership, and restore procedures before using real organizational data or considering shared deployment. These decisions remain open in the reviewed architecture and readiness documents.
- Take an appropriate recoverable backup before a migration that could affect existing data, and verify the backup by restoring it to an isolated target before relying on it for recovery.
- Test both migration success and failure/recovery paths against the target MariaDB version. Include a clean initial install, upgrade from the prior migration version, interrupted/failed migration behavior, and application integration tests after upgrade.
- Prefer a reviewed forward-fix for an applied migration. Restore from backup only through a separately approved recovery procedure after considering data written since the backup.
- Do not treat XAMPP, phpMyAdmin, an untested dump, or a successful migration run as proof of backup, restore, or production readiness.

## 7. Decisions and authorization gates

| Category | Items |
|---|---|
| **Confirmed tooling configuration** | Flyway Maven plugin 12.4.0 with plugin-only `flyway-mysql` 12.4.0 and MariaDB Connector/J 3.5.10; explicit `info`, `validate`, and `migrate` commands; migration location `src/main/resources/db/migration`; no application Flyway dependency or lifecycle binding; `ddl-auto: none`; no migration at normal startup. |
| **Technical compatibility verification still required** | Current XAMPP server version, MariaDB 10.11.16 UUID and key storage round trips; byte-exact issuer/subject uniqueness including case and trailing bytes; the 735-byte declared identity key plus 16-byte UUID suffix (751 bytes total), compatible even with the documented 4-KB page limit; strict SQL mode; generated membership uniqueness and concurrent insertion; lifecycle checks, restrictive FKs, and no-cascade behavior; UTC `DATETIME(6)` and other schema integration tests. The current XAMPP server version, page size, and SQL mode are not verified. |
| **Explicit authorization before database changes** | Approve physical DDL and migration files; create or alter database accounts or grants; create database objects or apply migrations; change XAMPP configuration; define backup credentials, retention, or recovery operations; use real organizational data or shared deployment. None of these actions is authorized or performed by this document. |

## 8. Relationship to existing documents

- `DATABASE_SETUP.md` remains the central source for setup facts and verification status. Its prior runtime results are historical; this task did not verify a database connection.
- `DATABASE_MODEL_RECONCILIATION.md` remains the canonical logical model and records the selected first-slice identifier, lifecycle, uniqueness, and deletion rules. Other model sections remain proposals and are not implemented by this strategy.
- `DATABASE_ARCHITECTURE.md` and `DATABASE_ER_DIAGRAM.md` summarize the canonical logical model; neither is executable DDL.
- `ADR-0008-mariadb-primary-database.md` continues to govern the accepted MariaDB choice and its unresolved retrieval decision.
- `IMPLEMENTATION_READINESS_PLAN.md` continues to govern broader implementation and real-data readiness gates. This strategy does not close those gates or claim production readiness.

## 9. Change boundary

This document records the plugin-only Maven integration, first-slice migration, and remaining gates. No database or phpMyAdmin connection was made, and no SQL/DDL was executed. V1 and the four entity/repository mappings are present in the repository; the migration remains unapplied. No database identity, grant, XAMPP setting, credential, commit, or push was created or changed. Flyway and its modules remain plugin-only, normal application startup does not run migrations, and `spring.jpa.hibernate.ddl-auto: none` remains required.
