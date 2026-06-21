# Changelog

## [Unreleased]
### Added
- **CI/CD**: Implemented generic GitHub Actions workflows for nightly dependency updates and security scanning (Gitleaks, Semgrep, Trivy).

### Fixed
- **Security**: Fixed unchecked generic cast compiler warnings in `OpaClient.java` by replacing raw `Map` responses with type-safe DTO classes (`OpaResponse` and `OpaResult`).
- **Web Crawler**: Added automatic protocol prepend (`https://`) to malformed URLs in the `crawl` method to prevent `MalformedURLException` and improve crawler reliability.
- **Logging**: Suppressed noisy `MethodToolCallback` JSON conversion warnings and associated stack traces by setting the logger level to `ERROR` in `application.properties`.
- **Logging**: Disabled the console output for "Search returned no URLs, falling back to default search sites" in `WebCrawlerConfig.java`.

### Changed
- **Blogger Prompt**: Updated the `BLOGGER_PROMPT` to enforce better sentence structure, requiring a noun and verb in sentences, opening paragraphs with new topics, and avoiding excessive adjectives and adverbs to prevent run-on sentences.
- **Blogger Prompt**: Added explicit instructions on punctuation usage, encouraging the LLM to use commas for changing sentence flows and to break up long sentences for better readability.
- **Blogger Prompt**: Removed explicit newline characters (`\n`) from WordPress Gutenberg block syntax and image insertions to prevent unwanted carriage returns in the generated HTML output.
- **WordPress Tool**: Removed extra carriage returns (`\n\n`) appended after the main title generation in the final drafts.
