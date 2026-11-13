# ADR-0006: Simulated Tool Execution

| Field | Value |
|---|---|
| ADR Number and Title | ADR-0006 — Simulated Tool Execution |
| Status | Proposed |
| Date | 2026-11-13 |

## Context

The approved MVP excludes live enterprise mutations and requires a small allow-list of simulated actions. The architecture assigns invocation to Spring Boot only after deterministic validation, authorized human approval, and execution-time revalidation. The exact tool catalog, schemas, effects, ownership, and approval expiry remain unresolved under High readiness gate AR-H-02.

## Problem Statement

The prototype needs to demonstrate a governed action lifecycle without exposing live systems, production credentials, arbitrary tools, or an AI-controlled execution route.

## Decision

The MVP boundary is that any enabled tool effect is simulated only:

- Tools must be explicitly allow-listed, documented, and constrained by validated input schemas.
- Spring Boot is the only component permitted to authorize and invoke the simulated adapter.
- Every enabled simulation requires deterministic policy checks, explicit authorized human approval of the exact proposal, and policy/approval revalidation immediately before invocation.
- Execution must have a finite timeout, duplicate-safe behavior, persisted outcome, and required audit record before a confirmed-success response.
- No live enterprise connector, production credential, arbitrary shell/database/network operation, or real system mutation is in MVP.

**This ADR remains Proposed** because the exact allow-list, schemas/effects, validity period, governance owner, and enablement evidence have not been approved. Until AR-H-02 is closed and verified, action execution remains disabled.

## Alternatives Considered

- Connect tools to live enterprise systems in the MVP: rejected; outside approved scope.
- Let FastAPI/LangGraph invoke tools directly: rejected; violates the authoritative backend boundary.
- Permit arbitrary model-generated tools or parameters: rejected; conflicts with allow-list and schema-validation requirements.
- Demonstrate action proposals without enabling execution: permitted as a bounded prototype option while the catalog/approval gate remains open; it does not represent an executed action.

## Rationale

Simulation preserves a demonstrable proposal/approval/execution lifecycle while avoiding live side effects. Backend-only invocation and explicit approval ensure the model remains a recommender, not an operator.

## Consequences

- Product/security/architecture review must approve each simulation, its schema, expected effect, and enablement/disablement ownership.
- Tool execution depends on the approval lifecycle in ADR-0005 and on trusted identity/service boundaries.
- Tests must cover unknown tools, invalid inputs, missing/rejected/expired/changed approvals, policy changes, replay, concurrency, timeout, ambiguous results, and audit/persistence failure.
- If readiness tests or required controls fail, keep the affected simulation disabled.

## Security Implications

- Do not treat an AI proposal, policy eligibility-to-review result, or approval alone as execution authority.
- Enforce deterministic server-side organization, actor, action, schema, and current approval checks.
- An ambiguous timeout must not be reported as success or blindly retried.
- Simulation-only is an MVP constraint, not a claim that the execution control has been implemented or verified.
- AR-H-02 remains open; AR-H-01 service identity and AR-H-03 operational/audit protections remain applicable gates for dependent exposure.

## Related Documents

- [PRD.md](../../product/PRD.md)
- [SRS.md](../../product/SRS.md)
- [SYSTEM_ARCHITECTURE.md](../SYSTEM_ARCHITECTURE.md)
- [SEQUENCE_DIAGRAMS.md](../SEQUENCE_DIAGRAMS.md)
- [API_CONTRACTS.md](../../api/API_CONTRACTS.md)
- [DATABASE_ER_DIAGRAM.md](../DATABASE_ER_DIAGRAM.md)
- [THREAT_MODEL.md](../../security/THREAT_MODEL.md)
- [ARCHITECTURE_REVIEW.md](../ARCHITECTURE_REVIEW.md)
- [IMPLEMENTATION_READINESS_PLAN.md](../IMPLEMENTATION_READINESS_PLAN.md)
