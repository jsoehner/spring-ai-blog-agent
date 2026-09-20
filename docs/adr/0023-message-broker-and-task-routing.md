# ADR-007: Message Broker & Task Routing

## Status
Proposed

## Context
The project uses RabbitMQ to distribute tasks among various agents (Researcher, Image, BlogAgent). Currently, the routing logic and queue definitions are somewhat scattered, making it difficult to manage complex task flows or scale specific types of workers independently.

## Decision
We will standardize the RabbitMQ configuration and routing logic:
1.  **Exchange-Based Routing**: Use a consistent exchange-based routing system where tasks are published to specific exchanges and routed to queues based on routing keys.
2.  **Unified Task Schema**: Define a standard JSON schema for all tasks, including `task_id`, `type`, `payload`, and `metadata`.
3.  **Queue-per-Agent**: Each agent type (Researcher, Image, BlogAgent) will have its own dedicated queue to allow for independent scaling.
4.  **Dead Letter Queues (DLQ)**: Implement Dead Letter Queues for all primary task queues to handle and retry failed messages.

## Consequences
- **Pros**:
  - **Scalability**: Easily scale specific worker types by adding more instances to their dedicated queues.
  - **Observability**: Clearer task flow makes it easier to monitor and debug specific segments of the pipeline.
  - **Reliability**: DLQs ensure that failed tasks are not lost and can be analyzed.
- **Cons**:
  - **Configuration Complexity**: Requires more initial setup for exchanges, bindings, and DLQs.
  - **Overhead**: Slightly more complex to manage the routing keys and schema validation.
