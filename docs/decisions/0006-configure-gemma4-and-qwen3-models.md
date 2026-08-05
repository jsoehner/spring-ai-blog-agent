# ADR-0006: Configure Gemma 4 and Qwen 3 Models for Agents

## Status
Accepted

## Date
2026-08-05

## Context
Across different agent definitions, there was a need to specify distinct models based on reasoning requirements:
1. **Researcher Agent** needs a high-reasoning model (`gemma4:12b`) to conduct deep fact-gathering passes, parse complex HTML, synthesize research, and structure final HTML documents without formatting errors.
2. **Supervisor Agent** acts as a coordination gateway and message listener. Since it requires less cognitive reasoning and needs to boot/respond quickly, it is best suited for a smaller model (`qwen3:4b`).
3. Configuring explicit options mapping ensures Spring AI client builder resolutions match these selected Ollama targets exactly across environments.

## Decision
1. **Model Assignment**:
   - Set the Researcher Agent model to `gemma4:12b` (high reasoning).
   - Set the Supervisor Agent model to `qwen3:4b` (low reasoning/coordination).
2. **Configuration Updates**:
   - Applied these assignments to `spring.ai.ollama.chat.model` and `spring.ai.ollama.chat.options.model` in both `config/` properties and `src/main/resources/` profiles.
   - Configured `SPRING_AI_OLLAMA_CHAT_OPTIONS_MODEL` to match for both agent services in `docker-compose.yml`.

## Alternatives Considered
* **Single Model Standardization (e.g., Llama 3.2)**: While standardizing on a single middle-tier model simplifies configuration, it under-utilizes the coordination layer's low footprint potential and limits the Researcher Agent's capabilities for high reasoning tasks.

## Consequences
* Maximized reasoning accuracy on research tasks with `gemma4:12b`.
* Minimized coordination latency and resource footprint on supervisor routing with `qwen3:4b`.
