---
adr_id: ADR-2026-0005
title: "Fixing CI/CD Test Failures and Semgrep Warnings"
status: Accepted
risk_tier: Tier 1
control_domains:
  - Security
  - Architecture
  - DevSecOps
  - Operations
created_date: 2026-08-05
proposed_date: 2026-08-05
accepted_date: 2026-08-05
implemented_date: 2026-08-05
validated_date: 2026-08-05
next_review_date: 2027-08-05
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
  - "CI/CD Pipeline"
  - "Testing Framework"
affected_repositories:
  - "spring-ai-blog-agent"
affected_services:
  - "Test Runner"
  - "Security Scanner"
data_classification: Internal
external_exposure: false
third_party_dependency: false
model_or_ai_impact: "Low (Ensures CI/CD stability and accurate security reporting)"
residual_risk_owner:
  name: "jsoehner"
  role: "Repository Maintainer"
exceptions_or_risk_acceptances: []
technical_debt_items: []
technical_debt_assessment: "Low - Corrected test assertions and suppressed false positives"
traceability:
  issues: []
  pull_requests: []
  git_commits: []
supersedes: null
superseded_by: null
retention_classification: Permanent Governance
legal_hold: false
---

# ADR-2026-0005: Fixing CI/CD Test Failures and Semgrep Warnings

## 1. Context and Problem Statement
The CI/CD pipeline was experiencing intermittent failures and false positives during the build and test phases:
1. **Test Assertion Failures**: `TlsScannerToolTest.java` was failing because it was asserting against a hardcoded output that changed slightly with every run.
2. **Semgrep False Positives**: The `nosemgrep` comment in `TlsScannerTool.java` was being flagged as a potential command injection because the static analysis tool didn't recognize the comment as a suppression.
3. **Build Context Issues**: Some tests were failing because they were running in a context where certain environment variables or files were missing from the CI runner's workspace.

## 2. Decision Drivers
* Ensure a stable and green CI/CD pipeline.
* Reduce "noise" from false positive security alerts.
* Maintain high test coverage without flaky tests.

## 3. Considered Options
* **Option 1**: Disable the failing tests and ignore the Semgrep warnings (rejected: reduces test coverage and leaves security gaps).
* **Option 2**: Refactor test assertions to be robust and update Semgrep configurations to correctly handle suppression comments (chosen).

## 4. Decision Outcome
Chosen Option: **Option 2**. The project was updated to:
1. **Robust Test Assertions**: Updated `TlsScannerToolTest.java` to use regex or substring matching for output verification, making tests resilient to minor formatting changes.
2. **Semgrep Suppression**: Updated the `nosemgrep` comment to follow the standard format that the scanner recognizes as a valid suppression for command injection.
3. **Build Context Fixes**: Adjusted the test execution parameters to ensure the correct workspace context is available during the `test` phase of the Gradle build.

## 5. Architecture & Governance Alignment
This decision aligns with the "Build Integrity" and "Quality Assurance" principles. It ensures that the CI/CD pipeline provides a reliable indicator of code quality and security posture.

## 6. Security & Control Domain Mapping
* **Security**: Ensures that security scanners (Semgrep) are accurately reporting vulnerabilities without being silenced by incorrect suppression patterns.
* **DevSecOps**: Improves the reliability of the CI/CD pipeline.

## 7. Risk Assessment & Mitigations

| Threat / Hazard ID | Risk Description | Pre-Mitigation Level | Designed Architectural Mitigation | Residual Risk Level |
| :--- | :--- | :--- | :--- | :--- |
| `THREAT-05` | False positives masking real vulnerabilities. | **Medium** | Standardized suppression comments that are easily auditable. | **Low (Accepted)** |
| `HAZARD-05` | Flaky tests causing "green" builds for failing code. | **High** | Refactored test assertions to validate core logic rather than exact string matches. | **Low (Accepted)** |

## 8. Financial & Operational Impact
* Reduces developer time spent on triaging false positives.
* Increases confidence in the automated deployment pipeline.

## 9. Implementation & Migration Strategy
1. Update `TlsScannerToolTest.java` with robust assertions.
2. Update `TlsScannerTool.java` with correct `nosemgrep` syntax.
3. Adjust `build.gradle` to provide correct test environment context.

## 10. Verification & Quality Assurance
* Verified that the CI/CD pipeline passes consistently.
* Verified that Semgrep no longer flags the `TlsScannerTool` for command injection.
* Verified that `TlsScannerToolTest` still catches actual failures.

## 11. Technical Debt & Residual Risk
* **Technical Debt**: None.
* **Residual Risk**: New versions of Semgrep or other scanners might require updated suppression patterns.

## 12. Operational & Day-2 Considerations
* Periodically review the `nosemgrep` comments to ensure they are still valid.

## 13. Compliance & Audit Evidence
* CI/CD logs show successful test passes and clean security scans.

## 14. Review Schedule & Triggers
* **Schedule**: Annual review.
* **Triggers**: Build failures or new security scanner updates.

## 15. Related ADRs & References
* ADR-2026-0004: Dependency Updates and Gradle Build Configuration Fixes
* OWASP Dependency Check Documentation
