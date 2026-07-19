# ADR-0003: Workflow and Agent Coordination Optimization

## Status
Accepted

## Date
2026-07-19

## Context
During CI/CD and multi-agent operations, several efficiency issues and test failures were identified:
1. **GitHub Actions Nightly Update Inefficiency**: The nightly dependency update workflow ran `./gradlew dependencies --refresh-dependencies` which did not actually update the version definitions in `build.gradle`, making the subsequent PR creation redundant and ineffective.
2. **Sequential CI/CD Scanning Latency**: The `security-scan.yml` workflow ran Gitleaks, Semgrep, and Trivy sequentially in a single job. This setup blocked subsequent scanners if one failed, increased build duration, and failed to utilize parallel runner resources.
3. **SSRF Fix and Test Mismatch**: The implementation of SSRF/DNS-rebinding prevention in [tls_scanner.py](file:///home/jsoehner/spring-ai-blog-agent/tls_scanner.py) (marking invalid/private domains as `Unsafe Host`) caused the JUnit test `TlsScannerToolTest` to fail because the test expected the string `Failed`, causing the build to fail.
4. **Blocking Multi-Agent Orchestration**: The Supervisor Agent message consumer (`processSupervisorTask` in [BlogAgentController.java](file:///home/jsoehner/spring-ai-blog-agent/src/main/java/com/example/demo/BlogAgentController.java)) processed tasks synchronously, blocking the RabbitMQ message listener thread on slow HTTP calls (e.g., Image Agent requests).

## Decision
1. **Automated Dependency Updater Script**: Created a Python script [.github/scripts/update-dependencies.py](file:///home/jsoehner/spring-ai-blog-agent/.github/scripts/update-dependencies.py) that queries Maven Central (`maven-metadata.xml`) for the latest stable versions of Spring Boot, Spring AI, and other explicit dependencies, automatically updating `build.gradle`.
2. **Parallelized CI/CD Jobs**: Split `security-scan.yml` into concurrent `gitleaks`, `semgrep`, and `trivy` jobs. The reports are uploaded as artifacts and aggregated in a final downstream `reporting` job.
3. **Test Assertion Alignment**: Updated [TlsScannerToolTest.java](file:///home/jsoehner/spring-ai-blog-agent/src/test/java/com/example/demo/TlsScannerToolTest.java) to accept either `Failed` or `Unsafe Host` to align with the new SSRF-prevention logic.
4. **Asynchronous Message Processing**: Wrapped `processSupervisorTask` execution in a non-blocking `CompletableFuture.runAsync()`, immediately freeing the RabbitMQ consumer thread for concurrent task consumption. Updated the JUnit tests to call `.join()` on the returned Future to ensure deterministic test runs.

## Alternatives Considered
* **Dependabot**: Standard Dependabot is another option, but custom scripts allow localized validation, dependency filtering rules, and self-testing in the pipeline before raising PRs.

## Consequences
* High-throughput, concurrent multi-agent system execution.
* Faster CI/CD execution times due to parallel security scanners.
* Dynamic, working nightly dependency updates with automated PRs.
* Stable, passing test suites.
