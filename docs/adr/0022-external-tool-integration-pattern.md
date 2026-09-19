# ADR-006: External Tool Integration Pattern

## Status
Proposed

## Context
The project frequently interacts with external command-line tools (e.g., `git`, `gh`, `openssl`, `curl`). Currently, these interactions are often implemented as ad-hoc `ProcessBuilder` calls scattered across the codebase. This approach is brittle, difficult to test, and lacks a unified way to handle timeouts, retries, and consistent error parsing.

## Decision
We will adopt a standard `ExternalTool` integration pattern. All external tool interactions will be encapsulated within dedicated tool classes that implement a common interface.

Key requirements for the integration pattern:
1.  **Standardized Execution**: A centralized mechanism for executing commands with consistent timeout and retry policies.
2.  **Type-Safe Responses**: Each tool implementation will be responsible for parsing its own output into strongly-typed Java objects.
3.  **Testability**: Each tool integration will be mockable for unit tests.
4.  **Registry**: Tools will be registered in a central registry, allowing for easy discovery and configuration.

## Consequences
- **Pros**:
  - **Maintainability**: Centralizes the logic for interacting with external tools, making it easier to update commands or handle changes in tool output.
  - **Testability**: Allows for easy mocking of external tools in unit tests.
  - **Robustness**: Standardizes timeout and retry logic across all external tool calls.
- **Cons**:
  - **Boilerplate**: Requires creating a separate class for each external tool integration.
  - **Initial Effort**: Requires refactoring existing ad-hoc command executions into the new pattern.
