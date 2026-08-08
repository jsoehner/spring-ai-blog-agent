# ADR-004: Version Control Abstraction

## Status
Proposed

## Context
Currently, `AutoDraftService` directly executes shell commands using `ProcessBuilder` to interact with `git` and `gh` CLI tools. This approach has several issues:
- **Brittle**: Changes in CLI output or environment variables can break the logic.
- **Hard to Test**: Mocking `ProcessBuilder` output is difficult and leads to flaky tests.
- **Portability**: The code assumes `git` and `gh` are installed and configured in the environment's PATH.
- **Security**: Passing strings to `ProcessBuilder` requires careful sanitization to prevent command injection.

## Decision
We will introduce a `VersionControlService` to abstract all version control operations. This service will provide a clean, high-level API for common tasks like creating branches, committing changes, pushing to remote, and creating pull requests.

The implementation will initially support Git via CLI but will be designed with an interface that allows for future alternatives (e.g., a native Java Git library or an API-based provider).

## Implementation Plan
1. **Define Interface**: Create a `VersionControlService` interface.
2. **Implement Git Provider**: Create a `GitVersionControlService` implementation that handles the CLI interactions.
3. **Refactor AutoDraftService**: Replace direct `runCommand` calls with calls to the `VersionControlService`.
4. **Dependency Injection**: Inject the `VersionControlService` into `AutoDraftService` and other relevant components.

## Consequences
- **Pros**:
    - **Testability**: The service can be easily mocked for unit tests.
    - **Maintainability**: Changes to the underlying VCS (e.g., moving to a different CLI tool) only require updating the implementation.
    - **Security**: Centralized sanitization and command construction.
- **Cons**:
    - **Abstraction Overhead**: Adds a layer of indirection.
    - **Initial Effort**: Requires defining the interface and migrating existing calls.
