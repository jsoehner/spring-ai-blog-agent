# ADR-009: Threading & Concurrency Model

## Status
Proposed

## Context
The application performs various types of tasks that have different concurrency requirements:
- **IO-Bound**: Calling external APIs (LLMs, WordPress, Image Agent), database operations, and file system access.
- **CPU-Bound**: Complex data processing, HTML cleaning, and large-scale string manipulations.
- **Scheduled Tasks**: The `AutoDraftService` runs on a schedule.

Currently, the application relies on default thread pools (e.g., Spring's TaskExecutor, `CompletableFuture` common pool). This can lead to:
- **Thread Exhaustion**: Long-running IO operations blocking threads needed for other tasks.
- **Lack of Isolation**: A slow external API could starve the entire application of threads.
- **Unpredictable Performance**: No guarantees on the number of concurrent tasks of a specific type.

## Decision
We will implement a dedicated thread pool strategy using Spring's `ThreadPoolTaskExecutor`.

We will define three distinct thread pools:
1.  **`io-bound-executor`**: A large pool for blocking operations (REST clients, file I/O).
2.  **`cpu-bound-executor`**: A smaller pool sized based on available cores for compute-intensive tasks.
3.  **`scheduled-executor`**: A dedicated pool for `@Scheduled` tasks to ensure they don't interfere with other operations.

## Implementation Plan
1.  **Configure Thread Pools**: Define the pools in `application.properties`.
2.  **Create `ExecutorConfig`**: A configuration class to initialize and expose these executors as Beans.
3.  **Refactor Services**:
    - Inject the appropriate `Executor` into services.
    - Use `CompletableFuture.supplyAsync(..., executor)` or `@Async("poolName")` to offload tasks.
4.  **Monitor Thread Pools**: Implement basic monitoring (e.g., logging pool size and active threads).

## Consequences
- **Pros**:
    - **Isolation**: Failure or slowness in one type of task won't affect others.
    - **Predictability**: Better control over resource allocation.
    - **Stability**: Prevents thread exhaustion and "noisy neighbor" problems.
- **Cons**:
    - **Complexity**: Requires careful tuning of pool sizes.
    - **Management**: More configuration to maintain.
