# ADR-003: Configuration Management

## Status
Proposed

## Context
Currently, several configuration parameters—including LLM prompts, cron expressions, internal service URLs, and RabbitMQ topic names—are either hardcoded in the source code or scattered across multiple configuration files. This makes it difficult to manage different environments (development, staging, production) without modifying the codebase. Furthermore, sensitive information (like API keys or secrets) must be moved into a secure configuration management system.

## Decision
We will adopt a centralized and externalized configuration management strategy:

1.  **Spring Boot Externalized Configuration**: All application-specific configurations (e.g., timeouts, retry counts, thread pool sizes) will be managed via `application.yml` and profile-specific overrides (e.g., `application-prod.yml`).
2.  **Prompt Versioning**: System prompts, which are critical for agent behavior, will be moved out of Java code and into externalized template files (e.g., `src/main/resources/prompts/`). These will be loaded at runtime and can be versioned independently of the code.
3.  **Secrets Management**: Sensitive secrets (API keys, database passwords, etc.) will never be committed to the repository. They will be injected via environment variables or a dedicated secrets manager (e.g., HashiCorp Vault, AWS Secrets Manager, or GitHub Secrets).
4.  **Dynamic Configuration**: For parameters that need to change without a restart (e.g., feature flags, rate limits), we will use Spring Cloud Config or a similar dynamic configuration provider.

## Consequences
- **Pros**:
  - **Portability**: The same build artifact can be deployed to any environment by simply changing the external configuration.
  - **Security**: Sensitive information is kept out of the version control system.
  - **Maintainability**: Prompts and business rules can be updated by non-developers without requiring code changes.
- **Cons**:
  - **Complexity**: Requires proper setup of environment variables and configuration profiles.
  - **Startup Overhead**: Fetching configuration from a remote provider (if used) adds a slight delay to application startup.
