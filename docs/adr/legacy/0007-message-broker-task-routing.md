# ADR-007: Message Broker & Task Routing

## Status
Proposed

## Context
The current implementation of `MessageService` uses a direct queue `research-tasks` to send topics for research.
```java
public void sendResearchTask(String topic) {
    rabbitTemplate.convertAndSend("research-tasks", topic);
}
```
While this works for a single research task, it lacks scalability and flexibility for a multi-agent system. As we add more agents (e.g., image generation, SEO optimization, fact-checking), we need a more robust routing strategy.

## Decision
We will transition from a direct queue to an exchange-based routing model using RabbitMQ.

### Topology:
1.  **Exchange**: A `topic` exchange named `agent.tasks.exchange`.
2.  **Routing Keys**: We will use dot-notated routing keys to categorize tasks:
    - `task.research.request`: For initial research requests.
    - `task.image.request`: For image generation requests.
    - `task.seo.request`: For SEO optimization requests.
    - `task.factcheck.request`: For fact-checking requests.
3.  **Queues**:
    - `research_worker_queue`: Bound to `task.research.*`.
    - `image_worker_queue`: Bound to `task.image.*`.
    - `seo_worker_queue`: Bound to `task.seo.*`.
    - `factcheck_worker_queue`: Bound to `task.factcheck.*`.

### Routing Logic:
- Producers will publish messages to `agent.tasks.exchange` with the appropriate routing key.
- Consumers (workers) will subscribe to specific queues based on their capabilities.

## Implementation Plan
1.  **Update Infrastructure**: Configure the RabbitMQ exchange and queues.
2.  **Refactor `MessageService`**: Update `sendResearchTask` to use the new exchange and routing keys.
3.  **Add New Methods**: Add methods to `MessageService` for other task types (e.g., `sendImageTask`, `sendSeoTask`).
4.  **Update Consumers**: Ensure that the corresponding worker services are listening on the correct queues.

## Consequences
- **Pros**:
    - **Scalability**: Easily add more workers for specific task types.
    - **Flexibility**: New task types can be added by defining new routing keys without changing existing infrastructure.
    - **Decoupling**: Producers don't need to know which specific worker handles a task.
- **Cons**:
    - **Complexity**: Slightly more complex configuration than a simple queue.
    - **Management**: Requires managing exchanges and bindings.
