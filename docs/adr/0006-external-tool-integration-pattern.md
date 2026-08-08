# ADR-006: External Tool Integration Pattern

## Status
Proposed

## Context
The project interacts with several external tools: WordPress, StockPrice, and various AI agents. Currently, these are integrated as individual tools (e.g., `WordPressTool`). As the number of external tools grows, we need a standardized way to:
- Register and discover available tools.
- Handle authentication and configuration for different tools.
- Standardize error handling and retries for external API calls.
- Provide a consistent interface for the agent to "call" these tools.

## Decision
We will implement a Registry pattern for external tools. Each tool will implement a common `ExternalTool` interface. A `ToolRegistry` will manage the lifecycle, configuration, and execution of these tools.

This will decouple the agent's logic from the specific implementation details of each tool and make it easier to add new tools or swap existing ones.

## Implementation Plan
1. **Define `ExternalTool` Interface**: Define common methods for tool execution and metadata (name, description, etc.).
2. **Create `ToolRegistry`**: A central repository to register and retrieve tools.
3. **Refactor existing tools**: Update `WordPressTool` (and others) to implement the new interface.
4. **Agent Tool Integration**: Update the agent's tool-calling logic to use the `ToolRegistry`.

## Consequences
- **Pros**:
    - **Extensibility**: Easy to add new tools without modifying core agent logic.
    - **Standardization**: Consistent error handling and logging for all external interactions.
    - **Configuration**: Centralized management of tool-specific configurations (API keys, URLs).
- **Cons**:
    - **Abstraction Overhead**: Adds a layer of indirection.
    - **Initial Effort**: Requires refactoring existing tool implementations.
