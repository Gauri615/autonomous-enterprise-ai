# Runtime Sequence Diagrams: Autonomous Enterprise AI System

## 1. Document Control

| Field | Value |
|---|---|
| Document | Runtime Sequence Diagrams |
| Project | Autonomous Enterprise AI System |
| Version | 1.0 |
| Status | Draft |
| Date | 2026-11-13 |
| Authority | Consistent with the approved PRD, SRS, Use Cases, Threat Model, and System Architecture |

This document describes logical MVP interactions. It does not assert that the components or controls have been implemented.

## 2. Purpose and Scope

These diagrams illustrate the important runtime interactions between the React client, authoritative Spring Boot business API, bounded Python FastAPI/LangGraph workflow, MariaDB 10.11.16, the unresolved Knowledge Agent retrieval component, deterministic policy and approval modules, and simulated tool adapter.

The diagrams cover the approved internal IT-support request workflow only. They do not introduce live enterprise integrations, an external identity provider, a message broker, or additional deployable services. Approval wait is outside the answer-only processing deadline. Specific API paths, wire formats, authentication mechanics, tool timeout values, and provider selection remain implementation decisions.

## 3. Architecture Participants and Responsibilities

Participants named below are logical components, not necessarily independently deployed services.

| Participant | Responsibility |
|---|---|
| Employee / Approver / IT Analyst / Reviewer | Authenticated human actors with role- and organization-scoped capabilities. |
| React Client | Presents request, response, status, source, approval, and review experiences and uses the agreed Axios API client for Spring Boot requests; client-side checks are not authoritative. |
| Spring Boot API | Authoritative business API, protected using the agreed Spring Security framework; derives trusted identity and organization context, validates inputs, applies policy, persists business state, and controls action execution. |
| Authentication Adapter | Applies the configured controlled-demo authentication mechanism; the specific protocol and identity provider remain undecided. |
| Policy Service | Logical Spring Boot module that makes deterministic authorization and action-eligibility decisions. |
| Knowledge Access | Logical Spring Boot module that applies organization and user permission constraints to retrieval. |
| Approval Module | Logical Spring Boot module that validates reviewer authority, records proposal-bound decisions, and revalidates approval state. |
| Tool Catalog / Simulation Adapter | Spring Boot allow-list and schema validation boundary, dispatching only approved MVP simulations. |
| Audit Module | Records attributable, correlated workflow events with minimized sensitive content. |
| MariaDB / Knowledge Agent retrieval | MariaDB 10.11.16 is the relational source of truth for organization-scoped application records, curated knowledge metadata, approval state, outcomes, and audit events. Retrieval technology is not selected. |
| FastAPI AI Service | Validates the bounded internal AI request/response contract and runs configured AI processing; it cannot authorize or execute tools. |
| LangGraph | Coordinates finite, request- and organization-scoped AI workflow steps. |
| Logical Agent Roles | Classification, Knowledge, Troubleshooting, and Reporting steps within the configured LangGraph workflow; they are not independent authorization principals. |
| LLM Provider | External provider behind the AI service's provider boundary; the provider is undecided. |

## 4. Sequence Diagram: User Authentication

The authentication mechanism is intentionally provider-neutral. JWT-based authentication is an initial direction in the architecture, subject to the unresolved authentication decision; this flow does not imply a specific identity-provider integration.

```mermaid
sequenceDiagram
    actor Employee
    participant UI as React Client
    participant API as Spring Boot API
    participant Auth as Authentication Adapter
    participant Policy as Policy Service
    participant DB as MariaDB
    participant Audit as Audit Module

    Employee->>UI: Open support application / authenticate
    UI->>API: Submit configured authentication material
    API->>Auth: Validate using configured demo mechanism
    alt Authentication is valid
        Auth-->>API: Verified identity result
        API->>DB: Resolve controlled demo user, organization, and assigned roles
        alt Trusted identity context is resolved
            DB-->>API: User and organization context
            API->>Policy: Check access to support application
            alt Access is permitted
                Policy-->>API: Deterministic allow
                API->>Audit: Record material authentication/access outcome
                Audit->>DB: Persist minimized correlated event
                DB-->>Audit: Persisted
                API-->>UI: Authenticated response and permitted capabilities
                UI-->>Employee: Display authorized experience
            else Access is denied
                Policy-->>API: Deterministic deny
                API->>Audit: Record material denial
                Audit->>DB: Persist denial event
                API-->>UI: Safe access-denied response
            end
        else Identity or organization is unresolved
            DB-->>API: No trusted context
            API-->>UI: Safe authentication failure; no protected data
        end
    else Authentication is invalid, missing, or expired
        Auth-->>API: Invalid authentication result
        API-->>UI: Authentication required / safe failure
    end
```

## 5. Sequence Diagram: Ticket Creation

“Ticket” means an internal support request. The API persists the request before starting the bounded AI workflow; the separate analysis sequence is detailed in Section 6.

```mermaid
sequenceDiagram
    actor Employee
    participant UI as React Client
    participant API as Spring Boot API
    participant Auth as Authentication Context
    participant Policy as Policy Service
    participant DB as MariaDB
    participant Audit as Audit Module
    participant AI as FastAPI AI Service

    Employee->>UI: Enter and submit IT issue
    UI->>API: Create internal support request
    API->>Auth: Resolve verified user and organization
    Auth-->>API: Trusted principal and organization
    API->>Policy: Authorize request creation and validate payload
    alt Request is valid and authorized
        Policy-->>API: Allow; organization scope confirmed
        API->>DB: Persist internal request and initial workflow state
        alt Request persistence succeeds
            DB-->>API: Request identifier and stored state
            API->>Audit: Record request creation with correlation
            Audit->>DB: Persist minimized audit event
            API->>AI: Start bounded workflow with minimum necessary context
            AI-->>API: Workflow result or truthful processing outcome
            API->>API: Validate AI result; retain API-owned status authority
            API->>DB: Persist permitted outcome and response references
            DB-->>API: Current stored outcome
            API-->>UI: Request identifier and confirmed current status/result
            UI-->>Employee: Display internal request and processing outcome
        else Request persistence fails
            DB-->>API: Persistence failure
            API-->>UI: Safe failure; request not reported as created
        end
    else Authentication, validation, or authorization fails
        Policy-->>API: Deny with safe reason
        API->>Audit: Record material denial where applicable
        API-->>UI: Safe validation/authentication/authorization error
    end
```

If AI processing fails or exceeds its deadline after the request is stored, the API reports an accurate unavailable/failed or escalated outcome; it does not claim an answer or action succeeded.

## 6. Sequence Diagram: AI Ticket Analysis

The model and agent workflow return recommendations, classifications, and possibly a structured proposal. Spring Boot validates the output and remains responsible for authorization and state changes.

```mermaid
sequenceDiagram
    participant API as Spring Boot API
    participant Policy as Policy Service
    participant AI as FastAPI AI Service
    participant Graph as LangGraph
    participant Classifier as Classification Agent
    participant Provider as LLM Provider
    participant DB as MariaDB
    participant Audit as Audit Module

    API->>Policy: Authorize bounded analysis for request and organization
    alt Analysis is authorized
        Policy-->>API: Allow minimum necessary context
        API->>AI: Correlated request, trusted scope, deadline, minimized text
        AI->>AI: Validate internal contract and request bounds
        AI->>Graph: Start isolated configured workflow
        Graph->>Classifier: Analyze untrusted request description
        Classifier->>Provider: Request bounded classification
        Provider-->>Classifier: Untrusted model output
        Classifier-->>Graph: Candidate classification and uncertainty
        Graph-->>AI: Bounded workflow result
        AI-->>API: Recommendation; optional structured proposal
        API->>API: Validate schema, correlation, references, and supported workflow
        alt Output is valid and in scope
            API->>DB: Persist permitted result and request outcome
            API->>Audit: Record correlated workflow outcome
            Audit->>DB: Persist minimized event
            API-->>API: Continue to answer, retrieval, escalation, or policy path
        else Output is malformed, unsupported, or unauthorized
            API->>Audit: Record rejected output / safe failure
            API-->>API: Do not change policy or execute an action
        end
    else Analysis is not authorized
        Policy-->>API: Deny
        API->>Audit: Record material denial
        API-->>API: Stop before AI processing
    end
```

## 7. Sequence Diagram: Knowledge Retrieval (RAG)

Organization and user permissions constrain candidates before retrieved content or source references are sent to FastAPI/LangGraph or the LLM. Retrieved content and any instructions within it remain untrusted.

```mermaid
sequenceDiagram
    participant API as Spring Boot API
    participant Policy as Policy Service
    participant Knowledge as Knowledge Access
    participant DB as MariaDB
    participant Retrieval as Knowledge Agent retrieval (undecided)
    participant AI as FastAPI AI Service
    participant Graph as LangGraph
    participant Provider as LLM Provider
    participant Audit as Audit Module

    API->>Policy: Authorize retrieval for verified user, organization, and request
    alt Retrieval is authorized
        Policy-->>API: Trusted permission scope
        API->>Knowledge: Search with trusted scope and bounded query
        Knowledge->>DB: Resolve current organization and document permission scope
        DB-->>Knowledge: Eligible document/chunk scope
        Knowledge->>Retrieval: Search only within eligible scope
        Retrieval-->>Knowledge: Candidate identifiers and relevance scores
        Knowledge->>DB: Recheck candidate status, organization, and current permissions
        DB-->>Knowledge: Eligible chunks and actual source references only
        Knowledge->>Audit: Record source references and access decision context
        Audit->>DB: Persist minimized retrieval event
        alt Eligible evidence is available
            Knowledge-->>API: Permission-filtered content and source references
            API->>AI: Forward eligible context only
            AI->>Graph: Compose using untrusted retrieved content
            Graph->>Provider: Request bounded grounded response
            Provider-->>Graph: Untrusted response and candidate citations
            Graph-->>AI: Response and source references
            AI-->>API: Bounded result
            API->>API: Verify citations against retrieved eligible sources
            API-->>API: Return verified source facts separately from suggestions
        else No eligible evidence is found
            Knowledge-->>API: Empty eligible result
            API-->>API: State limitation; offer clarification or escalation
        end
    else Retrieval is denied or scope is unresolved
        Policy-->>API: Deny
        API->>Audit: Record material denial without source content
        API-->>API: Do not query or disclose protected knowledge
    end
```

If MariaDB or the selected retrieval component is unavailable, the system must not represent unavailable evidence as verified or expose an unfiltered fallback.

## 8. Sequence Diagram: Troubleshooting Workflow

Troubleshooting steps are recommendations, not verified system changes. The workflow has no device, account, network, or enterprise-system access.

```mermaid
sequenceDiagram
    participant API as Spring Boot API
    participant Policy as Policy Service
    participant AI as FastAPI AI Service
    participant Graph as LangGraph
    participant Trouble as Troubleshooting Agent
    participant Provider as LLM Provider
    participant DB as MariaDB
    participant Audit as Audit Module
    actor Employee
    participant UI as React Client

    API->>Policy: Authorize request context and eligible evidence
    alt Authorized request context
        Policy-->>API: Allow scoped inputs
        API->>AI: Send minimum necessary request and eligible evidence
        AI->>Graph: Continue configured workflow
        Graph->>Trouble: Draft bounded diagnostic suggestions
        Trouble->>Provider: Generate recommendations from untrusted input
        Provider-->>Trouble: Untrusted candidate steps and claims
        Trouble-->>Graph: Steps, uncertainty, and source references
        Graph-->>AI: Bounded troubleshooting result
        AI-->>API: Recommendation; no authorization result
        API->>API: Validate output and verify source references
        alt Safe, supported response
            API->>DB: Persist response and current request outcome
            API->>Audit: Record workflow result
            Audit->>DB: Persist minimized correlated event
            API-->>UI: Facts, suggestions, uncertainty, and eligible sources
            UI-->>Employee: Display suggestions and escalation option
        else Insufficient evidence or invalid output
            API->>DB: Persist unavailable / clarification / escalation outcome
            API-->>UI: Safe limitation or escalation state
            UI-->>Employee: Display limitation and available next step
        end
    else Access denied
        Policy-->>API: Deny
        API-->>UI: Safe denied response
    end
```

## 9. Sequence Diagram: Policy Evaluation

Policy evaluation may permit a valid proposal to be shown to an authorized human. It never grants execution permission.

```mermaid
sequenceDiagram
    participant AI as FastAPI AI Service
    participant API as Spring Boot API
    participant Catalog as Tool Catalog
    participant Policy as Policy Service
    participant DB as MariaDB
    participant Audit as Audit Module
    participant UI as React Client

    AI-->>API: Return untrusted structured simulated-action proposal
    API->>API: Validate envelope, correlation, and trusted actor / organization
    API->>Catalog: Check exact tool identifier and parameter schema
    alt Tool or schema is invalid
        Catalog-->>API: Reject unknown tool or malformed/extra parameters
        API->>Audit: Record rejected proposal
        Audit->>DB: Persist correlated denial
        API-->>UI: No action; safe unsupported / rejected outcome
    else Tool and schema are valid
        Catalog-->>API: Registered simulation and validated schema
        API->>Policy: Evaluate organization, user role, action scope, and request
        alt Deterministic policy denies proposal
            Policy-->>API: Deny
            API->>Audit: Record policy denial
            Audit->>DB: Persist minimized decision event
            API-->>UI: Denied / escalated outcome; no execution
        else Proposal may be presented for review
            Policy-->>API: Eligible for human review only
            API->>DB: Persist exact proposal and approval-required state
            DB-->>API: Proposal reference and stored state
            API->>Audit: Record policy decision and proposal reference
            Audit->>DB: Persist correlated event
            API-->>UI: Show exact proposal, parameters, and simulation label
        end
    end
```

## 10. Sequence Diagram: Human Approval

Approval is an explicit, authenticated reviewer decision bound to the exact proposal. It does not itself execute the simulation or override deterministic policy.

```mermaid
sequenceDiagram
    actor Approver
    participant UI as React Client
    participant API as Spring Boot API
    participant Auth as Authentication Context
    participant Policy as Policy Service
    participant Approval as Approval Module
    participant DB as MariaDB
    participant Audit as Audit Module

    Approver->>UI: Open pending approval
    UI->>API: Request proposal details
    API->>Auth: Resolve verified reviewer and organization
    Auth-->>API: Trusted reviewer context
    API->>Policy: Authorize proposal read and action-specific review
    alt Reviewer may review this proposal
        Policy-->>API: Allow scoped review
        API->>DB: Load current proposal and approval state
        DB-->>API: Exact action, parameters, expected effect, simulation label
        API-->>UI: Display review context and exact proposal
        UI-->>Approver: Present approve / reject choices
        Approver->>UI: Explicitly approve or reject
        UI->>API: Submit decision for proposal reference
        API->>Auth: Reconfirm reviewer identity and organization
        Auth-->>API: Trusted reviewer context
        API->>Approval: Validate explicit decision and proposal binding
        Approval->>Policy: Recheck reviewer scope and current policy context
        alt Proposal unchanged, decision authorized, and state current
            Policy-->>Approval: Allow recording decision
            Approval->>DB: Persist proposal-bound decision and expiry state
            alt Decision persistence succeeds
                DB-->>Approval: Decision recorded
                Approval->>Audit: Record approver, decision, time, and proposal
                Audit->>DB: Persist minimized audit event
                alt Required audit event is persisted
                    DB-->>Audit: Audit event stored
                    Audit-->>Approval: Recording confirmed
                    Approval-->>API: Approved or rejected; execution not yet performed
                    API-->>UI: Recorded decision and current status
                else Required audit event cannot be persisted
                    DB-->>Audit: Persistence failure
                    Audit-->>Approval: Recording not confirmed
                    Approval-->>API: Not safely recorded
                    API-->>UI: Failure; no execution permitted
                end
            else Decision persistence fails
                DB-->>Approval: Decision persistence failure
                Approval-->>API: Not safely recorded
                API-->>UI: Failure; no execution permitted
            end
        else Unauthorized, changed, expired, or stale proposal
            Policy-->>Approval: Deny or invalidate
            Approval->>Audit: Record denial / invalidation
            Approval-->>API: No valid approval state
            API-->>UI: Safe denial or request for a new review
        end
    else Reviewer lacks permission or organization scope
        Policy-->>API: Deny without revealing protected proposal
        API-->>UI: Safe access-denied response
    end
```

An approval that expires or whose proposal or relevant policy context changes cannot be used for execution; a changed proposal requires a new review.

## 11. Sequence Diagram: Simulated Tool Execution

Only the Spring Boot API can invoke the allow-listed simulation adapter. The AI service and LangGraph have no execution path.

```mermaid
sequenceDiagram
    actor Approver
    participant UI as React Client
    participant API as Spring Boot API
    participant Auth as Authentication Context
    participant Approval as Approval Module
    participant Policy as Policy Service
    participant Catalog as Tool Catalog
    participant DB as MariaDB
    participant Tool as Simulated Tool Adapter
    participant Audit as Audit Module

    Approver->>UI: Request execution of previously approved proposal
    UI->>API: Submit proposal reference
    API->>Auth: Resolve verified actor and organization
    Auth-->>API: Trusted context
    API->>Approval: Load current proposal-bound approval state
    Approval->>DB: Read proposal, decision, expiry, and execution state
    DB-->>Approval: Current persisted state
    API->>Policy: Revalidate actor, organization, action, and current authorization
    API->>Catalog: Revalidate allow-list, schema, and simulation-only adapter
    alt Approval invalid, missing, expired, changed, or policy denied
        Policy-->>API: Deny execution
        API->>Audit: Record rejected execution attempt
        Audit->>DB: Persist correlated denial
        API-->>UI: No execution; truthful denied / expired state
    else Approval and policy are current
        Policy-->>API: Permit this simulation only
        Catalog-->>API: Valid registered simulation
        API->>DB: Persist execution-start state / duplicate guard
        alt Execution may proceed
            DB-->>API: State recorded; no prior completed effect
            API->>Tool: Invoke allow-listed simulated action with validated parameters
            alt Simulation returns confirmed outcome within timeout
                Tool-->>API: Simulated result
                API->>DB: Persist outcome and final execution state
                alt Outcome persistence succeeds
                    DB-->>API: Outcome stored
                    API->>Audit: Record tool, proposal, actor, timing, and outcome
                    Audit->>DB: Persist required correlated audit event
                    alt Required audit event is persisted
                        DB-->>Audit: Audit event stored
                        Audit-->>API: Required audit confirmed
                        API-->>UI: Confirmed simulated result
                    else Critical audit persistence failure
                        DB-->>Audit: Audit persistence failure
                        Audit-->>API: Required audit not confirmed
                        API-->>UI: Failure / unknown; do not claim success or blindly retry
                    end
                else Outcome persistence fails
                    DB-->>API: Outcome persistence failure
                    API->>Audit: Record persistence failure where available
                    API-->>UI: Failure / unknown; do not claim success or blindly retry
                end
            else Simulation rejects, fails, or times out
                Tool-->>API: Rejected / failed / ambiguous outcome
                API->>DB: Persist failed or unknown state where possible
                API->>Audit: Record attempted execution and outcome
                API-->>UI: Failed / unknown; no success claim
            end
        else Duplicate, stale, or concurrent invocation
            DB-->>API: Execution not eligible
            API->>Audit: Record rejected repeated attempt
            API-->>UI: Existing authoritative status; no duplicate execution
        end
    end
```

No live enterprise system is contacted. An ambiguous timeout is not automatically retried; execution status must be established before any duplicate-safe retry is considered.

## 12. Sequence Diagram: Ticket Resolution and Reporting

This flow combines permitted internal request-status transitions with an optional, clearly labeled AI summary for an authorized analyst. The API owns the recorded request outcome.

```mermaid
sequenceDiagram
    actor Analyst as IT Support Analyst
    participant UI as React Client
    participant API as Spring Boot API
    participant Auth as Authentication Context
    participant Policy as Policy Service
    participant DB as MariaDB
    participant AI as FastAPI AI Service
    participant Graph as LangGraph
    participant Reporter as Reporting Agent
    participant Provider as LLM Provider
    participant Audit as Audit Module

    Analyst->>UI: Open authorized internal support request
    UI->>API: Request request details and current outcome
    API->>Auth: Resolve analyst identity and organization
    Auth-->>API: Trusted context
    API->>Policy: Authorize request read and permitted transition
    alt Analyst is authorized
        Policy-->>API: Allow scoped request access
        API->>DB: Load current request state and permitted context
        DB-->>API: Authorized request data
        opt Analyst requests an AI summary
            API->>AI: Send minimum necessary authorized context
            AI->>Graph: Start bounded summary step
            Graph->>Reporter: Summarize user-reported facts and known evidence
            Reporter->>Provider: Request concise draft
            Provider-->>Reporter: Untrusted summary
            Reporter-->>Graph: Candidate summary and uncertainty
            Graph-->>AI: Bounded AI-generated summary
            AI-->>API: Summary for validation
            API->>API: Validate response and label it as AI-generated
            API-->>UI: Display summary separate from authoritative state
        end
        Analyst->>UI: Submit only a defined workflow event / status transition
        UI->>API: Request permitted transition or escalation
        API->>Policy: Recheck actor, organization, resource, and transition
        alt Transition is allowed
            Policy-->>API: Allow defined transition
            API->>DB: Persist API-authoritative status and outcome
            API->>Audit: Record transition and correlation
            Audit->>DB: Persist minimized event
            API-->>UI: Confirmed current stored status
        else Access or transition is denied
            Policy-->>API: Deny
            API-->>UI: Safe denial; stored state unchanged
        end
    else Analyst lacks request access
        Policy-->>API: Deny
        API-->>UI: Safe denial; no request context sent to AI
    end
```

The summary is not a status update, verified fact, approval, or audit record. No external service-desk ticket is created, assigned, or resolved.

## 13. Sequence Diagram: Agent Failure, Timeout and Retry

Failures are explicit. Only a transient, idempotent downstream read may be retried once, and only if the remaining end-to-end deadline allows it. AI/provider calls, policy denials, invalid inputs, and simulated action execution are not automatically retried by this flow.

```mermaid
sequenceDiagram
    participant API as Spring Boot API
    participant Policy as Policy Service
    participant AI as FastAPI AI Service
    participant Graph as LangGraph
    participant Knowledge as Knowledge Access
    participant DB as MariaDB
    participant Retrieval as Knowledge Agent retrieval (undecided)
    participant Provider as LLM Provider
    participant Audit as Audit Module
    participant UI as React Client

    API->>Policy: Authorize request and establish deadline
    alt Identity or policy check fails
        Policy-->>API: Deny / unavailable
        API->>Audit: Record material failure
        API-->>UI: Safe denied or unavailable state
    else Request is authorized
        Policy-->>API: Allow bounded processing
        API->>AI: Invoke bounded workflow with remaining deadline
        AI->>Graph: Start configured workflow
        Graph->>Knowledge: Request permission-filtered evidence
        Knowledge->>DB: Resolve current organization and user permission scope
        DB-->>Knowledge: Eligible document/chunk scope
        Knowledge->>Retrieval: Search only within eligible scope
        alt Transient idempotent retrieval read fails
            Retrieval-->>Knowledge: Temporary search failure
            Knowledge->>Retrieval: Retry same scoped read once only if deadline remains
            Retrieval-->>Knowledge: Result or second failure
        else Retrieval succeeds initially
            Retrieval-->>Knowledge: Candidate identifiers and relevance scores
            Knowledge->>DB: Recheck candidate status, organization, and current permissions
            DB-->>Knowledge: Permission-filtered result
        end
        alt Workflow reaches provider before deadline
            Knowledge-->>Graph: Eligible content or no-evidence result
            Graph->>Provider: Bounded generation request
            alt Provider returns before deadline
                Provider-->>Graph: Untrusted output
                Graph-->>AI: Bounded result
                AI-->>API: Workflow response
                API->>API: Validate output; keep policy authoritative
                API->>Audit: Record outcome
                API-->>UI: Validated answer or limitation
            else Provider times out / is unavailable
                Provider--xGraph: Timeout / unavailable
                Graph-->>AI: Terminate; no valid recommendation
                AI-->>API: Explicit timeout/failure
                API->>Audit: Record failure and correlation
                API-->>UI: Unavailable / failed or escalation state
            end
        else Deadline expires or a required service fails
            Knowledge-->>Graph: Failure / deadline exhausted
            Graph-->>AI: Stop workflow safely
            AI-->>API: Explicit unavailable/failed result
            API->>Audit: Record failure where persistence is available
            API-->>UI: Accurate failed / unavailable or escalation state
        end
    end
```

The request deadline is at most 10 seconds for the MVP answer-only path; approval wait is excluded. Failure of AI processing, retrieval, or the provider cannot authorize, approve, or trigger a tool.

## 14. Sequence Diagram: Audit Event Recording

Material events are recorded by the business application with actor/service, organization, time, correlation, outcome, and relevant resource references. Routine events omit credentials, full prompts, full documents, and unnecessary personal data.

```mermaid
sequenceDiagram
    participant API as Spring Boot API
    participant Policy as Policy Service
    participant Approval as Approval Module
    participant Tool as Simulated Tool Adapter
    participant Audit as Audit Module
    participant DB as MariaDB
    actor Reviewer
    participant UI as React Client

    API->>Policy: Evaluate protected operation
    Policy-->>API: Deterministic decision and correlation context
    API->>Audit: Record request / policy / workflow event
    Audit->>DB: Persist minimized organization-scoped event
    alt Audit event is persisted
        DB-->>Audit: Stored event reference
        Audit-->>API: Recording confirmed
        opt Approval or simulated execution occurs
            API->>Approval: Record proposal-bound approval decision
            Approval->>Audit: Record reviewer, decision, time, and proposal reference
            Audit->>DB: Persist approval event
            API->>Tool: Invoke only after approval and policy revalidation
            Tool-->>API: Simulated outcome
            API->>Audit: Record tool attempt and confirmed / failed outcome
            Audit->>DB: Persist tool event
        end
        Reviewer->>UI: Request authorized audit review
        UI->>API: Query scoped event references
        API->>Policy: Authorize reviewer and organization scope
        alt Audit read is authorized
            Policy-->>API: Allow scoped read
            API->>DB: Query permitted event metadata
            DB-->>API: Correlated minimized events
            API-->>UI: Authorized event metadata
        else Audit read is denied
            Policy-->>API: Deny
            API-->>UI: Safe denial without event disclosure
        end
    else Critical audit persistence fails
        DB-->>Audit: Persistence failure
        Audit-->>API: Recording not confirmed
        API-->>API: Stop approval / execution success response; expose operational failure
    end
```

Sensitive approval or execution workflows must not be reported as fully successful when a required critical audit event cannot be persisted. Audit retention duration and any external observability backend remain undecided.

## 15. Cross-Service Communication Rules

1. The React client calls Spring Boot business APIs; it does not call FastAPI, LangGraph, MariaDB, retrieval components, policy modules, or simulated tools directly.
2. Spring Boot is authoritative for authentication context, organization scope, authorization, request state, approval state, policy evaluation, and tool execution.
3. Spring Boot calls FastAPI only through the defined internal contract, with minimum necessary data, trusted scope, correlation context, and a finite deadline. FastAPI has no public tool-execution operation.
4. FastAPI runs only configured LangGraph steps and returns bounded recommendations, source references, or structured proposals. These outputs are untrusted until validated by Spring Boot.
5. MariaDB 10.11.16 is the relational source of truth. The retrieval component is undecided; all candidates must be checked against current organization and permission scope, and relevance is not authorization.
6. Only Spring Boot may invoke a tool, and only after allow-list/schema validation, explicit authorized human approval, and execution-time policy revalidation. The adapter is simulation-only in the MVP.
7. Material events share request/correlation and organization context. Sensitive event content is minimized; secrets and unnecessary personal data are excluded.
8. No cache, queue, external identity provider, ticketing system, or production enterprise connector is assumed by these sequences.

## 16. Security Considerations

- Treat browser fields, user/ticket text, retrieved documents, model output, candidate citations, and tool arguments as untrusted.
- Resolve user and organization context from verified server-side authentication; reject organization or role claims from prompts, documents, or client input.
- Apply deterministic access and tenant checks to every protected read, write, retrieval, approval, and execution. Client behavior and AI output cannot grant authority.
- Filter knowledge by organization and user permissions before it reaches FastAPI/LangGraph or the LLM; validate displayed citations against retrieved eligible sources.
- Present AI-generated facts, suggestions, uncertainty, and summaries distinctly. A recommendation is not a deterministic decision or a confirmed action.
- Bind approval to the exact proposal and invalidate it on material proposal/policy changes or expiry. Revalidate immediately before execution.
- Restrict the MVP tool catalog to schema-validated simulations. No live integrations, production credentials, arbitrary shell, database, network, or enterprise actions are allowed.
- Keep secrets outside source control, prompts, and routine logs. Use synthetic/demo data until provider data handling and organizational review are resolved.
- Scope audit reads by reviewer permission and organization. Do not use an AI-generated narrative as evidence of a recorded event.

## 17. Error Handling and Recovery

| Failure | Required runtime behavior |
|---|---|
| Authentication or organization resolution failure | Fail closed before protected access; return a safe error without disclosing records or knowledge. |
| Deterministic policy denial or unavailable policy context | Stop the protected operation; do not retry denial to bypass controls and do not invoke AI/tool paths that require authorization. |
| MariaDB/retrieval failure or no eligible evidence | Do not provide an unsupported verified answer; report the limitation and offer clarification/escalation where available. |
| FastAPI, LangGraph, or LLM timeout/invalid output | Reject incomplete or invalid output; mark the request unavailable/failed or offer escalation; never authorize or execute an action from the failure path. |
| Answer-only deadline expiry | Stop processing and return an accurate unavailable/failed or escalated state within the defined error handling. |
| Approval rejection, expiry, changed proposal, or failed revalidation | Terminate the action path; require a fresh proposal/review where applicable. |
| Tool timeout or ambiguous result | Report failed/unknown as appropriate; do not claim success or automatically repeat an ambiguous execution. |
| Critical state or audit persistence failure | Do not report a fully successful approval/execution; expose a truthful operational failure and prevent unsafe replay. |
| Transient idempotent read failure | Retry at most once only when the end-to-end deadline permits. No unbounded retries. |

The specific per-service timeout values and approval validity period are configuration decisions; this document does not invent numerical values beyond the SRS's 10-second maximum answer-only deadline.

## 18. Traceability to Use Cases and SRS

The following mappings identify the principal scenario and requirements represented in each sequence. They do not replace the complete traceability matrices in the SRS and System Architecture.

| Sequence | Related Use Cases | Principal SRS requirements |
|---|---|---|
| User Authentication | UC-001 | SRS-AUTH-001–006; SRS-AUTHZ-001, SRS-API-002, SRS-ERR-002 |
| Ticket Creation | UC-002 | SRS-FR-001, SRS-FR-002, SRS-FR-018; SRS-TKT-001; SRS-API-001, SRS-API-002, SRS-API-004; SRS-ERR-001 |
| AI Ticket Analysis | UC-004, UC-007 | SRS-FR-006, SRS-FR-020; SRS-AI-001–008; SRS-API-004, SRS-API-009; SRS-ERR-004, SRS-ERR-005, SRS-ERR-010 |
| Knowledge Retrieval (RAG) | UC-005 | SRS-FR-003–005, SRS-FR-015; SRS-TEN-002–004; SRS-RAG-001–009; SRS-SEC-004; SRS-ERR-003 |
| Troubleshooting Workflow | UC-006, UC-007 | SRS-FR-004, SRS-FR-005, SRS-FR-013; SRS-AI-002, SRS-AI-004–007; SRS-RAG-005, SRS-RAG-006, SRS-NFR-006 |
| Policy Evaluation | UC-008 | SRS-FR-006–008, SRS-FR-014; SRS-AUTHZ-001, SRS-AUTHZ-002, SRS-AUTHZ-007; SRS-TOOL-001–004; SRS-AUD-001, SRS-AUD-002 |
| Human Approval | UC-009 | SRS-FR-009–011, SRS-FR-014; SRS-HITL-001–009; SRS-AUTHZ-004, SRS-AUTHZ-008, SRS-AUTHZ-009; SRS-AUD-001, SRS-AUD-002, SRS-AUD-009 |
| Simulated Tool Execution | UC-010 | SRS-FR-007–012; SRS-HITL-007–010; SRS-TOOL-001–009; SRS-DB-005, SRS-DB-006; SRS-ERR-007–009 |
| Ticket Resolution and Reporting | UC-003, UC-011, UC-012 | SRS-FR-013, SRS-FR-018; SRS-AUTHZ-003; SRS-TKT-002–004; SRS-AI-001, SRS-AI-002, SRS-AI-004; SRS-REP-001, SRS-AUD-001 |
| Agent Failure, Timeout and Retry | UC-002, UC-004, UC-005, UC-007 | SRS-NFR-005, SRS-NFR-015–018; SRS-ERR-003–007; SRS-PR-002–006; SRS-AI-006 |
| Audit Event Recording | UC-008–UC-010, UC-013 | SRS-AUD-001–009; SRS-AUTHZ-007; SRS-DB-007; SRS-ERR-009 |

UC-014 (optional MVP-Should knowledge administration) and UC-015 (optional MVP-Should feedback) remain within their existing use-case and SRS scope but are not expanded into separate runtime sequences here. No new behavior or requirements are implied.

## 19. Open Decisions

The following decisions remain unresolved in the source documents and are not selected by these diagrams:

- Authentication protocol, JWT issuer/signing and lifecycle details, and any identity-provider integration.
- Internal Spring Boot-to-FastAPI service authentication and transport protection.
- LLM provider selection, data handling/retention terms, and approval before any non-synthetic organizational data is processed.
- Exact API paths, request/response schemas, asynchronous versus synchronous implementation details, and provider-specific configuration.
- Per-service timeout values, approval validity duration, and operational handling of an ambiguous simulated-tool outcome.
- MariaDB schema/migrations, deployment and database protections, Knowledge Agent retrieval selection, audit retention/deletion, and any external observability backend.
- Whether optional knowledge administration or response feedback is enabled for a given MVP demonstration.

Resolution of these items must remain consistent with the PRD, SRS, Use Cases, Threat Model, and System Architecture. No open decision permits an AI agent to authorize an operation or adds live integration to the MVP.
