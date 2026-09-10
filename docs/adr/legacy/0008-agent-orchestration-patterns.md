# ADR-008: Agent Orchestration Patterns

## Status
Proposed

## Context
The current blog generation flow is a hybrid of choreography and orchestration:
1.  **Choreography**: `queueBlogTopics` sends a research task to a queue. A worker (not currently implemented but planned) picks it up.
2.  **Orchestration**: `processSupervisorTask` receives facts and coordinates the writing, image generation, and publishing steps.

This hybrid approach is acceptable but lacks a clear definition of where the orchestration boundaries lie. As more agents are added (e.g., fact-checkers, SEO optimizers), the `processSupervisorTask` method will become a "God Method" for orchestration.

## Decision
We will adopt a **Hybrid Orchestration** model with a dedicated `AgentOrchestrator` component.

- **Choreography** will be used for independent, asynchronous tasks that don't require immediate feedback or tight sequencing (e.g., initial research, independent image generation).
- **Orchestration** will be used for complex, multi-step workflows where the sequence and state of the process are critical (e.g., the "Pass 2" blog writing and assembly).

A new `AgentOrchestrator` service will be introduced to manage the state and transitions of the `BlogGenerationWorkflow`.

## Implementation Plan
1.  **Define Workflow States**: Identify states for the blog generation process (e.g., `RESEARCHING`, `FACT_CHECKING`, `WRITING`, `ASSEMBLING`, `PUBLISHING`).
2.  **Create `AgentOrchestrator`**: A service that manages the transitions between these states and invokes the appropriate agents/tools.
3.  **Refactor `BlogAgentService`**: Move the orchestration logic from `processSupervisorTask` into the `AgentOrchestrator`.
4.  **Implement State Persistence**: Use a database or a shared cache (like Redis) to track the state of each blog generation request.

## Consequences
- **Pros**:
    - **Clarity**: Clearly separates "how to do a task" (Agent/Tool) from "when to do a task" (Orchestrator).
    - **Maintainability**: The `AgentOrchestrator` becomes the single source of truth for the workflow logic.
    - **Observability**: Easier to track the progress of a specific blog generation request by looking at its state.
- **Cons**:
    - **Complexity**: Adds a new component and state management logic.
    - **Overhead**: Requires extra calls to update and retrieve state.
