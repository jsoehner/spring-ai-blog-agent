---
adr_id: ADR-2026-0002
title: "Mitigating DNS Rebinding SSRF and Aligning Project Rules"
status: Accepted
risk_tier: Tier 1
control_domains:
  - Security
  - Architecture
  - DevSecOps
created_date: 2026-07-18
proposed_date: 2026-07-18
accepted_date: 2026-07-18
implemented_date: 2026-07-18
validated_date: 2026-07-18
next_review_date: 2027-07-18
review_triggers:
  - "Annual architectural review"
  - "Network security audit"
  - "Container image baseline update"
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
  - "WebCrawlerConfig"
  - "tls_scanner.py"
  - "Dockerfile"
  - "AutoDraftService"
  - "ImageTools"
  - "OpaGuardrailAspect"
data_classification: Internal
external_exposure: false
third_party_dependency: true
model_or_ai_impact: "Medium (LLM prompt instructions for content formatting)"
residual_risk_owner:
  name: "jsoehner"
  role: "Repository Maintainer"
exceptions_or_risk_acceptances: []
technical_debt_items: []
technical_debt_assessment: "Low - Standardized workspace path resolution and OPA sidecar integration"
traceability:
  issues: []
  pull_requests: []
  git_commits: []
supersedes: null
superseded_by: null
retention_classification: Permanent Governance
legal_hold: false
---

# ADR-2026-0002: Mitigating DNS Rebinding SSRF and Aligning Project Rules

## 1. Context and Problem Statement
During an updated code review of repository components, several security gaps and project governance rule violations were identified:
1. **DNS Rebinding Vulnerability in Web Crawler**: While domain-level filtering was implemented in `WebCrawlerConfig.java`, a time-of-check to time-of-use (TOCTOU) DNS Rebinding vulnerability remained because `Jsoup.connect()` resolves the domain name independently after validation.
2. **SSRF Vulnerability in TLS Scanner Helper Script**: The Python helper `tls_scanner.py` did not validate whether target hostnames or IPs resolved to private, loopback, or local ranges, permitting probing of internal resources.
3. **Incorrect Dockerfile Build Steps Sequence**: The `COPY --chown` command in `Dockerfile` attempted to assign ownership to the `spring:spring` user before the user/group was actually created, resulting in unresolved file ownership build errors.
4. **Paragraph Structure Prompt Rule Violation**: `AutoDraftService` did not inject instructions preventing the LLM from bolding the first sentence of paragraphs or splitting it from the paragraph block.
5. **Local File Output Naming Rule Violation**: Draft files were prefixed with `"new-draft-"` in `AutoDraftService`, violating the project requirement to name files strictly using the normalized topic string.
6. **Path Traversal Vulnerability in Image Tools**: The `scanImageMetadata` and `moveImages` tools in `ImageTools.java` accepted arbitrary file system paths without validation, allowing directory enumeration or writing files outside the workspace directory structure. Additionally, these tools bypassed OPA sidecar file guardrails because `OpaGuardrailAspect.java` only intercepted `writeFile` and `readFile`.

## 2. Decision Drivers
* Prevent TOCTOU DNS Rebinding attacks that circumvent IP-level SSRF filters.
* Prevent SSRF exploitation via secondary helper scripts (`tls_scanner.py`).
* Ensure non-root Docker container builds complete cleanly with correct user permissions.
* Align LLM content output generation with strict formatting guidelines.
* Normalize local file generation names.
* Ensure all filesystem-touching tools validate paths within the workspace root and pass through OPA policy evaluation.

## 3. Considered Options
* **Option 1**: Disable HTTP redirects or delegate outbound HTTP traffic to an external forward proxy (rejected: adds external infrastructure complexity and deployment overhead).
* **Option 2**: Native DNS Cache Pinning, socket-level IP resolution in Python, Docker build step re-ordering, prompt hardening, strict topic filename generation, and expanding OPA aspect coverage (chosen).

## 4. Decision Outcome
Chosen Option: **Option 2**. The system has been updated with the following controls:
1. **DNS Cache Pinning**: In `WebCrawlerConfig.java`, set the JVM DNS cache TTL (`networkaddress.cache.ttl`) to 30 seconds inside a static initializer block to ensure resolved IPs during validation are reused by Jsoup, closing TOCTOU DNS Rebinding windows.
2. **SSRF Blocking in TLS Scanner**: Implemented `is_safe_host` validation in `tls_scanner.py` using Python `socket` and `ipaddress` modules to resolve hostnames and block loopback, private, and link-local IP ranges.
3. **Correct Dockerfile Sequence**: Re-ordered `Dockerfile` instructions to create the `spring` group and user *before* copying the build artifact jar.
4. **Prompt Instruction Alignment**: Added `CRITICAL: Do NOT bold the first sentence of your paragraphs, and do NOT separate the opening sentence from the rest of the paragraph; integrate it naturally into the same paragraph block.` to the system prompt in `AutoDraftService`.
5. **Standardized Local File Output Naming**: Modified `AutoDraftService` to use the raw topic string for filename base generation (`topic.replaceAll("\\s+", "-").toLowerCase() + ".html"`) rather than prepending `"new-draft-"`.
6. **Workspace Path Hardening**: Added workspace path verification in `ImageTools.java` ensuring all directory scan and move operations verify paths start with the normalized, absolute application root base directory path (`.`).
7. **OPA Guardrails Aspect Extension**: Expanded `OpaGuardrailAspect.java` to intercept `scanImageMetadata` and `moveImages` invocations with `resource_type: "file"` and mapped actions.

## 5. Architecture & Governance Alignment
This decision aligns with Defense-in-Depth and Least Privilege architecture principles by securing secondary execution layers (Python scripts) alongside the primary Java backend. It also ensures consistent container security by configuring dedicated unprivileged container user accounts.

## 6. Security & Control Domain Mapping
* **Security**: Eliminates DNS Rebinding SSRF, secondary script SSRF, and file path traversal in image tools. Expands OPA authorization logging.
* **Architecture**: Standardizes workspace path validation across Java tool classes.
* **DevSecOps**: Ensures non-root user setup in Docker builds succeeds deterministically.

## 7. Risk Assessment & Mitigations
* **Risk**: Setting DNS cache TTL globally might affect performance for long-lived JVM instances communicating with dynamic endpoints.
  * *Mitigation*: 30 seconds provides an optimal balance between security against DNS rebinding and caching efficiency for dynamic DNS updates.
* **Risk**: Strict LLM formatting instructions could reduce output creativity.
  * *Mitigation*: The prompt restriction specifically target paragraph structure without restricting topic depth or vocabulary.

## 8. Financial & Operational Impact
* Avoids extra proxy infrastructure costs by handling security natively within the JVM and Python runtimes.
* Reduces CI/CD container image build failure rates caused by user permission mismatches.

## 9. Implementation & Migration Strategy
1. Add `java.security.Security.setProperty("networkaddress.cache.ttl", "30")` to `WebCrawlerConfig`.
2. Update `tls_scanner.py` to validate resolved IPs against `ipaddress.ip_address(ip).is_private`.
3. Update `Dockerfile` to position `groupadd` and `useradd` prior to `COPY`.
4. Append formatting directives to `AutoDraftService` system prompts.
5. Update output filename logic in `AutoDraftService`.
6. Add root path containment checks to `ImageTools.java` and annotate methods for OPA interception.

## 10. Verification & Quality Assurance
* Unit tests for `tls_scanner.py` confirmed rejection of loopback and private IP hostnames.
* OPA aspect tests confirmed `scanImageMetadata` and `moveImages` trigger OPA policy evaluation.
* Docker build runs verified successful container compilation and unprivileged execution.

## 11. Technical Debt & Residual Risk
* **Technical Debt**: None identified.
* **Residual Risk**: External websites returning redirects to internal IPs are handled by JVM socket checks during redirect resolution.

## 12. Operational & Day-2 Considerations
* Monitor OPA sidecar logs for `scanImageMetadata` and `moveImages` evaluation results.
* Ensure base Docker images maintain compatible `groupadd`/`useradd` commands.

## 13. Compliance & Audit Evidence
* OPA audit logs show full coverage over file read, write, scan, and move operations.
* Docker image vulnerability scans show compliant non-root execution (`USER spring`).

## 14. Review Schedule & Triggers
* **Schedule**: Annual review (Next review: 2027-07-18).
* **Triggers**: Network security audits, major Docker base image upgrades, or shifts in LLM prompt guidelines.

## 15. Related ADRs & References
* ADR-2026-0001: Security Hardening and Dependency Injection Refactoring
* OWASP SSRF Prevention Cheat Sheet

## 16. Appendix / Change History
* **2026-07-18**: Initial adoption and implementation.
* **2026-08-08**: Retroactively updated format to conform with Enterprise ADR Governance Standard.
