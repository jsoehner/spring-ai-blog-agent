# ADR-001: Layered Architecture & Separation of Concerns

## Status
Proposed

## Context
The current implementation of `BlogAgentController` violates the Single Responsibility Principle by handling HTTP requests, RabbitMQ messaging, OPA policy evaluation, AI orchestration, and file system operations. This "God Class" makes the code difficult to test, maintain, and scale.

## Decision
We will adopt a layered architecture to separate concerns into distinct components:

1.  **Web Layer**: Responsible only for handling HTTP requests, validating input, and returning responses.
2.  **Service Layer**: Orchestrates business logic, coordinating between different infrastructure and agent components.
3.  **Infrastructure Layer**: Handles external integrations (RabbitMQ, OPA, WordPress, File System, External APIs).
4.  **Agent Layer**: Contains the logic for LLM interactions, including prompt construction and response parsing.

## Consequences
- **Pros**: Improved testability (each layer can be mocked), better maintainability, and clearer boundaries between different parts of the system.
- **Cons**: Increased number of classes and slightly more complex initial setup.
