# ADR-0001: Technology Stack

| Field | Value |
|---|---|
| ADR Number and Title | ADR-0001 — Technology Stack |
| Status | Accepted |
| Date | 2026-11-13 |

## Context

The PRD and SRS specify an enterprise-oriented prototype beginning with AI IT Support. They identify an agreed web client, Java business backend, Python AI service, LangGraph orchestration, MariaDB 10.11.16 relational persistence, and supporting API documentation, testing, observability, and CI tools. ADR-0008 records the approved database change. Knowledge Agent retrieval technology remains unresolved.

## Problem Statement

The project needs a consistent technology baseline to guide architecture and implementation planning without implying that undecided providers, authentication details, infrastructure, or deployment choices have been finalized.

## Decision

Record the following stack as the project baseline:

- **Frontend:** React, TypeScript, Tailwind CSS, Axios; Recharts only if a lightweight prototype metrics presentation is included.
- **Business backend:** Java 21+ and Spring Boot, with Spring Security, Spring Data JPA/Hibernate, and Maven.
- **AI processing:** Python and FastAPI.
- **Agent orchestration:** LangGraph.
- **Persistence:** MariaDB 10.11.16 as the primary relational store. The local database environment is XAMPP and the management interface is phpMyAdmin.
- **Knowledge retrieval:** Undecided; see ADR-0008 for the alternatives and decision criteria. MariaDB selection does not select or imply a vector-search capability.
- **Supporting technologies:** OpenAPI/Swagger, OpenTelemetry, and GitHub Actions.
- **Testing:** JUnit, Pytest, Vitest, and Playwright at their relevant service boundaries.
- **Optional/deferred:** Redis only if justified by measured need; Docker/containerization in a later phase, not a local MVP prerequisite.

The LLM provider remains undecided between the options documented by the project. JWT is an initial authentication direction only; authentication/session design remains unresolved. No additional database, broker, gateway, service mesh, or microservice is selected by this ADR.

## Alternatives Considered

- Replace one or more agreed framework, language, or database choices with a different stack: not selected; the documents establish the current stack as the agreed baseline.
- Make Redis or Docker mandatory for initial MVP development: not selected; the documents explicitly make Redis optional and containerization later.
- Select a vector retrieval product or finalize JWT mechanics as part of this stack record: not selected; vector options and authentication details remain open.

## Rationale

This captures the stack already specified by the PRD, SRS, and System Architecture, keeps the two runtime boundaries explicit, and avoids adding infrastructure without a demonstrated need. Provider and authentication uncertainty remains visible instead of being silently resolved.

## Consequences

- Implementation planning should use the named stack and preserve the modular Spring Boot backend and bounded Python AI service.
- Redis, Docker, and Recharts are not mandatory MVP dependencies.
- Provider-specific model, embedding/vector retrieval, authentication, and deployment choices require separate decisions and reviews.
- Stack agreement does not establish operational readiness or implementation completion.

## Security Implications

- Tooling choices do not grant the AI service or agents authorization.
- Data sent to a provider must remain task-relevant and permission-eligible; use synthetic/demo data until provider/data handling decisions are reviewed.
- Authentication and service-to-service protections are not finalized by this ADR.
- Secrets remain outside source control, prompts, and routine logs.
- No production readiness, certification, or compliance claim follows from adopting this stack.

## Related Documents

- [PRD.md](../../product/PRD.md)
- [SRS.md](../../product/SRS.md)
- [SYSTEM_ARCHITECTURE.md](../SYSTEM_ARCHITECTURE.md)
- [ARCHITECTURE_REVIEW.md](../ARCHITECTURE_REVIEW.md)
- [IMPLEMENTATION_READINESS_PLAN.md](../IMPLEMENTATION_READINESS_PLAN.md)
