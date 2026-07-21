# Changelog

## [Unreleased]
### Added
- **CI/CD**: Implemented a custom `update-dependencies.py` Python script that dynamically queries Maven Central metadata and automatically updates version definitions in `build.gradle` inside the nightly workflow.

### Fixed
- **Testing**: Fixed JUnit test assertion in `TlsScannerToolTest.java` to accept `Unsafe Host` alongside `Failed` for invalid domains to align with new SSRF-prevention logic, resolving nightly workflow failures.

### Changed
- **CI/CD**: Parallelized the security scanning workflow jobs (Gitleaks, Semgrep, Trivy) to run concurrently on separate runner instances and upload reports as build artifacts for downstream reporting.
- **Architecture**: Optimized the Supervisor Agent `processSupervisorTask` to execute asynchronously using `CompletableFuture.runAsync()`, immediately freeing up RabbitMQ listener threads to increase system throughput.
- **CI/CD**: Implemented generic GitHub Actions workflows for nightly dependency updates and security scanning (Gitleaks, Semgrep, Trivy).

### Fixed
- **Security**: Hardened [OpaGuardrailAspect.java](file:///Users/jsoehner/spring-ai-blog-agent/src/main/java/com/example/demo/security/OpaGuardrailAspect.java) against path traversal attacks by normalizing target file paths before OPA policy evaluation.
- **Security**: Sanitized blog post titles and restricted output file resolution in [WordPressTool.java](file:///Users/jsoehner/spring-ai-blog-agent/src/main/java/com/example/demo/WordPressTool.java) to prevent writing files outside of the output directory.
- **Security**: Added target option termination (`--`) in [TlsScannerTool.java](file:///Users/jsoehner/spring-ai-blog-agent/src/main/java/com/example/demo/TlsScannerTool.java) to prevent command-line option/argument injection.
- **Security**: Implemented DNS and IP address range checks in [WebCrawlerConfig.java](file:///Users/jsoehner/spring-ai-blog-agent/src/main/java/com/example/demo/WebCrawlerConfig.java) to block loopback and private/local network crawling (SSRF prevention).
- **Security**: Added input format validation to ticker parameters in [StockPriceTool.java](file:///Users/jsoehner/spring-ai-blog-agent/src/main/java/com/example/demo/StockPriceTool.java).
- **Security**: Sanitized topic names in [AutoDraftService.java](file:///Users/jsoehner/spring-ai-blog-agent/src/main/java/com/example/demo/AutoDraftService.java) to prevent command injection during automatic pull request generation.
- **Security**: Refined path normalization in `OpaGuardrailAspect.java` to fail-closed by throwing a `SecurityException` if a path cannot be normalized, preventing bypasses of the OPA guardrail.
- **Security**: Enforced strict length limits on sanitized topics in `AutoDraftService.java` to prevent potential command-line argument overflow or buffer issues in downstream shell scripts.
- **Architecture**: Fixed `ChatClient.Builder` pollution across agent controllers/services by using builder mutation via `.mutate()`.
- **Architecture**: Refactored [ChatController.java](file:///Users/jsoehner/spring-ai-blog-agent/src/main/java/com/example/demo/ChatController.java) to use Spring dependency injection for tools, preventing `NullPointerException`s from unconfigured `@Value` fields.
- **Deployment**: Updated `run-and-submit.sh` to check for local changes in the `src/` directory and intelligently build a local Docker image instead of unconditionally pulling from Docker Hub.
- **Deployment**: Added health checks in `run-and-submit.sh` to determine if all expected containers are operational, skipping the Docker initialization phase to quickly submit topics directly to the queue.
- **Security**: Added `--v0-compatible` flag to the OPA container command in `docker-compose.yml` to resolve Rego v1 parsing errors caused by the latest debug image enforcing stricter syntax rules.
- **Startup**: Resolved an `UnsatisfiedDependencyException` for `OpaClient` by explicitly defining a `RestTemplate` bean in a new `RestTemplateConfig.java` class.
- **Security**: Fixed unchecked generic cast compiler warnings in `OpaClient.java` by replacing raw `Map` responses with type-safe DTO classes (`OpaResponse` and `OpaResult`).
- **Web Crawler**: Added automatic protocol prepend (`https://`) to malformed URLs in the `crawl` method to prevent `MalformedURLException` and improve crawler reliability.
- **Logging**: Suppressed noisy `MethodToolCallback` JSON conversion warnings and associated stack traces by setting the logger level to `ERROR` in `application.properties`.
- **Logging**: Disabled the console output for "Search returned no URLs, falling back to default search sites" in `WebCrawlerConfig.java`.
- **Output Files**: Updated `WordPressTool.java` to automatically save generated HTML blog drafts to the `output/` directory so they are visible on the local host machine when using Docker volumes.

### Changed
- **Researcher Agent**: Upgraded the fact-gathering pass to use structured JSON output via `BeanOutputConverter`. Included an automated retry mechanism to self-correct schema validation and parsing errors.
- **Blogger Prompt**: Updated the `BLOGGER_PROMPT` to enforce better sentence structure, requiring a noun and verb in sentences, opening paragraphs with new topics, and avoiding excessive adjectives and adverbs to prevent run-on sentences.
- **Blogger Prompt**: Added explicit instructions on punctuation usage, encouraging the LLM to use commas for changing sentence flows and to break up long sentences for better readability.
- **Blogger Prompt**: Removed explicit newline characters (`\n`) from WordPress Gutenberg block syntax and image insertions to prevent unwanted carriage returns in the generated HTML output.
- **WordPress Tool**: Removed extra carriage returns (`\n\n`) appended after the main title generation in the final drafts.
