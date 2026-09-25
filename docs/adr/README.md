# Architecture & Security Decision Log

This directory contains Architectural Decision Records (ADRs) and Security Architecture Decision Records (SecADRs) for this repository.

## Index of Architectural Decisions

| ID | Title | Date | Status | Classification |
| :--- | :--- | :--- | :--- | :--- |
| 0000 | [Record Architecture Decisions](0000-record-architecture-decisions.md) | 2026-09-13 | Accepted | Internal |
| 0001 | [Security Hardening and Dependency Injection Refactoring](0001-security-hardening-and-dependency-injection.md) | 2026-07-05 | Accepted | Internal |
| 0002 | [Mitigating DNS Rebinding SSRF and Aligning Project Rules](0002-mitigating-dns-rebinding-ssrf-and-aligning-project-rules.md) | 2026-07-18 | Accepted | Internal |
| 0003 | [Workflow and Agent Coordination Optimization](0003-workflow-and-agent-coordination-optimization.md) | 2026-07-19 | Accepted | Internal |
| 0004 | [Dependency Updates and Gradle Build Configuration Fixes](0004-dependency-updates-and-gradle-build-fixing.md) | 2026-08-05 | Accepted | Internal |
| 0005 | [Fixing CI/CD Test Failures and Semgrep Warnings](0005-fixing-ci-cd-test-failures.md) | 2026-08-05 | Accepted | Internal |
| 0006 | [Configure Gemma 4 and Qwen 3 Models](0006-configure-gemma4-and-qwen3-models.md) | 2026-08-05 | Accepted | Internal |
| 0007 | [Defense-in-Depth Path Validation](0007-defense-in-depth-path-validation.md) | 2026-08-08 | Accepted | Internal |
| 0008 | [OPA Client Hardening and Fail-Closed Behavior](0008-opa-client-hardening.md) | 2026-08-08 | Accepted | Internal |
| 0009 | [Integrating TextHumanize for AI Blog Post Naturalization](0009-integrating-texthumanize-for-blog-naturalization.md) | 2026-08-17 | Accepted | Internal |
| 0010 | [Lombok Integration and Java Build Compatibility](0010-lombok-integration-and-java-build-compatibility.md) | 2026-08-17 | Superseded by [ADR-0012](0012-java-25-upgrade-and-gutenberg-formatting-normalization.md) | Internal |
| 0011 | [Gitignore Maintenance for Agent Artefacts and README Gotchas Update](0011-gitignore-agent-artifacts-and-readme-gotchas.md) | 2026-08-17 | Accepted | Internal |
| 0012 | [Java 25 Upgrade and Gutenberg Block Formatting Normalization](0012-java-25-upgrade-and-gutenberg-formatting-normalization.md) | 2026-08-26 | Accepted | Internal |
| 0013 | [Align Docker Base Images with Java 25](0013-align-docker-base-images-with-java-25.md) | 2026-08-26 | Accepted | Internal |
| 0014 | [Multi-Agent Redundancy Elimination and Progressive Blog Generation Pipeline](0014-multi-agent-redundancy-elimination-and-progressive-generation.md) | 2026-09-06 | Accepted | Internal |
| 0015 | [Resolve CI/CD Workflow Failures, Gradle 10 Deprecations, and Dependency Upgrades](0015-resolve-ci-cd-workflow-failures-and-gradle-deprecations.md) | 2026-09-07 | Accepted | Internal |
| 0016 | [Multi-Layered Security Guardrails for AI Tool Execution](0016-multi-layered-security-guardrails.md) | 2024-05-23 | Accepted | Internal |
| 0017 | [Layered Architecture & Separation of Concerns](0017-layered-architecture-separation-of-concerns.md) | 2024-05-23 | Accepted | Internal |
| 0018 | [Logging & Observability Standard](0018-logging-and-observability-standard.md) | 2024-05-23 | Accepted | Internal |
| 0019 | [Configuration Management](0019-configuration-management.md) | 2024-05-23 | Accepted | Internal |
| 0020 | [Version Control Abstraction](0020-version-control-abstraction.md) | 2024-05-23 | Accepted | Internal |
| 0021 | [File System & Storage Strategy](0021-file-system-and-storage-strategy.md) | 2024-05-23 | Accepted | Internal |
| 0022 | [External Tool Integration Pattern](0022-external-tool-integration-pattern.md) | 2024-05-23 | Accepted | Internal |
| 0023 | [Message Broker & Task Routing](0023-message-broker-and-task-routing.md) | 2024-05-23 | Accepted | Internal |
| 0024 | [Agent Orchestration Patterns](0024-agent-orchestration-patterns.md) | 2024-05-23 | Accepted | Internal |
| 0025 | [Threading & Concurrency Model](0025-threading-and-concurrency-model.md) | 2024-05-23 | Accepted | Internal |
| 0026 | [Content Processing Pipeline](0026-content-processing-pipeline.md) | 2024-05-23 | Accepted | Internal |
| 0027 | [Prompt Engineering & Versioning](0027-prompt-engineering-and-versioning.md) | 2024-05-23 | Accepted | Internal |
| 0028 | [GitHub Actions SHA Pinning and Script Injection Remediation](0028-github-actions-sha-pinning-and-script-injection-remediation.md) | 2026-09-25 | Accepted | Internal |

## Legacy ADRs

| ID | Title | Date | Status | Classification |
| :--- | :--- | :--- | :--- | :--- |
| Legacy-0001 | [Layered Architecture & Separation of Concerns](legacy/0001-layered-architecture.md) | 2024-05-23 | Accepted | Internal |
| Legacy-0002 | [Logging & Observability Standard](legacy/0002-logging-observability.md) | 2024-05-23 | Accepted | Internal |
| Legacy-0003 | [Configuration Management](legacy/0003-configuration-management.md) | 2024-05-23 | Accepted | Internal |
| Legacy-0004 | [Version Control Abstraction](legacy/0004-version-control-abstraction.md) | 2024-05-23 | Accepted | Internal |
| Legacy-0005 | [File System & Storage Strategy](legacy/0005-file-system-storage-strategy.md) | 2024-05-23 | Accepted | Internal |
| Legacy-0006 | [External Tool Integration Pattern](legacy/0006-external-tool-integration-pattern.md) | 2024-05-23 | Accepted | Internal |
| Legacy-0007 | [Message Broker & Task Routing](legacy/0007-message-broker-task-routing.md) | 2024-05-23 | Accepted | Internal |
| Legacy-0008 | [Agent Orchestration Patterns](legacy/0008-agent-orchestration-patterns.md) | 2024-05-23 | Accepted | Internal |
| Legacy-0009 | [Threading & Concurrency Model](legacy/0009-threading-concurrency-model.md) | 2024-05-23 | Accepted | Internal |
| Legacy-0010 | [Content Processing Pipeline](legacy/010-content-processing-pipeline.md) | 2024-05-23 | Accepted | Internal |
| Legacy-0011 | [Prompt Engineering & Versioning](legacy/011-prompt-engineering-versioning.md) | 2024-05-23 | Accepted | Internal |
