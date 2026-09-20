# ADR-001: Layered Architecture & Separation of Concerns

## Status
Proposed

## Context
The current project structure, while functional, exhibits several "God Controller" patterns where the Web layer is directly involved in complex business logic, infrastructure calls (e.g., RabbitMQ, OPA), and file system operations. This tight coupling makes unit testing difficult, hinders scalability, and complicates the addition of new features or infrastructure changes.

## Decision
We will adopt a strict 4-layer architecture to enforce separation of concerns:

1. **Web Layer**: Responsible for handling HTTP requests, request validation, and mapping request-specific DTOs to internal models. It should only call the Service Layer.
2. **Service Layer**: Responsible for business logic, workflow orchestration, and coordinating interactions between different components. It should not have knowledge of HTTP specifics or low-level infrastructure implementation.
3. **Infrastructure Layer**: Responsible for all external system interactions, including database access, message broker communication (RabbitMQ), external API clients (OpaClient, WordPressTool), and file system operations.
4. **Agent Layer**: A specialized layer for LLM-specific logic, including prompt management, tool definitions, and handling LLM-specific responses.

## Consequences
- **Pros**:
  - **Testability**: Service and Agent layers can be unit-tested in isolation by mocking the Infrastructure layer.
  - **Maintainability**: Changes to infrastructure (e.g., switching RabbitMQ to Kafka) only affect the Infrastructure layer.
  - **Scalability**: Clear boundaries allow for easier identification of bottlenecks and potential for independent scaling of layers.
- **Cons**:
  - **Boilerplate**: May require more DTOs and mapping logic to pass data between layers.
  - **Complexity**: Slightly higher initial complexity for simple features that might have been faster to implement as "flat" code.
