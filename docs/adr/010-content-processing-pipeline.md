# ADR-010: Content Processing Pipeline

## Status
Proposed

## Context
The current method for cleaning AI-generated content is a simple string manipulation in `BlogAgentService`.
```java
private String cleanHtmlContent(String content) {
    String html = content.trim();
    if (html.startsWith("```html")) {
        html = html.substring(7);
    }
    if (html.endsWith("```")) {
        html = html.substring(0, html.length() - 3);
    }
    return html.trim();
}
```
This approach is fragile:
- It doesn't handle different markdown flavors or variations in whitespace.
- It doesn't validate that the output is actually valid HTML.
- It doesn't perform any transformations (e.g., adding specific classes, SEO meta tags, or checking for required elements).
- It's hard to test in isolation.

## Decision
We will implement a formal **Content Processing Pipeline**.

The pipeline will consist of multiple stages:
1.  **Sanitization**: Remove markdown artifacts and unwanted characters.
2.  **Validation**: Ensure the output is valid HTML and contains required elements (e.g., `<h1>`, at least 5 paragraphs).
3.  **Transformation**: Inject site-specific metadata, classes, and structure.
4.  **Finalization**: Ensure the content is ready for the `WordPressTool`.

We will implement this as a series of `ContentProcessor` components.

## Implementation Plan
1.  **Define `ContentProcessor` Interface**: Define a contract for processing a string.
2.  **Implement Specific Processors**:
    - `MarkdownSanitizer`
    - `HtmlValidator`
    - `SeoMetadataInjector`
3.  **Create `ContentPipeline` Service**: Orchestrate the execution of processors in the correct order.
4.  **Refactor `BlogAgentService`**: Replace `cleanHtmlContent` with a call to the `ContentPipeline`.

## Consequences
- **Pros**:
    - **Reliability**: Standardized and testable content processing.
    - **Extensibility**: Easy to add new processing steps (e.g., translation, fact-checking).
    - **Quality**: Ensures all published content meets minimum standards.
- **Cons**:
    - **Complexity**: More components and logic.
    - **Performance**: Slightly more overhead for processing.
