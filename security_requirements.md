# Security Requirements: spring-ai-blog-agent

Derived from the STRIDE threat model.

## 1. Authentication & Authorization
- **Requirement**: All requests to the WordPress tool and administrative endpoints must be authenticated.
- **User Story**: As an admin, I want to ensure only authorized users can publish to WordPress.
- **Acceptance Criteria**:
    - JWT or session tokens are validated for every request.
    - OPA policies must verify the user has the `blog:publish` permission.
- **Requirement**: Implement a "deny-by-default" posture in OPA.
- **Acceptance Criteria**:
    - Any request not explicitly allowed by a Rego policy is rejected.

## 2. Input Sanitization & Prompt Security
- **Requirement**: All user input used in prompts must be sanitized to prevent prompt injection.
- **User Story**: As a security engineer, I want to prevent users from overriding the agent's core instructions.
- **Acceptance Criteria**:
    - Use strict prompt templates that separate system instructions from user data.
    - Implement a sanitization layer to filter out common injection patterns (e.g., "ignore previous instructions").
- **Requirement**: Sanitize all data retrieved from the web crawler before it is processed by the AI model.
- **Acceptance Criteria**:
    - Use `jsoup` to strip dangerous scripts and HTML tags.
    - Validate that the content matches expected formats (e.g., text, clean HTML).

## 3. Secrets Management
- **Requirement**: No secrets (WordPress API keys, Ollama URLs, etc.) shall be hardcoded in the codebase.
- **User Story**: As a developer, I want to manage secrets securely using environment variables.
- **Acceptance Criteria**:
    - All secrets are loaded via `application.properties` using environment variable placeholders.
    - Secrets are never logged to the console or files.

## 4. Audit Logging
- **Requirement**: All actions taken by the AI agents (e.g., tool calls, content creation) must be logged with a correlation ID.
- **User Story**: As a security auditor, I want to see a trail of what the agent did.
- **Acceptance Criteria**:
    - Log tool inputs/outputs (masking sensitive data).
    - Include timestamps, user IDs, and correlation IDs in every log entry.

## 5. Infrastructure Hardening
- **Requirement**: Prevent SSRF attacks by restricting the Web Crawler to an allowlist of domains.
- **User Story**: As a security engineer, I want to prevent the crawler from accessing internal metadata services.
- **Acceptance Criteria**:
    - Implement a whitelist of allowed domains in `WebCrawlerConfig`.
    - Block requests to private IP ranges (e.g., 10.0.0.0/8, 169.254.169.254/32).
- **Requirement**: Limit file system usage for `StorageService`.
- **Acceptance Criteria**:
    - Implement disk quota checks for the application's storage directory.
    - Periodically clean up temporary files.
