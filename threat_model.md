# STRIDE Threat Model: spring-ai-blog-agent

## System Overview
The `spring-ai-blog-agent` is a Spring Boot application designed to orchestrate AI agents for blog content generation, research, and management. It integrates with Ollama for LLM capabilities, WordPress for publishing, and a web crawler for research.

## Trust Boundaries
1. **User <-> Application**: Public internet. Untrusted user input.
2. **Application <-> Ollama**: Local/Internal network. Model output is untrusted.
3. **Application <-> WordPress**: External third-party service.
4. **Application <-> Web Crawler**: Public internet. High risk of untrusted data injection.
5. **Application <-> File System**: Local storage for data persistence and intermediate results.

## STRIDE Analysis

| Threat Category | Threat Description | Potential Impact | Mitigation Strategy |
| :--- | :--- | :--- | :--- |
| **Spoofing** | Attacker spoofing user to access WordPress or app admin. | Unauthorized content publication/deletion. | Implement robust authentication (OAuth2/JWT) and OPA-based authorization. |
| **Spoofing** | Malicious web content spoofing trusted research data. | Compromised blog content, misinformation. | Sanitize all scraped content; use trusted sources where possible. |
| **Tampering** | Prompt Injection bypassing OPA guardrails. | Unauthorized actions, data exfiltration. | Use strict prompt templates, input sanitization, and OPA logic to validate agent actions. |
| **Tampering** | Modifying configuration files (prompts, properties). | System hijacking, redirecting output. | Restrict file system permissions; use environment variables for secrets. |
| **Repudiation** | Lack of audit trail for agent-initiated WordPress actions. | Inability to trace malicious activity. | Implement structured logging for all tool executions and agent decisions. |
| **Information Disclosure** | Leaking API keys (WordPress, Ollama) in logs. | Unauthorized access to external services. | Use secret management (Vault, AWS Secrets Manager) or environment variables; mask secrets in logs. |
| **Information Disclosure** | SSRF via Web Crawler. | Access to internal network resources. | Implement strict URL allowlisting for the web crawler. |
| **Denial of Service** | Overwhelming Ollama with concurrent requests. | Application unresponsiveness. | Implement rate limiting and request queuing. |
| **Denial of Service** | File system exhaustion via `StorageService`. | Application crash, data loss. | Implement disk quotas and cleanup policies for temporary files. |
| **Elevation of Privilege** | Bypassing OPA guardrails via logic flaws. | Full system compromise. | Regular audits of Rego policies; use a "deny-by-default" posture. |
| **Elevation of Privilege** | Exploiting vulnerable dependencies (e.g., `jackson-databind`). | Remote Code Execution (RCE). | Use `dependency-check` or similar tools; keep libraries updated. |

## Attack Trees
- **Goal: Unauthorized WordPress Post**
  - [ ] Spoof User Credentials -> Bypass Login -> Access WordPress API.
  - [ ] Prompt Injection -> Agent Orchestrator -> WordPressTool -> Post Content.
  - [ ] Steal WordPress API Key -> Directly Access WordPress API.

- **Goal: Exfiltrate Data via Web Crawler**
  - [ ] SSRF Attack -> Request internal metadata service (e.g., 169.254.169.254).
  - [ ] Prompt Injection -> Agent Orchestrator -> WebCrawler -> Send data to attacker URL.
