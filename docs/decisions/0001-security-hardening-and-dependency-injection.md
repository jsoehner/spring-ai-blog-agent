# ADR-0001: Security Hardening and Dependency Injection Refactoring

## Status
Accepted

## Date
2026-07-05

## Context
During code quality and security reviews of the Spring AI Blog Agent, multiple critical and required vulnerabilities and design smells were identified:
1. **Path Traversal Bypass via OPA Aspect**: The OPA guardrail aspect passed raw paths to OPA, allowing bypasses like `/allowed/path/../../etc/passwd`.
2. **SSRF via Web Crawler**: The crawler allowed requests to loopback and private/local network ranges.
3. **Option Injection in subprocess execution**: `TlsScannerTool` accepted command-line target parameters without option separators, exposing it to argument injection.
4. **Command Injection**: `AutoDraftService` executed shell scripts containing dynamic topic names.
5. **ChatClient.Builder Mutability**: Shared `ChatClient.Builder` instances were mutated directly across agents, leaking configurations.
6. **Spring DI Bypass / Tool Initialization**: Tools were instantiated inside `ChatController` using `new`, leaving `@Value` properties uninitialized (resulting in null pointers) and causing high garbage collection overhead.

## Decision
1. **OPA Path Normalization**: Intercept and normalize all paths inside [OpaGuardrailAspect.java](file:///Users/jsoehner/spring-ai-blog-agent/src/main/java/com/example/demo/security/OpaGuardrailAspect.java) using `java.nio.file.Paths.get().toAbsolutePath().normalize()` prior to sending evaluation payloads to OPA.
2. **SSRF Blocking**: Validate URL domains and DNS-resolved IP addresses in [WebCrawlerConfig.java](file:///Users/jsoehner/spring-ai-blog-agent/src/main/java/com/example/demo/WebCrawlerConfig.java) to explicitly block loopback, multicast, link-local, and site-local (private) ranges.
3. **Option Separator**: Inject `--` in subprocess arguments in [TlsScannerTool.java](file:///Users/jsoehner/spring-ai-blog-agent/src/main/java/com/example/demo/TlsScannerTool.java) before dynamic parameters.
4. **Topic Sanitization**: Enforce alphanumeric and whitespace-only sanitization regex checks on dynamic parameters before writing outputs or executing commands.
5. **ChatClient Mutation**: Branch configurations off the shared builder via `chatClientBuilder.build().mutate()` to prevent cross-component configuration pollution.
6. **Constructor Dependency Injection**: Register all tools as `@Component`s and inject them via Spring DI in [ChatController.java](file:///Users/jsoehner/spring-ai-blog-agent/src/main/java/com/example/demo/ChatController.java).

## Alternatives Considered
* **Disabling Tools**: Denying tool usage entirely would limit agent capability. Hardening the existing tools balances security and utility.
* **Manual String Checks for Path Traversal**: String-based checking is notoriously error-prone. Using Java's native `java.nio.file.Path` resolution is a more robust approach.

## Consequences
* Enhanced security posture with protections against SSRF, Argument/Command Injection, and Path Traversal.
* Clean application architecture adhering to standard Spring DI practices, resolving configuration pollution issues and ensuring correct `@Value` resolution.
