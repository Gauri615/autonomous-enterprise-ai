# Architecture Decision Records

This directory records architectural decisions and decision boundaries for the Autonomous Enterprise AI System MVP. The records reflect the current PRD, SRS, architecture, threat model, API contracts, architecture review, and implementation readiness plan. An ADR records documented direction; it does not prove implementation or override a higher-level requirement.

## Status overview

| ADR | Title | Status | Decision boundary / open gate |
|---|---|---|---|
| [ADR-0001](./ADR-0001-technology-stack.md) | Technology Stack | Accepted | Captures the agreed stack. LLM provider remains undecided; JWT is only an initial direction pending the authentication decision. |
| [ADR-0002](./ADR-0002-backend-and-ai-service-separation.md) | Backend and AI Service Separation | Accepted | Spring Boot is the authoritative business API; FastAPI is internal AI processing. Service-to-service authentication and transport remain unresolved under AR-H-01. |
| [ADR-0003](./ADR-0003-database-and-vector-storage.md) | Database and Vector Storage | Superseded | Historical PostgreSQL/pgvector direction; superseded by ADR-0008. |
| [ADR-0004](./ADR-0004-agent-orchestration.md) | Agent Orchestration | Accepted | LangGraph coordinates bounded workflows; it cannot authorize or invoke tools. Exact workflow configuration remains implementation work. |
| [ADR-0005](./ADR-0005-human-approval-and-policy-enforcement.md) | Human Approval and Policy Enforcement | Proposed | Core controls are requirements, but approval expiry and related identity/governance decisions are not closed; AR-H-01 and AR-H-02 remain gates. |
| [ADR-0006](./ADR-0006-simulated-tool-execution.md) | Simulated Tool Execution | Proposed | MVP simulations are the only in-scope tool effects; exact allow-list, schemas, ownership, expiry, and enablement evidence remain open under AR-H-02. |
| [ADR-0007](./ADR-0007-local-development-and-containerization.md) | Local Development and Containerization | Accepted | XAMPP provides the local MariaDB environment and phpMyAdmin the management interface. Docker remains deferred; no deployment topology is selected. |
| [ADR-0008](./ADR-0008-mariadb-primary-database.md) | MariaDB Primary Database | Accepted | MariaDB 10.11.16 is the primary relational store. Knowledge Agent vector/retrieval technology remains unresolved and must be separately evaluated. |

“Accepted” records only the stated architectural direction. It does not imply implementation completion, deployment approval, or closure of any unrelated readiness gate.

Vector-search alternatives (separate vector database, external vector-search service, and temporary non-vector retrieval) remain under evaluation. No alternative is accepted until technical and security criteria are assessed.

## Preserved High readiness gates

The following three High readiness gates are reproduced exactly from [IMPLEMENTATION_READINESS_PLAN.md](../IMPLEMENTATION_READINESS_PLAN.md). They remain unresolved and are not approved by these ADRs:

1. **AR-H-01 — Authentication and internal service trust are not selected.** Authentication/session semantics, trusted identity mapping, and Spring Boot-to-FastAPI authentication/transport controls remain open across the SRS, architecture, threat model, and API contract.
2. **AR-H-02 — Approval expiry and simulation governance are unresolved.** The approval expiry duration and the exact simulation allow-list/governance remain unresolved, although every enabled action must require bounded, proposal-bound human approval.
3. **AR-H-03 — Real-data and deployment protections are not selected.** LLM provider/data terms, retention/deletion, secrets handling, database protections, audit integrity/retention, and incident/operational ownership remain open.

Use synthetic/demo data until AR-H-03 is resolved for the intended data context. Keep protected service exposure gated under AR-H-01 and simulated execution disabled until AR-H-02 and its verification requirements are satisfied. These records do not claim production readiness, compliance, or security certification.
