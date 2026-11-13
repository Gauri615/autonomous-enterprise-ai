# STRIDE Threat Model: Autonomous Enterprise AI System

## 1. Document Control

| Field | Value |
|---|---|
| Product | Autonomous Enterprise AI System |
| Initial use case | AI IT Support Agent |
| Document | Security Threat Model |
| Version | 1.0 |
| Status | Draft for architecture and security review |
| Date | 2026-11-13 |
| Source baseline | PRD v1.0, SRS v1.0, Use Cases v1.0 |
| Owner | Enterprise Security Architecture |
| Review cadence | Revisit when architecture, provider, data handling, or MVP scope changes |

This is a design-time threat assessment based on the stated product requirements, not a penetration-test result or assurance that controls have been implemented. Risks are preliminary and must be reassessed against the deployed design. This document makes no claim of production readiness, compliance, or security certification.

## 2. Executive Summary

The Autonomous Enterprise AI System is a prototype AI IT support platform with a browser client, authoritative Java 21/Spring Boot business API, Python/FastAPI AI service, LangGraph orchestration, MariaDB 10.11.16 relational persistence, an unresolved Knowledge Agent retrieval approach, and an LLM provider that has not been selected. The primary security risks are unauthorized disclosure across users or organizations; model- or content-driven policy bypass; unsafe or replayed simulated actions; tampered or incomplete approval/audit records; and sensitive data exposure to external providers.

The product's safety case depends on strict separation of duties:

- LLM and agent output is untrusted and may only recommend, summarize, classify, or propose.
- The Spring Boot business API is authoritative for identity context, authorization, approval state, policy evaluation, and tool execution gating.
- Retrieval must enforce organization and user permissions before content reaches the AI service or model.
- Every enabled MVP action is allow-listed, input-validated, simulated, and explicitly approved by an authorized human before execution.
- Failures in authentication, policy, retrieval, approval, execution, or persistence fail closed and produce truthful status.

The prototype must use synthetic/demo data by default and must not connect to live enterprise systems. High and critical threats below require their listed controls and verification before the relevant MVP flow is enabled. Provider handling, authentication, data retention, approval expiry, and deployment controls remain open decisions; until resolved, the system must not process real employee data or represent these controls as implemented.

## 3. System Overview

### 3.1 Components in scope

- **React/TypeScript/Tailwind CSS web client:** Employee question and response interface, request status, escalation, optional feedback, and approver review.
- **Java 21/Spring Boot business API:** Authoritative application API and deterministic policy boundary for trusted identity/organization context, authorization, request state, approval workflow, and simulated tool execution.
- **Python/FastAPI AI service:** Internal boundary for bounded AI processing; receives only minimum necessary context and returns content or proposals for backend validation.
- **LangGraph:** Coordinates the configured stateful agent workflow. Workflow state is not an authorization mechanism.
- **Knowledge retrieval:** Curated demo knowledge metadata is stored in MariaDB 10.11.16. Retrieval technology is not selected; organization and user permission constraints must be enforced before model input.
- **MariaDB:** Primary relational persistence for organization-scoped requests, knowledge metadata, proposals, approvals, outcomes, and audit events.
- **LLM provider:** Gemini API or OpenAI API is under consideration. The provider is not finalized.
- **Allow-listed simulated tools:** Narrow prototype-only actions; no production credentials or live enterprise-system mutation.

Redis is optional and may be added only if justified; Docker is planned later. Neither is required for MVP security behavior. No live ticketing, identity, device, email, software deployment, or other enterprise integration is in scope.

### 3.2 Security posture

This document states required controls and residual risks, not proof of implementation. The prototype is not a production security boundary until implementation, deployment, testing, operational ownership, and provider/data decisions have been reviewed.

## 4. Security Objectives

1. **Protect confidentiality:** Users and agents receive only data permitted for the authenticated user and organization; minimize content sent to the LLM and written to logs.
2. **Preserve integrity:** Ticket/request state, knowledge permissions, tool parameters, approvals, configuration, and audit records cannot be changed or replayed outside authorized workflows.
3. **Enforce accountable authorization:** Deterministic Spring Boot policy is authoritative for every protected read and action; model output never grants access or approval.
4. **Constrain action:** MVP tools are explicitly allow-listed, schema-validated simulations. Every enabled action requires explicit human approval and execution-time policy revalidation.
5. **Isolate organizations:** Trusted server-side organization context is enforced on application records, vector retrieval, approval records, caches if introduced, and audit queries.
6. **Fail safely:** Timeouts and component failures do not result in false success, stale approval, unauthorized access, or duplicate execution; escalation is offered where possible.
7. **Provide accountability:** Important request, retrieval, policy, approval, configuration, and tool events are attributable and correlated while avoiding unnecessary sensitive content.
8. **Limit provider exposure:** Use synthetic/demo content by default; review the selected provider's data handling, retention, and organizational approval before any non-synthetic data is processed.
9. **Protect secrets:** Secrets must never be committed to Git, prompts, or routine logs; the actual secret-management mechanism is an implementation decision.
10. **Avoid unsupported assurance:** Do not imply production readiness, certification, or compliance based on this design or prototype.

## 5. System Architecture and Trust Boundaries

### 5.1 Logical architecture

```text
Untrusted user input
       |
       v
React browser client
       |
       | TB-1: client / authoritative application boundary
       v
Java 21 + Spring Boot business API
  | trusted identity/organization context
  | deterministic policy and request/approval state
  | MariaDB + unresolved retrieval component
  | simulated-tool execution gate
       |
       | TB-2: internal AI-service boundary; minimum necessary context
       v
Python FastAPI + LangGraph
       |
       | TB-3: external provider boundary; provider terms unresolved
       v
Selected LLM provider

Curated knowledge --> permission-filtered retrieval --> AI service/model
Authorized human --> exact proposal review --> backend approval/revalidation

MVP simulated tool only; no live enterprise system connection.
```

### 5.2 Trust boundaries

| Boundary | Crossing data/control | Security requirement |
|---|---|---|
| TB-1 Browser to Spring Boot API | Credentials/session, user input, request identifiers, feedback, approval decisions | Authenticate requests; derive actor and organization from trusted server-side identity; validate every input; enforce authorization and tenant scope in the API. Client controls and hidden UI state are not trusted. |
| TB-2 Spring Boot API to FastAPI/LangGraph | Request context, eligible retrieval references/content, AI outputs, proposals | Use a defined internal contract; pass minimum necessary, permission-eligible data; correlate request and organization; bound time; validate all returned output; AI service cannot invoke tools or authorize. |
| TB-3 AI service to LLM provider | Prompts, eligible knowledge, limited request context, model output | Provider is undecided. Use synthetic/demo data until provider/data terms and organizational approval are reviewed; minimize data; keep credentials in trusted configuration outside prompts/source control/logs. |
| TB-4 Application to MariaDB / retrieval component | Requests, organization metadata, knowledge, proposals, approvals, outcomes, audit events, and any retrieval index | Enforce organization and permission scope on every query/search/write; protect approval integrity and state transitions; limit and attribute audit access. If retrieval is external, assess and secure that additional boundary before use. |
| TB-5 API to simulated tool registry | Tool identifier, validated arguments, approval and policy state | Allow-listed operations only; schema and authorization checks; mandatory approval; revalidate before execution; simulation only; bounded timeout and duplicate-safe behavior. |
| TB-6 Human approval interface to business API | Review context, decision, approver identity | Authenticate and authorize reviewer; bind explicit decision to immutable proposal; expire/invalidate stale approval; record decision; approval never bypasses policy. |
| TB-7 Operations/configuration boundary | Provider config, knowledge configuration, tool allow-list, roles/policies, diagnostics | Restrict configuration to authorized actors; attribute changes; secrets outside source/Git/prompts/logs; no silent expansion of tools. Exact operational controls remain to be designed and verified. |
| TB-8 External retrieval boundary (conditional) | Eligible chunk identifiers/metadata, search query, relevance results, and possibly embeddings/content | Applies only if an external vector database or search service is selected. Review provider/data terms, identity/network controls, tenant and permission filtering, revocation freshness, retention/deletion, and failure behavior before use; no such service is currently selected. |

## 6. Assets and Sensitive Data

| Asset | Security interest | MVP handling constraint |
|---|---|---|
| Authentication/session context | Confidentiality and integrity | Trusted server-side validation; exact mechanism unresolved. |
| User identity, organization, roles, permissions | Integrity and confidentiality | Server-resolved context; never trust request or model claims. |
| Support request/ticket descriptions and conversation content | Confidentiality and integrity | Treat as untrusted input; minimize model/log exposure; organization/user scoped. |
| Curated knowledge documents, chunks, embeddings, source metadata, access attributes | Confidentiality and integrity | Synthetic/approved demo content; enforce permissions before retrieval reaches the model. |
| AI prompts, retrieved context, workflow state, outputs | Confidentiality and integrity | Minimum necessary content, request/organization correlation, isolated workflow state, output treated as untrusted. |
| Tool catalog, schemas, simulated arguments and outcomes | Integrity and authorization | Explicit allow-list, validated parameters, attributable configuration; no live side effects. |
| Action proposals, approval decisions, approver identity, expiry state | Integrity, accountability, and confidentiality | Bind decision to exact proposal; revalidate at execution; prevent stale or duplicate execution. |
| Internal request status and escalation context | Integrity and confidentiality | Business API is authoritative; only defined transitions; scoped access. |
| Audit events and operational metrics | Integrity, accountability, and confidentiality | Correlated and attributable; minimize content; authorized, organization-scoped review. |
| LLM provider credentials and service secrets | Confidentiality and integrity | Never in source code, Git, prompts, or routine logs; storage mechanism is unresolved. |
| Provider-bound data and provider response | Confidentiality and integrity | Provider undecided; do not send real organizational data before review and approval. |

Do not assume that encryption at rest, key management, backup protections, immutable logging, data residency, or deletion guarantees exist; these are not established by the source documents and remain deployment/provider decisions.

## 7. Actors and Threat Actors

### 7.1 Legitimate actors

- Employee using controlled demo identity.
- IT support analyst and authorized action approver.
- Organization administrator, only for optional enabled organization-scoped knowledge/configuration features.
- Governance/audit reviewer with explicit access.
- Platform operator responsible for prototype configuration.
- AI service and selected LLM provider as constrained system/external actors.

### 7.2 Threat actors and failure sources

- Unauthenticated or malicious external user submitting crafted requests.
- Authenticated employee attempting cross-user or cross-organization access or approval.
- Malicious or compromised tenant administrator, analyst, or operator abusing legitimate privileges.
- Attacker able to tamper with browser requests, replay API calls, or exploit authorization gaps.
- Malicious or compromised knowledge contributor supplying poisoned content or permission metadata.
- Prompt-injection content embedded in ticket descriptions or retrieved documents.
- Compromised or misconfigured AI service, provider integration, or provider account.
- Automated client or repeated requests causing resource exhaustion or disproportionate LLM cost.
- Accidental operator error, stale configuration, provider failure, database failure, or logging gaps.

Likelihood and severity below are preliminary design-time qualitative ratings, not measured exploitability. “Critical” and “High” identify risks that need verified mitigation before enabling the affected MVP path.

## 8. Entry Points and Attack Surfaces

1. Browser authentication/session and all user-facing business API endpoints.
2. Request creation, clarification, status lookup, escalation, feedback, and approval/rejection inputs.
3. User-controlled ticket descriptions and any user-supplied identifiers, text, or structured fields.
4. Internal Spring Boot-to-FastAPI calls and the AI service's request/response contract.
5. LangGraph state transitions, agent messages, prompt templates, and model-generated structured output.
6. Retrieval queries, vector candidates, document/chunk metadata, citations, and permission filters.
7. Optional knowledge/configuration management, tool catalog, role/policy, and provider configuration surfaces.
8. Approval UI and backend approval/execution state transitions, including replay/concurrency paths.
9. MariaDB query/persistence and selected retrieval boundaries; Redis/cache boundary only if later justified.
10. Provider API calls, provider credentials/configuration, response payloads, and external data retention.
11. Simulated tool invocation, parameter schemas, timeout/retry behavior, and outcome recording.
12. Audit query, event persistence, diagnostic/operational logging, and metric/reporting access.
13. Service-to-service identity, deployment configuration, secrets handling, and future container/network boundaries.

No live enterprise connector or production credential is an MVP entry point.

## 9. Data Flow and Trust Boundary Analysis

| Flow | Source -> destination | Data classification | Required controls | Primary threats |
|---|---|---|---|---|
| DF-1: Authenticate and create request | Browser -> Spring Boot API -> MariaDB | Identity, organization context, request content | Validate authentication; derive identity/organization server-side; authorize; validate request; persist organization scope; return safe errors. | Spoofing, tampering, disclosure, DoS |
| DF-2: Retrieve knowledge | Business API/retrieval -> MariaDB and selected retrieval component -> AI service | Query context, permissions, source text/metadata | Apply organization and user permissions before model input; verify source attribution; treat document text as untrusted. | Cross-tenant leakage, indirect injection, tampering |
| DF-3: AI processing | Spring Boot -> FastAPI/LangGraph -> LLM provider | Minimal task context, eligible knowledge, outputs | Defined internal contract, service identity, request correlation, finite timeout, provider review, output validation, no secrets/unnecessary PII. | Spoofing, disclosure, tampering, DoS, privilege escalation |
| DF-4: Proposal and approval | AI proposal -> business API -> approver -> business API | Tool name/arguments, request context, approval decision | Strict allow-list/schema validation; authorized reviewer; exact immutable proposal binding; expiry; policy revalidation; audit. | Hallucinated args, replay, repudiation, elevation |
| DF-5: Simulated execution | Business API -> simulated tool -> database/UI | Approved action and simulated result | No live side effect; backend-only invocation; bounded timeout; safe retry; persist outcome and audit before reporting success. | Elevation, replay, tampering, DoS, repudiation |
| DF-6: Review and feedback | Browser -> API -> request/audit/feedback records | Status, audit metadata, optional feedback | Per-user/role/organization authorization; do not expose unnecessary content; feedback cannot grant access. | Spoofing, disclosure, tampering |
| DF-7: Admin/configuration | Admin/operator -> API/config/knowledge/tool catalog | Permissions, approved demo documents, tool schemas, provider settings | Explicit roles, organization scope, validation, attribution, review of tool/provider changes; no silent scope expansion. | Privilege escalation, tampering, poisoning |

### 9.1 Trust boundary findings

- The browser, user-provided request text, retrieved document text, model output, and tool arguments are untrusted.
- Identity, organization, roles, policy decisions, approval status, request status, and execution outcomes must be established or revalidated by the Spring Boot business API.
- Permission checks must precede vector content delivery to the AI service/model; downstream prompt instructions cannot repair an earlier unauthorized disclosure.
- A model-generated citation is not evidence that the source exists or is authorized; the application must validate source references.
- Human approval is a separate trusted act, but still does not override deterministic policy or the tool allow-list.
- The external provider is outside the product trust boundary; provider data handling is an unresolved gate before real data use.

## 10. STRIDE Threat Analysis

### 10.1 Rating method

- **Likelihood:** Low, Medium, or High, based on exposure and plausible misuse at design time; not an empirical frequency.
- **Risk severity:** Critical, High, Medium, or Low, based on confidentiality, integrity, authorization, and workflow impact if the scenario succeeds.
- **Residual risk:** Expected remaining risk after the listed controls, assuming those controls are implemented and tested. It is not a claim that the risk has been eliminated.
- **High-risk mitigation gate:** Every Critical or High threat must have its listed mitigation implemented and verified for the relevant workflow before that workflow is enabled. If it is not verified, keep affected capability disabled and use synthetic/demo data.

### 10.2 Threat register

| Threat ID | Affected component | STRIDE category | Threat scenario | Potential impact | Likelihood | Risk severity | Mitigation | Residual risk | Related SRS requirement IDs |
|---|---|---|---|---|---|---|---|---|---|
| THR-001 | Browser authentication and Spring Boot API | Spoofing | An attacker submits forged, expired, or replayed identity/organization claims or manipulates client fields to act as another user or tenant. | Unauthorized request access, approval, or disclosure across organizations. | Medium | High | Authenticate protected requests; derive identity and organization only from verified server-side context; reject client/prompt claims as authority; authorize every operation; test spoofed identity and tenant cases. | Medium: authentication mechanism and session controls are unresolved until selected and verified. | SRS-AUTH-001, SRS-AUTH-002, SRS-AUTH-004, SRS-AUTHZ-001, SRS-AUTHZ-002, SRS-API-002 |
| THR-002 | Request API, request store, and workflow state | Tampering | A user alters a request identifier, request body, status, organization field, or workflow reference to change another user's ticket or influence processing. | Cross-user disclosure, incorrect request state, or unauthorized downstream workflow. | Medium | High | Validate input and state transitions in the business API; bind records to server identity/organization; reject arbitrary status transitions; use persisted authoritative state and request correlation. | Low to Medium: concurrency and implementation defects remain possible and require integration tests. | SRS-FR-001, SRS-FR-002, SRS-AUTHZ-003, SRS-TEN-002, SRS-TKT-001, SRS-TKT-007, SRS-API-002 |
| THR-003 | Approval, tool execution, and audit trail | Repudiation | A user, approver, or service denies submitting, approving, rejecting, or executing an action because actor/time/proposal linkage is missing or unreliable. | Inability to reconstruct decisions, investigate misuse, or establish whether a simulation occurred. | Medium | High | Persist attributable, correlated events for policy, approval, and tool outcomes; bind events to request/proposal and actor; report workflow success only when critical state and audit persistence succeed; restrict audit access. | Medium: audit retention, integrity protections, and operational review ownership are unresolved. | SRS-FR-011, SRS-FR-012, SRS-HITL-009, SRS-TOOL-009, SRS-AUD-001, SRS-AUD-002, SRS-AUD-006, SRS-AUD-009 |
| THR-004 | Retrieval, API responses, logs, and provider boundary | Information Disclosure | An API response, source citation, error, prompt, or routine log exposes request details, sensitive knowledge, secrets, or organization data to an unauthorized user or provider. | Confidentiality breach, privacy impact, loss of trust, or cross-tenant exposure. | Medium | High | Apply user and organization authorization before retrieval/return; minimize model and log payloads; suppress ineligible sources; use safe errors; do not log secrets or unnecessary content; review provider data handling before real data use. | Medium: provider terms, retention, and deployment logging controls are unresolved. | SRS-FR-003, SRS-FR-019, SRS-NFR-003, SRS-TEN-003, SRS-RAG-009, SRS-AUD-005, SRS-API-008, SRS-SEC-008, SRS-SEC-010 |
| THR-005 | Browser/API, FastAPI, LangGraph, LLM provider | Denial of Service | Repeated or oversized support requests, expensive prompts, retrieval loops, or provider calls exhaust prototype resources or request deadlines. | Unavailable support flow, elevated provider cost, queue saturation, or cascading failures. | Medium | Medium | Enforce finite request and downstream deadlines; bound configured workflow steps and input/output sizes; retry only safe idempotent reads at most once; return truthful unavailable/failed status and preserve escalation when possible. Specific rate/size limits are not specified and must be decided before exposed deployment. | Medium: capacity limits and abuse controls are not defined by the source documents. | SRS-NFR-005, SRS-NFR-015, SRS-NFR-016, SRS-NFR-017, SRS-AI-002, SRS-API-009, SRS-ERR-004, SRS-ERR-005, SRS-ERR-006, SRS-PR-004 |
| THR-006 | LLM/agent to policy service and simulated tools | Elevation of Privilege | A model output or prompt persuades the AI service to invoke a tool, grant access, or bypass a policy/approval check as though the model were authorized. | Unauthorized action or privilege escalation; unsafe workflow behavior. | Medium | Critical | Agents have no independent tool authority; expose only bounded outputs; invoke tools only from the Spring Boot API after deterministic authorization, schema validation, mandatory human approval, and execution-time revalidation; reject all unregistered/live operations. | Low to Medium: implementation or service-boundary defects could still create an unintended path; verify with negative tests before enabling tools. | SRS-FR-006, SRS-FR-007, SRS-FR-008, SRS-FR-009, SRS-NFR-001, SRS-AI-006, SRS-TOOL-001, SRS-TOOL-003, SRS-TOOL-005, SRS-TOOL-006, SRS-SEC-001, SRS-SEC-002, SRS-SEC-006 |
| THR-007 | Ticket description, prompt construction, LangGraph workflow | Tampering | Direct prompt injection in a malicious ticket description attempts to override system instructions, expose data, alter organization context, or request an unapproved action. | Misleading answer, data exposure if another control fails, or unsafe proposal generation. | High | High | Treat ticket text as untrusted data; keep policy and workflow controls outside model instructions; minimize context; constrain configured steps; validate every output and source; route any proposal through backend policy and mandatory approval; test adversarial inputs. | Medium: model behavior cannot be made fully deterministic; safe boundaries must contain failures. | SRS-NFR-018, SRS-AI-007, SRS-AI-004, SRS-AI-006, SRS-FR-006, SRS-TEST-007 |
| THR-008 | Knowledge ingestion, retrieved documents, RAG prompt | Tampering | Indirect prompt injection or malicious instructions embedded in a knowledge document are interpreted by an agent as privileged commands. | Policy bypass attempts, misleading answers, or attempted tool proposal manipulation. | High | High | Treat all retrieved content as untrusted data; curate/approve demo content; preserve instructions/policy in trusted application logic; filter retrieval permissions before generation; validate outputs; test malicious documents. | Medium: content can remain adversarial or misleading even when stored as approved knowledge; human curation and output validation remain necessary. | SRS-NFR-018, SRS-RAG-007, SRS-RAG-008, SRS-AI-007, SRS-SEC-005, SRS-TEST-007 |
| THR-009 | MariaDB knowledge metadata, selected retrieval component and cache if introduced | Information Disclosure | A missing or incorrect organization/user permission predicate, stale retrieval metadata, or unscoped cache key returns another tenant's or unauthorized user's knowledge to a requester or LLM. | High-impact cross-organization or permission-boundary data disclosure. | Medium | Critical | Enforce organization and user permission filters at query/retrieval time before content reaches the AI service; derive organization from trusted context; scope all reads/writes; test at least two organizations and differing permissions; if Redis is later justified, scope cache keys/invalidation. | Medium: complex query/index/cache paths can regress; continuous isolation regression tests are required. | SRS-FR-003, SRS-FR-015, SRS-NFR-002, SRS-TEN-001, SRS-TEN-002, SRS-TEN-003, SRS-TEN-005, SRS-TEN-006, SRS-RAG-002, SRS-RAG-003, SRS-DB-002, SRS-TEST-006 |
| THR-010 | Curated knowledge source, retrieval index/service, citations | Tampering | A malicious or erroneous knowledge document, permission label, chunk, or source reference poisons retrieval or causes a fabricated/unauthorized citation to appear authoritative. | Incorrect employee guidance, data exposure, or loss of trust in answer provenance. | Medium | High | Restrict and attribute optional knowledge changes; require organization and permission metadata; use curated synthetic/approved content; treat content as untrusted instructions; verify source identity and eligibility before display; do not accept model-generated citations without lookup. | Medium: semantic inaccuracies and compromised authorized contributors remain possible; content stewardship is an operational dependency. | SRS-FR-004, SRS-FR-005, SRS-FR-017, SRS-RAG-002, SRS-RAG-004, SRS-RAG-005, SRS-RAG-008, SRS-RAG-009, SRS-TOOL-010, SRS-AUD-001 |
| THR-011 | Model-generated action proposal and tool argument parser | Tampering | The LLM hallucinates, confuses, or maliciously shapes a tool name, target, or parameter; an incomplete validator accepts it. | Incorrect or unauthorized simulated operation; future expansion could magnify impact if controls are not retained. | High | High | Treat arguments as untrusted; allow-list tool identifiers and strict schemas; reject unknown, extra, malformed, or out-of-scope parameters; show exact effect to reviewer; bind approval to immutable parameters and revalidate policy before simulation. | Low to Medium: schema defects and model ambiguity remain possible; no real side effect is allowed in MVP. | SRS-FR-007, SRS-FR-008, SRS-FR-010, SRS-FR-014, SRS-TOOL-001, SRS-TOOL-002, SRS-TOOL-004, SRS-HITL-003, SRS-HITL-005, SRS-NFR-010 |
| THR-012 | LangGraph workflow, AI service, tool boundary | Elevation of Privilege | An agent takes actions outside its bounded role, chains tools, reuses another request's state, or treats its own output as authorization. | Unauthorized access/action, cross-request context leakage, or bypass of human oversight. | Medium | Critical | Limit workflow to configured steps; isolate and correlate state per request and organization; provide no ambient credentials or direct tool endpoint; validate outputs in business API; require deterministic policy and human approval for each enabled simulated action. | Low to Medium: orchestration/configuration bugs remain possible; verify state isolation and no-direct-tool paths. | SRS-FR-006, SRS-NFR-001, SRS-NFR-017, SRS-NFR-018, SRS-AI-002, SRS-AI-003, SRS-AI-006, SRS-API-005, SRS-TOOL-003, SRS-TEST-002, SRS-TEST-007 |
| THR-013 | FastAPI response parser, business API, UI | Tampering | Malformed, unexpected, or adversarial model output is trusted as a source citation, status, identity claim, or action proposal without strict validation. | False facts/status, unauthorized source display, workflow confusion, or attempted unauthorized execution. | High | High | Define a bounded service contract; validate response shape and allowed values; verify citations against actual permission-eligible retrieval references; only business API state determines status; reject malformed output and fail safely. | Medium: valid-shaped but false natural-language claims remain possible; clearly label suggestions and uncertainty and retain escalation. | SRS-FR-004, SRS-FR-005, SRS-FR-020, SRS-AI-004, SRS-API-004, SRS-API-008, SRS-NFR-006, SRS-RAG-005, SRS-RAG-009, SRS-ERR-004 |
| THR-014 | AI service to external LLM provider | Information Disclosure | Prompts contain unnecessary personal data, secrets, unauthorized content, or non-synthetic organizational data before provider selection and terms are reviewed. | Data exposure outside the organization, contractual/privacy concern, or credential compromise. | Medium | High | Use synthetic/demo data by default; minimize and permission-filter content; never send credentials/secrets; isolate provider-specific integration and trusted configuration; select provider and review data handling/retention and obtain organizational approval before real data use. | Medium: third-party processing and retention behavior remains outside direct product control and is unresolved until provider review. | SRS-FR-019, SRS-NFR-003, SRS-NFR-013, SRS-AI-001, SRS-AI-009, SRS-API-006, SRS-SEC-008, SRS-SEC-010, SRS-AUD-005 |
| THR-015 | Approval interface and execution lifecycle | Spoofing / Tampering | An attacker replays an approval, changes proposal parameters after approval, impersonates an approver, or exploits a race between approval and execution. | Simulated action executes without valid current consent or under changed parameters. | Medium | Critical | Authenticate and authorize approver for organization/action; require explicit decision; bind approval to exact proposal; expire approval; invalidate on parameter/policy change; revalidate immediately before execution; make transitions duplicate-safe and auditable. | Low to Medium: races and state-store defects require concurrency and replay testing; expiry value remains unresolved. | SRS-FR-009, SRS-FR-010, SRS-FR-011, SRS-HITL-001, SRS-HITL-002, SRS-HITL-004, SRS-HITL-005, SRS-HITL-006, SRS-HITL-007, SRS-HITL-008, SRS-NFR-010, SRS-DB-005, SRS-DB-006, SRS-TEST-008 |
| THR-016 | Spring Boot to FastAPI internal API | Spoofing / Tampering | An unauthorized caller reaches the AI service directly or a forged/altered internal response is accepted as trusted workflow output. | Unauthorized model use, data exposure, untrusted proposal injection, or bypass of application validation. | Medium | High | Keep AI processing behind a defined internal service contract; do not expose unauthenticated public tool execution; pass minimum necessary trusted context; correlate calls; validate all outputs at the business API; implement and verify service-to-service authentication and protected transport as deployment requirements. | Medium: concrete service identity and network controls are not specified in the documents and must be designed. | SRS-API-004, SRS-API-005, SRS-API-009, SRS-AI-001, SRS-AI-004, SRS-NFR-009, SRS-SEC-010 |
| THR-017 | MariaDB and persistence access | Information Disclosure / Tampering | A query, database role, migration, or tenant predicate defect permits unauthorized reads/writes or corrupts requests, permission labels, approvals, or audit associations. | Cross-organization disclosure, privilege escalation, invalid approval/execution state, or loss of evidence. | Medium | High | Enforce organization scope on every data access path; use least-privilege service access and parameterized/validated queries as implementation controls; preserve stable proposal/approval/execution relationships; test isolation, state integrity, and failure handling. Do not rely on opaque identifiers. | Medium: schema, database role, backup, encryption, and migration practices require implementation-specific review. | SRS-TEN-002, SRS-TEN-007, SRS-DB-001, SRS-DB-003, SRS-DB-005, SRS-DB-006, SRS-DB-007, SRS-TEST-005, SRS-TEST-006 |
| THR-018 | Audit store, audit API, operational logs | Tampering / Repudiation / Information Disclosure | A privileged or compromised actor alters/deletes events, log content leaks sensitive data, or cross-tenant audit queries disclose activity. | Loss of accountability, inability to investigate, sensitive-data exposure, or misleading review. | Medium | High | Attribute and correlate material events; restrict audit read access and scope; minimize logged content; do not log secrets; fail safely if critical event persistence fails; test audit reconstruction and tenant scope. Immutable storage or tamper-proof logging is not established and must not be claimed. | Medium: retention duration, administrative separation, and log-store integrity controls remain unresolved. | SRS-FR-012, SRS-NFR-004, SRS-NFR-011, SRS-AUD-001, SRS-AUD-002, SRS-AUD-004, SRS-AUD-005, SRS-AUD-006, SRS-AUD-007, SRS-AUD-008, SRS-AUD-009 |
| THR-019 | Tool allow-list, policy and knowledge configuration | Elevation of Privilege / Tampering | An administrator/operator account or configuration path silently adds a dangerous tool, broadens roles, changes permissions, or alters provider settings. | Expanded attack surface, unauthorized data access, or unsafe action proposals/execution. | Medium | High | Restrict configuration to authorized roles and organization scope; validate configuration against constrained schemas; attribute changes; ensure tool catalog changes cannot silently expand actions; review and test each enabled simulation; keep configuration outside user/agent control. | Medium: exact configuration review/approval workflow and separation of duties are not defined. | SRS-FR-017, SRS-AUTHZ-005, SRS-AUTHZ-006, SRS-TOOL-001, SRS-TOOL-010, SRS-RAG-008, SRS-AUD-001, SRS-AUD-002 |
| THR-020 | LLM provider, AI service, request handling | Denial of Service | Provider outage, slow response, rate/cost exhaustion, or orchestration loop causes requests to exceed their deadline or blocks escalation. | Loss of availability of prototype support and possible disproportionate provider cost. | Medium | Medium | Use bounded configured workflow steps, finite end-to-end/downstream deadlines, at most one safe read retry when time permits, truthful failure state, no unsafe tool retry, and escalation where possible; monitor latency/failures at prototype level. | Medium: provider availability and exact request-abuse limits are outside the product's control or unresolved. | SRS-NFR-005, SRS-NFR-007, SRS-NFR-015, SRS-NFR-016, SRS-NFR-017, SRS-API-009, SRS-ERR-004, SRS-ERR-005, SRS-ERR-006, SRS-PR-001, SRS-PR-002 |
| THR-021 | Simulated tool result and request state | Tampering / Repudiation | Duplicate submissions, ambiguous timeout, or retry race causes the same simulated effect to run twice or the UI to report success despite unknown execution/persistence state. | Incorrect request history, duplicate outcome, or false evidence of completion; risk if future designs copy unsafe retry behavior. | Medium | High | Use persisted state transitions and proposal linkage; do not retry an ambiguous non-idempotent invocation unless non-execution is established; record tool attempt/result; report unknown/failed instead of success; prevent duplicate/stale replay. | Low to Medium: depends on implementation consistency and concurrency tests. | SRS-FR-011, SRS-FR-020, SRS-NFR-005, SRS-NFR-010, SRS-NFR-016, SRS-NFR-019, SRS-TKT-007, SRS-TOOL-007, SRS-TOOL-008, SRS-TOOL-009, SRS-DB-006, SRS-ERR-007 |

## 11. AI-Specific Threat Analysis

| AI threat | Threat IDs | Required control / security interpretation |
|---|---|---|
| Direct prompt injection | THR-007 | User/ticket content is untrusted. It may affect the natural-language recommendation but cannot change server-side identity, permissions, policy, tools, or approval. Validate model output and test adversarial requests. |
| Indirect prompt injection | THR-008 | Retrieved content is untrusted data, even when curated. Do not let document instructions alter agent behavior or tool policy; keep authorization outside prompts. |
| Malicious ticket descriptions | THR-007, THR-013 | Validate request shape/size; treat description as content, not instruction authority; never use its claims to establish identity or permissions. |
| Malicious knowledge documents | THR-008, THR-010 | Use curated synthetic/approved content with scoped metadata; attribute configuration changes; enforce retrieval permissions; verify displayed source references. |
| RAG data leakage | THR-004, THR-009 | Enforce user and organization permissions before content is passed to the AI service/model; prevent leakage through snippets, citations, errors, prompts, and logs. |
| Cross-tenant retrieval | THR-001, THR-009, THR-017 | Organization comes from trusted server context; every vector and relational query is scoped; validate with two-organization denial tests, including any cache if later introduced. |
| Hallucinated tool arguments | THR-011, THR-013 | Model output is untrusted. Reject unknown tools and malformed/extra parameters; validate against the explicit allow-list and schema; display the exact proposal and bind approval to it. |
| Unauthorized agent actions | THR-006, THR-012 | AI components cannot authorize or directly execute. The Spring Boot API independently evaluates policy, approval, and tool eligibility; all MVP effects remain simulated. |
| LLM output validation | THR-013 | Parse against a bounded contract; validate allowed state values, citations, and proposals; do not let model text determine request status or operation success. |
| Excessive agent autonomy | THR-006, THR-012 | LangGraph executes only configured steps, with request/organization-scoped state; no ambient credentials, arbitrary tool discovery, open-ended loops, or direct tool endpoint. |
| Sensitive data exposure to provider | THR-004, THR-014 | Default to synthetic/demo data; minimize and permission-filter model input; never send secrets; select provider and review data handling/retention and organizational approval before real data. |
| Model hallucination or overclaim | THR-010, THR-013 | Distinguish source-backed facts from suggestions; show actual eligible citations only; communicate uncertainty and escalate when evidence is insufficient. |
| Provider response contamination or availability failure | THR-013, THR-020 | Treat provider responses as untrusted; enforce timeouts; reject malformed output; fail safely and offer escalation. |

Prompt defenses such as system instructions are not an authorization boundary and do not eliminate prompt-injection risk. The security design must remain safe if the model follows malicious content.

## 12. RAG and Knowledge Security

1. **Approved corpus boundary:** MVP retrieval is limited to a small curated synthetic or explicitly approved demo collection. Broad organization-wide ingestion without permission metadata and review is out of scope.
2. **Permission metadata:** Knowledge records/chunks carry organization and applicable permission attributes. Missing, invalid, or ambiguous attributes must make content unavailable for retrieval rather than public by default.
3. **Pre-generation filtering:** The retrieval query enforces organization and user access before candidate content is delivered to FastAPI/LangGraph or the LLM. Filtering only in generated text/UI is insufficient.
4. **Trusted context:** The organization identifier comes from server-validated identity context, not document metadata, model output, client input, or vector similarity.
5. **Source integrity:** The application validates that citations correspond to actual retrieved source references and that the current user may see them. The model cannot create trusted provenance by naming a source.
6. **Content as data:** Knowledge text, embedded instructions, malformed markup, and potentially poisoned facts are untrusted. They cannot change policy, workflow definitions, access control, or tool catalog.
7. **Ingestion/configuration control:** If optional administrator knowledge configuration is enabled, changes must be authenticated, authorized, attributable, organization-scoped, and validated for required metadata. Content curation remains a business governance responsibility.
8. **Storage and retrieval index:** MariaDB and any selected retrieval component/cache must preserve the same tenant and permission predicates as the business API. Embeddings and metadata can reveal content and are treated as sensitive.
9. **Failure behavior:** Retrieval/index failure returns no claim of verified evidence; the user sees uncertainty/unavailability and an escalation path where possible.
10. **Verification:** Test cross-tenant and cross-permission retrieval, unauthorized source display, malicious document instructions, stale permission metadata, missing source references, and no-evidence behavior.

## 13. Authentication and Authorization Threats

- Authentication establishes an identity only through a configured, verified mechanism. The SRS does not select an identity provider, protocol, session design, or MFA.
- The server resolves user and organization context; client-provided IDs, claims in prompts, and model assertions are untrusted.
- Authorization is evaluated by deterministic Spring Boot policy at each protected read and immediately before tool execution. UI visibility, model classification, or possession of an opaque record ID does not grant access.
- Reviewer authorization is scoped to the organization and action. An approval is explicit, proposal-bound, time-bounded, invalidated on material change, and revalidated before simulation.
- Organization administrator and platform operator privileges are distinct. Configuration ability does not imply user data access or audit access.
- Denied operations return safe outcomes without protected content; material policy decisions are auditable.
- Authentication mechanism, credential lifecycle, session invalidation, reviewer role provisioning, and service identity remain unresolved implementation decisions.

Related threats: THR-001, THR-002, THR-006, THR-015, THR-016, THR-019.

## 14. Multi-Organization Data Isolation Threats

### Threats

- Missing organization predicate in a request/knowledge/audit query.
- Wrong or client-supplied organization context accepted by an API or AI service.
- Vector retrieval returns cross-tenant candidates before permission filtering.
- Workflow state or prompt context reused across requests or organizations.
- Optional cache collision or stale entry returns content across permission boundaries.
- Source citation, error, analytics, or audit interface leaks another organization's information.
- Administrator or operator configuration changes alter another tenant's permissions or documents.

### Required safeguards

- Resolve organization from trusted authentication context on the server.
- Scope relational reads/writes, vector search, workflow state, approval records, audit queries, and any cache by organization and relevant user permissions.
- Apply retrieval controls before model input, not as post-generation filtering.
- Use at least two organizations and users with different permissions in isolation tests; verify request, knowledge, proposal, approval, and audit denial.
- Treat record identifiers as references, not authorization tokens.
- If Redis is later introduced, include organization, user/permission context, and freshness/invalidation in the cache design; caching is not required for MVP.

Related threats: THR-001, THR-004, THR-009, THR-012, THR-017, THR-018.

## 15. Tool Execution and Approval Security

### Required execution gate

```text
Untrusted AI proposal
  -> strict tool and parameter validation
  -> deterministic organization/user/action policy
  -> exact proposal shown to authorized human
  -> explicit approval bound to proposal and bounded validity
  -> execution-time authorization and approval revalidation
  -> allow-listed simulated tool only
  -> persisted outcome and audit event
  -> truthful user-visible result
```

Any failed, missing, expired, rejected, changed, or ambiguous state stops execution. This sequence is a required control model, not an assertion that implementation is complete.

### Security requirements and threat coverage

- Tool names and schemas are explicitly allow-listed; unknown tools, additional fields, malformed values, and out-of-scope requests are rejected (THR-006, THR-011).
- The LLM and AI service cannot invoke tools directly or authorize sensitive operations (THR-006, THR-012).
- Every enabled MVP action remains simulated and requires an authorized human decision (THR-015).
- Approval applies only to the exact action and parameters reviewed. Material changes, relevant policy changes, rejection, expiry, or failed revalidation invalidate execution eligibility (THR-015).
- Execution-time policy and approval state are checked by the business API. Approval alone never overrides authorization (THR-006, THR-015).
- Tool timeouts and retries must not create duplicate outcomes or success-shaped responses; ambiguous outcomes require safe failure/review (THR-021).
- Every attempted/rejected/timed-out/completed action is correlated with actor, organization, proposal, tool, outcome, and timing for audit (THR-003, THR-018).
- No live enterprise connector, production credential, or real account/access/device/ticket mutation is in MVP (THR-006, THR-011, THR-012).

The numeric approval validity period and exact simulated-tool timeout remain unresolved. Approval must not be enabled until the bounded expiry configuration is established as required by the SRS.

## 16. API and Backend Security

- The Spring Boot service is the authoritative business API. All protected reads, state transitions, approvals, and tool execution gates are enforced there.
- Browser inputs, path/query/body identifiers, ticket text, AI outputs, provider responses, and tool arguments are untrusted and require server-side validation.
- OpenAPI documentation describes service contracts but is not itself an access-control mechanism.
- Spring Boot-to-FastAPI calls use a defined internal contract, minimum necessary trusted context, request/organization correlation, finite timeouts, and output validation.
- FastAPI exposes bounded AI processing only; it must not provide an unauthenticated public tool-execution endpoint.
- Service-to-service authentication, network controls, transport protection, API rate/size limits, and deployment topology are not fully specified by the source documents. They must be designed and tested; do not assume they exist.
- Errors must not disclose secrets or protected cross-organization content. Distinguish validation, authorization, timeout, unavailable, and execution failure without success-shaped fallbacks.
- The provider integration is isolated and explicitly configured. The provider choice remains open.
- Do not add a live enterprise endpoint or connector to MVP.

Related threats: THR-001, THR-002, THR-005, THR-006, THR-013, THR-016, THR-020.

## 17. Database and Data Protection

- MariaDB 10.11.16 is the primary relational store; the Knowledge Agent retrieval approach remains undecided. Organization and permission attributes must support pre-generation filtering.
- Every organization-owned record and every access path must preserve trusted organization scope. Opaque identifiers do not substitute for authorization.
- Proposal, approval, and execution records need stable integrity linkage; state transitions must resist stale and duplicate operations.
- Database service permissions, credential handling, encryption, key management, network placement, backups, restore, deletion, and migration controls are not defined by the source documents. These must be resolved during implementation/deployment review and must not be claimed without evidence.
- Secrets are never stored in source code or committed to Git. Use a reviewed secret-management mechanism outside source control; exact mechanism is undecided.
- Routine logs and persistent records must not include credentials, secrets, or unnecessary personal/sensitive content.
- Retention and deletion rules for conversations and audit events must be defined before real employee data is used. Use synthetic/demo data until then.
- Redis is not a source of truth and is excluded unless justified. Any future cache must not weaken tenant, permission, or freshness controls.
- Failure to persist critical approval or execution state/audit must not be reported as successful execution.

Related threats: THR-009, THR-014, THR-017, THR-018, THR-021.

## 18. Audit Logging and Monitoring

### Required audit coverage

- Request lifecycle and material status transitions.
- Agent workflow start/important transitions/outcome and request correlation.
- Eligible knowledge source references and relevant access decision context.
- Authorization and policy decisions, including denials where material.
- Knowledge/tool/policy configuration changes where enabled.
- Approval/rejection, actor, time, proposal reference, expiry/validation, and execution outcome.
- Attempted, rejected, timed-out, and completed simulated tool calls.
- Operational status, latency, failures, and outcomes needed for prototype review.

### Logging controls and limitations

- Include actor/service, organization, event type, timestamp, outcome, and correlation/resource references where applicable.
- Restrict audit reads by role and organization. Do not assume every administrator has audit access.
- Avoid logging full sensitive prompts/documents, credentials, secrets, or unnecessary personal data.
- Critical audit persistence failure prevents a success-shaped action result.
- Audit access, retention, deletion, and tamper-resistance mechanisms require implementation decisions; the source documents do not establish immutable storage or a specific SIEM/monitoring product.
- Treat audit data as sensitive organization data and test cross-tenant access controls.

Related threats: THR-003, THR-018, THR-021.

## 19. Threat Mitigation Plan

Mitigations are requirements to implement and verify. They are not represented as already delivered controls.

| Priority | Threats | Mitigation work required | Verification / release gate |
|---|---|---|---|
| P0 — Critical | THR-006, THR-009, THR-012, THR-015 | Implement backend-only authorization and tool gate; pre-model tenant/permission filtering; isolate agent state; enforce explicit human approval bound to exact proposal with expiry and execution-time revalidation. Keep tools simulation-only. | Do not enable action or real-data flows until negative authorization, two-tenant retrieval, prompt injection, replay, changed proposal, missing/expired approval, and direct AI-to-tool tests pass. |
| P1 — High | THR-001, THR-002, THR-003, THR-004, THR-007, THR-008, THR-010, THR-011, THR-013, THR-014, THR-016, THR-017, THR-018, THR-019, THR-021 | Validate identity/context and request inputs; restrict access; curate and scope knowledge; validate model outputs and tool schemas; minimize provider/log payloads; attribute configuration; persist and correlate critical decisions/results; prevent duplicate/ambiguous execution. | Complete API, tenant-isolation, RAG, approval, audit, provider-data, and concurrency test cases; verify no secrets in prompts/logs/repository artifacts. |
| P2 — Medium | THR-005, THR-020 | Establish finite request/provider deadlines, bounded workflow execution, safe read-only retry behavior, operational visibility, and truthful escalation/error states. Decide request/size abuse limits for the chosen deployment. | Measure answer-only p95 against the SRS prototype target; exercise provider/AI/database outage and deadline paths. |

### 19.1 Operational response expectations

- If a high-risk control is not implemented or its test fails, disable the affected capability; do not fall back to model judgment.
- On uncertainty about execution outcome, record failed/unknown and prevent unsafe retry; do not report success.
- If unauthorized retrieval is suspected, stop disclosure, preserve the relevant minimal audit context, and route for human operational/security review under a process that must be defined before any real-data pilot.
- A tool or workflow disablement mechanism is a governance requirement to define before expanding scope; this document does not claim that a production kill switch exists.

## 20. Security Testing Strategy

Testing verifies the controls described in the SRS and this model; passing tests do not establish certification or production readiness.

1. **Identity and authorization:** Test missing/invalid identity, spoofed user/organization claims, unauthorized record access, unauthorized reviewer, and role changes while approval is pending.
2. **Tenant isolation:** Use at least two organizations and users with different permissions; test request, retrieval, proposal, approval, audit, and optional cache separation.
3. **Direct prompt injection:** Submit malicious ticket descriptions attempting to override system policy, reveal other data, or request unregistered tools; confirm output remains non-authoritative.
4. **Indirect prompt injection and knowledge poisoning:** Include hostile instructions, misleading claims, altered source references, and missing permission metadata in test documents; confirm they cannot change policy and are filtered/handled safely.
5. **RAG controls:** Verify organization/user filtering happens before AI/model input; verify no unauthorized snippets, source titles, embeddings-derived content, or citations are exposed.
6. **Output and proposal validation:** Test malformed model response, invalid status, fake citation, unknown tool, extra/missing parameters, wrong organization/target, type confusion, and hallucinated arguments.
7. **Approval integrity:** Test no approval, unauthorized approver, rejection, expiry, parameter changes, policy changes, replay, concurrent requests, and attempted direct tool invocation.
8. **Simulation guarantee:** Inspect the MVP tool catalog and execution boundary to verify that effects are simulated and no production credentials or live integration are reachable.
9. **Timeout, retry, and availability:** Test AI/provider/database/retrieval/tool timeouts, ambiguous tool outcome, idempotent read retry limit, deadline expiry, duplicate request, and truthful status/escalation.
10. **API/service boundary:** Verify Spring Boot-to-FastAPI contract, service identity, correlation, timeout, error safety, and that FastAPI cannot directly authorize or invoke tools.
11. **Audit and privacy:** Reconstruct a representative request-to-tool workflow; test event attribution, organization scope, critical audit failure, sensitive-data minimization, and secret absence from routine logs.
12. **Configuration and secrets:** Test unauthorized/unaudited tool, knowledge, permission, and provider configuration changes; scan project artifacts and prompts/logging test outputs for accidentally exposed secrets without placing real secrets in tests.
13. **Regression and performance:** Automate applicable cases with the SRS test stack (JUnit, Pytest, Vitest, Playwright) and measure the defined prototype latency target in a documented environment.

Testing should use synthetic/demo content. Any use of actual organization data requires prior provider, retention, privacy, and security review as required by the SRS.

## 21. Residual Risks

Even with the required mitigations implemented and tested, the following risks remain:

- LLMs can produce plausible but wrong, incomplete, or misleading output. Evidence labels, source validation, uncertainty, and escalation reduce but do not remove this risk.
- Prompt injection can influence model-generated language despite prompt controls. Security therefore depends on backend authorization, retrieval minimization, and the absence of direct AI tool authority.
- Semantic retrieval and content labels can be wrong or stale; content owners and review procedures are not fully defined.
- A selected third-party LLM provider processes data outside the application boundary; provider terms, retention, region, and model behavior are not yet selected.
- Authentication and service-to-service identity designs are unresolved; identity spoofing and service impersonation cannot be fully assessed until chosen.
- Human approvers can make mistakes or approve misleading proposals; exact presentation, explicit consent, authorization, and audit lower but do not eliminate human error.
- Database, operational, secret-management, deployment, and incident-response controls are not specified in sufficient detail to assess production exposure.
- Prototype testing may miss authorization, concurrency, logging, or isolation defects. No test result in this model is a security certification.
- Simulated tools reduce direct impact in MVP, but unsafe patterns could be copied into future integrations. Each future integration requires a new threat assessment and explicit review.
- Audit logs can be incomplete or sensitive; retention, access separation, and integrity mechanisms remain open.

## 22. Assumptions and Open Security Decisions

### Assumptions

- The MVP uses controlled demo identities and synthetic/demo knowledge by default.
- The MVP tool catalog is small, documented, allow-listed, validated, and simulation-only.
- Java 21/Spring Boot is authoritative for identity context, deterministic authorization, approval state, request status, and tool gating.
- Python/FastAPI and LangGraph provide bounded AI processing/orchestration and do not directly execute tools.
- MariaDB 10.11.16 supports organization-scoped relational persistence; the separate retrieval architecture remains unresolved.
- Redis is not required unless justified; Docker is planned later.
- No production readiness, certification, compliance, or SLA is implied.

### Open security decisions

1. **LLM provider:** Gemini or OpenAI selection; provider data handling, retention, region, training/use terms, and organizational approval.
2. **Authentication:** Demo authentication mechanism, session/token lifecycle, user provisioning, and future identity-provider integration.
3. **Service-to-service trust:** FastAPI authentication, internal network exposure, and transport protection for Spring Boot-to-FastAPI/provider traffic.
4. **Approval expiry:** Numerical validity period, configuration owner, and exact invalidation semantics before approvals are enabled.
5. **Data lifecycle:** Conversation, feedback, knowledge, and audit retention/deletion periods; define before real employee data.
6. **Secrets management:** Approved storage, rotation, access, and deployment mechanism; secrets must never enter source control/Git, prompts, or routine logs.
7. **Database protections:** Database roles, encryption/key management, backups, restore, migration control, and operational access.
8. **API abuse controls:** Request size limits, rate/concurrency limits, provider cost guardrails, and abuse response for the selected deployment.
9. **Audit operations:** Retention, read-access approval, integrity/tamper-resistance, monitoring integration, and incident response ownership.
10. **Configuration governance:** Review/approval process for tool allow-list, policy, role, provider, and knowledge changes, including emergency disablement ownership.
11. **Deployment context:** Hosting, network boundaries, browser support, TLS/secret provisioning, and environment separation.
12. **Real-data gate:** Explicit security/privacy/product approval before any non-synthetic organizational data or live integration is considered.

Until these decisions are resolved, use synthetic/demo data; do not enable a capability that depends on an unreviewed trust relationship; and do not assert that a control exists without implementation evidence.

## 23. Security Acceptance Criteria

The MVP security design is acceptable for a controlled synthetic-data demonstration only when:

1. Protected operations use authenticated, server-resolved user and organization context; client and model-supplied identity cannot change the principal.
2. The Spring Boot business API makes deterministic authorization decisions for protected reads, approvals, state transitions, and simulated actions; no LLM output can authorize.
3. A two-organization test suite demonstrates denial of cross-organization request, knowledge, proposal, approval, and audit access. Permission filtering occurs before knowledge content reaches the model.
4. Direct and indirect prompt injection tests using ticket descriptions and retrieved documents do not change permissions, workflow allow-lists, approval state, or tool invocation authority.
5. LLM output, citations, statuses, and tool arguments are validated; fabricated sources and unregistered/malformed operations are rejected or suppressed.
6. Every enabled MVP simulated action requires an authorized human's explicit approval bound to the exact proposal; rejection, expiry, mutation, missing approval, or failed revalidation produces zero execution.
7. The only reachable MVP tool effects are simulations; no production credentials or live enterprise-system integrations are available to the workflow.
8. Timeout, provider outage, invalid output, persistence failure, ambiguous tool result, and retry cases fail safely and do not present false success or create an unsafe duplicate effect.
9. An authorized reviewer can correlate representative request, retrieval, policy, approval, and tool events; audit reads are organization-scoped and routine logs exclude secrets and unnecessary sensitive content.
10. Secrets are not committed to Git or placed in prompts/logs. Provider and secret-handling decisions are documented before any real data is processed.
11. Optional feedback and knowledge administration remain disabled unless enabled and authorized as bounded MVP-Should capabilities with the relevant security checks.
12. Results are reported as prototype test evidence only; no production-readiness, certification, or compliance claim is made.

## 24. Traceability to SRS Requirements

The following matrix maps each threat group to existing SRS requirement identifiers. IDs are not new requirements and are not changed by this threat model.

| Threat ID | Threat focus | Related SRS requirement IDs |
|---|---|---|
| THR-001 | Identity and organization spoofing | SRS-AUTH-001, SRS-AUTH-002, SRS-AUTH-004, SRS-AUTHZ-001, SRS-AUTHZ-002, SRS-API-002 |
| THR-002 | Request and status tampering | SRS-FR-001, SRS-FR-002, SRS-AUTHZ-003, SRS-TEN-002, SRS-TKT-001, SRS-TKT-007, SRS-API-002 |
| THR-003 | Approval/action repudiation | SRS-FR-011, SRS-FR-012, SRS-HITL-009, SRS-TOOL-009, SRS-AUD-001, SRS-AUD-002, SRS-AUD-006, SRS-AUD-009 |
| THR-004 | Information disclosure | SRS-FR-003, SRS-FR-019, SRS-NFR-003, SRS-TEN-003, SRS-RAG-009, SRS-AUD-005, SRS-API-008, SRS-SEC-008, SRS-SEC-010 |
| THR-005 | Resource exhaustion / DoS | SRS-NFR-005, SRS-NFR-015, SRS-NFR-016, SRS-NFR-017, SRS-AI-002, SRS-API-009, SRS-ERR-004, SRS-ERR-005, SRS-ERR-006, SRS-PR-004 |
| THR-006 | AI authorization/tool privilege escalation | SRS-FR-006, SRS-FR-007, SRS-FR-008, SRS-FR-009, SRS-NFR-001, SRS-AI-006, SRS-TOOL-001, SRS-TOOL-003, SRS-TOOL-005, SRS-TOOL-006, SRS-SEC-001, SRS-SEC-002, SRS-SEC-006 |
| THR-007 | Direct prompt injection | SRS-NFR-018, SRS-AI-007, SRS-AI-004, SRS-AI-006, SRS-FR-006, SRS-TEST-007 |
| THR-008 | Indirect prompt injection | SRS-NFR-018, SRS-RAG-007, SRS-RAG-008, SRS-AI-007, SRS-SEC-005, SRS-TEST-007 |
| THR-009 | RAG/cross-tenant leakage | SRS-FR-003, SRS-FR-015, SRS-NFR-002, SRS-TEN-001, SRS-TEN-002, SRS-TEN-003, SRS-TEN-005, SRS-TEN-006, SRS-RAG-002, SRS-RAG-003, SRS-DB-002, SRS-TEST-006 |
| THR-010 | Knowledge poisoning and source integrity | SRS-FR-004, SRS-FR-005, SRS-FR-017, SRS-RAG-002, SRS-RAG-004, SRS-RAG-005, SRS-RAG-008, SRS-RAG-009, SRS-TOOL-010, SRS-AUD-001 |
| THR-011 | Hallucinated tool arguments | SRS-FR-007, SRS-FR-008, SRS-FR-010, SRS-FR-014, SRS-TOOL-001, SRS-TOOL-002, SRS-TOOL-004, SRS-HITL-003, SRS-HITL-005, SRS-NFR-010 |
| THR-012 | Excessive/unauthorized agent autonomy | SRS-FR-006, SRS-NFR-001, SRS-NFR-017, SRS-NFR-018, SRS-AI-002, SRS-AI-003, SRS-AI-006, SRS-API-005, SRS-TOOL-003, SRS-TEST-002, SRS-TEST-007 |
| THR-013 | Invalid/untrusted LLM output | SRS-FR-004, SRS-FR-005, SRS-FR-020, SRS-AI-004, SRS-API-004, SRS-API-008, SRS-NFR-006, SRS-RAG-005, SRS-RAG-009, SRS-ERR-004 |
| THR-014 | Provider data exposure | SRS-FR-019, SRS-NFR-003, SRS-NFR-013, SRS-AI-001, SRS-AI-009, SRS-API-006, SRS-SEC-008, SRS-SEC-010, SRS-AUD-005 |
| THR-015 | Approval spoofing, replay, or mutation | SRS-FR-009, SRS-FR-010, SRS-FR-011, SRS-HITL-001, SRS-HITL-002, SRS-HITL-004, SRS-HITL-005, SRS-HITL-006, SRS-HITL-007, SRS-HITL-008, SRS-NFR-010, SRS-DB-005, SRS-DB-006, SRS-TEST-008 |
| THR-016 | Internal service spoofing/tampering | SRS-API-004, SRS-API-005, SRS-API-009, SRS-AI-001, SRS-AI-004, SRS-NFR-009, SRS-SEC-010 |
| THR-017 | Database disclosure or integrity loss | SRS-TEN-002, SRS-TEN-007, SRS-DB-001, SRS-DB-003, SRS-DB-005, SRS-DB-006, SRS-DB-007, SRS-TEST-005, SRS-TEST-006 |
| THR-018 | Audit tampering/disclosure/repudiation | SRS-FR-012, SRS-NFR-004, SRS-NFR-011, SRS-AUD-001, SRS-AUD-002, SRS-AUD-004, SRS-AUD-005, SRS-AUD-006, SRS-AUD-007, SRS-AUD-008, SRS-AUD-009 |
| THR-019 | Configuration/allow-list tampering | SRS-FR-017, SRS-AUTHZ-005, SRS-AUTHZ-006, SRS-TOOL-001, SRS-TOOL-010, SRS-RAG-008, SRS-AUD-001, SRS-AUD-002 |
| THR-020 | Provider and workflow denial of service | SRS-NFR-005, SRS-NFR-007, SRS-NFR-015, SRS-NFR-016, SRS-NFR-017, SRS-API-009, SRS-ERR-004, SRS-ERR-005, SRS-ERR-006, SRS-PR-001, SRS-PR-002 |
| THR-021 | Duplicate/ambiguous simulated execution | SRS-FR-011, SRS-FR-020, SRS-NFR-005, SRS-NFR-010, SRS-NFR-016, SRS-NFR-019, SRS-TKT-007, SRS-TOOL-007, SRS-TOOL-008, SRS-TOOL-009, SRS-DB-006, SRS-ERR-007 |

All referenced identifiers above are intended to refer to definitions in the existing SRS. The SRS remains authoritative for requirement wording and status.
