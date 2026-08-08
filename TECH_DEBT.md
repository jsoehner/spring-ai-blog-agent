# Technical Debt Audit

## Overview
This project is a Spring AI-based agentic system for automated blog generation and research. While functional, several architectural and implementation choices contribute to technical debt, hindering scalability, maintainability, and security.

## Key Findings

### 1. Violation of Separation of Concerns (SoC)
- **God Controllers**: `BlogAgentController` handles HTTP requests, OPA policy evaluation, RabbitMQ messaging, AI orchestration, and file system operations.
- **Mixed Services**: `AutoDraftService` handles scheduling, AI prompting, WordPress interaction, and raw shell command execution.

### 2. Infrastructure Leakage
- **Shell Command Execution**: Direct use of `ProcessBuilder` for `git` and `gh` commands is brittle and hard to test.
- **File System Coupling**: Manual path traversal checks and file writing are scattered across components.
- **Hardcoded Configuration**: Cron expressions, topic lists, and large prompts are embedded in the source code.

### 3. Brittle Content Processing
- **Manual String Manipulation**: HTML content is cleaned and modified using manual string operations (e.g., stripping markdown blocks, replacing ampersands), which is prone to errors.

### 4. Observability & Concurrency
- **Logging**: Heavy reliance on `System.out.println` and `System.err.println` instead of a structured logging framework like SLF4J.
- **Thread Management**: Use of `CompletableFuture.runAsync` without a dedicated thread pool can lead to common pool exhaustion.

### 5. Security Concerns
- **Manual Security Checks**: Path traversal checks are implemented manually in multiple places.
- **Global Side Effects**: Running `git config --global` in a shared environment can cause conflicts.

## Impact
- **Difficult Testing**: High coupling makes unit testing nearly impossible without extensive mocking of external systems.
- **Reduced Scalability**: Lack of proper thread management and shared state could lead to issues under load.
- **Maintenance Overhead**: Changes to infrastructure (e.g., moving from GitHub to GitLab) require significant code changes.

## Progress Status
- [x] ADR-001: Layered Architecture & Separation of Concerns (Implemented)
- [x] ADR-002: Logging & Observability Standard (Implemented)
- [x] ADR-003: Configuration Management (Implemented)
- [x] ADR-004: Version Control Abstraction (Implemented)
- [x] ADR-005: File System & Storage Strategy (Implemented)
- [x] ADR-006: External Tool Integration Pattern (Implemented)
- [x] ADR-007: Message Broker & Task Routing (Implemented)
- [x] ADR-008: Agent Orchestration Patterns (Implemented)
- [x] ADR-009: Threading & Concurrency Model (Implemented)
- [x] ADR-010: Content Processing Pipeline (Implemented)
- [x] ADR-011: Prompt Engineering & Versioning (Implemented)
