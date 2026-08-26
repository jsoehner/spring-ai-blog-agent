# 0012. Java 25 Upgrade and Gutenberg Block Formatting Normalization

* Status: Accepted
* Deciders: Development Team
* Date: 2026-08-26

## Technical Story
* Issue: The project needed to be upgraded to support Java 25. Additionally, the generated blog output contained extra spaces, line feeds, carriage returns, and malformed Gutenberg comment block tags (e.g., `<!-- wp: paragraph -->` and multiline spacing) caused in part by text humanization running on HTML and raw LLM spacing quirks.

## Context and Problem Statement
When running on Java 25, annotation processor incompatibilities with Lombok caused build failures (`NoSuchFieldException: TypeTag.UNKNOWN`). Migrating to standard SLF4J loggers and plain Java POJO structures eliminated Lombok dependencies and unlocked native Java 25 builds.
In the blog generation pipeline, post-generation HTML content was previously subjected to text humanization which corrupted HTML markup with extra filler words, line breaks, and split Gutenberg block comments. Furthermore, the markdown sanitizer needed robust normalization to ensure Gutenberg blocks are tightly formatted without inner linefeeds, spaces in tag names, or extraneous carriage returns.

## Decision Drivers
* Java Modernization: Support Java 25 toolchains natively across compilation, testing, and runtime.
* Gutenberg Formatting Integrity: Ensure clean, valid, single-line Gutenberg blocks without extra line breaks, carriage returns, or invalid comment spacing.
* Pipeline Correctness: Adhere strictly to the pre-supervisor pass humanization rule, isolating `texthumanize` to raw research facts.

## Decision Outcome
* Upgraded Gradle Java toolchain to Java 25 (`JavaLanguageVersion.of(25)`).
* Replaced Lombok annotations across the codebase with standard SLF4J loggers and standard Java POJOs, removing the `io.freefair.lombok` Gradle plugin.
* Removed `TextHumanizerProcessor` from `ContentPipeline` so text naturalization runs exclusively before the supervisor blogger pass.
* Enhanced `MarkdownSanitizer` to normalize tag spacing (`<!-- wp:paragraph -->`), collapse internal linebreaks/carriage returns, and remove excess blank lines.
* Updated `blogger-prompt.txt` with strict Gutenberg single-line formatting constraints.

### Positive Consequences
* The project builds and tests cleanly under OpenJDK 25.
* Generated WordPress blog drafts maintain pristine block formatting without stray line feeds or corrupted comment tags.

### Negative Consequences
* None.
