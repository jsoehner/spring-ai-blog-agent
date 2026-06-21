# Changelog

## [Unreleased]
### Added
- **CI/CD**: Implemented generic GitHub Actions workflows for nightly dependency updates and security scanning (Gitleaks, Semgrep, Trivy).

### Fixed
- **Deployment**: Updated `run-and-submit.sh` to check for local changes in the `src/` directory and intelligently build a local Docker image instead of unconditionally pulling from Docker Hub.
- **Deployment**: Added health checks in `run-and-submit.sh` to determine if all expected containers are operational, skipping the Docker initialization phase to quickly submit topics directly to the queue.
- **Security**: Added `--v0-compatible` flag to the OPA container command in `docker-compose.yml` to resolve Rego v1 parsing errors caused by the latest debug image enforcing stricter syntax rules.
- **Startup**: Resolved an `UnsatisfiedDependencyException` for `OpaClient` by explicitly defining a `RestTemplate` bean in a new `RestTemplateConfig.java` class.
- **Security**: Fixed unchecked generic cast compiler warnings in `OpaClient.java` by replacing raw `Map` responses with type-safe DTO classes (`OpaResponse` and `OpaResult`).
- **Web Crawler**: Added automatic protocol prepend (`https://`) to malformed URLs in the `crawl` method to prevent `MalformedURLException` and improve crawler reliability.
- **Logging**: Suppressed noisy `MethodToolCallback` JSON conversion warnings and associated stack traces by setting the logger level to `ERROR` in `application.properties`.
- **Logging**: Disabled the console output for "Search returned no URLs, falling back to default search sites" in `WebCrawlerConfig.java`.

### Changed
- **Blogger Prompt**: Updated the `BLOGGER_PROMPT` to enforce better sentence structure, requiring a noun and verb in sentences, opening paragraphs with new topics, and avoiding excessive adjectives and adverbs to prevent run-on sentences.
- **Blogger Prompt**: Added explicit instructions on punctuation usage, encouraging the LLM to use commas for changing sentence flows and to break up long sentences for better readability.
- **Blogger Prompt**: Removed explicit newline characters (`\n`) from WordPress Gutenberg block syntax and image insertions to prevent unwanted carriage returns in the generated HTML output.
- **WordPress Tool**: Removed extra carriage returns (`\n\n`) appended after the main title generation in the final drafts.
