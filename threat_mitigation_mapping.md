# Threat Mitigation Mapping: spring-ai-blog-agent

This document maps identified threats to specific technical controls and requirements.

## Mitigation Matrix

| Threat | Requirement ID | Mitigation Control | Implementation Detail |
| :--- | :--- | :--- | :--- |
| **User Spoofing** | 1.1 | OPA Authorization | Use `OpaGuardrailAspect` to check `blog:publish` permissions before tool execution. |
| **Malicious Content Spoofing** | 2.2 | Content Sanitization | Use `jsoup` in the `WebCrawler` component to strip scripts and malicious HTML. |
| **Prompt Injection** | 2.1 | Template Separation | Use static prompt templates in `src/main/resources/prompts` with delimited user input. |
| **Configuration Tampering** | 1.2 | Environment Variables | Store all secrets and sensitive configs in environment variables, never in `application.properties`. |
| **Repudiation** | 4.1 | Structured Logging | Implement `CorrelationIdFilter` and log all `ToolRegistry` actions. |
| **Information Disclosure (Keys)** | 3.1 | Secrets Management | Use an environment-based secrets provider; mask keys in logs. |
| **Information Disclosure (SSRF)** | 5.1 | URL Allowlisting | Implement a domain allowlist in `WebCrawlerConfig`. |
| **Denial of Service (Ollama)** | 6.1 | Rate Limiting | Implement a request queue and rate limiter for the `Ollama` connection. |
| **Denial of Service (Storage)** | 6.2 | Quotas & Cleanup | Implement file size checks in `StorageService` and a daily cleanup task. |
| **Elevation of Privilege** | 1.2 | Deny-by-Default | Configure OPA policies to deny any action not explicitly permitted. |
| **Dependency Vulnerabilities** | 1.3 | Dependency Auditing | Use `dependency-check` or `snyk` to monitor `build.gradle` dependencies. |

## Priority Actions
1. **High Priority**: Implement URL allowlisting for the Web Crawler (SSRF).
2. **High Priority**: Ensure OPA policies are "deny-by-default".
3. **Medium Priority**: Implement rate limiting for the Ollama backend.
4. **Medium Priority**: Audit and sanitize all prompt templates.
