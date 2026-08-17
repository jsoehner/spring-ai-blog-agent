# 9. Integrating TextHumanize for AI Blog Post Naturalization

* Status: accepted
* Deciders: Blog Agent Core Team
* Date: 2026-08-17

## Context and Problem Statement
LLM-generated blog drafts often exhibit identifiable AI markers, overly complex phrasing, and robotic tone patterns. To produce human-like blog posts, the project needed a text naturalization capability (`texthumanize` Python package) incorporated into the multi-agent generation pipeline without breaking existing HTML formatting or WordPress upload compatibility.

## Decision Drivers
* Text Naturalization Quality: Reduce AI-like patterns and markers in generated content.
* Structural Integrity & WordPress Compatibility: Ensure HTML structure, headings, figure tags, and image embeds remain intact.
* Execution Architecture: Integrate a Python package into a Spring Boot Java codebase cleanly.

## Considered Options
1. **Sidecar REST Microservice**: Run a separate Python FastAPI service for `texthumanize`.
2. **Local CLI Process Execution**: Wrap `texthumanize` in a Python CLI script executed via Java `ProcessBuilder` inside the Spring Boot container.
3. **LLM-Only Prompt Engineering**: Rely solely on system prompt instructions to naturalize text.

## Decision Outcome
Chosen option: **Option 2 (Local CLI Process Execution)**.
A Python wrapper script `src/main/resources/scripts/humanize.py` was created and integrated via `TextHumanizerProcessor` into the pipeline.

Specifically:
- **Pre-Supervisor Pass Humanization**: The researcher's compiled facts are humanized via `TextHumanizerProcessor` *before* being provided to the supervisor blogger agent (`AgentOrchestrator.java`). This guarantees that the supervisor works with naturalized factual context while maintaining control over the final WordPress HTML structure.
- **Content Pipeline Integration**: `TextHumanizerProcessor` was also registered in `ContentPipeline.java` as part of the post-processing chain.
- **Dynamic Classpath Script Resolution**: In containerized environments where source files are packaged inside a JAR, the processor automatically resolves and extracts the script resource (`scripts/humanize.py`) from the classpath to a temporary file before execution. This eliminates dependencies on external filesystem directories inside multi-stage Docker builds.

### Positive Consequences
* Factual context and blog text are naturalized before WordPress assembly.
* Zero external service dependency (runs directly inside the container environment).
* Fully configurable via Spring environment properties (`humanizer.enabled`, `humanizer.python.path`, `humanizer.script.path`).

### Negative Consequences
* Container image footprint increased slightly due to `py3-pip` and `texthumanize` Python packages.
* Minor execution overhead when calling out to `python3` via `ProcessBuilder`.
