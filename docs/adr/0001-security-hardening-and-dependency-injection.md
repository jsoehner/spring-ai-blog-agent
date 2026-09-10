---
adr_id: ADR-2026-0001
title: "Security Hardening and Dependency Injection Refactoring"
status: Accepted
risk_tier: Tier 1
control_domains:
  - Security
  - Architecture
  - DevSecOps
  - Operations
created_date: 2026-07-05
proposed_date: 2026-07-05
accepted_date: 2026-07-05
implemented_date: 2026-07-05
validated_date: 2026-07-05
next_review_date: 2027-07-05
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
  - "ChatController"
  - "OpaGuardrailAspect"
  - "WebCrawlerConfig"
  - "TlsScannerTool"
  - "AutoDraftService"
data_classification: Internal
external_exposure: false
third_party_dependency: true
model_or_ai_impact: "High (ChatClient configuration isolation and safe tool injection)"
residual_risk_owner:
  name: "jsoehner"
  role: "Repository Maintainer"
exceptions_or_risk_acceptances: []
technical_debt_items: []
technical_debt_assessment: "Low - Replaced manual component instantiation with standard Spring Dependency Injection"
traceability:
  issues: []
  pull_requests: []
  git_commits: []
supersedes: null
superseded_by: null
retention_classification: Permanent Governance
legal_hold: false
---

# ADR-2026-0001: Security Hardening and Dependency Injection Refactoring

## 1. Context and Problem Statement
During code quality and security reviews of the Spring AI Blog Agent, multiple critical security vulnerabilities and architectural design smells were identified across the application stack:
1. **Path Traversal Bypass via OPA Aspect**: The OPA guardrail aspect passed raw file paths to OPA, permitting path traversal bypasses such as `/allowed/path/../../etc/passwd`.
2. **SSRF via Web Crawler**: The web crawler allowed outbound requests to resolve to loopback (`127.0.0.1`) and private/local network ranges (`10.0.0.0/8`, `192.168.0.0/16`, etc.).
3. **Option Injection in Subprocess Execution**: `TlsScannerTool` accepted command-line target parameters without explicit option separators, exposing the execution context to CLI option injection.
4. **Command Injection**: `AutoDraftService` executed shell scripts using unvalidated, dynamic topic names directly in command strings.
5. **ChatClient.Builder Mutability**: Shared `ChatClient.Builder` instances were mutated directly across agents, causing cross-agent configuration leaks.
6. **Spring DI Bypass / Tool Initialization**: Tools were instantiated inside `ChatController` using `new`, leaving `@Value` properties uninitialized (resulting in null pointers) and causing high garbage collection overhead.

## 2. Decision Drivers
* Prevent remote code execution (RCE), Server-Side Request Forgery (SSRF), and arbitrary file read/write vulnerabilities.
* Enforce strict isolation between distinct agent `ChatClient` builder states.
* Align application architecture with Spring Framework best practices (Dependency Injection, Bean lifecycle management).
* Eliminate GC overhead caused by per-request tool instantiation.

## 3. Considered Options
* **Option 1**: Disable tools entirely and restrict dynamic execution (rejected: severely degrades LLM agent utility).
* **Option 2**: Apply manual string-based path checks and regex filtering (rejected: error-prone and easily bypassed).
* **Option 3**: Comprehensive security hardening (Path normalization via Java NIO, SSRF IP validation, option separators, input sanitization regex, builder mutation cloning) alongside full Spring DI refactoring (chosen).

## 4. Decision Outcome
Chosen Option: **Option 3**. The codebase is retrofitted with robust security controls and standard Spring DI architecture:
1. **OPA Path Normalization**: Intercept and normalize all paths inside `OpaGuardrailAspect.java` using `java.nio.file.Paths.get().toAbsolutePath().normalize()` prior to sending evaluation payloads to OPA.
2. **SSRF Blocking**: Validate URL domains and DNS-resolved IP addresses in `WebCrawlerConfig.java` to explicitly block loopback, multicast, link-local, and site-local (private) ranges.
3. **Option Separator**: Inject `--` in subprocess arguments in `TlsScannerTool.java` before dynamic parameters.
4. **Topic Sanitization**: Enforce alphanumeric and whitespace-only sanitization regex checks on dynamic parameters before writing outputs or executing commands.
5. **ChatClient Mutation**: Branch configurations off the shared builder via `chatClientBuilder.build().mutate()` to prevent cross-component configuration pollution.
6. **Constructor Dependency Injection**: Register all tools as `@Component`s and inject them via Spring DI in `ChatController.java`.

## 5. Architecture & Governance Alignment
This decision aligns with standard enterprise Java/Spring architecture patterns by enforcing inversion of control (IoC) and dependency injection. It satisfies enterprise security governance guidelines regarding input validation, defense-in-depth, and least-privilege resource access.

## 6. Security & Control Domain Mapping
* **Security**: Blocks SSRF, Path Traversal, Command Injection, and CLI Option Injection.
* **Architecture**: Restructures controller dependencies to leverage Spring container-managed singletons.
* **DevSecOps**: Guarantees predictable tool state and safe environment variable injection (`@Value`).
* **Operations**: Reduces memory churn and unhandled null pointer exceptions in production.

## 7. Risk Assessment & Mitigations
* **Risk**: String sanitization might reject legitimate complex topic titles.
  * *Mitigation*: Regex permits standard alphanumeric characters, spaces, hyphens, and underscores while stripping control characters and shell metacharacters.
* **Risk**: SSRF blocking might reject internal microservices if hostnames are not properly configured.
  * *Mitigation*: SSRF validator explicitly checks public vs private IP ranges, allowing legitimate external web crawling while blocking internal infrastructure probing.

## 8. Financial & Operational Impact
* Reduces infrastructure costs by preventing memory leaks and excessive GC pauses caused by instantiating tool objects via `new`.
* Minimizes operational overhead and incident triage costs by securing the agent workflow against exploit vectors.

## 9. Implementation & Migration Strategy
1. Annotate all tool classes with `@Component`.
2. Refactor `ChatController` to accept tool dependencies via constructor injection.
3. Update `OpaGuardrailAspect` to invoke `toAbsolutePath().normalize()`.
4. Update `WebCrawlerConfig` to perform IP range validation before HTTP connection execution.
5. Refactor `TlsScannerTool` to insert `--` arguments into `ProcessBuilder`.
6. Apply `chatClientBuilder.build().mutate()` pattern across all agent controllers.

## 10. Verification & Quality Assurance
* Unit and integration tests verified that `@Value` fields are properly injected.
* Path traversal test suites confirmed `/allowed/path/../../etc/passwd` normalizes safely and complies with OPA policies.
* SSRF test suites confirmed connections to `127.0.0.1`, `10.0.0.1`, and `169.254.169.254` are blocked with security exceptions.

## 11. Technical Debt & Residual Risk
* **Technical Debt**: Eliminates debt associated with manual object lifecycle management.
* **Residual Risk**: External DNS changes could dynamically resolve a domain to private IPs (mitigated further in ADR-0002 via DNS pinning).

## 12. Operational & Day-2 Considerations
* Monitor OPA evaluation log outputs for blocked path traversal attempts.
* Ensure container network rules complement application-level SSRF defenses.

## 13. Compliance & Audit Evidence
* OPA policy execution logs capture normalized absolute file path evaluation payloads.
* SonarQube / Semgrep security scans confirm elimination of raw string concatenation in subprocess invocations.

## 14. Review Schedule & Triggers
* **Schedule**: Annual review (Next review: 2027-07-05).
* **Triggers**: Security vulnerability reports, Spring AI core framework updates, or changes to internal network topology.

## 15. Related ADRs & References
* ADR-2026-0002: Mitigating DNS Rebinding SSRF and Aligning Project Rules
* OWASP Top 10 API Security Risks: SSRF & Command Injection

## 16. Appendix / Change History
* **2026-07-05**: Initial adoption and implementation of security hardening and dependency injection refactoring.
* **2026-08-08**: Retroactively updated format to conform with Enterprise ADR Governance Standard.
