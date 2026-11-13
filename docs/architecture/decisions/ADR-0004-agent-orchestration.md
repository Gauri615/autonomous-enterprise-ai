# ADR-0004: Agent Orchestration

| Field | Value |
|---|---|
| ADR Number and Title | ADR-0004 — Agent Orchestration |
| Status | Accepted |
| Date | 2026-11-13 |

## Context

LangGraph is the agreed framework for stateful agent workflows in the Python AI service. The SRS and System Architecture constrain workflows to configured steps for request analysis, permission-filtered knowledge use, response composition, clarification/escalation recommendations, summaries, and optional simulated-action proposals.

## Problem Statement

The MVP needs coordinated AI steps while preventing model or agent output from acquiring identity, authorization, approval, status, or tool-execution authority.

## Decision

- Use LangGraph within the internal FastAPI AI service to coordinate bounded, configured, request-scoped workflows.
- Treat named agents as logical workflow roles, not independent security principals or deployable microservices.
- Return bounded recommendations, source references, uncertainty, or structured proposals to Spring Boot for validation.
- Keep workflow state correlated to the initiating request and organization; do not reuse state in a way that exposes another user’s or organization’s context.
- Do not give the graph a direct tool-executor path.
- Keep LLM provider selection separate and unresolved.

This ADR accepts the orchestration framework and authority boundary, not a final graph topology, model, prompt design, provider, or workflow implementation.

## Alternatives Considered

- Use a different agent orchestration framework: not selected; LangGraph is the agreed technology.
- Treat each logical agent as an independent microservice: not selected; the architecture keeps one bounded AI service and avoids unnecessary service decomposition.
- Allow model-controlled dynamic tool selection or execution: rejected; it conflicts with the deterministic backend and simulated-tool boundaries.

## Rationale

LangGraph is already selected for stateful orchestration. Keeping the graph bounded and non-authoritative preserves the distinct Python AI runtime without moving identity, access policy, business state, or execution control away from Spring Boot.

## Consequences

- The AI workflow requires a defined internal contract, bounded state transitions, finite deadlines, and explicit output schemas.
- Java-side validation must verify returned content, source references, and proposals.
- Untrusted user/document content may influence generated content but cannot alter backend policy or the configured tool allow-list.
- Workflow behavior and model quality still require evaluation; acceptance of LangGraph does not establish model reliability.

## Security Implications

- Ticket descriptions, retrieved documents, prompts, provider responses, and LangGraph outputs are untrusted.
- Retrieval must be permission-filtered before candidate content enters graph/model context.
- The graph cannot authorize sensitive operations, approve proposals, set trusted identity/status, or invoke tools.
- Prompt injection tests and service-boundary tests remain required; no security control is claimed as implemented.

## Related Documents

- [PRD.md](../../product/PRD.md)
- [SRS.md](../../product/SRS.md)
- [SYSTEM_ARCHITECTURE.md](../SYSTEM_ARCHITECTURE.md)
- [SEQUENCE_DIAGRAMS.md](../SEQUENCE_DIAGRAMS.md)
- [API_CONTRACTS.md](../../api/API_CONTRACTS.md)
- [THREAT_MODEL.md](../../security/THREAT_MODEL.md)
- [ARCHITECTURE_REVIEW.md](../ARCHITECTURE_REVIEW.md)
- [IMPLEMENTATION_READINESS_PLAN.md](../IMPLEMENTATION_READINESS_PLAN.md)
