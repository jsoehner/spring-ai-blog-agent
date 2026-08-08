---
title: Defense-in-Depth Path Validation
status: accepted
date: 2026-08-08
context:
  - Current security relies heavily on the `OpaGuardrailAspect` to intercept tool calls and validate paths.
  - If the Aspect is bypassed (e.g., via reflection, misconfigured AOP, or new tools added without annotations), the application is vulnerable to path traversal.
  - We need a secondary layer of validation within the tools themselves.

decision:
  - All tools that perform filesystem operations (read, write, move, scan) must normalize paths and verify they reside within the allowed workspace directory.
  - A shared utility `PathValidator` will be used to ensure consistency.

consequences:
  - Positive: Prevents path traversal even if the OPA guardrail is bypassed.
  - Negative: Slight increase in overhead for filesystem operations; requires updating all existing tools.
---
