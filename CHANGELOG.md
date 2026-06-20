# Changelog

## [Unreleased]
### Fixed
- **Web Crawler**: Added automatic protocol prepend (`https://`) to malformed URLs in the `crawl` method to prevent `MalformedURLException` and improve crawler reliability.
- **Logging**: Suppressed noisy `MethodToolCallback` JSON conversion warnings and associated stack traces by setting the logger level to `ERROR` in `application.properties`.
- **Logging**: Disabled the console output for "Search returned no URLs, falling back to default search sites" in `WebCrawlerConfig.java`.

### Changed
- **Blogger Prompt**: Updated the `BLOGGER_PROMPT` to enforce better sentence structure, requiring a noun and verb in sentences, opening paragraphs with new topics, and avoiding excessive adjectives and adverbs to prevent run-on sentences.
