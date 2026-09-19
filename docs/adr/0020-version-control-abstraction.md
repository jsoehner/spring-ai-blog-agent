# ADR-004: Version Control Abstraction

## Status
Proposed

## Context
The current implementation of Git commands is scattered across the codebase, often using `ProcessBuilder` directly. This tight coupling to the local shell and Git binary makes it difficult to:
1. Test interactions with version control in isolation.
2. Switch to different providers (e.g., GitLab, Bitbucket) or APIs (e.g., GitHub REST API, GraphQL).
3. Manage complex Git operations consistently across different parts of the system.

## Decision
We will abstract all version control operations behind a `VersionControlService` interface. This interface will define high-level operations such as creating branches, committing changes, pushing to remote, and creating pull requests.

Implementations will be provider-specific (e.g., `GitProcessVersionControlService`, `GitHubApiVersionControlService`).

## Consequences
- **Pros**:
  - **Testability**: Allows for easy mocking of version control operations in unit tests.
  - **Flexibility**: Enables the application to support multiple version control providers with minimal changes to the core logic.
  - **Consistency**: Ensures that all Git operations follow a standardized pattern and handle errors uniformly.
- **Cons**:
  - **Complexity**: Adds an extra layer of abstraction which may slightly increase the initial development time.
  - **Maintenance**: New providers require implementing the full interface.
