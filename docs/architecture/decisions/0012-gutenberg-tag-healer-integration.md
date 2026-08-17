# 0012. Gutenberg Tag Healer Integration for Valid WordPress HTML Structure

* Status: Accepted
* Deciders: Development Team
* Date: 2026-08-17

## Technical Story
* Issue: The LLM blogger agent occasionally generates WordPress Gutenberg blocks that lack correct wrapping tags (such as missing `<p>...</p>` inside `<!-- wp:paragraph -->` or missing `<h2>...</h2>` inside `<!-- wp:heading -->`), causing rendering errors in WordPress.

## Context and Problem Statement
To ensure post drafts import and render correctly in WordPress, we use Gutenberg block syntax comments. While prompt instructions require the LLM to wrap content inside these comments with standard HTML paragraph and heading tags, local LLMs sometimes fail to do so consistently, producing malformed blocks.
We need a robust post-processing healer in the blog content pipeline to automatically wrap the block contents in their appropriate HTML tags if they are missing, without duplicating existing tags.

## Decision Drivers
* Document Integrity: Guarantee that all WordPress Gutenberg blocks contain valid, well-formed HTML tags.
* Error Resilience: Automatically fix formatting errors from LLM output instead of failing the pipeline.
* Pipeline Order: Place this processor after the blogger pass in the content pipeline to sanitize final HTML outputs, while keeping TextHumanize execution *before* the blogger pass (as defined in ADR-0009).

## Decision Outcome
Chosen Option: Implement `GutenbergTagHealer` and register it as a `ContentProcessor` in the post-processing `ContentPipeline`.

### Implementation Details:
- **GutenbergTagHealer.java**: A new content processor that uses regular expressions to scan for `<!-- wp:paragraph -->` and `<!-- wp:heading -->` blocks. It checks the inner content and adds `<p>...</p>` or `<h2>...</h2>` (or appropriate header tag) if missing, while preserving existing tags.
- **GutenbergTagHealerTest.java**: Comprehensive unit tests validating healing of missing paragraph/heading tags, preservation of correct tags, and processing of mixed content blocks.
- **ContentPipeline.java**: Replaced the incorrect post-processing registration of `TextHumanizerProcessor` with `GutenbergTagHealer`. `TextHumanizerProcessor` is instead kept in the pre-supervisor facts humanization pass (as orchestrated in `AgentOrchestrator.java`).
- **Dockerfile**: Added `RUN git config --global --add safe.directory /app` to ensure git operations within the container are allowed.

### Positive Consequences
* Ensures all generated drafts have correct and valid Gutenberg syntax.
* Decouples raw fact naturalization (pre-supervisor) from final HTML block healing (post-processing).
* Prevents WordPress block rendering errors due to missing wrapping tags.

### Negative Consequences
* None.
