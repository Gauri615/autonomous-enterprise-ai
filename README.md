# Autonomous Enterprise AI System

**Intelligent AI Agents. Connected Enterprise. Autonomous Operations.**

## Product Vision

Make enterprise work safer and more effective by combining capable AI agents with clear policies, human control, and evidence of what happened. This is a commercial-oriented, MNC-focused product prototype—not an unrestricted automation platform or a production-ready service.

## First Use Case: AI IT Support Agent

The first workflow helps employees resolve routine IT questions using permission-eligible knowledge, clearly attributed sources, and escalation when evidence is insufficient. Where an allow-listed simulated action is relevant, the system can prepare a proposal for human review; it does not connect to or change live enterprise systems.

## Main Capabilities

- Employee IT support conversations and request tracking.
- Knowledge-grounded answers with source attribution, uncertainty, and suggestions distinguished from verified information.
- Organization- and user-permission-filtered knowledge retrieval.
- Bounded AI-agent workflows for classification, retrieval, response composition, and action proposals.
- Deterministic backend policy and authorization checks.
- Mandatory authorized human approval and execution-time revalidation for enabled simulated actions.
- Escalation and feedback paths.
- Correlated audit records for important requests, policy decisions, approvals, and simulated outcomes.

## High-Level Architecture

```text
React + TypeScript web app
          |
          v
Java + Spring Boot business API
  | authorization, tenant scope,
  | approvals, audit, simulation gate
  +-----------------------> MariaDB 10.11.16
  |
  +---- bounded internal API ----> Python + FastAPI AI service
                                     |
                                     +--> LangGraph workflows
                                     +--> LLM provider (undecided)
                                     +--> Knowledge retrieval (undecided)
```

Spring Boot is the authoritative business and authorization boundary. The AI service may recommend or propose; it cannot grant access, approve actions, or directly execute tools. Knowledge retrieval remains a separate unresolved decision.

## Approved Technology Stack

| Area | Technology / status |
|---|---|
| Frontend | React, TypeScript, Tailwind CSS |
| Business backend | Java 21+, Spring Boot, Spring Security, Spring Data JPA/Hibernate, Maven |
| AI service | Python, FastAPI |
| Agent orchestration | LangGraph |
| Primary database | MariaDB 10.11.16 |
| Local database environment | XAMPP; phpMyAdmin for management |
| Database name | `autonomous_enterprise_ai` |
| Vector retrieval | Unresolved; no technology selected |
| LLM provider | Unresolved |
| Redis | Optional; not currently required |
| Docker | Deferred; not required for initial local development |

For database environment and verification details, see [Database Setup](docs/architecture/DATABASE_SETUP.md).

## Security Principles

- **Tenant isolation:** Resolve organization scope from trusted server-side context and enforce it throughout data access.
- **Permission-filtered retrieval:** Filter knowledge by organization and user permissions before content reaches the AI service or model.
- **Deterministic backend authorization:** Spring Boot makes authoritative access, policy, and execution decisions; AI output is untrusted.
- **Human control:** Require explicit authorized human approval for enabled simulated actions and revalidate policy immediately before execution.
- **Simulated-only execution:** No live enterprise-system side effects are in scope for the prototype.
- **Auditability:** Preserve attributable, organization-scoped records of material workflow and security decisions.
- **Data minimization:** Use synthetic/demo data unless the required data, privacy, and operational reviews approve otherwise.

## Current Development Status

The project is entering the **Development Foundation** phase. Product, requirements, architecture, security, API, database, and readiness documentation exists. This initial structure does not initialize application projects or implement application code.

Architecture readiness is conditional: protected workflows and simulated actions remain gated by unresolved identity, lifecycle, tenant-scope, approval, and operational decisions. Real-data or shared deployment is not approved. Production readiness, compliance, and security certification are not claimed.

## Planned Development Phases

The phases follow the [Implementation Readiness Plan](docs/architecture/IMPLEMENTATION_READINESS_PLAN.md); they are dependency stages, not calendar commitments:

0. Resolve readiness decisions and align contracts.
1. Establish the local foundation, project scaffolding, synthetic fixtures, and test harness.
2. Implement identity, authorization, request lifecycle, and relational persistence.
3. Add bounded AI workflows and permission-aware retrieval after the retrieval decision is resolved.
4. Add audit and operational review.
5. Add human approval and simulated tools only after their gates are approved and verified.
6. Demonstrate the controlled prototype within the approved scope.
7. Consider real-data/shared deployment only after separate approvals and evidence.

## Local Development Setup Status

MariaDB 10.11.16 through XAMPP is the selected local database setup, with phpMyAdmin as the management interface. The user reports that `autonomous_enterprise_ai` has been created and that the verification query was executed; its output was not provided, so the observed server version, selected database, table list, and schema have not been independently verified.

`localhost` is the provided host. Port `3306` is default/expected but remains unverified. The database username, credentials, application connection string, installed XAMPP version, and application connectivity are not recorded here. Do not put secrets in documentation or source control. Docker is not required for this phase, and Redis is not currently required.

