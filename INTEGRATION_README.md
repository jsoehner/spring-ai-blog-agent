# Agent Security Bundle Integration

This project integrates the `agent-security-bundle` as a Git Submodule to enforce Open Policy Agent (OPA) guardrails on our Spring AI agents.

## Architecture

1. **OPA Sidecar Container**: The OPA engine runs as a separate service inside our `docker-compose.yml`. It loads the Rego policies natively and exposes an HTTP API on port `8181`.
2. **Spring AOP Enforcement**: We utilize Spring AOP (`spring-boot-starter-aop`) to wrap all methods annotated with `@Tool` (Spring AI's tool annotation). 
3. **Evaluation**: Whenever an agent attempts to invoke a tool, the AOP aspect (`OpaGuardrailAspect.java`) intercepts the call, marshals the tool name and arguments, and queries the OPA sidecar (`OpaClient.java`). If OPA returns `deny`, a `SecurityException` is thrown, blocking the tool execution and alerting the agent.
4. **Topic Filtering**: OPA is also directly invoked by the `BlogAgentController` to validate incoming blog topics (using `agent_topics.rego`) before they are placed onto the background messaging queues, rejecting inappropriate subjects.

## Components Created

*   **`docker-compose.yml`**: Added the `opa` service mounting the policies from `security-policies/opa-guardrails`.
*   **`build.gradle`**: Added `spring-boot-starter-aop`.
*   **`OpaClient.java`**: A simple Spring `@Service` utilizing `RestTemplate` to make synchronous HTTP POST requests to the OPA server's `/v1/data/agent/main` endpoint.
*   **`OpaGuardrailAspect.java`**: The core interceptor. It listens for executions of `@Tool` and maps the Java tool call to the expected OPA JSON input structure.

## Running Locally

1. Start the backend with OPA:
   ```bash
   docker-compose up -d --build
   ```
2. The OPA service will map the local `security-policies/opa-guardrails` into the container and start evaluating based on those rules.

## Extending Rules
To add new forbidden commands or block new domains, simply edit `security-policies/opa-guardrails/data/config.json`. Because the directory is volume mounted into Docker, OPA will dynamically pick up data changes on the fly (or require a restart if you add entirely new `.rego` files depending on OPA's hot-reload settings).
