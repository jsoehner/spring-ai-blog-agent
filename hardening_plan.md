# Code & Infrastructure Hardening Plan

Based on the threat model and scan reports, the following hardening actions are required:

## 1. Prompt Engineering & Input Validation
- [ ] **Action**: Refactor `AgentOrchestrator` to use a more robust prompt construction method.
- [ ] **Action**: Implement a "content validation" step after the LLM generates the blog post but before it is passed to `MarkdownSanitizer`.
- [ ] **Action**: Add a "Prompt Injection Detector" service that checks the raw user facts for adversarial patterns.

## 2. Content Sanitization
- [ ] **Action**: Replace the custom logic in `MarkdownSanitizer.java` with the `OWASP Java HTML Sanitizer` library.
- [ ] **Action**: Ensure all image URLs returned by the image agent are validated against a whitelist of safe protocols (https) and domains.

## 3. Rate Limiting & Resource Quotas
- [ ] **Action**: Configure Spring Boot's `Bucket4j` or similar to limit the number of `handleSupervisorTask` calls per user/IP.
- [ ] **Action**: Implement a request queue for the Ollama backend to prevent thread exhaustion.

## 4. Dependency Management
- [ ] **Action**: Add `dependency-check-maven` or a similar Gradle plugin to the CI/CD pipeline.
- [ ] **Action**: Upgrade `jackson-databind` to the latest stable version immediately.

## 5. Secrets & Logging
- [ ] **Action**: Ensure `application.properties` is never committed with real values.
- [ ] **Action**: Implement a log masking utility to automatically redact potential secrets (like API keys) from `AgentOrchestrator` logs.

## 6. Infrastructure
- [ ] **Action**: Verify that the OPA server is only accessible from the application server's internal IP.
- [ ] **Action**: Configure `StorageService` to use a dedicated volume with restricted permissions.
