---
adr_id: ADR-2026-0004
title: "Dependency Updates and Gradle Build Configuration Fixes"
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
  - "Build System"
  - "Dependency Manager"
affected_repositories:
  - "spring-ai-blog-agent"
affected_services:
  - "Build System"
  - "Dependency Manager"
data_classification: Internal
external_exposure: false
third_party_dependency: true
model_or_ai_impact: "Low (Ensures build stability and dependency security)"
residual_risk_owner:
  name: "jsoehner"
  role: "Repository Maintainer"
exceptions_or_risk_acceptances: []
technical_debt_items: []
technical_debt_assessment: "Low - Fixed broken build configuration and updated dependencies"
traceability:
  issues: []
  pull_requests: []
  git_commits: []
supersedes: null
superseded_by: null
retention_classification: Permanent Governance
legal_hold: false
---

# ADR-2026-0004: Dependency Updates and Gradle Build Configuration Fixes

## 1. Context and Problem Statement
During maintenance and local environment testing of the Spring AI Blog Agent, several issues prevented successful compilation, dependency updates, and containerization:
1. **Broken Dependency Check Plugin ID**: The OWASP dependency check plugin was configured with the incorrect ID `org.owasp.dependency-check` instead of `org.owasp.dependencycheck` in `build.gradle`.
2. **Deprecated Dependency Check Properties**: The `build.gradle` configured `nvdData_path` and the nested `reports` DSL, which are deprecated and absent in version 9.0.9+, failing the build configuration phase.
3. **Missing Versions Plugin**: The Gradle versions plugin was not applied to `build.gradle`, preventing the local execution of `./gradlew dependencyUpdates` to dynamically evaluate new packages.
4. **Syntax/Compilation Errors**: 
   - [AutoDraftService.java](file:///Users/jsoehner/spring-ai-blog-agent/src/main/java/com/example/demo/AutoDraftService.java) had an extra closing brace `}` at the end of the file.
   - [OpaGuardrailAspect.java](file:///Users/jsoehner/spring-ai-blog-agent/src/main/java/com/example/demo/security/OpaGuardrailAspect.java) referenced the `request` Map in `input.put(...)` before declaring the `request` variable.
5. **Outdated Dependencies**: Recommended dependency updates from the report needed to be applied to ensure the system is up-to-date.

## 2. Decision Drivers
* Ensure successful compilation and build of the project in all environments.
* Automate the identification and application of dependency updates.
* Align with the latest version requirements of the OWASP Dependency Check plugin.
* Eliminate syntax errors that block the CI/CD pipeline.

## 3. Considered Options
* **Option 1**: Manually update dependencies and fix build issues without automated tools (rejected: slow and error-prone).
* **Option 2**: Implement a robust Gradle build configuration with automated version management and corrected plugin IDs (chosen).

## 4. Decision Outcome
Chosen Option: **Option 2**. The build system was overhauled to:
1. **Gradle Build Hardening**: 
   - Corrected the plugin ID to `org.owasp.dependencycheck` in `build.gradle`.
  - Updated properties to be version 9.0.9+ compatible: replaced `nvdData_path` with `data.directory`, removed the `reports` DSL, and set `outputDirectory` and `format` directly.
  - Applied the `com.github.ben-manes.versions` plugin version `0.54.0` to `build.gradle`.
2. **Codebase Correction**:
   - Removed the extra closing brace at the end of [AutoDraftService.java](file:///Users/jsoehner/spring-ai-blog-agent/src/main/java/com/example/demo/AutoDraftService.java).
   - Rearranged variable declarations in [OpaGuardrailAspect.java](file:///Users/jsoehner/spring-ai-blog-agent/src/main/java/com/example/demo/security/OpaGuardrailAspect.java) to declare `request` prior to placing it in the `input` map.
3. **Dependency Upgrades**:
   - Executed `.venv/bin/python3 .github/scripts/update-dependencies.py` to automatically update `build.gradle`.
   - Upgraded Spring Boot to `4.1.0` (as recommended by the report), `jsoup` to `1.23.1`, and `metadata-extractor` to `2.21.0`.
4. **Docker Validation**:
   - Ran `./run-and-submit.sh --build "AI Security"` to verify that the build succeeds inside the multi-stage Docker environment and that the local containers orchestrate and process queued requests correctly.

## 5. Architecture & Governance Alignment
This decision aligns with the principle of "Build Integrity" and "Automated Maintenance." It ensures that the project remains buildable and secure through automated dependency analysis.

## 6. Security & Control Domain Mapping
* **Security**: Ensures the OWASP Dependency Check plugin runs correctly to identify known vulnerabilities.
* **DevSecOps**: Automates the dependency update lifecycle.

## 7. Risk Assessment & Mitigations

| Threat / Hazard ID | Risk Description | Pre-Mitigation Level | Designed Architectural Mitigation | Residual Risk Level |
| :--- | :--- | :--- | :--- | :--- |
| `THREAT-04` | Build failure due to incompatible dependency versions. | **Medium** | Automated version check via `versions` plugin and manual validation via `./run-and-submit.sh`. | **Low (Accepted)** |
| `HAZARD-04` | Dependency check plugin failing to run or report. | **High** | Corrected plugin ID and properties to match version 9.0.9+ specifications. | **Low (Accepted)** |

## 8. Financial & Operational Impact
* Reduces manual effort for dependency management.
* Prevents build failures in CI/CD pipelines.

## 9. Implementation & Migration Strategy
1. Correct `build.gradle` plugin IDs and properties.
2. Apply the `versions` plugin.
3. Run the update script to synchronize versions.
4. Verify with a full Docker build and run.

## 10. Verification & Quality Assurance
* Successful execution of `./gradlew dependencyUpdates`.
* Successful build and execution of the agent via `./run-and-submit.sh`.
* Verified `build.gradle` properties match OWASP Dependency Check 9.0.9+ requirements.

## 11. Technical Debt & Residual Risk
* **Technical Debt**: None.
* **Residual Risk**: Sudden breaking changes in major dependency versions (mitigated by staged updates and manual verification).

## 12. Operational & Day-2 Considerations
* Regularly run `./gradlew dependencyUpdates` to identify new vulnerabilities.

## 13. Compliance & Audit Evidence
* `build.gradle` configuration reflects the use of OWASP Dependency Check.
* Dependency check reports are generated as part of the build process.

## 14. Review Schedule & Triggers
* **Schedule**: Quarterly review.
* **Triggers**: Build failures, new security advisories, or major framework updates.

## 15. Related ADRs & References
* ADR-2026-0001: Security Hardening and Dependency Injection Refactoring
* OWASP Dependency Check Documentation
