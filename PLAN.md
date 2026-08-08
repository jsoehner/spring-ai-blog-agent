# Technical Debt Remediation Plan (ADR Production)

This plan outlines the sequence of Architecture Decision Records (ADRs) to be produced to systematically address the technical debt identified in the audit.

## Phase 1: Core Architecture & Governance
Goal: Establish the fundamental principles for the project's structure.

- **ADR-001: Layered Architecture & Separation of Concerns**: Define the boundaries between Web, Service, Infrastructure, and Agent layers.
- **ADR-002: Logging & Observability Standard**: Establish a standard for structured logging (SLF4J) and distributed tracing.
- **ADR-003: Configuration Management**: Move hardcoded prompts, cron expressions, and URLs into external configuration (e.g., Spring Boot properties, Vault).

## Phase 2: Infrastructure Abstraction
Goal: Decouple the core logic from external tools and the file system.

- **ADR-004: Version Control Abstraction**: Define a `VersionControlService` to abstract Git/GitHub operations.
- **ADR-005: File System & Storage Strategy**: Define a standard for file operations and a strategy for local vs. cloud storage.
- **ADR-006: External Tool Integration Pattern**: Standardize how external tools (WordPress, StockPrice, etc.) are registered and invoked.

## Phase 3: Messaging & Agent Orchestration
Goal: Formalize how agents communicate and coordinate.

- **ADR-007: Message Broker & Task Routing**: Define the RabbitMQ topology and task routing logic.
- **ADR-008: Agent Orchestration Patterns**: Choose between Choreography (event-driven) and Orchestration (centralized) for multi-agent workflows.
- **ADR-009: Threading & Concurrency Model**: Define dedicated executors for different types of tasks (IO-bound vs. CPU-bound).

## Phase 4: Content & AI Strategy
Goal: Improve the reliability of AI outputs and content processing.

- **ADR-010: Content Processing Pipeline**: Define a pipeline for cleaning, validating, and transforming AI-generated HTML.
- **ADR-011: Prompt Engineering & Versioning**: Establish a strategy for managing and versioning complex system prompts.

## Execution Strategy
1.  **Identify** the next ADR in the sequence.
2.  **Draft** the ADR based on the specific technical debt it addresses.
3.  **Review** the ADR with the team (or as a self-correction step).
4.  **Implement** the changes required by the ADR.
5.  **Update** `TECH_DEBT.md` to reflect the progress.
