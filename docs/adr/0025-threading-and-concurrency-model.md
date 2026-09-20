# ADR-009: Threading & Concurrency Model

## Status
Proposed

## Context
The current project uses `CompletableFuture.runAsync` for background tasks, which defaults to the common ForkJoinPool. This can lead to thread starvation if long-running tasks (e.g., LLM calls, heavy data processing, or network-bound operations) occupy the common pool, impacting other parts of the application.

## Decision
We will transition to using dedicated ThreadPoolExecutors for different types of tasks to ensure proper resource isolation and management:
1.  **IO-Bound Pool**: A large, bounded pool for network-intensive operations, such as calling the LLM, interacting with RabbitMQ, or making external API requests.
2.  **CPU-Bound Pool**: A smaller pool sized relative to the number of available cores for heavy data processing, content manipulation, and complex calculations.
3.  **Critical Task Pool**: A dedicated, high-priority pool for essential background tasks that must complete reliably.

All thread pools will be managed as Spring beans and configured via external properties to allow for environment-specific tuning.

## Consequences
- **Pros**:
  - **Stability**: Prevents thread starvation and ensures that long-running IO tasks do not block CPU-intensive work.
  - **Performance**: Allows for better optimization of thread counts and types based on actual resource usage.
  - **Observability**: Easier to monitor the health and saturation of specific thread pools.
- **Cons**:
  - **Configuration Overhead**: Requires careful tuning of pool sizes and types for different environments.
  - **Complexity**: Slightly more complex to manage and inject the appropriate executor for each task.
