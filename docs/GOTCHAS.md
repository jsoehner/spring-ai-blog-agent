# Gotchas & Troubleshooting

Complete reference of operational gotchas, framework quirks, and troubleshooting guidance for **Spring AI Autonomous Blog Agent**.

---

### Spring Boot 4.2 RabbitMQ Starter

In Spring Boot 4.2.0+, `org.springframework.boot:spring-boot-starter-amqp` was repurposed for generic AMQP 1.0 support. Applications using classic RabbitMQ (AMQP 0-9-1) classes such as `RabbitTemplate`, `@RabbitListener`, and `@Queue` must depend on `org.springframework.boot:spring-boot-starter-rabbitmq` in `build.gradle`.

### Docker Base Image Java Version Alignment

When updating Gradle's `toolchain { languageVersion = JavaLanguageVersion.of(...) }`, ensure the `Dockerfile` multi-stage base images (both JDK builder and JRE runtime) are updated to match (e.g., `eclipse-temurin:25-jdk-alpine` and `eclipse-temurin:25-jre-alpine`). Otherwise, container builds in CI/CD environments (like GitHub Actions `docker/build-push-action`) will fail during `./gradlew build -x test --no-daemon` because Gradle cannot locate or auto-provision the matching JDK toolchain in the container.

### Java 25 Native Support & Logging Patterns

The codebase is configured with `toolchain { languageVersion = JavaLanguageVersion.of(25) }`. To prevent JDK 25 javac internal annotation processing conflicts, standard SLF4J `LoggerFactory.getLogger(...)` and plain POJO constructs are used in place of Lombok.

### WordPress Gutenberg Block Formatting & Whitespace Normalization

When generating WordPress block HTML, ensure comment tags are well-formed (e.g., `<!-- wp:paragraph -->` with no space inside `wp:`) and individual blocks (`<!-- wp:paragraph --><p>...</p><!-- /wp:paragraph -->`) reside on single lines without internal carriage returns or line feeds. `MarkdownSanitizer` automatically cleans and normalizes these blocks.

### Gitignore Agent Artifacts

Agent execution frameworks may create `.pi` and `.pi-subagents` directories during local execution. These are ignored in `.gitignore` to prevent tracking temporary agent context and state files.

### Prompting for Paragraph Structure

When prompting an LLM to generate blog posts or articles, explicitly instruct it with: `CRITICAL: Do NOT bold the first sentence of your paragraphs, and do NOT separate the opening sentence from the rest of the paragraph; integrate it naturally into the same paragraph block.` This prevents the common LLM quirk of aggressively highlighting and isolating topic sentences.

### Class Cyclic Inheritance

Ensure service implementations do not accidentally declare `implements ServiceClass` targeting their own class name (e.g., `public class StorageService implements StorageService`), which causes compiler cyclic inheritance errors.

### Checked Exceptions in Spring Component Constructors

When reading classpath resources (e.g., `Resource.getInputStream().readAllBytes()`) in `@Service` constructors, always wrap the read in a `try-catch` block to handle checked `IOException`s rather than letting unhandled exceptions break component initialization.

### TextHumanize & WordPress Compatibility Order

When applying `texthumanize` to AI-generated blog posts, execute text naturalization on the researcher's raw facts *before* providing them to the supervisor blogger model. Applying text naturalization after HTML structure generation can alter custom HTML markup, figure tags, and WordPress shortcode comments (`<!-- wp:image -->`). Running humanization on factual context before the blogger pass preserves both naturalized text tone and valid WordPress HTML output.

### Python CLI Execution in Spring Boot Containers

When calling Python CLI utilities (like `humanize.py`) via `ProcessBuilder` inside multi-stage Docker containers, the source files (e.g., `src/main/resources/scripts/...`) are typically not present on the container's filesystem. Ensure `py3-pip` and required packages are explicitly installed in the runtime stage of your `Dockerfile`. Additionally, implement a classpath extractor to dynamically copy the script resource from the packaged JAR to a temporary filesystem path (like `/tmp/`) before execution, and gracefully handle non-zero exit codes by logging stderr and falling back to original content.

### Asynchronous Unit Tests & CompletableFuture

When wrapping RabbitMQ listener methods or controller actions in asynchronous execution (`CompletableFuture.runAsync`), the main thread will return immediately. Ensure your JUnit tests call `.join()` on the returned Future before verifying assertions (e.g., checking file creation) to avoid race conditions and false successes.

### GitHub Actions & Node 24

When a GitHub Action runner complains about Node 20 deprecation, simply injecting `setup-node` does not fix third-party actions. You **must** bump the major version of the affected actions (e.g., `actions/checkout@v7`, `peter-evans/create-pull-request@v8`) for native Node 24 support.

### Spring AI ChatModel Conflicts

Do not include both `spring-ai-starter-model-ollama` and `spring-ai-starter-model-openai` dependencies without disabling auto-configuration for one of them (e.g., `spring.autoconfigure.exclude=org.springframework.ai.model.ollama.autoconfigure.OllamaChatAutoConfiguration`). Otherwise, Spring will throw an `UnsatisfiedDependencyException` due to ambiguous `ChatModel` beans.

### Docker Volume Mapping for Logs

When mapping a single log file (like `request-activity.log`) as a volume in `docker-compose.yml`, you **must** ensure the file exists on the host machine first. If it does not exist, Docker will automatically create it as a directory, which will cause Logback to crash on startup.

### Ollama vs Open-WebUI Base URLs

- When using the Spring AI OpenAI starter pointing directly to Ollama, ensure the base URL explicitly includes `/v1`.
- When pointing to Open-WebUI, append `/api` to the base URL, or else it will hit the web frontend and result in a `405 Method Not Allowed` error.

### Spring AI ChatClient.Builder Mutability

`ChatClient.Builder` instances are mutable. Do **not** reuse the same builder instance to configure multiple clients by calling `.defaultSystem(...)`, as it will overwrite the configuration for all clients. Call `.build()` to create a base `ChatClient`, and then use `.mutate()` to branch off separate configurations.

### Docker Networking

Avoid using hostnames like `open-webui` in your configuration unless they are explicitly resolvable in the Docker network. Prefer explicit IP addresses (e.g., `192.168.100.190`).

### Spring AI 2.0.0 Naming

Starting with Spring AI 2.0.0, dependencies require the `-model-` segment (e.g., `spring-ai-starter-model-openai` instead of `spring-ai-starter-openai`).

### Search Tool Fallbacks

When implementing web search tools for AI agents (like the Researcher Agent), prefer using open APIs (like the Wikipedia API) instead of scraping search engines (e.g., DuckDuckGo) as they aggressively block automated requests. Additionally, ensure the fallback `webcrawler.default.urls` contains a solid list of generic security-related URLs.

### Local File Output Naming

When saving generated HTML blog posts locally, standardize the filename by using the topic string, replacing spaces with hyphens, and converting to lowercase (e.g., `topic.replaceAll("\\s+", "-").toLowerCase() + ".html"`).
