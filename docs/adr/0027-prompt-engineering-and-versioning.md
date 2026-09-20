# ADR-0027: Prompt Engineering & Versioning

## Status
Proposed

## Context
The project currently uses several system prompts, many of which are hardcoded or embedded in Java classes. As the agents become more complex, prompt engineering becomes a significant part of the development cycle. Managing prompt versions, A/B testing different prompt variations, and ensuring consistency across different LLM models (e.g., Gemma, Qwen, GPT) is currently difficult.

## Decision
We will establish a formal **Prompt Engineering & Versioning** strategy:

1.  **Externalized Prompts**: All system prompts and few-shot examples will be moved out of the Java source code and into a versioned configuration system (e.g., YAML files or a dedicated Prompt Management Service).
2.  **Prompt Versioning**: Every prompt will have a unique version ID. This allows the application to request specific versions of a prompt, facilitating safe rollouts and A/B testing.
3.  **Prompt Templates**: We will use a templating engine (e.g., Handlebars or Jinja2-style) to inject dynamic variables (like `topic`, `facts`, `user_query`) into the prompts.
4.  **Evaluation Framework**: We will establish a standard for evaluating prompt performance using both automated metrics (e.g., BLEU, ROUGE, or LLM-as-a-judge) and human feedback.

## Consequences
- **Pros**:
  - **Iteration Speed**: Prompt engineers can update prompts without requiring a full code re-compile and deployment.
  - **Consistency**: Ensures that the same version of a prompt is used across all instances of an agent.
  - **A/B Testing**: Enables systematic comparison of different prompt variations to optimize LLM outputs.
- **Cons**:
  - **Infrastructure**: Requires a system to host and serve the prompts.
  - **Complexity**: Adds a layer of indirection between the code and the actual prompt used by the model.
