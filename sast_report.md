# SAST Vulnerability Scan Report

## Summary
Static analysis was performed on the codebase to identify common security vulnerabilities.

## Findings & Remediation Status

### 1. Potential Prompt Injection (Medium) — RESOLVED
- **Location**: `AgentOrchestrator.java`
- **Description**: User-provided facts were previously concatenated into the prompt without strict boundaries.
- **Risk**: A user could provide input that instructs the LLM to ignore previous instructions or perform unauthorized actions.
- **Remediation Implemented**: Wrapped user-provided facts inside `<untrusted_user_input>` delimiters and established strict system prompt guardrails instructing the model to treat all enclosed content strictly as raw reference data, never as executable instructions or overrides.

### 2. Weak HTML Sanitization / XSS (Low) — RESOLVED
- **Location**: `MarkdownSanitizer.java`
- **Description**: The sanitizer previously only stripped `<script>` and `<iframe>`.
- **Risk**: Injections involving other executable or vector elements could lead to XSS.
- **Remediation Implemented**: Hardened `MarkdownSanitizer.java` to comprehensively strip:
  - Dangerous elements: `<object>`, `<embed>`, `<applet>`, `<style>`, and `<svg>`.
  - Inline DOM event handlers: `onload=`, `onerror=`, `onclick=`, `on\w+\s*=`.
  - `javascript:` and `vbscript:` URIs in `href` and `src` attributes.
  - Comprehensive unit test suite added in `MarkdownSanitizerTest.java` verifying all vectors.

### 3. Actuator Exposure — RESOLVED
- **Location**: `src/main/resources/application.properties` and `application.properties.template`
- **Description**: Management endpoints were not explicitly restricted.
- **Remediation Implemented**: Enforced Actuator web exposure (`management.endpoints.web.exposure.include`) to strictly allow `health` only across application configurations.

### 4. Supply Chain Security (GitHub Actions Pinning) — RESOLVED
- **Location**: `.github/workflows/sbom-cbom.yml`
- **Remediation Implemented**: Pinned all actions (`actions/checkout`, `actions/setup-node`) to immutable commit SHAs.

### 5. Lack of Rate Limiting (Low) — TRACKED
- **Location**: `AgentOrchestrator.java`
- **Description**: No explicit rate limiting is enforced on the `handleSupervisorTask` entry point.
- **Remediation**: Tracked in backlog for deployment ingress/gateway rate limiter.

### 6. SSRF Domain Allowlist Suffix & HTTP Redirect Bypass (High) — RESOLVED
- **Location**: `WebCrawlerConfig.java`
- **Description**: `ALLOWED_DOMAINS.stream().anyMatch(host::endsWith)` allowed domains sharing a suffix (e.g., `evilowasp.org`), `Jsoup.connect(...)` followed HTTP redirects by default, and `isSafeUrl` omitted `isAnyLocalAddress()`.
- **Remediation Implemented**: Enforced exact/subdomain boundary matching (`normalizedHost.equals(domain) || normalizedHost.endsWith("." + domain)`), disabled redirects (`.followRedirects(false)`), and added `isAnyLocalAddress()` check. Verified in `WebCrawlerConfigTest.java`.

### 7. Path Traversal Prefix Bypass & Unchecked `moveImages` Target (High) — RESOLVED
- **Location**: `OpaGuardrailAspect.java`
- **Description**: `normalizedPath.startsWith(workspacePath)` used `String` prefix comparison (allowing sibling directory prefix bypasses such as `/app/workspace-evil`), and `moveImages` only validated `sourceDirectory()` without checking `targetDirectory()`.
- **Remediation Implemented**: Switched to `Path.startsWith(Path)` segment-aware comparison and validated both `sourceDirectory()` and `targetDirectory()` for `moveImages`. Verified in `OpaGuardrailAspectTest.java`.

### 8. Stacktrace, Debug Logging & Actuator `info` Exposure (Medium) — RESOLVED
- **Location**: `src/main/resources/application.properties`, `application.properties.template`
- **Description**: `server.error.include-stacktrace=always`, `logging.level.org.springframework.ai=DEBUG`, and `management.endpoints.web.exposure.include=health,info` exposed internal diagnostics.
- **Remediation Implemented**: Set `server.error.include-stacktrace=never`, `logging.level.org.springframework.ai=INFO`, and restricted Actuator web exposure to `health` only.

### 9. Null / Symbol-Only Topic Filename Edge Case & Timeout/Tempfile Hardening (Low) — RESOLVED
- **Location**: `StorageService.java`, `WordPressTool.java`, `TextHumanizerProcessor.java`, `RestTemplateConfig.java`
- **Description**: Symbol-only topics produced hidden `.html` filenames; `TextHumanizerProcessor` created world-readable temp scripts; `RestTemplate` lacked explicit connect/read timeouts.
- **Remediation Implemented**: Added alphanumeric topic validation in `StorageService`/`WordPressTool` (`StorageServiceTest.java`), restricted temp script permissions to owner-only, and configured 5s connect / 120s read timeouts on `RestTemplate`.

## Conclusion
All active SAST findings, SSRF allowlist/redirect bypasses, path traversal boundary checks, AI prompt boundaries, HTML sanitization vectors, actuator/stacktrace exposures, and supply-chain action pinnings have been repaired and verified with passing unit tests.

