# ADR-003: Configuration Management

## Status
Proposed

## Context
Several configuration parameters are currently hardcoded in the source code:
- **Prompts**: The `BLOGGER_PROMPT` is a large string embedded in the `BlogAgentService` constructor.
- **Image Agent URL**: While it has a default, it's still partially managed in code.
- **Cron Expressions**: (If any) should be externalized.
- **Topic Lists**: Hardcoded defaults in the controller.

Hardcoding these makes it difficult to change behavior without recompiling the code and makes it harder to manage different environments (dev, staging, prod).

## Decision
All configuration parameters will be moved to `src/main/resources/application.properties` (or `application.yml`).

## Implementation Plan
1. **Identify Constants**: List all strings, URLs, and numbers that vary by environment.
2. **Define Properties**: Create keys in `application.properties`.
3. **Inject Values**: Use Spring's `@Value` or `@ConfigurationProperties` to inject these values into the appropriate beans.
4. **Prompt Management**: For very large prompts, consider using a separate file or a dedicated prompt management system if they become too unwieldy for properties files.

## Consequences
- **Pros**:
    - Environment-specific configuration without code changes.
    - Easier management of prompts and URLs.
    - Better security (can use environment variables for sensitive values).
- **Cons**:
    - Requires careful management of property keys to avoid collisions.
    - Slightly more complex to track where a value is coming from during initial development.
