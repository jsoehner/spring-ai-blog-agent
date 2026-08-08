---
adr_id: ADR-2026-0003
title: "Workflow and Agent Coordination Optimization"
status: Accepted
risk_tier: Tier 2
control_domains:
  - DevSecOps
  - Architecture
  - Operations
  - Resilience
created_date: 2026-07-19
proposed_date: 2026-07-19
accepted_date: 2026-07-19
implemented_date: 2026-07-19
validated_date: 2026-07-19
next_review_date: 2027-07-19
review_triggers:
  - "Annual architectural review"
  - "CI/CD runner policy update"
  - "Messaging system architecture changes"
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
  - "BlogAgentController"
  - "security-scan.yml"
  - "update-dependencies.py"
  - "TlsScannerToolTest"
data_classification: Internal
external_exposure: false
third_party_dependency: true
model_or_ai_impact: "Low (Multi-agent asynchronous execution)"
residual_risk_owner:
  name: "jsoehner"
  role: "Repository Maintainer"
exceptions_or_risk_acceptances: []
technical_debt_items: []
technical_debt_assessment: "Low - Non-blocking thread execution and parallelized CI/CD workflow matrix"
traceability:
  issues: []
  pull_requests: []
  git_commits: []
supersedes: null
superseded_by: null
retention_classification: Permanent Governance
legal_hold: false
---

# ADR-2026-0003: Workflow and Agent Coordination Optimization

## 1. Context and Problem Statement
During CI/CD pipeline runs and multi-agent coordination operations, several efficiency bottlenecks and test failure patterns were identified:
1. **GitHub Actions Nightly Update Inefficiency**: The nightly dependency update workflow ran `./gradlew dependencies --refresh-dependencies`, which did not update version definitions in `build.gradle`, rendering the automated pull request creation redundant and ineffective.
2. **Sequential CI/CD Scanning Latency**: The `security-scan.yml` workflow executed Gitleaks, Semgrep, and Trivy sequentially in a single job. This blocked downstream scanners if an earlier scanner failed, increased build execution time, and underutilized parallel runner resources.
3. **SSRF Fix and Test Mismatch**: The implementation of SSRF/DNS-rebinding mitigations in `tls_scanner.py` (which flagged private/invalid hosts as `Unsafe Host`) caused the JUnit test `TlsScannerToolTest` to fail because the test strictly asserted the output string `Failed`.
4. **Blocking Multi-Agent Orchestration**: The Supervisor Agent message consumer (`processSupervisorTask` in `BlogAgentController.java`) processed tasks synchronously on the RabbitMQ listener thread, blocking incoming task consumption during long-running downstream HTTP requests (such as Image Agent calls).

## 2. Decision Drivers
* Accelerate CI/CD pipeline feedback loops by parallelizing independent security scanners.
* Automate genuine dependency version bumps in `build.gradle` for nightly dependency update workflows.
* Maximize RabbitMQ messaging consumer throughput by decoupling message listening from task processing.
* Maintain deterministic unit and integration test execution.

## 3. Considered Options
* **Option 1**: Use standard GitHub Dependabot for updates and retain synchronous message processing (rejected: Dependabot lacks custom validation logic for this project, and synchronous processing starves RabbitMQ consumers under load).
* **Option 2**: Custom Python dependency updater script, parallel GitHub Actions scanner jobs, updated test assertions, and asynchronous message execution using `CompletableFuture` (chosen).

## 4. Decision Outcome
Chosen Option: **Option 2**. The following optimizations were implemented:
1. **Automated Dependency Updater Script**: Created `.github/scripts/update-dependencies.py` to query Maven Central (`maven-metadata.xml`) for the latest stable releases of Spring Boot, Spring AI, and explicit dependencies, updating `build.gradle` directly before PR submission.
2. **Parallelized CI/CD Jobs**: Split `security-scan.yml` into concurrent `gitleaks`, `semgrep`, and `trivy` jobs. Security reports are uploaded as build artifacts and aggregated in a downstream `reporting` job.
3. **Test Assertion Alignment**: Updated `TlsScannerToolTest.java` to accept either `Failed` or `Unsafe Host`, aligning unit tests with security control outputs.
4. **Asynchronous Message Processing**: Wrapped `processSupervisorTask` execution in a non-blocking `CompletableFuture.runAsync()`, freeing the RabbitMQ consumer thread immediately. Unit tests invoke `.join()` on returned futures to maintain deterministic test execution.

## 5. Architecture & Governance Alignment
This decision aligns with Reactive and Asynchronous Architecture guidelines for message-driven microservices. It also follows DevSecOps guidelines regarding matrix job parallelization and automated dependency management.

## 6. Security & Control Domain Mapping
* **DevSecOps**: Parallelizes security scanning jobs (Gitleaks, Semgrep, Trivy) and automates dependency updates.
* **Architecture**: Converts blocking messaging controllers to non-blocking asynchronous task execution.
* **Operations**: Increases queue processing throughput and prevents listener thread starvation under heavy load.
* **Resilience**: Prevents slow downstream subagents from cascading failures or blocking queue processing.

## 7. Risk Assessment & Mitigations
* **Risk**: Unbounded asynchronous thread creation under high RabbitMQ queue pressure could exhaust system memory.
* **Mitigation**: The thread pool used by `CompletableFuture` is managed by Spring's task executor infrastructure, capping total concurrent worker threads.
* **Risk**: Automated dependency updates could introduce breaking API changes.
* **Mitigation**: The nightly workflow runs full unit and integration test suites on the update branch prior to opening a PR.

## 8. Financial & Operational Impact
* Reduces GitHub Actions runner compute minutes by executing security scans concurrently rather than serially.
* Improves agent system responsiveness and request throughput without requiring additional compute hardware.

## 9. Implementation & Migration Strategy
1. Add `.github/scripts/update-dependencies.py` and update `.github/workflows/nightly-dependency-update.yml`.
2. Refactor `.github/workflows/security-scan.yml` into separate parallel jobs.
3. Modify `TlsScannerToolTest.java` assertion regex.
4. Update `BlogAgentController.java` to execute tasks via `CompletableFuture.runAsync()` and return futures for test joining.

## 10. Verification & Quality Assurance
* CI/CD security scanning duration was reduced significantly.
* Nightly update workflow successfully modified `build.gradle` versions and generated valid PRs.
* Unit tests for `BlogAgentController` passed consistently using `.join()`.

## 11. Technical Debt & Residual Risk
* **Technical Debt**: None.
* **Residual Risk**: Maven Central API rate limiting during script execution (mitigated by retry logic in the Python script).

## 12. Operational & Day-2 Considerations
* Monitor RabbitMQ queue depth and processing latency metrics.
* Verify artifact retention settings for parallel security scan uploads in GitHub Actions.

## 13. Compliance & Audit Evidence
* Combined security report generated by the downstream `reporting` job in `security-scan.yml`.
* Automated PR logs proving regular dependency scanning and updating.

## 14. Review Schedule & Triggers
* **Schedule**: Annual review (Next review: 2027-07-19).
* **Triggers**: CI/CD pipeline performance degradation, RabbitMQ queue performance changes, or major GitHub Actions runner updates.

## 15. Related ADRs & References
* ADR-2026-0001: Security Hardening and Dependency Injection Refactoring
* ADR-2026-0002: Mitigating DNS Rebinding SSRF and Aligning Project Rules

## 16. Appendix / Change History
* **2026-07-19**: Initial adoption and implementation of workflow and agent coordination optimizations.
* **2026-08-08**: Retroactively updated format to conform with Enterprise ADR Governance Standard.
