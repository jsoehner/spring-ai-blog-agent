---
adr_id: ADR-2026-0007
title: "Defense-in-Depth Path Validation"
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
  - "OpaGuardrailAspect"
  - "CodeTools"
  - "ImageTools"
data_classification: Internal
external_exposure: false
third_party_dependency: false
model_or_ai_impact: "Medium (Ensures LLM-driven tool execution does not bypass filesystem boundaries)"
residual_risk_owner:
  name: "jsoehner"
  role: "Repository Maintainer"
exceptions_or_risk_acceptances: []
technical_debt_items: []
technical_debt_assessment: "Low - Implemented using standard Java NIO normalization"
traceability:
  issues: []
  pull_requests: []
  git_commits: []
supersedes: null
superseded_by: null
retention_classification: Permanent Governance
legal_hold: false
---

# ADR-2026-0007: Defense-in-Depth Path Validation

## 1. Context and Problem Statement
During a security audit of the Spring AI Blog Agent, it was identified that while the `OpaGuardrailAspect` intercepts tool calls, it relies on the raw path strings provided by the agent. An attacker (or a confused agent) could potentially use path traversal sequences (e.g., `../../etc/passwd`) to access files outside the intended workspace. Even if OPA policies are strict, the lack of normalization at the application layer creates a vulnerability if the OPA policy engine interprets these sequences differently than the underlying filesystem.

## 2. Decision Drivers
* Prevent path traversal attacks on the local filesystem.
* Ensure consistency between the path validated by OPA and the path accessed by the Java application.
* Implement a defense-in-depth strategy where security checks are not solely reliant on a single interceptor.

## 3. Considered Options
* **Option 1**: Rely solely on OPA policies to block traversal (rejected: risk of interpretation mismatch).
* **Option 2**: Manually check for `..` in every tool (rejected: error-prone and hard to maintain).
* **Option 3**: Use Java NIO to normalize paths to absolute form and verify they start with the workspace root before any action (chosen).

## 4. Decision Outcome
Chosen Option: **Option 3**. The application will now:
1. Normalize all paths to their absolute, canonical form using `Paths.get(path).toAbsolutePath().normalize()`.
2. Verify that the resulting path starts with the application's base workspace directory.
3. This check is performed inside the `OpaGuardrailAspect` to ensure the normalized path is what gets sent to OPA, and again inside individual tools as a secondary check.

## 5. Architecture & Governance Alignment
This decision aligns with the principle of "Defense in Depth" and "Fail Secure." It ensures that even if the AOP aspect is bypassed, the underlying tool implementation provides a secondary layer of protection.

## 6. Security & Control Domain Mapping
* **Security**: Blocks Path Traversal.
* **Architecture**: Standardizes path handling across all file-system-touching tools.
* **DevSecOps**: Simplifies security auditing by providing a single, predictable point of path normalization.

## 7. Risk Assessment & Mitigations

| Threat / Hazard ID | Risk Description | Pre-Mitigation Level | Designed Architectural Mitigation | Residual Risk Level |
| :--- | :--- | :--- | :--- | :--- |
| `THREAT-01` | Path traversal to sensitive system files (e.g., `/etc/passwd`). | **High** | Mandatory path normalization and workspace prefix verification in both Aspect and Tool layers. | **Low (Accepted)** |
| `HAZARD-02` | Symbolic link bypass of workspace boundaries. | **Medium** | Use of `toAbsolutePath().normalize()` to resolve path segments. | **Low (Accepted)** |

## 8. Financial & Operational Impact
* Negligible performance overhead for path normalization.
* Reduces the risk of catastrophic data loss or unauthorized file access.

## 9. Implementation & Migration Strategy
1. Update `OpaGuardrailAspect.java` to normalize paths before OPA evaluation.
2. Update `CodeTools.java` and `ImageTools.java` to perform the same check.
3. Document the new path validation logic in the technical documentation.

## 10. Verification & Quality Assurance
* Unit tests verified that `../../etc/passwd` is correctly normalized and rejected.
* Integration tests confirmed that valid paths within the workspace are permitted.

## 11. Technical Debt & Residual Risk
* **Technical Debt**: None.
* **Residual Risk**: Symbolic links pointing outside the workspace (mitigated by checking the normalized absolute path).

## 12. Operational & Day-2 Considerations
* Monitor for `SecurityException` logs related to path traversal attempts.

## 13. Compliance & Audit Evidence
* OPA evaluation logs will now show normalized absolute paths for all file operations.

## 14. Review Schedule & Triggers
* **Schedule**: Annual review.
* **Triggers**: Changes to the application workspace structure or new filesystem-based tools.

## 15. Related ADRs & References
* ADR-2026-0001: Security Hardening and Dependency Injection Refactoring
* OWASP Top 10: Path Traversal
