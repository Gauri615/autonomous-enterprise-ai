# Architecture Review: Autonomous Enterprise AI System

## 1. Document Control

| Field | Value |
|---|---|
| Document | Cross-document Architecture Review |
| Version | 0.1 |
| Status | Draft review |
| Review date | 2026-11-13 |
| Reviewer role | Principal Enterprise Architect |
| Scope | MVP product, logical architecture, data model, API contracts, and security boundaries |
| Readiness statement | Architecture documentation review only; no implementation or deployment controls were assessed |

This is a design review of the documents listed in Section 3. It does not certify implementation, security, production readiness, compliance, or operational effectiveness.

## 2. Review Objectives

- Assess whether the project documents express one coherent MVP and preserve the approved product boundaries.
- Compare component responsibilities, runtime flows, data relationships, APIs, security controls, and traceability across documents.
- Identify confirmed inconsistencies separately from missing specifications and intentionally unresolved decisions.
- Recommend corrective actions and state the conditions for implementation and later capability enablement.

Severity describes the architecture impact if a gap is left unresolved, not a measured exploitability rating. A readiness gate is not evidence that an implementation vulnerability exists.

## 3. Documents Reviewed

1. [PRD.md](../product/PRD.md) — product scope and priorities.
2. [SRS.md](../product/SRS.md) — normative system requirements and acceptance criteria.
3. [USE_CASES.md](../product/USE_CASES.md) — actor and workflow detail.
4. [THREAT_MODEL.md](../security/THREAT_MODEL.md) — STRIDE and AI/security threats.
5. [SYSTEM_ARCHITECTURE.md](./SYSTEM_ARCHITECTURE.md) — component, trust-boundary, and technology design.
6. [SEQUENCE_DIAGRAMS.md](./SEQUENCE_DIAGRAMS.md) — runtime interactions and failure paths.
7. [DATABASE_ER_DIAGRAM.md](./DATABASE_ER_DIAGRAM.md) — MariaDB 10.11.16 logical model and unresolved Knowledge Agent retrieval.
8. [API_CONTRACTS.md](../api/API_CONTRACTS.md) — proposed public and internal API contracts.

The review compared the full document structures and the portions governing product scope, requirements, responsibilities, flows, entities, API behavior, security controls, and open decisions. Identifier checks found 184 distinct SRS requirement IDs and 15 use-case IDs; all literal references resolve to definitions. The threat model contains 21 unique threat-register IDs.

## 4. Product Scope Consistency

**Assessment: Consistent.**

The PRD, SRS, use cases, threat model, architecture, sequence diagrams, database model, and API contracts consistently describe an internal IT support request workflow, not a live service-desk integration. The MVP uses demo identities and curated demo knowledge, with external actions limited to allow-listed simulations. Feedback and administrator-managed knowledge/configuration remain conditional MVP-Should capabilities. Redis is optional, Docker is deferred, and the LLM provider is undecided.

The use of “ticket” in API paths and data names is explicitly defined as an internal request in the SRS and supporting documents. Preserve that clarification in user-facing and API descriptions to avoid implying an external service-desk connection.

## 5. Technology Stack Consistency

**Assessment: Consistent.**

- React, TypeScript, Tailwind CSS, Axios, and optional Recharts remain the frontend stack.
- Java 21+ / Spring Boot is the authoritative business API and policy boundary.
- Python / FastAPI is an internal AI-processing service; LangGraph coordinates bounded workflows.
- MariaDB 10.11.16 is the relational system of record; XAMPP/phpMyAdmin are the local environment and management interface.
- Knowledge Agent retrieval is not selected; ADR-0008 records the alternatives requiring evaluation.
- OpenAPI/Swagger, OpenTelemetry, and GitHub Actions are included as documented, with collector/exporter and deployment details still open.
- Redis is not required for correctness; Docker is not required for initial local development.
- No final LLM provider, new broker, database, gateway, or microservice is selected.

## 6. Component Architecture Consistency

**Assessment: Substantially consistent, subject to the actor-diagram correction in Finding AR-M-03.**

System Architecture, Sequence Diagrams, Database ER Diagram, and API Contracts consistently place business state, trusted identity context, deterministic authorization, approval state, and simulated execution under Spring Boot. FastAPI/LangGraph return bounded, untrusted recommendations or proposals. The database and API mapping support request, workflow, knowledge, approval, execution, and audit records.

The use-case diagram alone assigns some system use cases to actors in a way that conflicts with the detailed actor definitions. This is a documentation-model issue, not evidence that the implementation has such an authority path.

## 7. Sequence Flow Consistency

**Assessment: Security gates are consistent; ticket processing mode and lifecycle remain underspecified.**

The reviewed sequences show server-side identity and policy checks, permission filtering before AI/model input, proposal validation, explicit authorized human review, execution-time revalidation, simulated-only invocation, and truthful failure behavior. No sequence shows a direct agent-to-tool authorization or execution path.

Ticket creation is shown awaiting AI processing before returning a result, while the API also defines an asynchronous analysis initiation contract and leaves processing mode open. The documents do not yet establish one canonical initial request lifecycle. See Finding AR-M-01.

## 8. Database Model Consistency

**Assessment: Core workflows are represented; tenant-scope integrity for shared/tenant definitions needs a precise invariant.**

The logical model includes all 16 requested entities and supports tenant-scoped users/roles, internal tickets/comments, permission-bearing knowledge documents/chunks, agent runs/events, approvals, simulated executions, audit events, and optional feedback. It preserves proposal/approval/execution linkage, avoids plaintext password and secret fields, and describes MariaDB-compatible relational persistence. Knowledge Agent retrieval remains unselected and requires an evaluated security/quality design (AR-M-07).

Most tenant-owned relationships are modeled with organization-aware keys. References from runs and action records to definitions use definition IDs while definitions may be global or tenant-owned. The model describes a scope rule but does not show a database relationship that enforces it in all cases. See Finding AR-M-04.

## 9. API Contract Consistency

**Assessment: Public/internal service responsibilities and security boundaries are consistent; several lifecycle contracts need decisions before implementation.**

Public business operations terminate at Spring Boot; FastAPI is private and is called through an internal contract. API examples distinguish AI recommendations from policy, human decisions, and tool results. The API-to-database mapping is broadly consistent with the ER model.

The unresolved ticket create/analysis mode, the missing explicit status transition graph, and the combined AI-summary/backend-status response shape should be resolved before implementing those contracts (Findings AR-M-01, AR-M-02, and AR-M-06). Optional knowledge administration and feedback contracts are incomplete if those features are enabled (Finding AR-L-01).

## 10. Security and Threat Model Consistency

**Assessment: Security principles align; controls are requirements, not verified implementation.**

Across the reviewed documents:

- LLM and agent output is untrusted and cannot authorize protected operations.
- Spring Boot deterministic policy is authoritative.
- Human approval is required for every enabled MVP simulation, and policy is revalidated before execution.
- Tools are allow-listed, schema-validated, simulated only, and have no live enterprise side effects.
- Knowledge access is filtered by organization and user permissions before content reaches AI/model processing.
- Ticket descriptions and retrieved documents are treated as untrusted.
- Sensitive workflow decisions and outcomes are auditable; secret storage, retention, and operational protections are not claimed as implemented.

The threat model appropriately treats high-risk mitigations as enablement gates. No direct AI-to-tool path was found in the component, orchestration, human-approval, tool-execution, or API flow descriptions.

## 11. Authentication and Authorization Review

**Assessment: Authority boundaries are well stated; identity and service trust are not yet implementable as a final contract.**

The SRS requires controlled demo identities, server-resolved user/organization context, and deterministic per-operation authorization. Reviewer, administrator, audit reviewer, and operator responsibilities are separated conceptually. The API correctly leaves the login proof and session/token format abstract.

The JWT direction does not settle issuer, claim mapping, lifecycle, browser handling, provisioning, or revocation. Spring Boot-to-FastAPI service identity and transport protection are also unresolved. These are explicit high-priority readiness gates, not confirmed defects. See Finding AR-H-01.

## 12. Organization Data Isolation Review

**Assessment: Isolation is consistently required and logically supported; implementation evidence is absent.**

All documents require trusted organization context, server-side record authorization, organization-scoped persistence and retrieval, and at least two-organization denial tests. The database model uses organization references and composite keys for many tenant-owned relationships. Knowledge filters are required before model input, not after generation.

The definition-reference scope invariant needs clarification as described in Finding AR-M-04. Actual query, retrieval, cache (if later added), and database enforcement must be demonstrated by tests; documentation alone does not establish isolation.

## 13. AI Agent Governance Review

**Assessment: Consistent.**

LangGraph is a bounded workflow coordinator, not an authorization principal. The AI service may classify, retrieve eligible context through the authorized boundary, compose a recommendation, summarize, or propose a configured simulation. Backend validation controls what is presented or persisted. Agents cannot set trusted identity, update authoritative status, approve proposals, or invoke tools.

The use-case diagram's LLM actor relationships should be corrected to reflect its supporting-provider role and mediated calls (AR-M-03).

## 14. Human Approval and Tool Execution Review

**Assessment: Control sequence is coherent; sensitive action enablement remains gated.**

The flow is consistently defined as proposal validation, deterministic policy evaluation, exact proposal review, explicit authorized human decision, proposal-bound/expiring approval, execution-time revalidation, simulated invocation, persisted outcome, and audit. Approval alone never authorizes execution.

The numeric approval validity period and exact enabled tool catalog/governance are unresolved. The Threat Model explicitly says affected action paths must remain disabled until these conditions and tests are satisfied. See Finding AR-H-02.

## 15. Audit and Observability Review

**Assessment: Required event content and access scope are consistent; operational guarantees remain open.**

The documents identify request, workflow, retrieval-reference, policy, approval, configuration (when enabled), and simulated execution events, with actor/service, organization, time, outcome, and correlation context. They consistently require minimized content and tenant-scoped audit access.

Retention, deletion, tamper-resistance, backup/restore, external monitoring, incident response ownership, and telemetry export are not selected. No immutable audit or production monitoring capability is claimed. These decisions and data-use gates are captured in AR-H-03 and Section 19.

## 16. Requirement Traceability Review

**Assessment: Identifier references resolve; traceability matrices are present across the SRS, architecture, sequences, database, threat model, and API contract.**

- Automated literal-ID comparison found 184 distinct SRS requirement identifiers referenced across the reviewed set; each matched a defined SRS requirement. No duplicate SRS requirement definitions were found.
- All 15 use-case identifiers are defined in the Use Cases document and resolve where referenced.
- The threat register contains 21 unique IDs; its traceability table maps threat groups to SRS requirements.
- The SRS matrix maps all PRD FR-001–FR-020 and NFR-001–NFR-014; the documents preserve PRD “Should” priority for feedback and administrator configuration.

Passing identifier checks does not prove that every implementation behavior has a sufficiently precise requirement. The unresolved status-transition rules and optional-feature API gaps remain as recorded below.

## 17. Identified Inconsistencies

### Critical

**None identified.** In particular, no confirmed direct AI-to-tool execution or authorization path was found in the reviewed architecture and runtime descriptions.

### High

No confirmed contradictory requirement was found. The following explicit unresolved decisions are high-priority readiness gates; they are not claims of existing implementation vulnerabilities.

| ID | Issue | Evidence | Corrective action |
|---|---|---|---|
| AR-H-01 | Authentication and internal service trust are not selected. Authentication/session semantics, trusted identity mapping, and Spring Boot-to-FastAPI authentication/transport controls remain open across the SRS, architecture, threat model, and API contract. | SRS Sections 11 and 29; System Architecture Sections 17, 19, and 26; Threat Model Section 22; API Contracts Sections 6, 22, and 31. | Approve a controlled-demo identity/session design, user/organization/role mapping, and protected internal service identity/transport. Update the contracts and tests. Do not expose protected services until the chosen controls are implemented and verified. |
| AR-H-02 | The approval expiry duration and the exact simulation allow-list/governance remain unresolved, although every enabled action must require bounded, proposal-bound human approval. | PRD Sections 10 and 18; SRS SRS-HITL-006 and SRS-TOOL-001–010; Threat Model Sections 15 and 19; System Architecture Section 26; Database ER Diagram Section 27; API Contracts Section 31. | Decide the bounded validity period, invalidation rules, exact demo tools/schemas/effects, and configuration/disablement owner. Keep execution disabled until policy, persistence, audit, replay, and concurrency tests pass. |
| AR-H-03 | Real-data and deployment protections are not selected: LLM provider/data terms, retention/deletion, secrets handling, database protections, audit integrity/retention, and incident/operational ownership remain open. | PRD NFRs and Section 21; SRS SRS-NFR-003/014 and SRS-SEC-008–012; Threat Model Sections 17–22; System Architecture Sections 22 and 26; Database ER Diagram Sections 22–23 and 27; API Contracts Section 31. | Continue with synthetic/demo data only. Before real organizational data or shared deployment, complete provider/privacy review, retention, secret/key, database/backup, audit/monitoring, and operational-response decisions; evidence controls and tests. |

### Medium

| ID | Issue | Evidence | Corrective action |
|---|---|---|---|
| AR-M-01 | Ticket creation and analysis have incompatible-looking timing presentations: the ticket-creation sequence waits for AI and returns an outcome, while the API defines a separate `202` analysis/run flow and leaves synchronous/asynchronous behavior open. Initial/processing status vocabulary is also unresolved. | Sequence Diagrams Sections 5–6 and 19; API Contracts Sections 9, 13, 15–16, and 31; Database ER Diagram Sections 9 and 11. | Choose one canonical create/analysis contract (including persisted initial state, status polling, deadline behavior, idempotency, and failure outcome). Align the ticket sequence, API examples, and database status mapping. |
| AR-M-02 | The documents define allowable outcome values but not a complete state-transition table. The API PATCH contract accepts one “allowlisted” `outcomeStatus` without enumerating legal source-to-destination transitions and actor permissions for each transition. | SRS Sections 14 and 27; Use Cases UC-011; Database ER Diagram Sections 9 and 21; API Contracts Section 13. | Define a backend-owned state machine with initial/intermediate/terminal states, legal transitions, initiating role/event, concurrency behavior, and approval interaction. Add negative transition tests before status APIs are implemented. |
| AR-M-03 | The Use Case Diagram gives the Platform Operator direct associations to policy evaluation and tool execution, and the Administrator a direct audit-review association; the detailed use-case actors and SRS distinguish these capabilities and require explicit audit/reviewer authorization. It also draws the LLM as a direct actor for agent use cases rather than a provider mediated by FastAPI/LangGraph. | Use Cases Sections 4–6 and UC-008–UC-010/UC-013; SRS SRS-AUTHZ-004–006; System Architecture Sections 6–7; Sequence Diagrams Sections 9–11. | Correct the diagram to show operator configuration only, audit access only for an explicitly authorized reviewer, and LLM interaction mediated through the AI service. Retain the textual boundary that only Spring Boot gates tools. |
| AR-M-04 | Organization-scoped AgentDefinition and ToolDefinition references are described as scope-constrained, but AgentRun and action records show direct definition-ID foreign keys rather than a demonstrated constraint that prevents referencing a definition owned by another organization. Global definitions are also permitted, so the valid global-versus-tenant rule is not fully encoded. | Database ER Diagram Sections 11–12, 16–18, and 21; API Contracts Section 29. | Specify the allowed global/tenant reference invariant. Enforce it with suitable keys/constraints or a clearly defined transactional application invariant, then test cross-tenant and global-definition cases. |
| AR-M-05 | Escalation is required, but the analyst handoff/discovery mechanism and ownership are not specified. The SRS excludes assignment queues and external ticketing, while the user journey requires unresolved requests to reach human support. | PRD FR-013 and Sections 10/15; SRS SRS-TKT-003–004 and Section 14; Use Cases UC-002/UC-003/UC-011; API Contracts Sections 13 and 21. | Define a minimal MVP operating path (for example, an authorized analyst view of escalated requests and who monitors it), without implying external routing, notification, assignment, or SLA capabilities. |
| AR-M-06 | The ticket-summary response example places an AI-generated summary and `ticketOutcomeStatus` in one result object without defining the status field's provenance. Other documents say request status is backend-authoritative and the summary is not a status update. | Use Cases UC-012; Sequence Diagrams Section 12; API Contracts Section 21. | Define `ticketOutcomeStatus` explicitly as a server-read authoritative field or return it separately from the AI summary. Keep AI output and backend status provenance distinguishable in schema and UI. |
| AR-M-07 | Knowledge Agent retrieval technology is unresolved after the relational database changed to MariaDB 10.11.16. The prior retrieval design no longer applies; no separate vector database, managed search service, or temporary lexical approach has been evaluated sufficiently for selection. | ADR-0003 (superseded), ADR-0008; PRD FR-003/FR-015; SRS SRS-DB-002 and SRS-RAG-001–009; System Architecture Sections 13 and 26; Database ER Diagram Sections 20 and 27; Threat Model Sections 5 and 12. | Compare the three alternatives using the curated corpus and documented criteria for retrieval quality, pre-model tenant/permission filtering, permission revocation freshness, privacy/data handling, latency, cost, operations, and safe failure. Keep the decision Proposed/Deferred until evidence supports selection; preserve backend authorization and citation validation. |

### Low

| ID | Issue | Evidence | Corrective action |
|---|---|---|---|
| AR-L-01 | Optional knowledge administration is described as adding, updating, and removing documents, but the API defines only conditional document creation. Optional feedback is modeled in requirements/use cases/data, but no feedback endpoint is defined. | PRD FR-016–017; SRS SRS-FR-016–017; Use Cases UC-014–015; Database ER Diagram Sections 14–15; API Contracts Sections 17, 29–31. | Keep these capabilities disabled unless selected. If enabled, add scoped lifecycle/feedback contracts and tests—or explicitly narrow the supported optional behavior and update its use case consistently. |
| AR-L-02 | “Ticket” is used throughout endpoint and entity naming even though the product is explicitly an internal support request and not a live service-desk ticket. The distinction is documented but can still be lost in demos or client integrations. | SRS Section 14; Database ER Diagram Section 9; API Contracts Section 13; PRD Section 10. | Preserve explicit “internal support request; no external ticket created” labels in API documentation, UI copy, and demos. |

## 18. Missing Specifications

The following details are required for an implementation-ready baseline or for enabling the corresponding optional capability:

1. Approved identity/session contract, demo identity provisioning, organization mapping, role assignment source, and internal service authentication.
2. A single ticket/agent lifecycle vocabulary and legal transition table, including initial state, polling behavior, and how approval/rejection/tool outcomes affect request state.
3. A clear analyst escalation discovery/ownership process that does not imply an unapproved queue, notification, or service-level commitment.
4. Approval expiry configuration, tool catalog contents and schema versions, proposal canonicalization/fingerprint rules, and disablement/change ownership.
5. Formal data contracts for separation of AI recommendation, deterministic policy result, human decision, authoritative ticket state, and simulated tool outcome.
6. Scope-integrity constraints for optional tenant-specific agent/tool definitions, in addition to global definitions.
7. If optional features are enabled: knowledge document update/removal/reindexing contracts; feedback submission contract and evaluation scale; and role/action configuration lifecycle.
8. Before real data or shared deployment: provider terms and retention, data retention/deletion, secret/key management, database/network/backup controls, audit integrity/access, rate/size limits, incident response, and operational ownership.

These are not all mandatory MVP product features. They are specification gaps for the applicable implementation or enablement path.

## 19. Unresolved Architecture Decisions

The following decisions are recorded as open rather than assumed:

- Authentication protocol, demo identity source, JWT issuer/claims/lifecycle or alternative session mechanism, browser storage, user provisioning, and reviewer-role assignment.
- Spring Boot-to-FastAPI service identity, authentication, transport protection, private network exposure, and deployment topology.
- LLM/embedding provider and model, data handling/retention terms, provider-bound fields, model behavior evaluation, and embedding dimensions.
- Knowledge Agent retrieval: separate vector database, external vector-search service, or temporary non-vector retrieval; no alternative is accepted pending technical and security evaluation.
- Ticket create/analysis synchronous or asynchronous contract, initial/intermediate status vocabulary, and completion/polling behavior.
- Legal request-state transitions and analyst escalation operating model.
- Numerical approval validity, exact allow-listed simulations, proposal fingerprint/canonicalization, policy versioning, and disablement ownership.
- Tenant/global definition scope enforcement strategy and knowledge-permission JSON shape.
- Conversation, knowledge, embedding, feedback, and audit retention/deletion; database roles, encryption/key management, backups, restore, and migration process.
- API request limits, rate/concurrency limits, idempotency-key retention, per-service timeouts, and ambiguous simulation recovery.
- OpenTelemetry exporter/collector, retention, alerting/incident ownership, and optional feature enablement.
- Redis and Docker remain optional/later-phase decisions; neither is an MVP correctness dependency.

## 20. Architecture Risks

| Risk | Impact if not controlled | Required response |
|---|---|---|
| Identity or internal service impersonation | Unauthorized data access or untrusted AI-service calls | Resolve AR-H-01 and test identity, tenant mapping, and service boundary before exposure. |
| Approval or tool configuration is incomplete | Stale, confusing, or unauthorized simulated action flow | Resolve AR-H-02; disable affected simulations until verified. |
| Provider/data lifecycle is unknown | Sensitive data may be exposed or retained outside intended controls | Synthetic/demo data only until AR-H-03 is resolved. |
| State and response contracts diverge | Duplicate analysis, inaccurate status, or user-facing success/failure ambiguity | Resolve AR-M-01, AR-M-02, and AR-M-06 before implementation of affected APIs. |
| Tenant-specific definition reference is mis-scoped | Wrong workflow or tool configuration may be selected across organizations | Resolve AR-M-04 and add database/application isolation tests. |
| Retrieval alternative is selected without adequate isolation evidence | Unauthorized or stale knowledge candidates could reach the AI service/model | Resolve AR-M-07; test retrieval quality, permissions, revocation freshness, source validation, and external trust boundaries before enabling the chosen path. |
| Escalation is not operationally visible | Employee request can be marked escalated without a defined human follow-up path | Resolve AR-M-05 for the MVP operating model. |
| Documentation is mistaken for delivered controls | Stakeholders may infer security or readiness not established by code/tests | Track implementation evidence and do not claim production readiness or certification. |

## 21. Recommendations and Corrective Actions

1. Treat the SRS as the normative functional source and update dependent diagrams/contracts after decisions; do not silently change PRD scope.
2. Close AR-H-01 through AR-H-03 as explicit architecture/security approvals with owners and evidence. Keep real data, shared exposure, and action execution behind their respective gates.
3. Resolve AR-M-01 and AR-M-02 in one canonical lifecycle contract before implementing create, analysis, status, or polling endpoints.
4. Correct the use-case diagram (AR-M-03) and make sure no operator, AI, or provider relationship can be read as authorization.
5. Resolve AR-M-04 with schema-level or transactional enforcement and automated tenant-isolation tests.
6. Define a manual, bounded escalation operating process (AR-M-05) and separate authoritative ticket state from AI summaries (AR-M-06).
7. Decide whether optional administration and feedback are off for the MVP. If on, complete AR-L-01's API/lifecycle contracts before enabling them.
8. Build the first vertical slice with synthetic data and mocked/simulated effects; add required automated tests for auth, two-tenant isolation, RAG, prompt injection, approval integrity, audit failure, timeout, and duplicate execution.
9. Resolve AR-M-07 before committing to a retrieval implementation; a temporary approach, if used, must be explicitly approved and tested without weakening permission filtering.
10. Record design decisions and evidence in the owning architecture/security documents; this review does not itself approve an open decision.

## 22. Architecture Readiness Assessment

**Overall: Conditionally ready for implementation planning; not ready for unrestricted implementation, shared deployment, real organizational data, or production use.**

The product boundary, agreed relational database (MariaDB 10.11.16), component authority boundaries, permission-aware retrieval principle, deterministic policy, human approval flow, simulated-only MVP actions, and audit requirements are sufficiently consistent to guide planning. No critical cross-document contradiction or direct AI-to-tool path was identified. The retrieval principle is established, but the technology remains unresolved under AR-M-07.

The architecture is not implementation-ready for protected or action-bearing flows until identity/service trust, lifecycle/status behavior, approval expiry, tool catalog governance, and related corrections are resolved. Production implementation of Knowledge Agent retrieval also requires resolution of AR-M-07. It is not ready for real employee data or shared/external deployment until provider, retention, secret, database, audit, and operational controls are selected and evidenced. Passing documentation and identifier checks does not demonstrate that any control is implemented.

### Checklist before implementation begins

- [ ] Approve the demo authentication/session design, trusted user/organization/role mapping, and Spring Boot-to-FastAPI service identity/protection.
- [ ] Choose one canonical ticket creation and AI-analysis interaction, with persisted initial state, async/sync behavior, timeouts, polling, and idempotency.
- [ ] Approve the request status vocabulary and role/event-based transition matrix.
- [ ] Define how analysts discover and own escalated requests in the controlled MVP demonstration.
- [ ] Set approval expiry/invalidation rules and approve the exact simulated tool allow-list, schemas, expected effects, and configuration owner.
- [ ] Confirm the global-versus-tenant definition reference invariant and enforce/test it.
- [ ] Separate AI-generated summary/recommendation data from authoritative status, policy, human decision, and execution result in contracts and UI.
- [ ] Correct the use-case diagram actor relationships; confirm that optional admin and feedback features are either disabled or fully contracted.
- [ ] Resolve AR-M-07 through evidence-based comparison before selecting Knowledge Agent retrieval; ensure tenant/permission filtering, revocation freshness, source validation, and failure tests cover the selected approach.
- [ ] Baseline API schema/version, input limits, rate limits, timeout budgets, idempotency retention, and safe ambiguous-execution recovery.
- [ ] Define implementation evidence and automated acceptance tests for authentication/authorization, two-tenant isolation, permission-aware RAG, prompt injection, approval integrity, audit failure, and duplicate/timeout behavior.

### Additional gates before real data or shared deployment

- [ ] Select and review the LLM/embedding provider, data terms, regions, retention, and approved data classes.
- [ ] Define retention/deletion, secret/key storage and rotation, database privileges/protection, backup/restore, network controls, and environment separation.
- [ ] Define audit integrity/access/retention, observability export and access, incident response, and operational ownership.
- [ ] Verify implementation and test evidence; do not represent the prototype as production-ready or certified.
