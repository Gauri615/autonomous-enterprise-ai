# ADR-0003: Database and Vector Storage

| Field | Value |
|---|---|
| ADR Number and Title | ADR-0003 — Database and Vector Storage |
| Status | Superseded |
| Date | 2026-11-13 |

Superseded by [ADR-0008 — MariaDB Primary Database](./ADR-0008-mariadb-primary-database.md). This ADR is retained only as the historical record of the previously selected PostgreSQL/pgvector direction.

## Context

At the time this ADR was written, the SRS and Database ER Diagram specified PostgreSQL as the primary relational store and pgvector for retrieval over a small curated knowledge collection. That decision has been superseded. The requirement for organization- and permission-filtered retrieval remains in force, but its storage/search implementation is unresolved.

## Problem Statement

The prior decision combined relational persistence and vector search in PostgreSQL. It is no longer the selected primary database or retrieval design.

## Decision

- Historical decision only: PostgreSQL was the primary relational database and pgvector the proposed embedding search.
- Still-valid security constraints: enforce tenant and document permission constraints before retrieved content or source references are returned to FastAPI/LangGraph or the model; preserve organization scope and tenant-consistent relationships.
- Retrieval product, embedding provider/model, vector dimensions, similarity metric, and index configuration were not finalized by this historical decision and remain open under ADR-0008.

## Alternatives Considered

- Separate vector database, external vector-search service, or temporary non-vector retrieval: not decided; these options are now being evaluated under ADR-0008.
- Introduce Redis as a required source of truth or authorization dependency: rejected; Redis is optional and cannot be required for correctness.

## Rationale

The prior rationale selected PostgreSQL and pgvector to co-locate relational and vector data. This rationale no longer supports the current architecture decision.

## Consequences

- The prior PostgreSQL/pgvector-specific schema and migration assumptions are not current implementation direction.
- Tenant isolation, referential integrity, permission-filtered retrieval, and audit controls remain requirements independent of storage choice.
- Refer to ADR-0008 and the current SRS/Database ER Diagram for current decisions and open items.

## Security Implications

- Opaque IDs and vector similarity do not authorize access.
- Query/write/search paths require server-derived organization scope; user permission checks must precede model input.
- Do not store plaintext passwords or unapproved provider/API secrets in database records.
- This historical ADR does not authorize PostgreSQL, pgvector, or real-data use.
- High readiness gate AR-H-03 remains open for real-data and deployment protections.

## Related Documents

- [PRD.md](../../product/PRD.md)
- [SRS.md](../../product/SRS.md)
- [SYSTEM_ARCHITECTURE.md](../SYSTEM_ARCHITECTURE.md)
- [DATABASE_ER_DIAGRAM.md](../DATABASE_ER_DIAGRAM.md)
- [API_CONTRACTS.md](../../api/API_CONTRACTS.md)
- [THREAT_MODEL.md](../../security/THREAT_MODEL.md)
- [ARCHITECTURE_REVIEW.md](../ARCHITECTURE_REVIEW.md)
- [IMPLEMENTATION_READINESS_PLAN.md](../IMPLEMENTATION_READINESS_PLAN.md)
