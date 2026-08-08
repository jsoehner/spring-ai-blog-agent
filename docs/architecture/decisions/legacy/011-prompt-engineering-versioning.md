# ADR-011: Prompt Engineering & Versioning

## Status
Implemented

## Context
System prompts are critical to the behavior of our agents. Currently, they are stored in text files or properties. This presents several issues:
- **Lack of Versioning**: It's hard to know what prompt version was used for a specific blog post.
- **No A/B Testing**: Hard to compare the performance of different prompt versions.
- **No History**: Hard to track why a prompt was changed or what the previous versions were.
- **Deployment Risk**: Changing a prompt in a file can have immediate and potentially large effects on the system.

## Decision
We will implement a **Prompt Management System**.

Prompts will be treated as first-class assets with versioning and metadata.

### Strategy:
1.  **Prompt Repository**: Store prompts in a structured format (e.g., YAML or JSON) that includes version, description, and parameters.
2.  **Prompt Versioning**: Each prompt will have a version ID (e.g., `v1.0.0`).
3.  **Dynamic Loading**: The system will request a specific version of a prompt based on configuration or request context.
4.  **Audit Trail**: Every execution will log the prompt version used.

## Implementation
We have implemented a `PromptManager` service that maintains a registry of `PromptTemplate` objects. 
- Prompts are registered as Beans in `PromptConfiguration`.
- The `PromptManager` allows retrieval of prompts by name and version, or by fetching the "latest" version.
- `AgentOrchestrator` and `BlogAgentService` have been refactored to use `PromptManager` to retrieve the `blogger-prompt`.

## Consequences
- **Pros**:
    - **Reproducibility**: Can perfectly reproduce a blog post by using the same prompt version.
    - **Safe Iteration**: Easier to test and roll back prompt changes.
    - **A/B Testing**: Foundation for comparing different prompt versions.
- **Cons**:
    - **Complexity**: More infrastructure for prompt management.
    - **Initial Effort**: Requires migrating existing prompts into the new format.
