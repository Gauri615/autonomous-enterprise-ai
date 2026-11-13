# Implementation Readiness Plan: Autonomous Enterprise AI System

## 1. Document Control

| Field | Value |
|---|---|
| Document | Implementation Readiness Plan |
| Version | 0.1 |
| Status | Draft; decisions and actions remain unapproved / not started |
| Date | 2026-11-13 |
| Owner | Technical Project Management |
| Audience | Product, Architecture, Security, Backend, AI, and DevOps |
| Governing review | `docs/architecture/ARCHITECTURE_REVIEW.md` |

This plan sequences readiness work; it does not approve any open architecture/security decision or assert that controls have been implemented. The Software Requirements Specification remains the normative system requirement source. This document does not expand MVP scope or claim production readiness.

## 2. Executive Summary

The documentation set is coherent enough to plan a controlled prototype, and the approved relational database and MVP boundary are established. The architecture review reports **0 Critical, 3 High, 7 Medium, and 2 Low findings**. The High findings are unresolved readiness gates, not confirmed implementation vulnerabilities.

Implementation may begin with low-risk scaffolding and synthetic-data work. Protected user access, AI processing of user data, permission-filtered retrieval, escalation operations, and simulated action execution must remain gated until their respective identity, lifecycle, tenant, policy, human-approval, and audit conditions are resolved and verified. Real organizational data and shared/external deployment require additional data-provider, privacy, infrastructure, and operational approvals.

Every action in Section 8 begins with status **Not Started**. No decision in this plan should be read as approved.

## 3. Current Architecture Readiness

| Area | Readiness | Basis / boundary |
|---|---|---|
| Product scope and agreed technology | Ready for planning | Documents define internal IT support requests, bounded AI, MariaDB 10.11.16 relational persistence, and the agreed stack. Knowledge Agent retrieval remains open under AR-M-07. |
| Documentation consistency | Conditional | Core responsibilities and security principles align; the 11 review findings remain open. |
| Non-sensitive scaffolding | May begin | Repository setup, static UI shell, local test harness, and synthetic fixtures do not require protected data or enabled actions. |
| Protected request workflows | Not ready | Authentication/service trust and canonical request lifecycle/transition rules need resolution and verification. |
| AI/RAG workflow with user context | Not ready for exposure | Permission filtering, identity propagation, provider handling, and safe failure must be implemented and tested; provider remains undecided. |
| Simulated action execution | Not ready | Keep disabled until approval expiry, tool catalog governance, authorization, proposal integrity, persistence, audit, replay, and concurrency controls are approved and verified. |
| Real organizational data or shared deployment | Not ready | Provider, retention, privacy, secret, database, network, audit, and operational controls are unresolved. |
| Production readiness | Not assessed / not claimed | The review evaluated documentation only, not implementation or deployment evidence. |

No Critical issue was identified by the architecture review. Its finding inventory is 3 High (AR-H-01–03), 7 Medium (AR-M-01–07), and 2 Low (AR-L-01–02). AR-M-07 records the unresolved Knowledge Agent retrieval choice.

## 4. Critical Issues

**None identified in ARCHITECTURE_REVIEW.md.**

This is not a security certification or proof of implementation safety. The High-priority readiness gates in Section 5 still block their dependent workflows.

## 5. High Priority Readiness Gates

The following are the exact three High findings from the architecture review. All remain unresolved and must not be presented as approved.

1. **AR-H-01 — Authentication and internal service trust are not selected.** Authentication/session semantics, trusted identity mapping, and Spring Boot-to-FastAPI authentication/transport controls remain open across the SRS, architecture, threat model, and API contract.
2. **AR-H-02 — Approval expiry and simulation governance are unresolved.** The approval expiry duration and the exact simulation allow-list/governance remain unresolved, although every enabled action must require bounded, proposal-bound human approval.
3. **AR-H-03 — Real-data and deployment protections are not selected.** LLM provider/data terms, retention/deletion, secrets handling, database protections, audit integrity/retention, and incident/operational ownership remain open.

### Gate behavior

- **AR-H-01:** Do not expose protected public operations or the internal AI service until identity/session and service-to-service controls are approved, implemented, and tested.
- **AR-H-02:** Do not enable action proposal approval or simulated execution until the validity/invalidation rules, catalog, schemas, ownership, and negative/replay/concurrency tests are complete.
- **AR-H-03:** Use only synthetic/demo data until provider/data handling and operational controls are reviewed. Do not treat this plan as approval for shared or external deployment.

## 6. Medium Priority Issues

The exact six Medium findings from the architecture review are:

1. **AR-M-01:** Ticket creation and analysis have incompatible-looking timing presentations: the ticket-creation sequence waits for AI and returns an outcome, while the API defines a separate `202` analysis/run flow and leaves synchronous/asynchronous behavior open. Initial/processing status vocabulary is also unresolved.
2. **AR-M-02:** The documents define allowable outcome values but not a complete state-transition table. The API PATCH contract accepts one “allowlisted” `outcomeStatus` without enumerating legal source-to-destination transitions and actor permissions for each transition.
3. **AR-M-03:** The Use Case Diagram gives the Platform Operator direct associations to policy evaluation and tool execution, and the Administrator a direct audit-review association; the detailed use-case actors and SRS distinguish these capabilities and require explicit audit/reviewer authorization. It also draws the LLM as a direct actor for agent use cases rather than a provider mediated by FastAPI/LangGraph.
4. **AR-M-04:** Organization-scoped AgentDefinition and ToolDefinition references are described as scope-constrained, but AgentRun and action records show direct definition-ID foreign keys rather than a demonstrated constraint that prevents referencing a definition owned by another organization. Global definitions are also permitted, so the valid global-versus-tenant rule is not fully encoded.
5. **AR-M-05:** Escalation is required, but the analyst handoff/discovery mechanism and ownership are not specified. The SRS excludes assignment queues and external ticketing, while the user journey requires unresolved requests to reach human support.
6. **AR-M-06:** The ticket-summary response example places an AI-generated summary and `ticketOutcomeStatus` in one result object without defining the status field's provenance. Other documents say request status is backend-authoritative and the summary is not a status update.
7. **AR-M-07:** Knowledge Agent retrieval technology is unresolved after the relational database changed to MariaDB 10.11.16. The prior vector-specific design does not apply; no separate vector database, managed search service, or temporary lexical approach has been evaluated sufficiently for selection.

Corrective work, ownership, dependencies, and completion evidence are assigned in Section 8.

## 7. Low Priority Issues

The exact two Low findings from the architecture review are:

1. **AR-L-01:** Optional knowledge administration is described as adding, updating, and removing documents, but the API defines only conditional document creation. Optional feedback is modeled in requirements/use cases/data, but no feedback endpoint is defined.
2. **AR-L-02:** “Ticket” is used throughout endpoint and entity naming even though the product is explicitly an internal support request and not a live service-desk ticket. The distinction is documented but can still be lost in demos or client integrations.

These items do not promote MVP-Should features into mandatory scope. If optional features are not selected, keep their product surfaces disabled. Preserve the internal-request labeling in API, UI, and demonstration material.

## 8. Corrective Action Register

**Status for every action: Not Started.** Dependencies identify prerequisites or related actions; they do not imply approval. Exact issue descriptions below are copied from ARCHITECTURE_REVIEW.md.

| Issue ID | Original severity | Exact issue description | Impact on implementation | Required corrective action | Dependencies | Responsible role | Completion evidence | Status |
|---|---|---|---|---|---|---|---|---|
| AR-H-01 | High | Authentication and internal service trust are not selected. Authentication/session semantics, trusted identity mapping, and Spring Boot-to-FastAPI authentication/transport controls remain open across the SRS, architecture, threat model, and API contract. | Protected endpoints, identity/organization derivation, reviewer authorization, and internal AI calls cannot be safely exposed or finalized. | Obtain explicit decisions for controlled-demo authentication/session, identity/organization/role mapping, and internal service identity/transport. Update affected contracts and implement and test the chosen controls before exposing dependent routes. | Product/security/architecture approval; coordinate with AR-M-01, AR-M-02, AR-M-04. | Architecture / Security / Backend | Recorded decision and updated docs; implemented controls; tests for invalid/spoofed identity, organization resolution, unauthorized access, and internal caller rejection. | Not Started |
| AR-H-02 | High | The approval expiry duration and the exact simulation allow-list/governance remain unresolved, although every enabled action must require bounded, proposal-bound human approval. | Action proposals and tool execution cannot be enabled; the exact approval lifecycle and no-side-effect guarantees are undefined. | Decide bounded validity and invalidation rules, exact demo simulations/schemas/effects, and configuration/disablement owner. Implement backend-only policy, proposal binding, human review, execution-time revalidation, persistence/audit, duplicate protection, and negative/replay/concurrency tests. | AR-H-01; AR-M-02; AR-M-04; AR-M-06 where status is shown; audit persistence decisions under AR-H-03. | Product / Architecture / Security / Backend | Approved catalog and expiry decision; updated contracts/model; tests prove missing/rejected/expired/changed/unauthorized proposals cause zero execution and duplicate/concurrent calls do not duplicate effects. | Not Started |
| AR-H-03 | High | Real-data and deployment protections are not selected: LLM provider/data terms, retention/deletion, secrets handling, database protections, audit integrity/retention, and incident/operational ownership remain open. | Real organizational data, provider processing of such data, and shared/external deployment cannot be approved. | Continue synthetic/demo data only. Before real-data or shared-deployment consideration, decide provider/data terms, retention/deletion, secret/key management, database/backup protections, audit/monitoring controls, and operational/incident ownership; implement and evidence applicable controls. | Product/security/architecture/DevOps approvals; AR-H-01 for deployment identity; provider selection before embedding dimensions and model use. | Security / Architecture / DevOps / Product | Approved decisions and data-use boundary; operational runbooks/ownership; configuration and access evidence; privacy/security tests. Does not itself grant production approval. | Not Started |
| AR-M-01 | Medium | Ticket creation and analysis have incompatible-looking timing presentations: the ticket-creation sequence waits for AI and returns an outcome, while the API defines a separate `202` analysis/run flow and leaves synchronous/asynchronous behavior open. Initial/processing status vocabulary is also unresolved. | Create/analysis endpoints, persisted agent runs, user polling, idempotency, and timeout behavior may diverge across services. | Select one canonical create/analysis interaction, including initial persisted state, sync/async behavior, polling, deadline/failure behavior, and idempotency; align sequence, API examples, and database status mapping. | AR-M-02; AR-H-01 for protected access; API owner review. | Product / Architecture / Backend / AI | Approved lifecycle decision; aligned sequence/API/schema; contract tests for accepted, completed, failed, timed-out, and duplicate cases. | Not Started |
| AR-M-02 | Medium | The documents define allowable outcome values but not a complete state-transition table. The API PATCH contract accepts one “allowlisted” `outcomeStatus` without enumerating legal source-to-destination transitions and actor permissions for each transition. | Backend state mutation and UI status cannot be implemented consistently or safely; stale and unauthorized transitions are underspecified. | Define backend-owned states and legal transitions, including role/event, approval interaction, terminal behavior, concurrency/version checks, and error response. Add transition tests and align SRS-supported user-visible outcomes. | AR-M-01; AR-M-05 for escalation states; AR-M-06 for summary/status distinction. | Product / Architecture / Backend | Approved transition matrix; API/database/sequence alignment; tests for allowed, denied, stale, and concurrent transitions. | Not Started |
| AR-M-03 | Medium | The Use Case Diagram gives the Platform Operator direct associations to policy evaluation and tool execution, and the Administrator a direct audit-review association; the detailed use-case actors and SRS distinguish these capabilities and require explicit audit/reviewer authorization. It also draws the LLM as a direct actor for agent use cases rather than a provider mediated by FastAPI/LangGraph. | Reviewers and implementers could misread actor authority and create incorrect role bindings or direct provider/agent paths. | Correct the use-case diagram so operators have configuration responsibilities only, audit access requires explicit reviewer permission, and provider calls are mediated by FastAPI/LangGraph. Preserve Spring Boot as sole deterministic policy/tool gate. | None for correction; confirm role definitions under AR-H-01. | Architecture / Security | Updated diagram reviewed against actor table, SRS authorization, and sequence diagrams; no actor relationship suggests authorization bypass. | Not Started |
| AR-M-04 | Medium | Organization-scoped AgentDefinition and ToolDefinition references are described as scope-constrained, but AgentRun and action records show direct definition-ID foreign keys rather than a demonstrated constraint that prevents referencing a definition owned by another organization. Global definitions are also permitted, so the valid global-versus-tenant rule is not fully encoded. | Cross-tenant workflow/tool definition selection could undermine isolation if the intended ownership invariant is not enforced. | Specify the valid global-versus-tenant reference rule and enforce it using suitable database keys/constraints or a clearly specified transactional application invariant; test global and cross-organization references. | AR-M-01 for run lifecycle; AR-H-02 for tool catalog scope; database design review. | Architecture / Backend / Security | Approved invariant; updated ER/schema design; integration tests reject tenant mismatch and accept only explicitly permitted global definitions. | Not Started |
| AR-M-05 | Medium | Escalation is required, but the analyst handoff/discovery mechanism and ownership are not specified. The SRS excludes assignment queues and external ticketing, while the user journey requires unresolved requests to reach human support. | Escalated requests could have no defined human discovery/ownership path, or implementation could accidentally add out-of-scope queue, notification, assignment, or SLA behavior. | Define the minimal manual MVP operating path for authorized analysts to discover and review escalated requests and identify operational ownership, without implying external routing, notifications, assignment queues, or service-level commitments. | AR-M-02 for escalation status; AR-H-01 for analyst identity/authorization. | Product / Architecture | Approved bounded operating procedure and user/analyst flow; updated use case/API only if needed; test or demo evidence that no out-of-scope integration is implied. | Not Started |
| AR-M-06 | Medium | The ticket-summary response example places an AI-generated summary and `ticketOutcomeStatus` in one result object without defining the status field's provenance. Other documents say request status is backend-authoritative and the summary is not a status update. | Client code may treat an AI response field as authoritative request state. | Specify the summary as AI-generated and identify any status as a separate server-read authoritative field or return it as a distinct backend-owned value; align response schema and UI presentation. | AR-M-01 and AR-M-02 for finalized status model. | Architecture / Backend / AI | Revised API schema/example; contract/UI tests prove AI output cannot change or impersonate authoritative status. | Not Started |
| AR-M-07 | Medium | Knowledge Agent retrieval technology is unresolved after the relational database changed to MariaDB 10.11.16. The prior vector-specific design does not apply; no separate vector database, managed search service, or temporary lexical approach has been evaluated sufficiently for selection. | The Knowledge Agent cannot have a finalized retrieval implementation or tested data/trust boundary. | Compare separate vector database, external vector-search service, and temporary non-vector retrieval against curated-corpus quality, pre-model tenant/permission filtering, permission revocation freshness, privacy/data handling, latency, cost, operations, and safe failure. Keep the decision Proposed/Deferred until evidence supports selection. | ADR-0008; provider/data handling decisions under AR-H-03; tenant-isolation design and tests. | Architecture / Security / AI / Data | Recorded evaluation with corpus/test evidence, trust-boundary and permission model, and a separately reviewed decision; no alternative marked Accepted without evidence. | Not Started |
| AR-L-01 | Low | Optional knowledge administration is described as adding, updating, and removing documents, but the API defines only conditional document creation. Optional feedback is modeled in requirements/use cases/data, but no feedback endpoint is defined. | If these MVP-Should features are enabled, clients and services lack complete supported contracts; implementing ad hoc behavior risks scope drift. | Decide whether each optional feature is off or on. If off, keep UI/routes disabled. If on, complete organization-scoped knowledge lifecycle/reindexing and feedback contracts, validation, audit, and tests—or narrow the optional use case consistently. | Product feature selection; AR-H-01 for protected admin/feedback access; AR-M-04 for tenant-scoped definitions if configuration is included. | Product / Architecture / Backend / Security | Recorded feature decision; if enabled, complete reviewed API/data contracts and tests; if disabled, evidence that capability is unavailable. | Not Started |
| AR-L-02 | Low | “Ticket” is used throughout endpoint and entity naming even though the product is explicitly an internal support request and not a live service-desk ticket. The distinction is documented but can still be lost in demos or client integrations. | Users, demo audiences, or future integrators may infer live external ticketing that does not exist in the MVP. | Preserve explicit “internal support request; no external ticket created” labels in API documentation, UI copy, and demos. Do not add a live connector or rename public contracts without an approved scope/compatibility decision. | None. | Product / Architecture | Reviewed terminology in user-facing copy, API descriptions, and demo script; no claim of external ticket creation. | Not Started |

## 9. Decisions Requiring Product Approval

These items require product decisions; none is approved by listing it here.

- Select or defer optional MVP-Should feedback and organization-admin knowledge/configuration (AR-L-01).
- Confirm the employee/analyst escalation experience and minimal human ownership model without adding external routing, assignment queues, notifications, or SLAs (AR-M-05).
- Confirm the expected ticket creation and analysis experience (synchronous or asynchronous) and user-visible lifecycle outcomes, in coordination with AR-M-01 and AR-M-02.
- Approve the exact set and user-visible descriptions of simulated actions, if any are to be enabled (AR-H-02).
- Approve the intended authentication experience for controlled demo users and role assignment expectations, jointly with Architecture and Security (AR-H-01).
- Before real organizational data is considered, approve the data classes and product use case subject to provider and privacy/security review (AR-H-03).

## 10. Decisions Requiring Architecture Approval

These items remain decisions to be recorded and reviewed; no implementation choice is finalized here.

- Authentication/session architecture, trusted claim/organization mapping, reviewer identity, and protected Spring Boot-to-FastAPI service identity and transport (AR-H-01).
- Canonical ticket/agent lifecycle, transition model, concurrency behavior, polling/idempotency, and authoritative-versus-AI response field boundaries (AR-M-01, AR-M-02, AR-M-06).
- Knowledge Agent retrieval alternative and its tenant/permission, freshness, privacy, and failure controls (AR-M-07).
- Use-case diagram actor and provider relationship corrections (AR-M-03).
- Global-versus-tenant AgentDefinition and ToolDefinition reference invariant and enforcement mechanism (AR-M-04).
- Approval expiry/invalidation, proposal integrity, policy revalidation, tool schemas, catalog governance, and disablement controls (AR-H-02).
- Data-provider and embedding-provider boundary, privacy/retention, secrets, database/backup, audit integrity/retention, monitoring, incident response, and deployment controls for the relevant stage (AR-H-03).
- Whether optional feature contracts remain disabled or are completed before enabling them (AR-L-01).

Any approved decision that changes normative requirements or scope must be reflected in the owning PRD/SRS/architecture/API documents through their normal review process. This plan itself is not that approval.

## 11. Security Readiness Checklist

The checklist is for implementation evidence, not a declaration that controls exist.

- [ ] Resolve AR-H-01; verify protected routes derive user, organization, and roles from authenticated server-side context.
- [ ] Verify deterministic Spring Boot authorization for every protected read, status transition, approval, and execution; do not delegate decisions to the LLM.
- [ ] Use synthetic/demo identities and data until AR-H-03 data-use conditions are satisfied.
- [ ] Verify at least two organizations across request, knowledge/vector retrieval, proposal, approval, audit, and workflow state; unauthorized cases are denied.
- [ ] Verify organization and document permission filters run before content reaches FastAPI/LangGraph/LLM.
- [ ] Test direct and indirect prompt injection; ticket/document content cannot alter identity, policy, workflow, permissions, or tool allow-list.
- [ ] Validate model output, citations, status values, and action parameters; classify AI recommendations separately from backend decisions.
- [ ] Keep tools disabled until AR-H-02 is closed; when enabled, prove explicit authorized human approval, exact proposal binding, expiry/invalidation, and execution-time revalidation.
- [ ] Verify no direct AI-to-tool path, live integration, or production credential is available in the MVP.
- [ ] Verify timeout, persistence/audit failure, replay, concurrency, ambiguous result, and safe retry paths do not falsely report success or create duplicate effects.
- [ ] Keep secrets outside source control, prompts, and routine logs; verify applicable configuration and log behavior.
- [ ] Resolve AR-H-03 before real organizational data or shared/external deployment; document residual risks and operating ownership.

## 12. Database Readiness Checklist

- [ ] Use MariaDB 10.11.16 as primary relational source of truth with XAMPP locally and phpMyAdmin for management. Keep Knowledge Agent retrieval open until AR-M-07 is resolved.
- [ ] Keep all MVP tenant-owned records organization-scoped and enforce tenant-consistent foreign relationships.
- [ ] Resolve and test the global-versus-tenant definition invariant (AR-M-04).
- [ ] Implement the canonical request/run statuses and state transitions only after AR-M-01 and AR-M-02 are resolved.
- [ ] Preserve proposal, approval, tool execution, request, and audit linkage; verify transaction and duplicate/concurrency behavior before action enablement.
- [ ] Compare the separate vector database, external vector-search service, and temporary non-vector options before selecting retrieval; do not assume vector compatibility or persist embeddings without an approved design.
- [ ] Keep feedback and optional administration records/capabilities conditional on the product decision (AR-L-01).
- [ ] Do not persist plaintext passwords or unapproved secrets. Define database privileges, migrations, backup/restore, and lifecycle before real data (AR-H-03).
- [ ] Define retention/deletion before real employee data; no duration is established by this plan.
- [ ] Treat database schema and local synthetic fixtures as unverified until migrations, integrity, tenant, retrieval, and failure tests pass.

## 13. API Readiness Checklist

- [ ] Keep public business contracts on Spring Boot; keep FastAPI internal and unavailable to the browser.
- [ ] Resolve authentication and internal caller protection before exposing protected public/internal routes (AR-H-01).
- [ ] Align ticket creation, AI analysis, run polling, and initial status in one contract (AR-M-01).
- [ ] Enumerate legal status transitions, initiating role/event, concurrency behavior, and error states (AR-M-02).
- [ ] Define an authorized analyst discovery path for escalation without promising external routing or assignment (AR-M-05).
- [ ] Keep recommendation, policy decision, human decision, authoritative ticket status, and tool result as distinct contract semantics; correct summary/status provenance (AR-M-06).
- [ ] Preserve explicit simulation labels, approval preconditions, and truthful execution status; keep action contracts unavailable until AR-H-02 passes.
- [ ] Set request size/rate limits, timeout budgets, idempotency behavior, and ambiguous-outcome handling before exposure; do not invent values without approval.
- [ ] If optional knowledge administration or feedback is enabled, complete its lifecycle and endpoint contracts first; otherwise keep endpoints/UI disabled (AR-L-01).
- [ ] Preserve “internal support request; no external ticket created” in API and UI wording (AR-L-02).
- [ ] Derive API contract tests from the reviewed SRS and revised contracts; no endpoint in the design is evidence of an implemented endpoint.

## 14. Development Environment Prerequisites

Use the agreed stack and the local-development posture already documented. This section does not install or select packages, provider models, or deployment services.

- Java 21+ and Maven for the Spring Boot modular business backend.
- Python environment for FastAPI and LangGraph.
- Frontend development environment for React, TypeScript, Tailwind CSS, Axios, and optional Recharts; exact frontend runtime/tool versions remain an implementation choice.
- MariaDB 10.11.16 via XAMPP for local synthetic/demo relational persistence; phpMyAdmin is the management interface. Retrieval tests use only an explicitly approved approach and synthetic data.
- Agreed test tools by layer: JUnit, Pytest, Vitest, and Playwright, as applicable to implemented slices.
- OpenAPI/Swagger artifacts for public Spring Boot contracts and a separately controlled internal FastAPI contract.
- Synthetic/demo users, organizations, and curated knowledge only; no real employee data or credentials in fixtures.
- Secrets/configuration kept outside source control, prompts, and routine logs.
- Local development must not depend on Docker or Redis. Docker remains planned for a later phase; Redis is optional only if measurement justifies it.
- LLM and embedding providers remain undecided. Do not configure production or real-data provider traffic as a prerequisite for scaffolding.
- No shared deployment, production-like networking, or observability backend is implied by local setup.

## 15. Implementation Entry Criteria

### May begin as low-risk, non-exposed scaffolding

These activities can proceed without resolving every runtime decision, provided they use no protected data and expose no protected/action-bearing paths:

- Project/module scaffolding for the agreed frontend, Spring Boot backend, and Python AI service boundaries.
- Static UI shell and synthetic mock screens for response labeling, status, escalation, and approval review.
- Test harness setup and synthetic fixtures, including two organizations and differing permissions.
- Logical schema/migration prototyping with synthetic data, while leaving embedding dimensions and definition-scope enforcement explicit and unfinalized.
- API contract tests and local interface stubs that do not expose FastAPI publicly or call a real LLM provider.
- Documentation corrections and decision records for the findings in Section 8.

These are not approvals to process real user requests or enable a capability. Authentication may be prototyped only in a local test context until AR-H-01 is decided; AI/RAG may use test doubles until trusted boundaries and provider decisions are resolved.

### Entry criteria before protected MVP workflows

- AR-H-01 has an approved design and implemented, tested identity/session and internal service boundary.
- AR-M-01 and AR-M-02 have a canonical persisted request/run lifecycle and legal transition model; AR-M-05 defines the escalation operating path.
- AR-M-03 diagram relationships are corrected; AR-M-04 scope invariant is enforced/tested; AR-M-06 provenance is explicit.
- Server-side authorization, tenant scope, and permission-before-model retrieval pass the relevant security and integration tests.
- AI output validation, source verification, fact/suggestion labeling, safe timeout, and truthful failure behavior are verified for synthetic/demo use.
- Required audit events and critical persistence failure behavior are verified for the implemented workflow.
- Any enabled capability has passed its own acceptance tests; unverified capability remains disabled.

### Entry criteria before simulated action execution

- AR-H-02 is closed with an approved allow-list, schema/effects, approval validity/invalidation, ownership, and disablement decision.
- All protected workflow entry criteria above are met.
- Tests prove unauthorized actors, missing/rejected/expired/changed approvals, policy changes, replay, concurrency, tool timeout, and persistence/audit failures result in no unauthorized or duplicate effect.
- The only reachable effects are simulated, and the UI clearly communicates that fact.

### Entry criteria before real organizational data or shared/external deployment

- AR-H-03 is resolved for the intended data and environment, with security/privacy/product/architecture approvals and implementation evidence.
- Provider data terms, retention/deletion, secrets, database/network/backup protections, audit/monitoring, rate/size limits, incident response, and operational ownership are defined and tested as applicable.
- This gate does not approve production use; any such use requires separate readiness review and authorization.

## 16. Deferred Items and Their Risks

| Deferred item | Why deferred / current boundary | Risk while deferred |
|---|---|---|
| LLM provider and provider data terms | Provider is undecided; synthetic/demo data remains the default. | No real organizational content can be approved for provider processing; model behavior/latency remains unmeasured. |
| Knowledge Agent retrieval approach | Compare separate vector database, external vector-search service, and temporary non-vector retrieval under AR-M-07. | No retrieval architecture is accepted; do not assume MariaDB vector compatibility or persist embeddings against an unselected design. |
| Feedback and administrator-managed knowledge/configuration | MVP-Should; enable only after product selection and complete contracts. | No in-product feedback or administration may be assumed; curated content can remain controlled outside the optional feature. |
| Redis | Not required unless measurement justifies it. | Do not assume caching behavior or use it for authorization/source-of-truth state. |
| Docker/containerization | Planned for a later phase and not needed for local MVP development. | Shared deployment topology remains undefined. |
| Live enterprise integrations | Explicitly outside MVP. | No live ticket, identity, device, email, account, or access mutations; any future integration needs separate approval and review. |
| Production SLAs, certifications, compliance, and production readiness | Not established by requirements or documentation. | Prototype evidence must not be presented as such assurance. |
| Audit retention/immutability and external observability | Decisions and deployment controls remain open. | Do not claim immutable logs, a selected SIEM/collector, retention guarantees, or production monitoring. |

Deferred means not selected or not in scope; it does not authorize an implicit default or an unreviewed substitute.

## 17. Implementation Phase Dependencies

| Phase | Scope and deliverables | Entry/dependency gate | Exit evidence |
|---|---|---|---|
| 0. Readiness decisions and document alignment | Resolve applicable AR-H/AR-M items; correct diagram; settle lifecycle, actor, scope, and provenance contracts; record explicit decisions and owners. | No code-level protected flow required. Decisions remain open until approved. | Reviewed decision records and aligned requirements/API/sequence/data model for the planned slice. |
| 1. Local foundation | Agreed project scaffolding, static UI, module boundaries, synthetic fixtures, build/test harness, XAMPP/MariaDB 10.11.16 local setup and phpMyAdmin management. | Phase 0 may proceed in parallel for non-exposed work; local synthetic-only boundary. | Repeatable local build/test; no public protected route or real provider call required. |
| 2. Identity, authorization, request lifecycle, persistence | Controlled demo identity, trusted organization context, deterministic Spring Boot policy, tenant-scoped request records, legal status transitions, analyst escalation discovery. | AR-H-01, AR-M-01, AR-M-02, AR-M-05; AR-M-04 for tenant/global references. | Authz and two-tenant integration tests, transition tests, accurate request status and escalation demonstration. |
| 3. Bounded AI and permission-aware RAG | Internal Spring Boot-to-FastAPI contract, configured LangGraph steps, synthetic curated knowledge, pre-model permission filters, response/citation validation, safe errors. | Phase 2 identity/tenant controls; AR-H-01 internal service trust; AR-M-07 retrieval decision before selecting a retrieval implementation; provider decision before real provider-bound data; AR-M-01 canonical async/sync mode. | Contract, injection, source-grounding, timeout/failure, and permission-isolation tests; no AI authority or tool path. |
| 4. Audit and operational review | Correlated event persistence, scoped audit reads, minimized content, audit failure behavior, prototype outcome metrics. | Request and AI workflow definitions; protected audit access requires AR-H-01. | Authorized workflow reconstruction and audit-failure tests; unresolved retention is documented and real data remains gated. |
| 5. Approval and simulated tools | Exact catalog, proposal validation/fingerprint, authorized human decision, expiry, policy revalidation, simulation dispatcher, duplicate protection. | AR-H-02 plus relevant AR-H-01/03 controls, AR-M-02, AR-M-04, and passing authorization/audit foundations. | End-to-end tests prove approval integrity, simulated-only effects, safe retries/timeouts, no direct AI execution, and audit correlation. |
| 6. Controlled prototype demonstration | Integrate the accepted employee-to-answer/escalation path and, only if gate passed, the approved simulation path; preserve synthetic/demo data by default. | Prior phases' applicable exit evidence; optional features explicitly selected or disabled. | SRS acceptance evidence for the demonstrated scope; truthful limitations and no production-readiness claim. |
| 7. Real-data/shared deployment consideration | Review provider, privacy, data lifecycle, deployment/database/secret/audit/operations controls and perform a separate readiness assessment. | AR-H-03 and all relevant high-risk mitigations implemented and verified; explicit stakeholder approval. | Separate documented approval and evidence for the proposed environment; not granted by this plan. |

Phases express dependencies, not calendar commitments. A later phase may not bypass an unmet gate by changing terminology or treating a mock decision as authorization.

## 18. Final Readiness Assessment

**Planning readiness:** Conditionally ready. The consistent product boundary, stack, component responsibilities, and security principles support planning and low-risk scaffolding with synthetic data.

**Implementation readiness:** Not ready for unrestricted implementation of protected or action-bearing workflows. Close AR-H-01 and the applicable lifecycle, actor, tenant-scope, escalation, and provenance issues before exposing dependent paths. Keep simulated execution disabled until AR-H-02 and its verification evidence are complete.

**Real-data/shared deployment readiness:** Not ready. AR-H-03 remains unresolved; synthetic/demo data is the boundary until the applicable provider, privacy, retention, secret, database, audit, deployment, and operational controls are approved and evidenced.

**Production readiness:** Not assessed and not claimed. A successful prototype or passing test suite does not establish production readiness, certification, or compliance.

Release/readiness status must be updated only against recorded decisions and implementation evidence. All 12 findings remain **Not Started** at the time this plan is issued; no unresolved decision is represented as finalized.
