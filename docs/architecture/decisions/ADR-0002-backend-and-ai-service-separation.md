# ADR-0002: Backend and AI Service Separation

| Field | Value |
|---|---|
| ADR Number and Title | ADR-0002 — Backend and AI Service Separation |
| Status | Accepted |
| Date | 2026-11-13 |

## Context

The SRS, System Architecture, API Contracts, and sequence diagrams consistently assign business authority to Java Spring Boot and bounded AI processing to Python FastAPI. The browser calls Spring Boot; FastAPI is an internal service and returns untrusted output for backend validation.

## Problem Statement

The system must keep business state, access control, and action authorization outside model-driven processing while supporting AI capabilities implemented with the agreed Python stack.

## Decision

- Use Java 21+ / Spring Boot as the authoritative public business API and policy/application boundary.
- Keep the business backend a modular monolith; its policy, approval, audit, data-access, and simulated-tool responsibilities are logical modules, not additional microservices.
- Use Python / FastAPI as an internal AI-processing service called by Spring Boot through a defined, bounded contract.
- FastAPI/LangGraph may return recommendations, source references, and structured action proposals; Spring Boot validates these outputs and owns business state and authorization.
- The browser must not call FastAPI or LangGraph directly.
- Do not add a direct AI-service-to-tool execution path.

This decision does **not** select service-to-service authentication, network exposure, or transport protection. Those remain unresolved under High readiness gate AR-H-01.

## Alternatives Considered

- Place AI processing inside the Java business API: not selected; the agreed stack defines a separate Python/FastAPI AI boundary.
- Make FastAPI the public business API or policy authority: rejected; this conflicts with the SRS and agreed backend authority.
- Split logical agents and business modules into additional microservices: not selected; the architecture explicitly avoids unnecessary microservices.

## Rationale

The split preserves the agreed language/runtime boundary while keeping authorization, persistence, approval, status, and tool gating deterministic and centralized in Spring Boot. A modular monolith avoids distributed business-state complexity; the separate AI service isolates the Python/LangGraph runtime.

## Consequences

- A versioned internal request/response contract and output validation are required.
- Correlation, trusted minimum context, finite deadlines, and explicit error behavior must cross the service boundary.
- Internal service identity and transport controls must be decided and verified before exposing protected services.
- AI processing failures must not bypass backend policy or become success-shaped outcomes.

## Security Implications

- Spring Boot derives trusted identity and organization context and makes deterministic authorization decisions.
- FastAPI is not an authorization authority and must not invoke tools, access live enterprise systems, or hold ambient enterprise credentials.
- Service-to-service authentication, network controls, and protected transport are unresolved; this ADR does not claim they are implemented.
- User and retrieved content and AI outputs remain untrusted across this boundary.

## Related Documents

- [PRD.md](../../product/PRD.md)
- [SRS.md](../../product/SRS.md)
- [SYSTEM_ARCHITECTURE.md](../SYSTEM_ARCHITECTURE.md)
- [SEQUENCE_DIAGRAMS.md](../SEQUENCE_DIAGRAMS.md)
- [API_CONTRACTS.md](../../api/API_CONTRACTS.md)
- [THREAT_MODEL.md](../../security/THREAT_MODEL.md)
- [ARCHITECTURE_REVIEW.md](../ARCHITECTURE_REVIEW.md)
- [IMPLEMENTATION_READINESS_PLAN.md](../IMPLEMENTATION_READINESS_PLAN.md)
