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
