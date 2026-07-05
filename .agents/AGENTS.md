## Spring AI Best Practices & Gotchas

* **ChatClient.Builder Mutability:** Spring AI `ChatClient.Builder` instances are mutable. Do NOT reuse the same builder instance to configure multiple clients by calling `.defaultSystem(...)`, as it will overwrite the configuration for all clients. Instead, call `.build()` to create a base `ChatClient`, and then use `.mutate()` to branch off separate configurations.
* **Open-WebUI Base URL:** When pointing Spring AI to Open-WebUI, append `/api` to the base URL (e.g., `http://<ip>:8080/api`). Spring AI automatically appends `/v1/chat/completions`, and omitting `/api` will result in a `405 Method Not Allowed` error because it hits the web frontend.
* **Ollama Base URL:** When using the Spring AI OpenAI starter to point directly to Ollama, ensure the base URL explicitly includes `/v1` (e.g., `http://<ip>:11434/v1`). Failing to include `/v1` results in a `404 Not Found` error.
* **ChatModel Conflicts:** Do not include both `spring-ai-starter-model-ollama` and `spring-ai-starter-model-openai` dependencies without disabling auto-configuration for one of them (e.g. `spring.autoconfigure.exclude=org.springframework.ai.model.ollama.autoconfigure.OllamaChatAutoConfiguration`). Otherwise, Spring will throw an `UnsatisfiedDependencyException` due to ambiguous `ChatModel` beans.
* **Spring AI 2.0.0 Naming:** Starting with Spring AI 2.0.0, dependencies require the `-model-` segment (e.g., `spring-ai-starter-model-openai` instead of `spring-ai-starter-openai`).
* **Docker Networking:** Avoid using hostnames like `open-webui` in your configuration unless they are explicitly resolvable in the Docker network. Prefer explicit IP addresses (e.g., `192.168.100.190`).

## Blog Agent Project Specifics & Gotchas

* **Search Tool Fallbacks:** When implementing web search tools for AI agents (like the Researcher Agent), prefer using open APIs (like the Wikipedia API) instead of scraping search engines (e.g., DuckDuckGo) as they aggressively block automated requests. Additionally, ensure the fallback `webcrawler.default.urls` contains a solid list of generic security-related URLs (like The Hacker News, CISA, OWASP, etc.). This ensures the agent has relevant general security material to fall back on if specific topics cannot be dynamically searched.
* **Prompting for Paragraph Structure:** When prompting an LLM to generate blog posts or articles, explicitly instruct it with: `CRITICAL: Do NOT bold the first sentence of your paragraphs, and do NOT separate the opening sentence from the rest of the paragraph; integrate it naturally into the same paragraph block.` This prevents the common LLM quirk of aggressively highlighting and isolating topic sentences.
* **Local Application Logging:** To maintain an ephemeral local activity log, configure a `logback-spring.xml` file with a `FileAppender` pointing to `request-activity.log` and set `<append>false</append>` so the log is freshly overwritten on each application restart rather than appending infinitely.
  **Gotcha:** When mapping this log file (or any single file) as a volume in `docker-compose.yml`, you **must** ensure the file exists on the host machine first (e.g. `touch request-activity.log`). If it does not exist, Docker will automatically create it as a directory, which will cause Logback to crash on application startup.
* **Local File Output Naming:** When saving generated artifacts (like blog posts) locally, standardize the filename by using the topic string, replacing spaces with hyphens, and converting to lowercase (e.g., `topic.replaceAll("\\s+", "-").toLowerCase() + ".html"`).

## Agent Skills

* **docker-expert:** Equipped to optimize Dockerfiles, perform multi-stage builds, harden container security, and analyze runtime execution issues. It utilizes local tools (Glob, Grep, Read) for static analysis and validates configurations using `docker-compose config`.

