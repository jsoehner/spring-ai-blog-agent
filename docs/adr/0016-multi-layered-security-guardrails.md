# ADR 0001: Multi-Layered Security Guardrails for AI Tool Execution

* Status: accepted
* Deciders: jsoehner
* Date: 2024-05-23

## Technical Story
* N/A

## Context and Problem Statement
The `spring-ai-blog-agent` allows an AI agent to execute various tools, including filesystem access, web crawling, and image processing. Without proper controls, an agent could be manipulated to read sensitive system files, write malicious scripts, or perform unauthorized actions. We need a robust, multi-layered security architecture to ensure that tool execution remains within the bounds of the project workspace and follows predefined safety policies.

## Decision Drivers
* Security: Prevent path traversal and unauthorized command execution.
* Observability: Provide a clear audit trail of tool requests and their authorization status.
* Reliability: Ensure the security layer fails closed (denies access) if the security server or logic fails.

## Considered Options
* Option 1: Simple local validation (check file paths in Java code).
* Option 2: Centralized Policy Engine (OPA) for all tool decisions.
* Option 3: Combined Approach (OPA for policy + Java Aspect for path normalization and input flattening).

## Decision Outcome
Chosen option: Option 3, because it provides the best balance of fine-grained policy control (OPA), robust path protection (AspectJ), and protection against type confusion (Argument Flattening).

### Positive Consequences
* **Zero Trust**: Every tool call is verified against a centralized policy.
* **Path Integrity**: Prevents path traversal by normalizing and checking all file paths against a workspace root.
* **Type Safety**: Flattening arguments ensures that OPA receives predictable string values, preventing interpretation errors.
* **Fail-Safe**: The system defaults to "Deny" if the OPA server is unreachable or the policy is ambiguous.

### Negative Consequences
* **Latency**: Every tool call now requires an external network request to the OPA server.
* **Complexity**: Adds an extra layer of infrastructure (OPA server) to maintain.
* **Debugging**: Troubleshooting a "Denied" action requires checking both the Aspect logic and the OPA policy rules.

## Pros and Cons of Options

### Option 1: Simple local validation
* Good, because it's fast and requires no external infrastructure.
* Bad, because it's hard to maintain complex logic in Java code and doesn't allow for easy policy updates without redeploying.

### Option 2: Centralized Policy Engine (OPA)
* Good, because it allows for very complex, dynamic policies (e.g., "allow only during business hours").
* Bad, because sending raw Java objects to OPA can lead to serialization issues and "type confusion" where the policy interprets the data differently than the Java code.

### Option 3: Combined Approach (Chosen)
* Good, because it leverages OPA for high-level authorization while using Java for low-level "hard" security (path normalization, argument flattening).
* Bad, because it requires maintaining both the Java Aspect and the OPA Rego policies.
