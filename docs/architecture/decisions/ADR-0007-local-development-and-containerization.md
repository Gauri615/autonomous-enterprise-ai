# ADR-0007: Local Development and Containerization

| Field | Value |
|---|---|
| ADR Number and Title | ADR-0007 — Local Development and Containerization |
| Status | Accepted |
| Date | 2026-11-13 |

## Context

The PRD, SRS, and System Architecture specify local development using the agreed frontend, Spring Boot backend, Python/FastAPI and LangGraph AI service, and MariaDB 10.11.16. XAMPP is the selected local database environment and phpMyAdmin is the selected database management interface. Docker is planned for a later phase; Redis is optional and is not required for MVP correctness.

## Problem Statement

The project needs a practical initial development posture that does not make deferred infrastructure a prerequisite or imply a deployment architecture that has not been selected.

## Decision

- Initial local development must not require Docker or Redis.
- Developers may run the agreed application services with MariaDB 10.11.16 through XAMPP and manage it through phpMyAdmin, using synthetic/demo data. These local tools do not select a production hosting/deployment topology or a Knowledge Agent retrieval technology.
- Redis may be considered only if measured requirements justify it and tenant-/permission-safe cache behavior is designed.
- Docker/containerization is planned for a later phase, after prototype validation and deployment requirements are defined.
- This ADR does not select shared hosting, deployment topology, network boundaries, or observability infrastructure.

## Alternatives Considered

- Require Docker for initial local development: not selected; the architecture explicitly says it is not an MVP prerequisite.
- Require Redis for persistence, authorization, or workflow correctness: rejected; MariaDB is the source of truth and Redis is optional.
- Select a shared/container deployment topology now: deferred; the project documents deployment and network decisions as open.

## Rationale

Local-first development keeps the MVP focused on product behavior and the agreed stack while avoiding infrastructure work without a demonstrated need. Keeping Docker and Redis optional preserves the documented scope and does not foreclose later deployment decisions.

## Consequences

- Local developer setup must document the Java/Maven, Python, frontend, and XAMPP/MariaDB 10.11.16 prerequisites actually used; phpMyAdmin is the management interface.
- Synthetic data and local configuration are required; real employee data and secrets must not be placed in local fixtures or source control.
- Any later container or cache adoption requires its own configuration, security, and tenant-isolation review.
- This decision does not define CI/runtime parity or a production deployment model.

## Security Implications

- Local development must not include real secrets or real employee data by default.
- Provider credentials, if eventually configured, must remain outside source control, prompts, and routine logs.
- No network isolation, TLS deployment, secrets manager, database protection, or production control is implied by running services locally.
- Real-data or shared deployment remains gated by AR-H-03; internal service trust remains gated by AR-H-01.

## Related Documents

- [PRD.md](../../product/PRD.md)
- [SRS.md](../../product/SRS.md)
- [SYSTEM_ARCHITECTURE.md](../SYSTEM_ARCHITECTURE.md)
- [IMPLEMENTATION_READINESS_PLAN.md](../IMPLEMENTATION_READINESS_PLAN.md)
- [ARCHITECTURE_REVIEW.md](../ARCHITECTURE_REVIEW.md)
- [THREAT_MODEL.md](../../security/THREAT_MODEL.md)
