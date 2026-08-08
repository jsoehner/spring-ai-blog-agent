# ADR-002: Logging & Observability Standard

## Status
Proposed

## Context
The current implementation relies heavily on `System.out.println` and `System.err.println` for logging. This approach has several drawbacks:
- **Lack of Levels**: No distinction between INFO, DEBUG, WARN, and ERROR.
- **No Context**: No timestamps, thread IDs, or structured metadata.
- **Performance**: `System.out.println` is synchronized and can become a bottleneck.
- **Configuration**: Difficult to redirect logs to files, external systems (like ELK or Datadog), or change log levels dynamically.

## Decision
We will adopt **SLF4J** as the logging facade and **Logback** as the underlying implementation (the default for Spring Boot).

## Implementation Plan
1. **Standardize Imports**: Replace all `System.out.println` and `System.err.println` with `org.slf4j.Logger`.
2. **Logger Injection**: Use `@Slf4j` annotation (via Lombok) or manual `LoggerFactory.getLogger()` for all classes.
3. **Structured Logging**: Ensure that important events (e.g., "Topic rejected", "Processing topic", "Uploading draft") are logged at the appropriate level (INFO, DEBUG, ERROR).
4. **Exception Handling**: Log full stack traces for unexpected exceptions using the SLF4J error parameter.

## Consequences
- **Pros**:
    - Proper log levels for filtering.
    - Ability to output logs in JSON format for easier parsing by log aggregators.
    - Better performance and thread safety.
    - Standardized approach across the entire project.
- **Cons**:
    - Requires a one-time refactor of all existing log statements.
    - Slightly more complex configuration (logback.xml).
