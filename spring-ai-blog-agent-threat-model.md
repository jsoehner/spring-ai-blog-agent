# Threat Model: spring-ai-blog-agent

## System Summary
The `spring-ai-blog-agent` is a Java-based application using Spring Boot 3.4.1. It integrates with Ollama for LLM inference, processes metadata from external URLs using Jsoup, and consumes/produces messages via AMQP. It exposes monitoring via Actuator and Prometheus.

## Scope
- **In-scope**: Web API, AMQP Consumers, AI Integration Layer, Metadata Processing, Actuator Endpoints.
- **Out-of-scope**: Build-time tooling (Gradle), local test suites, CI/CD scripts.

## Components & Entry Points
- **Web API**: HTTP endpoints for user interaction. (Entry Point: HTTP Request)
- **AMQP Consumer**: Processes messages from a queue. (Entry Point: AMQP Message)
- **AI Integration**: Communicates with Ollama via HTTP. (Trust Boundary: External AI Service)
- **Metadata Processor**: Fetches and parses external URLs via Jsoup. (Trust Boundary: External Web Content)
- **Actuator**: Provides health, metrics, and management endpoints. (Trust Boundary: Internal/External Management)

## Assets
- **Credentials**: Ollama API keys, AMQP connection strings, Actuator tokens.
- **Data**: User queries, processed metadata, AI conversation history.
- **Integrity**: Model prompts, system configurations, application logic.
- **Availability**: AI inference availability, AMQP message processing, web API uptime.

## Trust Boundaries
- **Internet -> Web API**: Unauthenticated/Authenticated HTTP requests.
- **Internet -> Metadata Processor**: Arbitrary URL content fetched by Jsoup.
- **Application -> Ollama**: Outbound HTTP requests for LLM inference.
- **Message Queue -> AMQP Consumer**: Messages from potentially untrusted sources.
- **Internal -> Actuator**: Management endpoints.

## Attacker Capabilities
- **External Attacker**: Can send HTTP requests, send AMQP messages (if queue is exposed), and provide URLs for parsing.
- **Malicious User**: Authenticated user can interact with the Web API and AI features.
- **Compromised Internal System**: Can access Actuator endpoints or AMQP queue.

## Threats & Abuse Paths
- **Cross-user Data Leakage**: Attackers exploit `ChatMemory` to access other users' conversation history.
- **Remote Code Execution (RCE)**: Malicious payloads in AMQP messages or HTTP requests leading to command execution.
- **SSRF**: Forcing the Metadata Processor to fetch internal resources via Jsoup.
- **Denial of Service (DoS)**: Flooding the AI inference layer or AMQP queue.
- **Actuator Bypass**: Accessing sensitive management endpoints without proper authentication.

## Prioritization
- **Critical**: RCE, Cross-user data leakage, Actuator bypass.
- **High**: SSRF, DoS of AI inference.
- **Medium**: Metadata tampering, minor info leaks.

## Assumptions
- Ollama is running on a trusted internal network or is properly isolated.
- AMQP queue is not exposed to the public internet.
- Actuator endpoints are protected by Spring Security.
- Jsoup is used to parse content that is not expected to contain executable code in the parser.

## Mitigations & Focus Paths
- **AuthZ**: Enforce strict Spring Security on all Actuator and Web API endpoints.
- **Input Validation**: Sanitize all AMQP messages and HTTP request bodies.
- **SSRF Protection**: Implement an allowlist for URLs processed by Jsoup.
- **Isolation**: Run the application in a container with limited network access.
- **Rate Limiting**: Implement rate limits on AI inference calls and Web API endpoints.
