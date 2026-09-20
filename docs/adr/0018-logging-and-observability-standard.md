# ADR-002: Logging & Observability Standard

## Status
Proposed

## Context
The current project relies heavily on `System.out.println` and `System.err.println` for logging. This approach is unsuitable for production environments as it lacks proper log levels, structured formatting, and the ability to easily aggregate or rotate logs. Furthermore, there is no unified correlation ID mechanism to track requests across asynchronous tasks (e.g., from a RabbitMQ consumer back to the original request).

## Decision
We will adopt SLF4J with Logback as the standard logging framework.

**Logging Standards:**
- **Levels**: Use `INFO` for general flow, `WARN` for recoverable issues, and `ERROR` for exceptions that require intervention.
- **Structured Logging**: All logs should include context (e.g., topic ID, request ID, user ID) using SLF4J's MDC (Mapped Diagnostic Context).
- **No Print Statements**: Replace all `System.out.println` and `System.err.println` with appropriate logger calls.

**Observability Standards:**
- **Correlation IDs**: Every entry point (REST request, RabbitMQ message) must generate or propagate a unique correlation ID. This ID must be included in every log entry and passed in headers to external services.
- **Metrics**: Implement Micrometer to export key performance indicators (KPIs) such as task completion time, failure rates, and LLM token usage.
- **Tracing**: Use OpenTelemetry for distributed tracing to visualize the flow of requests across the microservices architecture.

## Consequences
- **Pros**:
  - Centralized log management (easier searching and alerting).
  - Improved debugging capability through correlation IDs.
  - Better visibility into system performance and bottlenecks.
- **Cons**:
  - Initial effort required to replace all existing print statements.
  - Slight overhead in MDC management and correlation ID propagation.
