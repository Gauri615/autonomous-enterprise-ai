# ADR-0005: Human Approval and Policy Enforcement

| Field | Value |
|---|---|
| ADR Number and Title | ADR-0005 — Human Approval and Policy Enforcement |
| Status | Proposed |
| Date | 2026-11-13 |

## Context

The PRD and SRS require deterministic backend authorization and explicit human approval before every enabled MVP simulated action. The System Architecture and API Contracts place policy and approval state in Spring Boot. Approval duration, identity/session details, and parts of approval governance remain open in the implementation readiness plan.

## Problem Statement

AI may recommend or propose actions, but must never authorize them. The system needs an explicit, reviewable control sequence that prevents an AI recommendation, approval record, or stale proposal from being treated as execution authority.

## Decision

The following are established project constraints:

- Spring Boot is the deterministic authorization authority for protected reads, proposal eligibility, approval state, and execution-time revalidation.
- Every enabled MVP simulated action requires an explicit decision by an authenticated, authorized human reviewer.
- The approval is bound to the exact proposal and relevant policy context; material changes, expiry, rejection, or failed revalidation stop execution.
- Approval is necessary but does not override current backend policy.
- Sensitive decisions and outcomes must be auditable, and failures must not be reported as success.

**This ADR remains Proposed** because the numerical approval validity period, detailed invalidation/configuration ownership, and final identity/reviewer model have not been approved. These must be resolved before approval-bearing actions are enabled. This decision does not choose authentication/session mechanics.

## Alternatives Considered

- Let the LLM authorize or approve a proposed operation: rejected; conflicts with the PRD/SRS and deterministic backend authority.
- Treat proposal display or conversation continuation as consent: rejected; approval must be explicit.
- Allow approval alone to bypass execution-time policy: rejected; current policy must be revalidated.
- Enable approvals with an implicit or unbounded validity period: rejected; the SRS requires bounded validity and the numeric value is unresolved.

## Rationale

This control model preserves human oversight without delegating authorization to probabilistic output. Binding and revalidating the exact proposal limits stale approval reuse, while truthful audit and failure behavior support review.

## Consequences

- Approval UI must show exact action, target/context, parameters, expected effect, and simulation status.
- The backend must persist proposal integrity, reviewer, decision, time, expiry, and execution outcome with appropriate consistency.
- Approval actions require authorization at read/decision time and policy revalidation immediately before execution.
- Do not enable action-bearing workflows until High readiness gates AR-H-01 and AR-H-02 are resolved and implementation tests pass.

## Security Implications

- AI output is untrusted and cannot grant permission or approve its own proposal.
- Missing, rejected, expired, changed, unauthorized, or ambiguous state must prevent execution.
- Audit persistence failure must not result in a success-shaped approval or execution response.
- Exact authentication, service identity, and protected transport remain unresolved under AR-H-01; approval validity and simulation governance remain unresolved under AR-H-02.
- No production readiness or compliance claim is implied.

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
