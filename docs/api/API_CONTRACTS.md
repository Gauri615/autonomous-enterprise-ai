# API Contracts: Autonomous Enterprise AI System

## 1. Document Control

| Field | Value |
|---|---|
| Document | API Contracts |
| Project | Autonomous Enterprise AI System |
| Version | 1.0 |
| Status | Draft |
| Date | 2026-11-13 |
| Authority | Approved PRD, SRS, Use Cases, Threat Model, System Architecture, Sequence Diagrams, and Database ER Diagram |

This document defines proposed logical contracts for review and implementation planning. It does not create, expose, or claim implementation of actual endpoints.

## 2. API Design Objectives

- Give the React + TypeScript frontend one authoritative public business API: Java 21+ / Spring Boot.
- Keep Python FastAPI and LangGraph behind a private, bounded service-to-service contract.
- Make request, error, status, authorization, approval, and audit semantics consistent and testable.
- Bind all protected operations to trusted server-resolved identity, organization, and membership context.
- Distinguish model recommendations from backend policy decisions, human decisions, and confirmed simulated results.
- Support the approved MVP with internal `SupportRequest` records and allow-listed simulated actions only.
- Avoid choosing an unresolved authentication mechanism, LLM provider, endpoint wire convention, or deployment topology without an explicit decision.

## 3. API Architecture and Responsibilities

| Boundary | Responsibility | Must not do |
|---|---|---|
| React + Axios -> Spring Boot | All browser-facing operations: authentication entry point, identities/memberships, support requests/messages, analysis requests/status, knowledge reads, approvals, simulations, audit review, and bounded reporting. | Call FastAPI, LangGraph, MariaDB, a retrieval component, or tool adapters directly. |
| Spring Boot API | Authoritative identity context, tenant scope, deterministic authorization, request state, validation, policy evaluation, approvals, simulated execution, persistence, and audit decisions. | Treat AI output as authorization or report unconfirmed success. |
| Spring Boot -> FastAPI | Internal, bounded AI request with minimum necessary trusted context, eligible knowledge, correlation, and deadline. Validate all response data. | Delegate business state or execution authorization. |
| FastAPI -> LangGraph | Run only configured request-scoped workflow steps. | Grant access, approve actions, or call tools. |
| LangGraph -> model provider | Provider-isolated generation using task-relevant, permission-eligible data. Provider remains undecided. | Send secrets or unauthorized knowledge; use model output as policy. |
| Spring Boot -> MariaDB / retrieval component | Persist organization-scoped relational entities and metadata; coordinate permission-filtered retrieval and referentially consistent workflow state. Retrieval technology is undecided. | Trust an identifier, relevance score, or client organization claim as authorization. |

“Policy service”, approval module, audit module, knowledge access, and tool catalog are logical Spring Boot modules, not new microservices.
Only Spring Boot may invoke a tool adapter. FastAPI and LangGraph have no tool-execution endpoint or direct execution path.

## 4. API Standards and Conventions

- Proposed public API is JSON over HTTPS in a deployed environment; exact local transport is deployment-specific.
- UTF-8 JSON uses `camelCase`; timestamps are RFC 3339 UTC strings.
- IDs are opaque UUID strings. Possession of an ID grants no access.
- Public resource names use plural nouns. `/api/v1/support-requests` is the canonical public route naming convention for the tenant-owned `SupportRequest` resource.
- Public operations use `/api/v1`; internal AI processing uses a separately protected `/internal/v1` namespace.
- Responses and errors use the common envelopes in Sections 9–10. `204 No Content` is used only when no representation is returned.
- Unknown fields are rejected on security-sensitive request bodies unless a specific contract explicitly allows an extension field.
- All organization, membership, role, document, proposal, and audit filters are derived or checked server-side.
- `Location` may identify a newly created resource; it is not proof of authorization.
- A request body MUST NOT include an authoritative `organizationId`, acting `identityPrincipalId`, acting `membershipId`, role, approval state, or policy outcome.
- `IdentityPrincipal` is the global identity. `ExternalIdentity` is the verified provider issuer/subject pair linked to that principal. `OrganizationMembership` is the organization-scoped actor and must be used for tenant-owned operations.

## 5. Base URLs and Versioning

The following are proposed logical bases, not selected deployment hostnames:

| Audience | Base path | Exposure |
|---|---|---|
| React client | `/api/v1` | Public business API served by Spring Boot. |
| FastAPI AI service | `/internal/v1` | Private service-to-service path accessible only to Spring Boot through deployment controls still to be decided. Never expose through the browser API gateway. |

Version in the path is the public contract major version. Additive optional response fields may be introduced compatibly; removing/renaming fields or changing semantics requires a version decision. The exact hostname, TLS termination, gateway, and internal network boundary are unresolved.

## 6. Authentication Contract

The authentication and authorization mechanisms described in this contract are proposed and are not implemented by the current Spring Boot foundation. Protected endpoints must not be exposed before a verified OIDC or approved session mechanism and trusted server-side identity-context resolution are implemented.

The SRS does not select a login protocol, identity provider, password flow, OAuth/OIDC profile, JWT issuer, cookie/session approach, or token lifecycle. Therefore:

- The login endpoint below is an abstract adapter contract for controlled demo identities; its `authentication` object is mechanism-specific and must be fixed in an architecture decision before implementation.
- Do not infer password authentication from the endpoint or sample. Never persist or log plaintext passwords or credentials.
- JWT-based authentication is an initial direction only, subject to the documented decision. If bearer JWT is selected, the API validates issuer/signature, expiry, and required claims; values are not trusted from request body fields.
- The authentication mechanism returns/establishes whatever client credential/session form is eventually approved. This document deliberately does not prescribe a token JSON response or cookie name.
- Every protected request resolves the verified external identity, global `IdentityPrincipal`, organization, and `OrganizationMembership` on the server. Missing or invalid identity, principal, organization, membership, or authorization fails closed.
- Exact OIDC issuer and opaque, case-sensitive subject matching is required. Identity must not be inferred from email or request fields.
- Platform permissions and organization permissions are separate. A membership or role in one organization grants no authority in another organization.
- The logout contract invalidates the current mechanism's client session/credential only to the extent that the selected mechanism supports invalidation; exact lifecycle is open.

## 7. Authorization and RBAC

Logical permissions below describe intended policy scopes; they are not fixed role names or an exhaustive policy language.

| Capability | Intended actor | Server-side rule |
|---|---|---|
| Own support request create/read/message/escalation | Employee | Authenticated active membership; same organization and permitted record ownership. |
| Scoped support request/membership review | IT support analyst | Explicit `MembershipRole`, organization, resource, and action scope. |
| Proposal review/decision | Authorized approver | Explicit reviewer authorization for the proposal's organization and action. |
| Knowledge curation | Organization administrator | Only when optional admin configuration is enabled; organization-scoped. |
| Audit read | Governance/audit reviewer | Explicit audit-read permission; administrator status alone is insufficient. |
| Metrics read | Authorized analyst/reviewer | Role- and organization-scoped; aggregate/minimized output. |

Spring Boot evaluates authorization for every protected read/write and again before simulated execution. AI, model, UI, role labels, request IDs, and approval records are not authority by themselves. A recorded approval does not bypass current deterministic policy.

## 8. Common Request Headers

| Header | Direction | Required | Meaning |
|---|---|---:|---|
| `Content-Type: application/json` | Request with body | Yes | JSON request representation. |
| `Accept: application/json` | Public request | Recommended | Preferred response format. |
| Configured authentication header/cookie | Public request | Protected paths | Exact name and scheme depend on the unresolved authentication decision; never use an AI/service credential in the browser. |
| `X-Correlation-ID` | Request/response | Optional inbound; always returned/generated | Opaque correlation value. The server validates length/format or replaces invalid input. Not an authorization credential. |
| `Idempotency-Key` | Selected POST requests | Required on create/action requests as noted | Client-generated opaque key scoped to authenticated actor, organization, route, and request-body fingerprint. Exact retention window is unresolved. |
| `If-Match` | Conditional update where enabled | Optional contract decision | If implemented, must be tied to current representation/version; unsupported preconditions return `428` or documented conflict. No ETag/version field is assumed by this document. |

The server must not echo credentials, authorization headers, or sensitive client-provided correlation data in response bodies or logs.

## 9. Common Response Structure

Public JSON success responses use:

```json
{
  "data": {},
  "meta": {
    "correlationId": "c0a80123-4567-489a-9abc-123456789abc"
  }
}
```

Collections additionally return:

```json
{
  "data": [],
  "meta": {
    "correlationId": "c0a80123-4567-489a-9abc-123456789abc",
    "page": {
      "limit": 20,
      "nextCursor": null
    }
  }
}
```

For `202 Accepted`, `data` contains the accepted resource/run reference and its current state; it does not mean AI processing or an action completed. Resource status is the backend's persisted state, not an AI narrative.

## 10. Error Response Standards

Errors use the stable flat envelope implemented by `ApiErrorResponse`:

```json
{
  "timestamp": "2026-11-13T07:55:00Z",
  "status": 400,
  "errorCode": "VALIDATION_FAILED",
  "message": "Request validation failed.",
  "path": "/api/v1/support-requests",
  "correlationId": "c0a80123-4567-489a-9abc-123456789abc",
  "fieldErrors": [
    {
      "field": "description",
      "message": "must not be blank"
    }
  ]
}
```

This is the existing implementation contract and the source of truth. The previous nested `error`/`details` shape is not used. The implementation exposes `timestamp`, `status`, `errorCode`, `message`, `path`, `correlationId`, and `fieldErrors[]`; no second or aliased envelope will be added.

Do not disclose stack traces, SQL, prompts, secrets, internal hostnames, unauthorized resource existence, or provider credentials. Expected status mapping:

| HTTP | Use |
|---:|---|
| 400 | Malformed JSON, invalid query/path syntax, unsupported fields, or validation errors. Return `VALIDATION_FAILED` or another existing 400-specific error code. |
| 401 | Missing or invalid OIDC/session authentication. Return `UNAUTHORIZED` when implemented. No principal or organization data is inferred in this state. |
| 403 | Authenticated caller lacks the required platform or organization capability. Return `FORBIDDEN`; use a concealed 404 only where policy requires hiding resource existence. |
| 404 | Resource not found or intentionally concealed by authorization policy. Return `RESOURCE_NOT_FOUND`. |
| 409 | State conflict, uniqueness conflict, duplicate/idempotency conflict, invalid lifecycle transition, stale proposal, or already-decided approval. Return `CONFLICT` when implemented. |
| 413 | Request exceeds documented size limit. |
| 415 | Unsupported media type. |
| 422 | Semantically invalid request when the implementation distinguishes it from 400; use consistently. |
| 429 | Rate limit exceeded; return `Retry-After` if meaningful. |
| 500 | Unexpected internal failure with safe message. Return `INTERNAL_SERVER_ERROR` and never disclose stack traces or persistence/provider details. |
| 503 | Required service unavailable or deadline expired before successful processing; truthful state only. |
| 504 | Upstream AI/internal processing deadline exceeded; no success or execution implied. |

Validation errors do not retry. Authorization/policy denials are not retryable. If a support request was persisted before AI failure, return its current persisted state rather than a success-shaped answer.

## 11. Authentication API Contracts

### POST `/api/v1/auth/login`

| Contract field | Definition |
|---|---|
| Purpose | Authenticate a controlled demo identity through the configured authentication adapter. |
| Authentication | Not required to begin authentication; apply abuse controls. |
| Required permission | None before login. |
| Path/query | None. |
| Request JSON | Mechanism-neutral envelope; `authentication` shape is intentionally unresolved. |
| Success | `200 OK`, common envelope with the authenticated principal/membership profile and permitted capability summary. The selected mechanism establishes its session/credential without this contract exposing a provider or token format. |
| Errors | `400`, `401`, `429`, `500`, `503`. Do not distinguish unknown identity if that would disclose account state. |
| Validation | Require configured authentication fields; reject client-supplied organization/role as authority; never log `authentication` values. |
| Traceability | SRS-AUTH-001–006, SRS-AUTHZ-001; UC-001. |

```json
{
  "identity": "demo.employee",
  "authentication": {
    "mechanism": "configured-demo-mechanism",
    "proof": "<mechanism-specific-value>"
  }
}
```

```json
{
  "data": {
    "authenticated": true,
    "identity": {
      "identityPrincipalId": "11111111-1111-4111-8111-111111111111",
      "organizationId": "aaaaaaaa-aaaa-4aaa-8aaa-aaaaaaaaaaaa",
      "membershipId": "bbbbbbbb-bbbb-4bbb-8bbb-bbbbbbbbbbbb",
      "displayName": "Demo Employee",
      "roles": ["employee"]
    },
    "capabilities": ["support-requests:create", "support-requests:read:own"]
  },
  "meta": {
    "correlationId": "c0a80123-4567-489a-9abc-123456789abc"
  }
}
```

### POST `/api/v1/auth/logout`

| Contract field | Definition |
|---|---|
| Purpose | End the current authenticated client session where supported by the selected mechanism. |
| Authentication | Required current session/credential. |
| Required permission | Authenticated principal. |
| Path/query | None. |
| Request body | Empty; no JSON body. |
| Success | `204 No Content`. |
| Errors | `401`, `500`, `503`; no protected details. |
| Validation | Only the current caller session is targeted; no principal/session ID may select another principal's session. |
| Traceability | SRS-AUTH-001, SRS-AUTH-004; UC-001. |

### GET `/api/v1/identity/me`

| Contract field | Definition |
|---|---|
| Purpose | Return the authenticated principal's allowed organization-scoped profile and capabilities. |
| Authentication | Required. |
| Required permission | Authenticated principal. |
| Path/query/body | None. |
| Success | `200 OK`; only server-resolved `identityPrincipalId`, `organizationId`, `membershipId`, and non-secret `Role`/capability data. |
| Errors | `401`, `403`, `500`. |
| Validation | Never return authentication proof, signing material, provider/service credentials, or another principal's data. The identity principal may hold memberships in multiple organizations, but this response shows only the server-selected current organization and membership context. Organization selection/switching is an unresolved open contract decision; it is not a client-controlled organization-switch mechanism. |
| Traceability | SRS-AUTH-002, SRS-AUTH-005, SRS-AUTHZ-002; UC-001. |

```json
{
  "data": {
    "identityPrincipalId": "11111111-1111-4111-8111-111111111111",
    "organizationId": "aaaaaaaa-aaaa-4aaa-8aaa-aaaaaaaaaaaa",
    "membershipId": "bbbbbbbb-bbbb-4bbb-8bbb-bbbbbbbbbbbb",
    "displayName": "Demo Employee",
    "roles": ["employee"],
    "capabilities": ["support-requests:create", "support-requests:read:own"]
  },
  "meta": {
    "correlationId": "c0a80123-4567-489a-9abc-123456789abc"
  }
}
```

The example represents one server-resolved current organization and membership context. An `IdentityPrincipal` may have memberships in multiple organizations. Selection or switching of the current organization is not defined by this contract and must not be implemented as a client-controlled organization or membership authority.

## 12. Identity and Organization Membership API Contracts

### GET `/api/v1/organization-memberships`

| Contract field | Definition |
|---|---|
| Purpose | List organization actors visible to an authorized organization-scoped reviewer or analyst. |
| Authentication | Required. |
| Required permission | Explicit organization-scoped membership-directory read; exact role mapping is policy configuration. |
| Path parameters | None. |
| Query parameters | `limit`, `cursor`, optional `status`; see Section 23. |
| Request body | None. |
| Success | `200 OK`, paginated minimal membership profiles. |
| Errors | `400`, `401`, `403`, `429`, `500`. |
| Validation | Organization context is derived from the trusted server context; email and unnecessary principal/external identity fields are omitted. |
| Traceability | SRS-AUTH-005, SRS-AUTHZ-002–003, SRS-TEN-001–002; UC-001, UC-012. |

### GET `/api/v1/organization-memberships/{membershipId}`

| Contract field | Definition |
|---|---|
| Purpose | Read an organization membership within the caller's permitted organization and membership scope. |
| Authentication | Required. |
| Required permission | Own membership or explicit organization-scoped membership-directory read. |
| Path parameters | `membershipId`: organization-scoped membership UUID. |
| Query parameters/body | None. |
| Success | `200 OK`, minimal membership representation. |
| Errors | `400`, `401`, `403` or concealed `404`, `500`. |
| Validation | Validate UUID syntax and tenant scope; use the membership's organization plus membership key for authorization and disclosure. |
| Traceability | SRS-AUTH-002, SRS-AUTHZ-002–003, SRS-TEN-002, SRS-API-002; UC-001. |

## 13. Support Request API Contracts

All support request endpoints operate on the canonical `SupportRequest` entity. No live service-desk API is called.

### POST `/api/v1/support-requests`

| Contract field | Definition |
|---|---|
| Purpose | Create an organization-scoped internal support request. |
| Authentication | Required. |
| Required permission | `support-requests:create` or equivalent employee policy. |
| Path/query | None. |
| Request body | Required `title`, `description`; do not accept authoritative requester membership, organization, status, external request ID, role, or approval state. |
| Success | `201 Created`, `Location` to resource; the `SupportRequest` is persisted and the bounded initial analysis may complete within the answer deadline. Return the accurate SRS workflow outcome and any validated result. |
| Errors | `400`, `401`, `403`, `409` idempotency conflict, `413`, `429`, `500`, `503`. |
| Validation | Nonblank bounded title/description; reject unknown security-sensitive fields; treat text as untrusted. Persist only after validation. |
| Traceability | SRS-FR-001–002, SRS-TKT-001, SRS-TEN-001–002, SRS-API-001–002; UC-002. |

```json
{
  "title": "VPN connection issue",
  "description": "The VPN client disconnects shortly after connecting."
}
```

```json
{
  "data": {
    "requestId": "22222222-2222-4222-8222-222222222222",
    "title": "VPN connection issue",
    "description": "The VPN client disconnects shortly after connecting.",
    "outcomeStatus": "answered",
    "recommendation": {
      "summary": "The request may relate to an unstable network connection.",
      "facts": [],
      "suggestions": [
        {
          "text": "Check whether the connection remains stable on another approved network.",
          "sourceIds": []
        }
      ],
      "uncertainty": "No eligible source confirmed a specific VPN configuration."
    },
    "createdAt": "2026-11-13T07:55:00Z",
    "updatedAt": "2026-11-13T07:55:00Z"
  },
  "meta": {
    "correlationId": "c0a80123-4567-489a-9abc-123456789abc"
  }
}
```

If the initial analysis exceeds its deadline after the support request is persisted, return the actual persisted `failed` or `escalated` state; never claim an answer. `POST /support-requests/{requestId}/analysis` is available for a separately requested/repeated bounded analysis. Any internal initial state must not be presented as a terminal SRS outcome.

### GET `/api/v1/support-requests`

| Contract field | Definition |
|---|---|
| Purpose | List support requests accessible to the caller. |
| Authentication | Required. |
| Required permission | Employee own-request read or explicit analyst organization-scoped request read. |
| Path/query | Query: `limit`, `cursor`, optional `status`, `sort`; no client authority via `organizationId`, `identityPrincipalId`, or `membershipId`. |
| Request body | None. |
| Success | `200 OK`, paginated support request summaries. |
| Errors | `400`, `401`, `403`, `429`, `500`, `503`. |
| Validation | Allowlisted status/sort; all filters combined with server-resolved tenant and record scope. |
| Traceability | SRS-FR-002, SRS-FR-018, SRS-TEN-002, SRS-TKT-002, SRS-API-002; UC-003, UC-011. |

### GET `/api/v1/support-requests/{requestId}`

| Contract field | Definition |
|---|---|
| Purpose | Retrieve an authorized support request and permitted response/source metadata. |
| Authentication | Required. |
| Required permission | Request owner or explicitly authorized analyst/reviewer. |
| Path parameters | `requestId`: tenant-owned support request UUID. |
| Query/body | None. |
| Success | `200 OK`, support request representation. |
| Errors | `400`, `401`, `403` or concealed `404`, `500`, `503`. |
| Validation | Validate UUID; check organization and resource permissions before loading/disclosing details. |
| Traceability | SRS-FR-002–005, SRS-TEN-002, SRS-TKT-002, SRS-API-002; UC-003. |

### PATCH `/api/v1/support-requests/{requestId}`

| Contract field | Definition |
|---|---|
| Purpose | Apply a defined workflow transition; this is the only supported support request update in MVP. |
| Authentication | Required. |
| Required permission | Employee escalation on an accessible owned support request, or analyst transition explicitly allowed by workflow policy. |
| Path parameters | `requestId`: tenant-owned support request UUID. |
| Query | None. |
| Request body | Exactly one `outcomeStatus` transition from the allowlisted workflow; no arbitrary field patching. |
| Success | `200 OK`, persisted current support request status. |
| Errors | `400`, `401`, `403` or concealed `404`, `409` invalid/stale transition, `500`, `503`. |
| Validation | Reject unsupported statuses and transitions; do not accept AI-only state updates. Persist transition and audit reference before reporting success. |
| Traceability | SRS-FR-013, SRS-FR-018, SRS-AUTHZ-008, SRS-TKT-002–004, SRS-TKT-007, SRS-DB-006, SRS-ERR-009; UC-011. |

```json
{
  "outcomeStatus": "escalated",
  "reason": "The suggested steps did not resolve the issue."
}
```

```json
{
  "data": {
    "requestId": "22222222-2222-4222-8222-222222222222",
    "outcomeStatus": "escalated",
    "updatedAt": "2026-11-13T08:03:00Z"
  },
  "meta": {
    "correlationId": "c0a80123-4567-489a-9abc-123456789abc"
  }
}
```

No separate arbitrary status endpoint is defined; the PATCH operation is the constrained status transition contract. Initial/in-progress state names must be fixed as an implementation detail consistent with the SRS outcome set.

## 14. Request Message API Contracts

### GET `/api/v1/support-requests/{requestId}/messages`

| Contract field | Definition |
|---|---|
| Purpose | List permitted `RequestMessage` rows for an accessible support request. |
| Authentication | Required. |
| Required permission | Support request owner or explicitly authorized analyst. |
| Path parameters | `requestId`: tenant-owned support request UUID. |
| Query | `limit`, `cursor`, optional `messageKind` allowlist. |
| Body | None. |
| Success | `200 OK`, paginated request message entries. |
| Errors | `400`, `401`, `403` or concealed `404`, `429`, `500`. |
| Validation | Tenant/resource check before returning body/source references. Author attribution uses the canonical optional `authorMembershipId` for human or analyst authors and omits it for system or AI authors. The canonical `author_kind` vocabulary and public representation remain a contract decision until approved; do not expose an invented author enum. |
| Traceability | SRS-TKT-002–004, SRS-TEN-002, SRS-AUTHZ-003; UC-003, UC-011. |

### POST `/api/v1/support-requests/{requestId}/messages`

| Contract field | Definition |
|---|---|
| Purpose | Add an employee clarification or authorized analyst note to an internal support request. |
| Authentication | Required. |
| Required permission | Request owner for a member clarification; explicit analyst permission for an analyst note. |
| Path parameters | `requestId`: tenant-owned support request UUID. |
| Query | None. |
| Request body | `body`, `messageKind`; author membership and organization are server-derived. The canonical optional `authorMembershipId` is never accepted from the request body. |
| Success | `201 Created`, persisted request message representation. |
| Errors | `400`, `401`, `403` or concealed `404`, `409`, `413`, `429`, `500`, `503`. |
| Validation | Nonblank bounded body; allowed message kinds only; reject author, organization, status, and external identity fields; content is untrusted. |
| Traceability | SRS-AI-007, SRS-TKT-001, SRS-TKT-003–004, SRS-TEN-001–002, SRS-API-002; UC-002, UC-011. |

```json
{
  "messageKind": "clarification",
  "body": "The issue occurs on the office Wi-Fi network."
}
```

```json
{
  "data": {
    "messageId": "33333333-3333-4333-8333-333333333333",
    "requestId": "22222222-2222-4222-8222-222222222222",
    "authorMembershipId": "bbbbbbbb-bbbb-4bbb-8bbb-bbbbbbbbbbbb",
    "messageKind": "clarification",
    "body": "The issue occurs on the office Wi-Fi network.",
    "createdAt": "2026-11-13T08:04:00Z"
  },
  "meta": {
    "correlationId": "c0a80123-4567-489a-9abc-123456789abc"
  }
}
```

## 15. AI Support Request Analysis API Contracts

### POST `/api/v1/support-requests/{requestId}/analysis`

| Contract field | Definition |
|---|---|
| Purpose | Request configured AI analysis for an accessible support request; returns a run reference and does not expose FastAPI. |
| Authentication | Required. |
| Required permission | Support request owner or explicitly authorized analyst; backend authorizes analysis. |
| Path parameters | `requestId`: tenant-owned support request UUID. |
| Query | None. |
| Request body | Optional bounded `analysisMode` from configured allowlist and optional `includeTroubleshooting` boolean. No prompt, provider, organization, tool, or authorization override. |
| Success | `202 Accepted` with persisted `agentRunId`, current run status, and poll URL. |
| Errors | `400`, `401`, `403` or concealed `404`, `409` duplicate/conflicting request, `429`, `500`, `503`, `504`. |
| Validation | Request-scoped deadline; actor/tenant access check before FastAPI call; input and retrieval context minimized and permission-filtered. Idempotency key recommended/required by deployment contract to suppress duplicate runs. |
| Traceability | SRS-FR-006, SRS-FR-019–020, SRS-AI-001–009, SRS-API-004–005, SRS-API-009, SRS-ERR-004–006; UC-004–007. |

```json
{
  "analysisMode": "support_answer",
  "includeTroubleshooting": true
}
```

```json
{
  "data": {
    "agentRunId": "44444444-4444-4444-8444-444444444444",
    "requestId": "22222222-2222-4222-8222-222222222222",
    "status": "queued",
    "statusUrl": "/api/v1/agent-runs/44444444-4444-4444-8444-444444444444"
  },
  "meta": {
    "correlationId": "c0a80123-4567-489a-9abc-123456789abc"
  }
}
```

The exact asynchronous execution mechanism is unresolved; `202` specifies the public accepted-work contract, not a queue/broker or separate service.

## 16. Agent Run API Contracts

### GET `/api/v1/agent-runs/{agentRunId}`

| Contract field | Definition |
|---|---|
| Purpose | Read current persisted status and validated result of a correlated workflow. |
| Authentication | Required. |
| Required permission | Access to the parent support request and organization; analyst scope may apply. |
| Path parameters | `agentRunId`: UUID. |
| Query/body | None. |
| Success | `200 OK`, run status, timestamps, response semantics, verified eligible source references, optional proposal reference. |
| Errors | `400`, `401`, `403` or concealed `404`, `500`, `503`. |
| Validation | Reauthorize parent resource; never return raw prompt, internal credentials, or unvalidated model citations. Status comes from backend state. |
| Traceability | SRS-FR-004–006, SRS-AI-003–008, SRS-AUD-006, SRS-API-002; UC-004–007. |

### GET `/api/v1/agent-runs/{agentRunId}/events`

| Contract field | Definition |
|---|---|
| Purpose | Retrieve permitted, minimized events for a workflow run. |
| Authentication | Required. |
| Required permission | Parent support request access; more detailed agent-event visibility may be restricted to analysts. |
| Path parameters | `agentRunId`: UUID. |
| Query | `limit`, `cursor`, optional allowlisted `eventType`. |
| Body | None. |
| Success | `200 OK`, ordered event metadata with timestamps/outcomes. |
| Errors | `400`, `401`, `403` or concealed `404`, `429`, `500`. |
| Validation | Tenant scope and parent access; omit full prompts, sensitive chunks, credentials, and untrusted narrative as proof. |
| Traceability | SRS-AI-003, SRS-AI-008, SRS-AUD-001–006, SRS-TEN-002; UC-007, UC-013. |

Example result shape:

```json
{
  "data": {
    "agentRunId": "44444444-4444-4444-8444-444444444444",
    "status": "completed",
    "recommendation": {
      "summary": "The request may relate to an unstable network connection.",
      "facts": [],
      "suggestions": [
        {
          "text": "Check whether the connection remains stable on another approved network.",
          "sourceIds": []
        }
      ],
      "uncertainty": "No eligible source confirmed a specific VPN configuration."
    },
    "policyDecision": null,
    "humanDecision": null,
    "toolResult": null
  },
  "meta": {
    "correlationId": "c0a80123-4567-489a-9abc-123456789abc"
  }
}
```

`recommendation` is AI-generated and non-authoritative. `policyDecision`, `humanDecision`, and `toolResult` are separate backend/human/simulation records and must not be inferred from recommendation text.

## 17. Knowledge Management API Contracts

Knowledge retrieval is permission-aware and server-scoped. Knowledge document listing does not expose unrestricted chunk contents.

### GET `/api/v1/knowledge/documents`

| Contract field | Definition |
|---|---|
| Purpose | List approved/active curated documents the caller may discover. |
| Authentication | Required. |
| Required permission | Authenticated organization membership with permission to view document metadata; platform/organization administration is separate. |
| Path/query | Query: `limit`, `cursor`, optional `status` restricted to allowed values. |
| Body | None. |
| Success | `200 OK`, organization- and permission-filtered document metadata. |
| Errors | `400`, `401`, `403`, `429`, `500`, `503`. |
| Validation | Ignore/reject client organization filters; do not expose inaccessible titles, snippets, or citations. |
| Traceability | SRS-FR-003, SRS-FR-015, SRS-RAG-001–004, SRS-RAG-009, SRS-TEN-002–003; UC-005, UC-014. |

### GET `/api/v1/knowledge/documents/{documentId}`

| Contract field | Definition |
|---|---|
| Purpose | Retrieve authorized document metadata, not arbitrary chunk content. |
| Authentication | Required. |
| Required permission | Document visibility under the current organization membership, role, and grant attributes. |
| Path parameters | `documentId`: UUID. |
| Query/body | None. |
| Success | `200 OK`, permitted metadata and status. |
| Errors | `400`, `401`, `403` or concealed `404`, `500`. |
| Validation | Check document status, tenant, and permission before response; never let title/source ID imply access. |
| Traceability | SRS-FR-003, SRS-RAG-002–004, SRS-RAG-009, SRS-TEN-002–004; UC-005, UC-014. |

### POST `/api/v1/knowledge/documents` — Optional MVP-Should

| Contract field | Definition |
|---|---|
| Purpose | If optional admin configuration is enabled, register curated demo text and permission metadata for controlled processing. This is not a general-purpose binary upload/bulk ingestion API. |
| Authentication | Required. |
| Required permission | Explicit organization-scoped knowledge administrator and enabled optional capability. |
| Path/query | None. |
| Request body | `title`, curated `content`, validated `permissionAttributes`, optional provenance reference. |
| Success | `201 Created` only after content/chunks are safely stored and available according to processing status; otherwise return truthful pending/failed state. |
| Errors | `400`, `401`, `403`, `409`, `413`, `415`, `500`, `503`. |
| Validation | Synthetic/approved demo content only; enforce size/format limits, organization and permissions; treat text as untrusted; do not claim indexing success before persistence. Broad ingestion and uploaded executable/binary formats are out of scope. |
| Traceability | SRS-FR-017 (MVP-Should), SRS-AUTHZ-005, SRS-RAG-008, SRS-DB-001–004, SRS-AUD-001–002; UC-014. |

```json
{
  "title": "VPN troubleshooting guide",
  "content": "Curated demo guidance for checking a supported VPN connection.",
  "sourceReference": "demo://knowledge/vpn-guide",
  "permissionAttributes": {
    "visibility": "organization",
    "allowedRoleKeys": ["employee", "it_analyst"]
  }
}
```

This contract is conditional and does not require the capability to be enabled in the MVP. Any separate document processing/embedding operation is not defined until the optional ingestion lifecycle and embedding provider are decided.

## 18. Approval API Contracts

### GET `/api/v1/approvals`

| Contract field | Definition |
|---|---|
| Purpose | List pending approval requests visible to an authorized reviewer. |
| Authentication | Required. |
| Required permission | Explicit approver permission for organization/action. |
| Path/query | Query: `limit`, `cursor`, optional `status` restricted to reviewer-visible states. |
| Body | None. |
| Success | `200 OK`, minimized proposal summaries scoped by server-resolved organization and reviewer scope. |
| Errors | `400`, `401`, `403`, `429`, `500`. |
| Validation | Do not accept organization scope from client; do not reveal unauthorized proposal existence. |
| Traceability | SRS-FR-014, SRS-HITL-001–003, SRS-AUTHZ-004, SRS-TEN-001–002; UC-009. |

### GET `/api/v1/approvals/{approvalId}`

| Contract field | Definition |
|---|---|
| Purpose | Retrieve exact request context, proposed action/parameters, expected simulated effect, and state for human review. |
| Authentication | Required. |
| Required permission | Explicit organization/action-scoped approver permission. |
| Path parameters | `approvalId`: UUID. |
| Query/body | None. |
| Success | `200 OK`, exact stored proposal and simulation label. |
| Errors | `400`, `401`, `403` or concealed `404`, `500`. |
| Validation | Verify current proposal and reviewer scope; reading is not approval; omit unrelated protected context. |
| Traceability | SRS-FR-014, SRS-HITL-002–005, SRS-AUTHZ-004, SRS-AUD-004; UC-009. |

### POST `/api/v1/approvals/{approvalId}/approve`

| Contract field | Definition |
|---|---|
| Purpose | Record an explicit human approval for the exact unchanged simulated proposal. Approval does not execute it. |
| Authentication | Required. |
| Required permission | Authorized human approver for organization and action. |
| Path parameters | `approvalId`: UUID. |
| Query | None. |
| Request body | Explicit `decision: "approve"` and optional bounded comment. Reviewer identity is server-derived. |
| Success | `200 OK` with recorded decision, expiry, and `executionStatus: "not_executed"`. |
| Errors | `400`, `401`, `403` or concealed `404`, `409` already decided/expired/changed, `500`, `503`. |
| Validation | Require explicit request and proposal binding; recheck current policy; approval persistence and required audit must succeed before success response. |
| Traceability | SRS-FR-009–011, SRS-HITL-001–009, SRS-AUTHZ-004, SRS-AUTHZ-008–009, SRS-AUD-009; UC-009. |

```json
{
  "decision": "approve",
  "comment": "Reviewed as a simulation."
}
```

```json
{
  "data": {
    "approvalId": "55555555-5555-4555-8555-555555555555",
    "decision": "approved",
    "decidedAt": "2026-11-13T08:10:00Z",
    "expiresAt": "2026-11-13T08:20:00Z",
    "executionStatus": "not_executed"
  },
  "meta": {
    "correlationId": "c0a80123-4567-489a-9abc-123456789abc"
  }
}
```

The timestamp values illustrate fields only; they do not prescribe an approval validity period.

### POST `/api/v1/approvals/{approvalId}/reject`

| Contract field | Definition |
|---|---|
| Purpose | Record an explicit human rejection; no simulated tool may execute. |
| Authentication | Required. |
| Required permission | Authorized human approver for organization and action. |
| Path parameters | `approvalId`: UUID. |
| Query | None. |
| Request body | Explicit `decision: "reject"` and optional bounded reason. |
| Success | `200 OK`, persisted rejected state and execution prohibited. |
| Errors | `400`, `401`, `403` or concealed `404`, `409` already decided/stale, `500`, `503`. |
| Validation | Server derives reviewer; persist decision and audit before success; repeated conflicting decision is `409`. |
| Traceability | SRS-FR-009–011, SRS-HITL-001–009, SRS-AUTHZ-004, SRS-AUD-001–002; UC-009. |

```json
{
  "decision": "reject",
  "reason": "The proposed simulated action is not needed."
}
```

## 19. Simulated Tool Execution API Contracts

### GET `/api/v1/tools`

| Contract field | Definition |
|---|---|
| Purpose | List enabled allow-listed simulated tool summaries visible to the caller. |
| Authentication | Required. |
| Required permission | Authenticated active membership; only tools eligible for the current organization/action context are shown. |
| Path/query | Optional `requestId` UUID to evaluate display context; server still authorizes. |
| Body | None. |
| Success | `200 OK`, tool identifiers, descriptions, required approval indicator, simulation label; no executable code/secrets. |
| Errors | `400`, `401`, `403`, `500`. |
| Validation | Never expose disabled tools or live integrations; listing does not authorize or execute. |
| Traceability | SRS-FR-007–009, SRS-TOOL-001, SRS-TOOL-005, SRS-TOOL-010; UC-008, UC-009. |

### POST `/api/v1/tool-executions`

| Contract field | Definition |
|---|---|
| Purpose | Request execution of an already approved, unchanged, allow-listed simulated action. |
| Authentication | Required. |
| Required permission | Actor authorized for the approved action; execution policy revalidated. |
| Path/query | None. |
| Request body | `approvalId`; optional idempotency header. Never accept arbitrary tool name/parameters to bypass proposal review. |
| Success | `202 Accepted` if bounded execution is accepted asynchronously, or `200 OK` only for confirmed completed simulation. Implementation mode is open; both must return truthful status. |
| Errors | `400`, `401`, `403`, `404`, `409` missing/rejected/expired/changed approval, `429`, `500`, `503`, `504`. |
| Validation | Revalidate user/org/role, approval expiry and fingerprint, tool/schema, simulation-only status, current policy; persist duplicate guard and audit. No automatic retry after ambiguous timeout. |
| Traceability | SRS-FR-007–012, SRS-HITL-007–010, SRS-TOOL-002–009, SRS-AUTHZ-008, SRS-ERR-007–009; UC-010. |

```json
{
  "approvalId": "55555555-5555-4555-8555-555555555555"
}
```

```json
{
  "data": {
    "executionId": "66666666-6666-4666-8666-666666666666",
    "approvalId": "55555555-5555-4555-8555-555555555555",
    "status": "succeeded",
    "simulated": true,
    "result": {
      "summary": "Simulation completed; no external system was changed."
    },
    "completedAt": "2026-11-13T08:12:00Z"
  },
  "meta": {
    "correlationId": "c0a80123-4567-489a-9abc-123456789abc"
  }
}
```

This success example is valid only after confirmed simulated result, persisted outcome, and required audit. AI recommendation, backend policy permission, and human approval are different fields and never imply tool success.

### GET `/api/v1/tool-executions/{executionId}`

| Contract field | Definition |
|---|---|
| Purpose | Retrieve the authoritative stored status/result for a permitted simulated execution. |
| Authentication | Required. |
| Required permission | Same organization and permitted support-request/action scope; reviewer/actor policy applies. |
| Path parameters | `executionId`: UUID. |
| Query/body | None. |
| Success | `200 OK`, status `pending`, `started`, `succeeded`, `failed`, or `unknown` as applicable and simulation label. |
| Errors | `400`, `401`, `403` or concealed `404`, `500`, `503`. |
| Validation | Return persisted backend result only; never infer success from AI narrative. |
| Traceability | SRS-FR-011–012, SRS-TOOL-007–009, SRS-AUD-006, SRS-API-001; UC-010. |

## 20. Audit API Contracts

### GET `/api/v1/audit-events`

| Contract field | Definition |
|---|---|
| Purpose | List minimized audit metadata within an authorized review scope. |
| Authentication | Required. |
| Required permission | Explicit audit-review permission; organization administrator alone is insufficient. |
| Query parameters | `limit`, `cursor`, optional `requestId`, `correlationId`, `eventType`, time bounds. All are scoped to the authorized organization. |
| Body | None. |
| Success | `200 OK`, paginated event summaries. |
| Errors | `400`, `401`, `403`, concealed `404`, `429`, `500`, `503`. |
| Validation | Validate UUID/time formats and allowlisted event types; do not leak cross-tenant existence or raw sensitive content. |
| Traceability | SRS-FR-012, SRS-AUTHZ-007, SRS-TEN-001–002, SRS-AUD-001–007; UC-013. |

### GET `/api/v1/audit-events/{eventId}`

| Contract field | Definition |
|---|---|
| Purpose | Retrieve one authorized audit event's minimized metadata. |
| Authentication | Required. |
| Required permission | Explicit audit reviewer with event organization scope. |
| Path parameters | `eventId`: UUID. |
| Query/body | None. |
| Success | `200 OK`, event type, actor/service, time, organization-safe correlation, outcome, and resource references. |
| Errors | `400`, `401`, `403` or concealed `404`, `500`, `503`. |
| Validation | Exclude credentials, full prompts, full documents, and unnecessary personal data. |
| Traceability | SRS-AUD-002–007, SRS-ERR-010; UC-013. |

Example event:

```json
{
  "data": {
    "eventId": "77777777-7777-4777-8777-777777777777",
    "eventType": "approval.decision",
    "actorType": "user",
    "occurredAt": "2026-11-13T08:10:00Z",
    "outcome": "approved",
    "requestId": "22222222-2222-4222-8222-222222222222",
    "approvalId": "55555555-5555-4555-8555-555555555555",
    "correlationId": "c0a80123-4567-489a-9abc-123456789abc",
    "metadata": {
      "proposalFingerprint": "base64url-digest-reference"
    }
  },
  "meta": {
    "correlationId": "c0a80123-4567-489a-9abc-123456789abc"
  }
}
```

## 21. Reporting API Contracts

Reporting remains bounded to support request summaries and operational review; no standalone analytics product is introduced.

### POST `/api/v1/support-requests/{requestId}/summary`

| Contract field | Definition |
|---|---|
| Purpose | Ask the bounded Reporting Agent to draft a summary of a support request the analyst may access. |
| Authentication | Required. |
| Required permission | Analyst support-request read and summary permission. |
| Path parameters | `requestId`: tenant-owned support request UUID. |
| Query | None. |
| Request body | Optional bounded output preference; no external audience or provider override. |
| Success | `200 OK` with clearly labeled AI-generated summary, or `202 Accepted` with run reference if asynchronous; exact execution mode is open. |
| Errors | `400`, `401`, `403` or concealed `404`, `429`, `500`, `503`, `504`. |
| Validation | Authorize before sending minimized context to FastAPI; summary is not support request state, audit evidence, approval, or verified fact. |
| Traceability | SRS-FR-019, SRS-AUTHZ-003, SRS-TKT-004, SRS-AI-001–008, SRS-NFR-003, SRS-REP-005; UC-012. |

```json
{}
```

Example completed result:

```json
{
  "data": {
    "requestId": "22222222-2222-4222-8222-222222222222",
    "generatedBy": "ai",
    "summary": "The employee reports repeated VPN disconnections. No eligible source confirmed a specific root cause.",
    "uncertainty": "Root cause not verified.",
    "ticketOutcomeStatus": "escalated"
  },
  "meta": {
    "correlationId": "c0a80123-4567-489a-9abc-123456789abc"
  }
}
```

### GET `/api/v1/reports/operations`

| Contract field | Definition |
|---|---|
| Purpose | Return basic, organization-scoped operational outcomes needed for MVP evaluation. |
| Authentication | Required. |
| Required permission | Explicit analyst/evaluation permission for the organization. |
| Query parameters | Optional bounded `from`, `to`, `outcomeStatus`; aggregation and window limits must be configured. |
| Body | None. |
| Success | `200 OK`, minimized counts/latency/evaluation signals with period and environment metadata where available. |
| Errors | `400`, `401`, `403`, `429`, `500`, `503`. |
| Validation | Enforce role/tenant scope and bounded window; suppress small cohorts if needed by privacy policy (threshold unresolved); do not claim guaranteed business outcomes or SLA. |
| Traceability | SRS-FR-018, SRS-REP-001–005, SRS-AUD-001, SRS-AUTHZ-003; UC-003, UC-012, UC-013. |

## 22. Internal Spring Boot to FastAPI Contracts

### POST `/internal/v1/workflows:run`

This is a private logical contract, not a public endpoint. The exact private hostname, service identity, authentication, and transport protection remain deployment decisions. No browser, employee, or external caller may invoke it.

| Contract field | Definition |
|---|---|
| Caller | Spring Boot API only. |
| Authentication | Private service authentication required by deployment; exact mechanism unresolved. |
| Required permission | Authenticated Spring Boot service identity under the internal service policy; no browser principal or agent identity can invoke this route directly. |
| Purpose | Execute one configured, bounded LangGraph workflow for analysis, answer, troubleshooting, or summary. |
| Path parameters | None. |
| Query parameters | None. |
| Request body | `requestId`, `correlationId`, trusted `scope` from backend, task, bounded content, permission-filtered evidence/source refs, deadline, and response schema version. |
| Success | Synchronous `200 OK` bounded AI result, or documented error. The public API may expose accepted work asynchronously, but no callback is defined. |
| Errors | `400` invalid envelope, `401/403` internal caller failure, `422` unsupported workflow/output request, `429` internal capacity, `500` processing failure, `503` provider unavailable, `504` deadline exceeded. |
| Validation | Reject unsupported task/schema, missing correlation/deadline, invalid envelope, or ineligible evidence references. |
| Security | Spring Boot authorizes operation and resolves identity/tenant; FastAPI validates the caller envelope but does not re-authorize business actions. No tool invocation endpoint; no model-supplied identity/organization/policy; no secrets; content is untrusted. |
| Traceability | SRS-FR-006, SRS-FR-019, SRS-AI-001–009, SRS-API-004–006, SRS-API-009, SRS-ERR-004–006; UC-004–007, UC-012. |

Request example:

```json
{
  "schemaVersion": "1",
  "requestId": "22222222-2222-4222-8222-222222222222",
  "agentRunId": "44444444-4444-4444-8444-444444444444",
  "correlationId": "c0a80123-4567-489a-9abc-123456789abc",
  "task": "support_answer",
  "scope": {
    "organizationId": "aaaaaaaa-aaaa-4aaa-8aaa-aaaaaaaaaaaa",
    "requestingUserId": "11111111-1111-4111-8111-111111111111"
  },
  "input": {
    "ticketDescription": "The VPN client disconnects shortly after connecting."
  },
  "eligibleKnowledge": [
    {
      "chunkId": "88888888-8888-4888-8888-888888888888",
      "documentId": "99999999-9999-4999-8999-999999999999",
      "sourceTitle": "VPN troubleshooting guide",
      "content": "Curated demo guidance for checking a supported VPN connection."
    }
  ],
  "deadlineAt": "2026-11-13T08:00:10Z",
  "outputSchemaVersion": "1"
}
```

The trusted `scope` is supplied by Spring Boot over its internal contract; it must never be copied from untrusted client or model claims. `eligibleKnowledge` must already be filtered before this call.

### FastAPI workflow result / response

There is no AI result callback endpoint in the approved architecture. The initial contract is the response to the internal Spring Boot request. If asynchronous callback delivery is later needed, it requires a new decision and authenticated callback design.

```json
{
  "schemaVersion": "1",
  "requestId": "22222222-2222-4222-8222-222222222222",
  "agentRunId": "44444444-4444-4444-8444-444444444444",
  "correlationId": "c0a80123-4567-489a-9abc-123456789abc",
  "workflowStatus": "completed",
  "recommendation": {
    "summary": "The request may relate to an unstable network connection.",
    "facts": [],
    "suggestions": [
      {
        "text": "Check whether the connection remains stable on another approved network.",
        "sourceIds": []
      }
    ],
    "uncertainty": "No eligible source confirmed a specific VPN configuration."
  },
  "actionProposal": null,
  "providerMetadata": {
    "provider": "not-exposed",
    "model": "not-exposed"
  }
}
```

`recommendation` and `actionProposal` are untrusted AI outputs. Spring Boot validates schema, supported workflow, source IDs, tool allow-list, actor/organization, and action policy before display or state change. Provider metadata must not expose secrets or require choosing a provider.

### Internal error and timeout response

```json
{
  "timestamp": "2026-11-13T08:05:00Z",
  "status": 504,
  "errorCode": "AI_DEADLINE_EXCEEDED",
  "message": "The bounded AI workflow did not complete before its deadline.",
  "path": "/api/v1/support-requests/22222222-2222-4222-8222-222222222222/analysis",
  "correlationId": "c0a80123-4567-489a-9abc-123456789abc",
  "fieldErrors": []
}
```

FastAPI must use finite downstream deadlines. A transient, idempotent read may be retried at most once only if remaining end-to-end time permits. Do not retry policy denials, invalid input, or generation to manufacture success. AI failure cannot authorize or execute a tool.

## 23. Pagination, Filtering and Sorting

- List endpoints use cursor pagination with `limit` bounded to a documented maximum; proposed default is 20 and maximum is 100, subject to implementation agreement.
- Return `meta.page.nextCursor` as an opaque cursor; it is not a user/tenant authority token and is bound server-side to the caller scope and filters.
- Use deterministic server-side ordering, typically newest first (`createdAt`/`occurredAt` descending) with a stable unique-ID tie-breaker.
- Accept only allowlisted filters and sort fields; reject unknown fields/directions with `400`.
- Never let `organizationId`, `membershipId`, `identityPrincipalId`, or role query parameters broaden caller scope. Such filters may narrow an already authorized scope only if the operation's policy permits it.
- Cursor format, retention, and pagination consistency under concurrent updates are implementation decisions.

## 24. Validation Rules

1. Validate JSON shape, content type, field lengths, UUIDs, timestamp formats, enumerations, and allowed fields at the API boundary.
2. Derive actor, organization, role, approval status, request status, and execution authority from trusted server state, never request JSON.
3. Treat support request/message text, knowledge text, model output, candidate citations, action proposals, and tool parameters as untrusted.
4. Enforce strict tool allow-list and exact schema; reject unknown tool names, extra parameters, malformed values, and any live action.
5. Apply organization and document permission predicates before returning/forwarding any knowledge content.
6. Bind approvals to unchanged proposal content and policy context; expiry and current authorization are checked again at execution.
7. Validate source/citation IDs against retrieved, actually eligible sources before presentation.
8. Reject sensitive unknown request fields such as `organizationId`, `identityPrincipalId`, `membershipId`, `approved`, `policyDecision`, `toolResult`, `providerApiKey`, or `systemPrompt`.
9. Keep validation errors safe and field-specific without echoing sensitive values.
10. Request body maximums, per-field lengths, knowledge content limits, and rating scale require documented implementation limits; no unapproved bulk-upload limit is implied.

## 25. Idempotency and Retry Strategy

- Require `Idempotency-Key` for support request creation, analysis initiation, message creation, and tool-execution requests where a client/network retry could duplicate state. Approval decisions should be state-idempotent for the same decision but conflicting repeats return `409`.
- Store/reconcile idempotency by authenticated actor, organization, operation, key, and canonical request fingerprint. A key replay with a different body returns `409`.
- Key retention duration and storage are open implementation decisions; idempotency keys are not authorization tokens.
- Retry transient idempotent reads at most once and only within the remaining end-to-end deadline. Never retry validation failures, authentication failures, authorization/policy denials, or malformed model output.
- Do not automatically retry non-idempotent simulated execution after an ambiguous timeout unless non-execution is established and duplicate safety is proven. Return `unknown`/failed state and require review as applicable.
- Idempotency does not replace proposal integrity, approval binding, backend policy, database uniqueness, or audit.

## 26. Rate Limiting and Timeout Considerations

- Every public and internal request has a finite timeout. The answer-only MVP end-to-end deadline is no greater than 10 seconds; approval waiting is outside that deadline.
- Downstream AI/provider timeouts must fit within remaining request budget. Tool timeout is configured but its numeric value remains unresolved.
- Return accurate `429`, `503`, or `504` errors; include `Retry-After` only when the server can provide a meaningful value.
- Apply rate limits at the authenticated actor and organization boundary, especially for login, analysis, request messages, knowledge management, reporting, and execution. Numeric quotas and implementation point are unresolved.
- A timeout never implies an action succeeded. Tool execution status is `unknown` when the result cannot be established; do not blindly repeat.
- No production throughput, availability SLA, or provider failover is implied.

## 27. API Security Requirements

- Public business operations terminate at Spring Boot. The browser never receives internal service credentials or calls FastAPI directly.
- Authenticate every protected call and enforce role, organization, resource, and action permissions server-side.
- Fail closed when identity, tenant scope, policy, approval, or required persistence is unavailable.
- Filter knowledge permissions before AI/model input; source references returned to organization actors are verified against actual eligible rows.
- LLM output is untrusted. It may recommend or propose; it cannot set support request status, approve itself, authorize retrieval, or execute a tool.
- Require explicit authorized human approval for every enabled MVP simulated action and revalidate deterministic policy immediately before execution.
- Permit only allow-listed, schema-validated simulated tool actions; no live integration, production credential, arbitrary shell, network, or database tool exists in MVP.
- Audit material access/policy, workflow, retrieval references, approvals, decisions, execution attempts/results, and optional configuration changes. Minimize sensitive event content.
- Do not expose secrets, stack traces, full prompts, unauthorized resource existence, provider credentials, or internal service authentication details.
- Store secrets outside source control, prompts, and routine logs; do not include provider secrets in database-backed API configuration.
- Use synthetic/demo content by default until provider data terms and organizational review are resolved.

## 28. API Documentation Strategy

- Publish the public Spring Boot contract as OpenAPI/Swagger, including authentication decision caveats, request/response schemas, enumerations, error envelopes, status codes, security requirements, and examples.
- Document internal FastAPI contract separately from the public OpenAPI surface; do not advertise it as browser-accessible.
- Keep examples synthetic and free of credentials, real employee details, or actual provider identifiers.
- Derive contract tests from the reviewed OpenAPI and internal schemas; validate success, validation, tenant denial, policy denial, approval integrity, retries, timeout, and safe failure.
- Version review and approvals should trace to SRS and Use Case IDs in this document and CI validation; no tooling/package additions are specified here.
- OpenAPI security scheme must remain abstract until authentication selection. Do not publish a fake bearer/JWT contract as finalized.

## 29. API-to-Database Mapping

| API resource/operation | Logical database entities | Important rule |
|---|---|---|
| Authentication / profile / memberships | `IdentityPrincipal`, `ExternalIdentity`, `Organization`, `OrganizationMembership`, `Role`, `MembershipRole` | Verified issuer/subject resolution; no password or secret storage; organization/actor context derived server-side. |
| Support request create/list/detail/update | `SupportRequest`, `OrganizationMembership`, `Organization`, `AuditEvent` | Tenant-owned request; requester is a same-organization membership; only defined transitions may update status. |
| Request messages | `RequestMessage`, `SupportRequest`, `OrganizationMembership`, `AuditEvent` | Parent and child organization IDs match; author is a membership or null for system/AI; content is untrusted. |
| AI analysis / run status / events | `AgentDefinition`, `AgentRun`, `AgentEvent`, `SupportRequest`, `AuditEvent` | Correlated and organization-scoped; only bounded metadata persisted. |
| Knowledge reads / optional curation | `KnowledgeDocument`, `KnowledgeChunk`, `OrganizationMembership`, `AuditEvent` | Filter by organization and permission before candidate content enters AI context. |
| Approval operations | `ApprovalRequest`, `OrganizationMembership`, `ToolDefinition`, `SupportRequest`, `AuditEvent` | Exact proposal, requester/reviewer membership, expiry, fingerprint, and policy context; decision is explicit. |
| Simulated tools | `ToolDefinition`, `ApprovalRequest`, `ToolExecution`, `AuditEvent` | Approved, revalidated, allow-listed simulation only; uniqueness/transactions prevent duplicate effects. |
| Audit reads | `AuditEvent` and typed related entities | Explicit reviewer permission; organization filter; minimized metadata. |
| Support request summary and operations reporting | `SupportRequest`, `AgentRun`, `AgentEvent`, `AuditEvent`, optional `ResponseFeedback` | Summary is AI-generated; analytics are scoped/minimized; no standalone analytics product. |
| Optional feedback | `ResponseFeedback`, `RequestMessage`, `SupportRequest`, `AgentRun`, `OrganizationMembership` | MVP-Should; no authorization effect. |

MariaDB 10.11.16 is the relational source of truth. Knowledge Agent retrieval is not selected; any search service is subordinate to backend authorization and must return only currently eligible candidate content. MariaDB JSON fields follow the Database ER document's bounded schemas and secret-exclusion rules.

## 30. API Traceability Matrix

| API area | Related SRS requirement families | Related Use Cases |
|---|---|---|
| Authentication/profile | SRS-AUTH-001–SRS-AUTH-006; SRS-AUTHZ-001–SRS-AUTHZ-002; SRS-API-002; SRS-ERR-002 | UC-001 |
| Identity principals/memberships | SRS-AUTH-002, SRS-AUTH-005; SRS-AUTHZ-002–SRS-AUTHZ-003; SRS-TEN-001–SRS-TEN-002; SRS-API-002 | UC-001, UC-012 |
| Support requests/messages | SRS-FR-001–SRS-FR-002, SRS-FR-018; SRS-TKT-001–SRS-TKT-007; SRS-TEN-001–SRS-TEN-002; SRS-AI-007; SRS-DB-004, SRS-DB-006; SRS-ERR-001, SRS-ERR-009 | UC-002, UC-003, UC-011 |
| Analysis and agent runs | SRS-FR-006, SRS-FR-019–SRS-FR-020; SRS-AI-001–SRS-AI-009; SRS-API-004–SRS-API-005, SRS-API-009; SRS-NFR-005, SRS-NFR-015–SRS-NFR-017; SRS-ERR-004–SRS-ERR-006 | UC-004, UC-006, UC-007 |
| Knowledge | SRS-FR-003, SRS-FR-015, SRS-FR-017; SRS-RAG-001–SRS-RAG-009; SRS-TEN-002–SRS-TEN-004; SRS-DB-001–SRS-DB-004; SRS-SEC-004 | UC-005, UC-014 |
| Approval | SRS-FR-009–SRS-FR-011, SRS-FR-014; SRS-HITL-001–SRS-HITL-009; SRS-AUTHZ-004, SRS-AUTHZ-008–SRS-AUTHZ-009; SRS-AUD-001–SRS-AUD-002, SRS-AUD-009; SRS-ERR-008–SRS-ERR-009 | UC-008, UC-009 |
| Simulated execution | SRS-FR-007–SRS-FR-012; SRS-TOOL-001–SRS-TOOL-009; SRS-HITL-007–SRS-HITL-010; SRS-DB-005–SRS-DB-006; SRS-AUD-009; SRS-ERR-007–SRS-ERR-009 | UC-010 |
| Audit | SRS-AUD-001–SRS-AUD-009; SRS-TEN-001–SRS-TEN-002; SRS-AUTHZ-007; SRS-ERR-010 | UC-013 |
| Reporting/summary | SRS-REP-001–SRS-REP-005; SRS-AI-001–SRS-AI-008; SRS-TKT-002–SRS-TKT-004; SRS-AUTHZ-003 | UC-003, UC-012, UC-013 |
| Optional feedback | SRS-FR-016, SRS-REP-004, SRS-DB-004 | UC-015 |
| Internal Spring Boot/FastAPI | SRS-AI-001–SRS-AI-009; SRS-API-004–SRS-API-006, SRS-API-009; SRS-FR-006, SRS-FR-019; SRS-NFR-003, SRS-NFR-015–SRS-NFR-017; SRS-ERR-004–SRS-ERR-006 | UC-004–UC-007, UC-012 |

Endpoint-level requirement and use-case references appear on each contract. Traceability ranges denote inclusive IDs from the existing SRS; no new requirement IDs are introduced.

### Successful response examples for endpoints without inline examples

These are illustrative JSON representations for contracts whose request/response behavior is described in the endpoint tables. Every example uses the common envelope in Section 9. A `204` response has no JSON body.

**GET `/api/v1/organization-memberships`**

```json
{"data":[{"membershipId":"bbbbbbbb-bbbb-4bbb-8bbb-bbbbbbbbbbbb","identityPrincipalId":"11111111-1111-4111-8111-111111111111","displayName":"Demo Employee","roles":["employee"]}],"meta":{"correlationId":"c0a80123-4567-489a-9abc-123456789abc","page":{"limit":20,"nextCursor":null}}}
```

**GET `/api/v1/organization-memberships/{membershipId}`**

```json
{"data":{"membershipId":"bbbbbbbb-bbbb-4bbb-8bbb-bbbbbbbbbbbb","identityPrincipalId":"11111111-1111-4111-8111-111111111111","displayName":"Demo Employee","roles":["employee"]},"meta":{"correlationId":"c0a80123-4567-489a-9abc-123456789abc"}}
```

**GET `/api/v1/support-requests`**

```json
{"data":[{"requestId":"22222222-2222-4222-8222-222222222222","title":"VPN connection issue","outcomeStatus":"answered","createdAt":"2026-11-13T07:55:00Z"}],"meta":{"correlationId":"c0a80123-4567-489a-9abc-123456789abc","page":{"limit":20,"nextCursor":null}}}
```

**GET `/api/v1/support-requests/{requestId}`**

```json
{"data":{"requestId":"22222222-2222-4222-8222-222222222222","title":"VPN connection issue","description":"The VPN client disconnects shortly after connecting.","outcomeStatus":"answered","createdAt":"2026-11-13T07:55:00Z","updatedAt":"2026-11-13T08:00:00Z"},"meta":{"correlationId":"c0a80123-4567-489a-9abc-123456789abc"}}
```

**GET `/api/v1/support-requests/{requestId}/messages`**

```json
{"data":[{"messageId":"33333333-3333-4333-8333-333333333333","messageKind":"response","body":"The request may relate to an unstable network connection.","createdAt":"2026-11-13T08:00:00Z"}],"meta":{"correlationId":"c0a80123-4567-489a-9abc-123456789abc","page":{"limit":20,"nextCursor":null}}}
```

**GET `/api/v1/agent-runs/{agentRunId}`**

```json
{"data":{"agentRunId":"44444444-4444-4444-8444-444444444444","requestId":"22222222-2222-4222-8222-222222222222","status":"completed","completedAt":"2026-11-13T08:00:00Z","recommendation":{"summary":"The request may relate to an unstable network connection.","facts":[],"suggestions":[],"uncertainty":"Root cause not verified."},"policyDecision":null,"humanDecision":null,"toolResult":null},"meta":{"correlationId":"c0a80123-4567-489a-9abc-123456789abc"}}
```

**GET `/api/v1/agent-runs/{agentRunId}/events`**

```json
{"data":[{"eventId":"88888888-8888-4888-8888-888888888888","eventType":"workflow.completed","outcome":"completed","occurredAt":"2026-11-13T08:00:00Z"}],"meta":{"correlationId":"c0a80123-4567-489a-9abc-123456789abc","page":{"limit":20,"nextCursor":null}}}
```

**GET `/api/v1/knowledge/documents`**

```json
{"data":[{"documentId":"99999999-9999-4999-8999-999999999999","title":"VPN troubleshooting guide","status":"active","sourceReference":"demo://knowledge/vpn-guide"}],"meta":{"correlationId":"c0a80123-4567-489a-9abc-123456789abc","page":{"limit":20,"nextCursor":null}}}
```

**GET `/api/v1/knowledge/documents/{documentId}`**

```json
{"data":{"documentId":"99999999-9999-4999-8999-999999999999","title":"VPN troubleshooting guide","status":"active","sourceReference":"demo://knowledge/vpn-guide"},"meta":{"correlationId":"c0a80123-4567-489a-9abc-123456789abc"}}
```

**GET `/api/v1/approvals`**

```json
{"data":[{"approvalId":"55555555-5555-4555-8555-555555555555","requestId":"22222222-2222-4222-8222-222222222222","decisionStatus":"pending","toolKey":"mock_access_request","simulated":true,"requestedAt":"2026-11-13T08:05:00Z"}],"meta":{"correlationId":"c0a80123-4567-489a-9abc-123456789abc","page":{"limit":20,"nextCursor":null}}}
```

**GET `/api/v1/approvals/{approvalId}`**

```json
{"data":{"approvalId":"55555555-5555-4555-8555-555555555555","requestId":"22222222-2222-4222-8222-222222222222","decisionStatus":"pending","proposedAction":{"toolKey":"mock_access_request","parameters":{"requestType":"demo"},"expectedEffect":"Record a simulated request only"},"simulated":true,"expiresAt":"2026-11-13T08:20:00Z"},"meta":{"correlationId":"c0a80123-4567-489a-9abc-123456789abc"}}
```

**POST `/api/v1/approvals/{approvalId}/reject`**

```json
{"data":{"approvalId":"55555555-5555-4555-8555-555555555555","decision":"rejected","decidedAt":"2026-11-13T08:10:00Z","executionStatus":"prohibited"},"meta":{"correlationId":"c0a80123-4567-489a-9abc-123456789abc"}}
```

**GET `/api/v1/tools`**

```json
{"data":[{"toolKey":"mock_access_request","description":"Prepare a demonstration-only access request result.","simulated":true,"approvalRequired":true}],"meta":{"correlationId":"c0a80123-4567-489a-9abc-123456789abc"}}
```

**GET `/api/v1/tool-executions/{executionId}`**

```json
{"data":{"executionId":"66666666-6666-4666-8666-666666666666","approvalId":"55555555-5555-4555-8555-555555555555","status":"succeeded","simulated":true,"result":{"summary":"Simulation completed; no external system was changed."},"completedAt":"2026-11-13T08:12:00Z"},"meta":{"correlationId":"c0a80123-4567-489a-9abc-123456789abc"}}
```

**GET `/api/v1/audit-events`**

```json
{"data":[{"eventId":"77777777-7777-4777-8777-777777777777","eventType":"approval.decision","occurredAt":"2026-11-13T08:10:00Z","outcome":"approved","requestId":"22222222-2222-4222-8222-222222222222","correlationId":"c0a80123-4567-489a-9abc-123456789abc"}],"meta":{"correlationId":"c0a80123-4567-489a-9abc-123456789abc","page":{"limit":20,"nextCursor":null}}}
```

**GET `/api/v1/audit-events/{eventId}`**

```json
{"data":{"eventId":"77777777-7777-4777-8777-777777777777","eventType":"approval.decision","actorKind":"MEMBERSHIP","occurredAt":"2026-11-13T08:10:00Z","outcome":"approved","requestId":"22222222-2222-4222-8222-222222222222","approvalId":"55555555-5555-4555-8555-555555555555","correlationId":"c0a80123-4567-489a-9abc-123456789abc","metadata":{}},"meta":{"correlationId":"c0a80123-4567-489a-9abc-123456789abc"}}
```

**GET `/api/v1/reports/operations`**

```json
{"data":{"period":{"from":"2026-11-01T00:00:00Z","to":"2026-11-13T23:59:59Z"},"environment":"prototype-evaluation","ticketOutcomes":{"answered":12,"escalated":3,"awaitingApproval":2,"rejected":1,"failed":1},"latency":{"p50Ms":1800,"p95Ms":6200}},"meta":{"correlationId":"c0a80123-4567-489a-9abc-123456789abc"}}
```

The analytics example is illustrative only; any reported metric requires a documented cohort and measurement method and is not a production SLA or guaranteed business outcome.

## 31. Open API Decisions

1. Authentication protocol, identity provider, demo credential shape, bearer/cookie/session mechanism, issuer/claims, logout semantics, and token/session lifecycle.
2. Public/internal hostnames, HTTPS termination, gateway/routing, FastAPI service identity, internal authentication, and transport protection.
3. Exact endpoint naming/versioning approval, content negotiation, ETag/conditional update strategy, and correlation-ID format.
4. Whether support request analysis and summary are synchronous or asynchronous, and if asynchronous, the implementation mechanism. No callback endpoint or message broker is currently defined.
5. Initial/processing support request and agent status vocabulary, while preserving required user-visible outcomes.
6. Exact request/response schema versions, field-size limits, pagination cursor implementation, and error-code catalog.
7. Rate-limit quotas, request-size limits, idempotency-key retention, and per-service timeout values; answer-only deadline remains bounded by the SRS maximum of 10 seconds.
8. LLM provider/model and provider data terms. No provider is selected, no provider-specific public fields are exposed, and non-synthetic organizational data requires review.
9. Whether optional organization-level knowledge/role/action administration and `ResponseFeedback` are enabled. `ResponseFeedback` remains MVP-Should; binary upload/bulk ingestion is not defined.
10. Tool execution response mode (synchronous `200` or asynchronous `202`) and recovery for ambiguous simulation outcomes.
11. Exact RBAC role names, permissions, audit review policy, privacy thresholds, and external observability integration.
12. Retention and deletion policies for API records and provider-bound content, to be resolved before real employee data is processed.
13. How a principal with memberships in multiple organizations selects or switches the trusted current organization context. No client-controlled organization or membership-switching mechanism is currently specified.

Decisions must remain consistent with the approved PRD, SRS, threat model, architecture, sequence diagrams, and database model. None may create a direct AI authorization path, remove required human approval, or introduce live integrations into the MVP.
