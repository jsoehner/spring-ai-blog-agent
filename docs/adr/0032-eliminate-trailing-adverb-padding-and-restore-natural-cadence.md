# 0032. Eliminate Trailing Adverb Padding and Restore Natural Sentence Cadence

* Status: Accepted
* Deciders: Development Team
* Date: 2026-09-30

## Technical Story
* Issue: Generated blog posts exhibited synthetic sentence structures ending in repetitive, unnatural adverbs or temporal tags (such as `now`, `soon`, `actively`, `always`, `currently`, `recently`, `effectively`). Root cause analysis confirmed this was not caused by the `texthumanize` library, but rather by the Blogger LLM attempting to satisfy overly rigid word-count constraints (`crisp (15–20 words)`, `~75 words per paragraph`, `~5 sentences per paragraph`) combined with prompt duplication between system and user prompts.

## Context and Problem Statement
When inspecting generated blog output (e.g., `Architecture Review Boards and the Importance of Well Documented Decision Records`), paragraphs consistently featured sentences ending in artificial adverbial qualifiers:
- *"...ensure technology decisions align with organizational requirements directly now."*
- *"...prevent future bottlenecks or vulnerabilities from emerging soon."*
- *"...without structured regulatory intervention from leadership teams actively."*
- *"...within production systems safely and securely always."*

Investigation confirmed:
1. **`texthumanize` Library Scope:** `texthumanize` executes exclusively in Pass 1 on researcher bullet points to simplify vocabulary and replace awkward connectors. It does not process supervisor HTML or blog text and does not append trailing adverbs.
2. **LLM Word-Count Bracket Artifact:** The system prompt instructed: `Keep sentences crisp (15–20 words) in active voice, avoiding compound run-on sentences` coupled with `Target ~75 words per paragraph block` and `~5 sentences`. Local models (`gemma4:12b`, `qwen3.5:9b`) treat `(15–20 words)` as a strict lower bound. When an active voice thought completes in 12–14 words, the model uses trailing adverbs before the period to reach the 15–20 word window without forming compound sentences.
3. **Prompt Duplication:** Prompt instructions were duplicated between `blogger-prompt.txt` (system prompt) and `AgentOrchestrator.java` (user prompt), amplifying constraint weights in the LLM's attention mechanism.

## Decision Drivers
* **Natural Sentence Rhythm & Flow:** Eliminate artificial token-padding artifacts while maintaining scannable, high-density technical analysis.
* **Negative Guardrails:** Explicitly prohibit trailing adverb and temporal tag padding at the sentence level.
* **Prompt Cleanliness & Versioning:** Remove duplicate prompt blocks from `AgentOrchestrator.java` and maintain prompt versioning in `PromptConfiguration.java` (`v1.4.0`).
* **Preservation of Existing Project Rules:** Maintain mandatory unbolded topic sentence rules and single-line compact WordPress Gutenberg block syntax.

## Decision Outcome
1. **Updated Blogger Prompt (`src/main/resources/prompts/blogger-prompt.txt`):**
   - Replaced the rigid `15–20 words` lower bound with flexible natural cadence (typically 12–25 words), encouraging alternation between punchy statements and detailed explanations.
   - Broadened paragraph targets to 3–5 sentences and 60–90 words, instructing the model never to pad sentences with filler words simply to satisfy length targets.
   - Added an explicit negative guardrail:
     > `CRITICAL: Do NOT pad sentences or append superfluous trailing adverbs or temporal tags (such as 'now', 'soon', 'actively', 'always', 'effectively', 'currently', 'recently', 'constantly', or 'easily done') to sentence endings. Stop each sentence naturally as soon as the core technical thought is stated.`
2. **Streamlined Supervisor Orchestration (`AgentOrchestrator.java` & `RunLiveBlogGenerationTest.java`):**
   - Refactored the user prompt to avoid echoing formatting rules already declared in the system prompt.
   - Reinforced the anti-padding guardrail alongside untrusted data boundaries.
3. **Prompt Registry Update (`PromptConfiguration.java`):**
   - Registered `v1.4.0` in `PromptConfiguration` with natural cadence instructions to ensure `PromptManager` consumers dynamically resolve the updated prompt.

## Consequences
### Positive
* Generated blog posts read naturally with clean sentence terminations rather than repetitive adverbs.
* Prevents autoregressive token padding in smaller and quantized local LLMs.
* Eliminates prompt redundancy between system and user prompts in `AgentOrchestrator`.

### Negative
* Requires downstream integration tests and evaluation to monitor sentence length distribution across varied topics.
