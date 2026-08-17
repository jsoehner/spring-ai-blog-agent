# 0011. Gitignore Maintenance for Agent Artefacts and README Gotchas Update

* Status: Accepted
* Deciders: Development Team
* Date: 2026-08-17

## Technical Story
* Issue: Un-tracked agent output directories (`.pi` and `.pi-subagents`) cluttering workspace repository git status and key project gotchas needing central documentation in the README.

## Context and Problem Statement
During developer interactions with execution environments and agent skills, local working directories like `.pi` and `.pi-subagents` are created. These operational artifact directories should be ignored by version control to keep git status clean and prevent accidental commits. Additionally, key operational lessons and gotchas documented in AGENTS.md needed sync into the main `README.md` to ensure developer visibility.

## Decision Drivers
* Repository Hygiene: Maintain a clean repository status by preventing temporary agent metadata directories from being tracked.
* Knowledge Centralization: Ensure all architectural, LLM-specific, and environment gotchas are updated in `README.md`.

## Decision Outcome
Chosen Option: Add `.pi/` and `.pi-subagents/` to `.gitignore` and document the decision in an Architecture Decision Record (ADR-0011), alongside updating `README.md` with full project gotchas.

### Positive Consequences
* `.pi` and `.pi-subagents` directories are safely ignored by git.
* Developers can easily reference complete gotchas in `README.md`.

### Negative Consequences
* None.
