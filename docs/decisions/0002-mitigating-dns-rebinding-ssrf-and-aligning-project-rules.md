# ADR-0002: Mitigating DNS Rebinding SSRF and Aligning Project Rules

## Status
Accepted

## Date
2026-07-18

## Context
During an updated manual code review of the repository components, several security gaps and violations of established project guidelines were identified:
1. **DNS Rebinding Vulnerability in Web Crawler**: While domain-level filtering was implemented in [WebCrawlerConfig.java](file:///home/jsoehner/spring-ai-blog-agent/src/main/java/com/example/demo/WebCrawlerConfig.java), a time-of-check to time-of-use (TOCTOU) DNS Rebinding vulnerability remained because `Jsoup.connect()` resolves the domain name independently after validation.
2. **SSRF Vulnerability in TLS Scanner Helper Script**: The Python helper [tls_scanner.py](file:///home/jsoehner/spring-ai-blog-agent/tls_scanner.py) did not validate whether target hostnames or IPs resolved to private, loopback, or local ranges, allowing internal resources to be probed.
3. **Incorrect Dockerfile Build Steps Sequence**: The `COPY --chown` command in [Dockerfile](file:///home/jsoehner/spring-ai-blog-agent/Dockerfile) attempted to assign ownership to the `spring:spring` user before the user/group was actually created, resulting in bad or unresolved file ownership.
4. **Paragraph Structure Prompt Rule Violation**: The `AutoDraftService` did not inject instructions preventing the LLM from bolding the first sentence of paragraphs or splitting it from the paragraph block.
5. **Local File Output Naming Rule Violation**: Draft files were prefixed with `"new-draft-"` in `AutoDraftService`, violating the requirement to name the files strictly by using the topic string.
6. **Path Traversal Vulnerability in Image Tools**: The `scanImageMetadata` and `moveImages` tools in [ImageTools.java](file:///home/jsoehner/spring-ai-blog-agent/src/main/java/com/example/demo/ImageTools.java) accepted arbitrary file system paths without validation, allowing directory enumeration or writing files outside the workspace directory structure. Additionally, these tools bypassed OPA sidecar file guardrails because [OpaGuardrailAspect.java](file:///home/jsoehner/spring-ai-blog-agent/src/main/java/com/example/demo/security/OpaGuardrailAspect.java) only intercepted file-handling for `writeFile` and `readFile`.

## Decision
1. **DNS Cache Pinning**: In [WebCrawlerConfig.java](file:///home/jsoehner/spring-ai-blog-agent/src/main/java/com/example/demo/WebCrawlerConfig.java), set the JVM DNS cache TTL (`networkaddress.cache.ttl`) to 30 seconds inside a static initializer block. This ensures that the IP resolved during validation is guaranteed to be reused by Jsoup, preventing DNS Rebinding.
2. **SSRF Blocking in TLS Scanner**: Implement a robust `is_safe_host` validation in [tls_scanner.py](file:///home/jsoehner/spring-ai-blog-agent/tls_scanner.py) using Python's native `socket` and `ipaddress` APIs to resolve hostnames and block loopback/private/local IP address ranges.
3. **Correct Dockerfile Sequence**: Re-ordered [Dockerfile](file:///home/jsoehner/spring-ai-blog-agent/Dockerfile) commands to create the `spring` group and user *before* copying the build artifact jar, ensuring proper file ownership assignment.
4. **Prompt Instruction Alignment**: Added the `CRITICAL: Do NOT bold the first sentence of your paragraphs, and do NOT separate the opening sentence from the rest of the paragraph; integrate it naturally into the same paragraph block.` instruction to the system prompt of `AutoDraftService`.
5. **Standardized Local File Output Naming**: Modified `AutoDraftService` to use the raw topic string for filename base generation (`topic.replaceAll("\\s+", "-").toLowerCase() + ".html"`) rather than prepending the `"new-draft-"` string prefix.
6. **Workspace Path Hardening**: Added workspace path verification in `ImageTools.java` ensuring all directory scan and move operations verify that paths start with the normalized, absolute application root base directory path (`.`).
7. **OPA Guardrails Aspect Extension**: Expanded `OpaGuardrailAspect.java` to intercept `scanImageMetadata` and `moveImages` invocations, classifying them as `resource_type: "file"` with mapped actions and normalized paths, closing the OPA bypass gap.

## Alternatives Considered
* **Disabling HTTP Redirects**: Disabling redirects or using an external proxy. However, setting the DNS cache TTL and resolving hosts before socket connect covers the vectors natively with minimum configuration overhead.

## Consequences
* Enhanced security posture with comprehensive mitigations against DNS Rebinding, SSRF, and local path traversal.
* Seamless security logging and auditing via OPA for all filesystem-touching tools.
* Fully compliant file naming structures and LLM generation formatting adhering to the project style guide.
* Deterministic non-root user permissions in Docker container builds.
