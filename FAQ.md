# Frequently Asked Questions (FAQ) & Gotchas

This document contains a list of common issues, "gotchas", and lessons learned during the development of the Spring AI Blog Agent. If you are experiencing weird behavior or build failures, check here first!

### 1. Why is my Researcher Agent getting the Blogger Agent's system prompt?
**Symptom:** The logs show that your first AI call (e.g., `factGathererClient`) is using the system instructions meant for the second AI call (e.g., `bloggerClient`).

**Cause:** In Spring AI, the `ChatClient.Builder` is **mutable**. If you use the same builder instance to construct multiple clients and call `.defaultSystem(...)`, it will overwrite the system prompt for all clients built from that builder.

**Solution:** Build a base `ChatClient` first, and then use the `.mutate()` method to branch off separate configurations:
```java
ChatClient baseClient = chatClientBuilder.build();

this.clientOne = baseClient.mutate()
        .defaultSystem("Prompt 1")
        .build();

this.clientTwo = baseClient.mutate()
        .defaultSystem("Prompt 2")
        .build();
```

### 2. Spring AI is throwing a `405 Method Not Allowed` when hitting my local LLM
**Symptom:** You receive an exception like `405: {"detail":"Method Not Allowed"}`.

**Cause:** You are likely pointing Spring AI's `spring.ai.openai.base-url` at an Open-WebUI instance without appending the API path. Spring AI automatically appends `/v1/chat/completions` to your base URL. If your base URL is `http://192.168.100.190:8080`, Spring AI hits `http://192.168.100.190:8080/v1/chat/completions`, which is an Open-WebUI frontend HTML route, not an API route. 

**Solution:** Ensure your Open-WebUI base URL ends with `/api`.
*Correct:* `spring.ai.openai.base-url=http://192.168.100.190:8080/api`

### 3. Application fails to start with `UnsatisfiedDependencyException` involving `ChatModel`
**Symptom:** Spring Boot crashes on startup complaining about `expected single matching bean but found 2: ollamaChatModel, openAiChatModel`.

**Cause:** You have both `spring-ai-starter-model-ollama` and `spring-ai-starter-model-openai` on your classpath. Spring tries to auto-configure both chat models, leaving the `ChatClient.Builder` confused about which one to inject.

**Solution:** Exclude the auto-configuration for the model you aren't using in your `application.properties`. For example, to use OpenAI and disable Ollama Chat auto-configuration:
```properties
spring.autoconfigure.exclude=org.springframework.ai.model.ollama.autoconfigure.OllamaChatAutoConfiguration
```

### 4. Gradle build fails with `Could not find org.springframework.ai:spring-ai-starter-openai:`
**Symptom:** Gradle cannot resolve the OpenAI starter dependency when compiling the project.

**Cause:** In Spring AI version `2.0.0` (and later), the naming convention for starter dependencies was changed to include `-model-`.

**Solution:** Update your `build.gradle`:
*Old:* `implementation 'org.springframework.ai:spring-ai-starter-openai'`
*New:* `implementation 'org.springframework.ai:spring-ai-starter-model-openai'`

### 5. `java.net.UnknownHostException: open-webui` in Docker
**Symptom:** The AI agent throws an `UnknownHostException` when attempting to reach the local LLM.

**Cause:** You set the base URL to use a hostname (like `open-webui`) that is not resolvable within the Docker network where the agent is running.

**Solution:** Use the explicit IP address of the host machine running the LLM (e.g., `http://192.168.100.190:8080/api`).

### 6. Spring AI throws `404: 404` NotFoundException when pointing to Ollama's OpenAI API
**Symptom:** You attempt to use Ollama's OpenAI-compatible endpoint by setting `spring.ai.openai.base-url=http://192.168.100.190:11434` and get a `404 Not Found` error.

**Cause:** Starting with version 2.0.0, Spring AI utilizes the official OpenAI Java Client. By default, the official client expects the base URL to include the `/v1` path segment. If you omit it, the client appends `/chat/completions` directly to port 11434, which is not a valid endpoint on Ollama.

**Solution:** Append `/v1` to the end of your Ollama base URL.
*Correct:* `spring.ai.openai.base-url=http://192.168.100.190:11434/v1`

### 7. Files saved by the agent in Docker aren't appearing locally
**Symptom:** The agent successfully generates a file (like a blog draft) and saves it to `/app/blog.html`, but you don't see it on your local machine.

**Cause:** The root `/app/` directory inside the Docker container is not mounted to your local host. Only specific subdirectories (like `/app/config/` and `/app/output/`) are mapped via volumes in `docker-compose.yml`.

**Solution:** Instruct the agent or update your code to save output files to `/app/output/` instead of `/app/`. They will immediately appear in the `./output/` directory on your local machine.

### 8. `run-and-submit.sh` times out waiting for the Supervisor API (404 Not Found)
**Symptom:** When running `./run-and-submit.sh "<topic>"`, the script hangs on `⏳ Waiting for Supervisor Agent API to become available...` and eventually fails. Checking the logs reveals a `404 Not Found` for the `/actuator/health` endpoint.

**Cause:** By default, the script pulls a pre-built Docker image from `ghcr.io` which may be outdated and missing the proper Actuator endpoint configurations. However, your local codebase might have these correctly configured in `build.gradle` and `application.properties`.

**Solution:** Force the script to build the image from your local source code instead of pulling the remote image by appending the `--build` flag:
```bash
./run-and-submit.sh --build "Your topic here"
```

### 9. Application crashes on startup with `java.io.FileNotFoundException: request-activity.log (Is a directory)`
**Symptom:** When running `docker-compose up`, the Supervisor Agent immediately exits. The logs show Logback failing to open `request-activity.log` because it "Is a directory".

**Cause:** You mapped `request-activity.log` as a file volume in `docker-compose.yml`, but the file did not exist on your host machine before running Docker Compose. Docker assumes missing host paths are directories and automatically creates an empty folder in its place.

**Solution:** Remove the directory and create an empty file:
```bash
rm -rf request-activity.log
touch request-activity.log
```

### 10. Spring AI ChatModel beans ambiguity in test context
**Symptom:** Running local tests (e.g. `./gradlew test`) fails with `NoUniqueBeanDefinitionException` during Spring application context loading.

**Cause:** The project includes both `spring-ai-starter-model-ollama` and `spring-ai-starter-model-openai` starters. When tests run, Spring tries to auto-configure both chat models, leaving the container unable to uniquely resolve a single `ChatModel` bean.

**Solution:** Provide a dedicated test configuration file `src/test/resources/application.properties` that disables the unused chat model auto-configuration (e.g., `spring.autoconfigure.exclude=org.springframework.ai.model.ollama.autoconfigure.OllamaChatAutoConfiguration`) and sets dummy API keys.

### 11. OPA path authorization failing on tool objects (e.g., WriteRequest)
**Symptom:** OPA policies fail to authorize file writes, throwing a `SecurityException`, even when writing to an allowed path (like `/tmp`).

**Cause:** In `OpaGuardrailAspect.java`, path extraction for `writeFile` was using `args[0].toString()`. Since the first parameter is a `WriteRequest` record, this evaluated to `"WriteRequest[absolutePath=/tmp/..., content=...]"` which failed simple prefix checks in `agent_files.rego`.

**Solution:** Safely typecast the argument to `WriteRequest` inside the aspect and extract the raw path field (`writeRequest.absolutePath()`).

### 12. AutoDraftService Git PR creation failing with untracked files
**Symptom:** The automated PR creation process in `AutoDraftService.java` fails with a git error during branch staging.

**Cause:** The service was adding files to git from the root directory, while `WordPressTool.java` actually saved them under `output/`. Furthermore, `output/` is ignored by `.gitignore`, causing git to ignore these files by default.

**Solution:** Correct the path mapping in the git command to use the `output/` directory, and add the force flag (`git add -f`) to ensure the ignored files are staged correctly.
