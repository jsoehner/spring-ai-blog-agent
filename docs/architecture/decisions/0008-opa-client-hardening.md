---
adr_id: ADR-2026-0008
title: "OPA Client Hardening and Fail-Closed Behavior"
status: Accepted
risk_tier: Tier 1
control_domains:
  - Security
  - Architecture
  - DevSecOps
  - Operations
created_date: 2026-08-08
proposed_date: 2026-08-08
accepted_date: 2026-08-08
implemented_date: 2026-08-08
validated_date: 2026-08-08
next_review_date: 2027-08-08
review_triggers:
  - "Annual architectural review"
  - "Security vulnerability policy update"
  - "Major Spring AI framework update"
adr_owner:
  name: "jsoehner"
  role: "Repository Maintainer"
decision_owner:
  name: "jsoehner"
  role: "Repository Maintainer"
accountable_role_or_forum: "Architecture Review Board"
acceptors:
  - name: "jsoehner"
    role: "Repository Maintainer"
affected_systems:
  - "spring-ai-blog-agent"
affected_repositories:
  - "spring-ai-blog-agent"
affected_services:
  - "OpaClient"
  - "OpaGuardrailAspect"
data_classification: Internal
external_exposure: false
third_party_dependency: true
model_or_ai_impact: "Low (Ensures security policy evaluation is robust and logged correctly)"
residual_risk_owner:
  name: "jsoehner"
  role: "Repository Maintainer"
exceptions_or_risk_acceptances: []
technical_debt_items: []
technical_debt_assessment: "Low - Replaced System.err with SLF4J and enforced configuration"
traceability:
  issues: []
  pull_requests: []
  git_commits: []
supersedes: null
superseded_by: null
retention_classification: Permanent Governance
legal_hold: false
---

# ADR-2026-0008: OPA Client Hardening and Fail-Closed Behavior

## 1. Context and Problem Statement
The initial implementation of the `OpaClient` had several production-readiness issues:
1. **Insecure Logging**: Used `System.err.println` for error reporting, which is not suitable for production environments and can leak sensitive information or be lost in containerized environments.
2. **Hardcoded Defaults**: Included a default OPA URL in the `@Value` annotation, which could lead to accidental connections to local/default instances in production.
3. **Fail-Closed Ambiguity**: While the client correctly returned `false` on error (Fail-Closed), it did not distinguish between a "Policy Deny" and an "Infrastructure Failure" (e.g., timeout, connection refused), making it harder for operators to troubleshoot.

## 2. Decision Drivers
* Standardize logging using SLF4J to support proper log rotation and aggregation.
* Enforce explicit configuration for infrastructure endpoints to prevent accidental misconfiguration.
* Ensure the system defaults to a secure state (Deny) while providing enough telemetry to differentiate between policy results and system errors.

## 3. Considered Options
* **Option 1**: Allow default URLs and use `System.out` for simplicity (rejected: violates security and production standards).
* **Option 2**: Implement a robust, production-ready client with SLF4J, mandatory configuration, and clear error handling (chosen).

## 4. Decision Outcome
Chosen Option: **Option 2**. The `OpaClient` has been refactored to:
1. Use SLF4J `Logger` for all error reporting.
2. Remove default values from `@Value` annotations, requiring `opa.url` to be explicitly provided.
3. Maintain the Fail-Closed behavior (return `false` on exception) while ensuring the specific exception is logged with sufficient context for debugging.

## 5. Architecture & Governance Alignment
This decision follows standard enterprise patterns for infrastructure clients: explicit configuration, standardized logging, and fail-secure error handling.

## 6. Security & Control Domain Mapping
* **Security**: Ensures that a failure in the security infrastructure (OPA) results in a "Deny" state.
* **Operations**: Provides better observability into the health of the security policy engine.

## 7. Risk Assessment & Mitigations
* **Risk**: Infrastructure failures will cause all tool executions to be denied.
  * *Mitigation*: This is the intended security behavior (Fail-Closed). Monitoring should be set up to alert on OPA connection errors.

## 8. Financial & Operational Impact
* Reduces incident response time by providing clearer logs for security policy failures.
* Prevents accidental connections to incorrect OPA instances in multi-tenant or complex environments.

## 9. Implementation & Migration Strategy
1. Replace `System.err` with `LoggerFactory.getLogger`.
2. Update `@Value("${opa.url}")` to remove the default URL.
3. Update all environment configurations (e.g., `application.properties`) to include the required `opa.url`.

## 10. Verification & Quality Assurance
* Verified that `OpaClient` throws a `SecurityException` (via the Aspect) or returns `false` when the OPA server is unreachable.
* Verified that logs contain the correct error messages and URLs.

## 11. Technical Debt & Residual Risk
* **Technical Debt**: None.
* **Residual Risk**: If the OPA server is down, the agent becomes non-functional. This is an acceptable trade-off for a security-first architecture.

## 12. Operational & Day-2 Considerations
* Ensure `opa.url` is correctly set in all deployment environments (Dev, Staging, Prod).

## 13. Compliance & Audit Evidence
* Logs provide a clear audit trail of OPA connection attempts and failures.

## 14. Review Schedule & Triggers
* **Schedule**: Annual review.
* **Triggers**: Changes to the OPA infrastructure or security policy requirements.

## 15. Related ADRs & References
* ADR-2026-0001: Security Hardening and Dependency Injection Refactoring
* ADR-2026-0007: Defense-in-Depth Path Validation
