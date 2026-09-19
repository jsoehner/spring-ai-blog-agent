# ADR-008: Agent Orchestration Patterns

## Status
Proposed

## Context
The current project uses a central `AgentOrchestrator` to manage the flow of tasks between agents. While effective for the current scope, as the system grows in complexity and the number of agents increases, a purely centralized orchestrator can become a bottleneck and a complex state machine that is difficult to maintain.

## Decision
We will adopt a hybrid orchestration-choreography pattern for agent coordination:
1.  **Orchestration**: Use a central orchestrator (the `AgentOrchestrator`) for high-level workflow state management, handling complex multi-step dependencies, and coordinating transitions between major project phases.
2.  **Choreography**: Use RabbitMQ for point-to-point messaging and task distribution between agents for simple, independent tasks. This allows agents to react to messages and perform their work without the orchestrator needing to manage every intermediate step.
3.  **State Machine**: Formalize the `WorkflowState` as a state machine with clearly defined transitions and side effects for each state change.

## Consequences
- **Pros**:
  - **Scalability**: Decentralizing simple tasks via choreography allows for easier scaling of specific agent types.
  - **Maintainability**: Clearer separation between high-level workflow control and low-level task execution.
  - **Flexibility**: Easier to add new independent agents without significantly complicating the central orchestrator.
- **Cons**:
  - **Complexity**: Can be harder to visualize the entire end-to-end flow compared to a pure orchestration model.
  - **Debugging**: Distributed choreography can be more difficult to trace without robust correlation IDs and logging.
