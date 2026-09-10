# 0014. Multi-Agent Redundancy Elimination and Progressive Blog Generation Pipeline

* Status: Accepted
* Deciders: Development Team
* Date: 2026-09-06

## Technical Story
* Issue: Generated blog posts suffered from severe paragraph duplication, repetitive sentences, and circular arguments. Root cause analysis revealed contradictory prompt instructions forcing 3+ similar sentences per fact, researcher fact starvation, excessive text naturalization intensity altering technical vocabulary, and an absence of automated sentence deduplication in the Spring pipeline prior to WordPress Gutenberg formatting.

## Context and Problem Statement
During review of generated blog posts (such as "Why Architectural Security Requirements are necessary"), several quality defects were identified:
1. **Prompt-Induced Duplication:** The blogger prompt explicitly instructed the LLM with: `CRITICAL: Expand each factual claim into at least 3 sentences of similar message.` This directly commanded the model to generate redundant, near-identical sentences for every fact, padding paragraphs with artificial repetition.
2. **Researcher Fact Starvation:** The research agent conducted a single high-level query that produced only 2–3 generic claims. Starved of diverse technical angles, the downstream blogger LLM was forced to loop and restate the few available facts to meet length expectations.
3. **Over-Naturalization of Technical Vocabulary:** The `texthumanize` Python script was executed at an intensity of 75. At this high threshold, the tool replaced specialized cybersecurity and software architecture nomenclature with colloquial alternatives, degrading technical precision and confusing prompt context.
4. **Lack of Automated Content Deduplication:** The post-generation pipeline lacked an algorithmic safeguard to detect and eliminate duplicate or near-duplicate sentences before formatting into Gutenberg blocks.

## Decision Drivers
* **Content Quality and Progressive Argumentation:** Ensure each section delivers distinct, non-overlapping architectural arguments that build sequentially.
* **Multi-Dimensional Research:** Expand factual gathering across core technical dimensions: architectural foundations, cost economics, threat modeling (STRIDE / Zero Trust), and compliance governance.
* **Terminology Preservation:** Retain domain-accurate cybersecurity terminology while ensuring natural, non-robotic phrasing.
* **Algorithmic Guardrails:** Provide an automated Java-based deduplication layer in the pipeline to protect against LLM hallucination and repetition loops.
* **Java 25 Execution:** Maintain full compatibility with the Java 25 runtime and Gradle toolchain across all services.

## Decision Outcome
We implemented a comprehensive, multi-layer quality enhancement pipeline:

1. **Blogger Prompt Overhaul (`blogger-prompt.txt`):**
   - Removed the mandate requiring "3+ sentences of similar message".
   - Enforced progressive technical argumentation across distinct H2 subheadings.
   - Constrained target post length to 600–800 words.
   - Added strict negative constraints prohibiting restating definitions across sections or repeating introductory topic sentences.
   - Enforced natural paragraph integration without bolding opening sentences.

2. **Multi-Dimensional Research Context (`ResearcherController.java`):**
   - Structured research queries across 4 explicit dimensions:
     1. Architectural Foundations (Security by Design, preventative controls vs. reactive patching).
     2. Cost Economics & Blast Radius (Shift Left cost disparity, remediation metrics).
     3. Threat Modeling & Zero Trust (STRIDE analysis, micro-segmentation, identity-aware proxies).
     4. Governance & Compliance (NIST SP 800-53, ISO/IEC 27001, architectural flaws vs. coding bugs).
   - Ingested facts are deduplicated before compilation and handoff to the supervisor.

3. **Text Naturalization Tuning (`humanize.py`):**
   - Reduced `texthumanize` intensity from 75 to 30.
   - Preserved domain-specific nomenclature while eliminating synthetic phrasing.

4. **Algorithmic Sentence Deduplication (`SentenceDeduplicator.java`):**
   - Created a dedicated service utilizing normalized token sets and Jaccard similarity thresholding (threshold = 0.70) to detect and filter out repeated sentences.
   - Integrated the deduplicator into both `ContentPipeline.java` and `AgentOrchestrator.java` prior to WordPress Gutenberg block wrapping.

5. **WordPress Gutenberg Block Sanitization:**
   - Ensured Gutenberg blocks (`<!-- wp:paragraph -->`, `<!-- wp:heading -->`, `<!-- wp:image -->`) are compactly formatted on single lines without internal linefeeds, carriage returns, or trailing spaces.

### Positive Consequences
* Completely eliminated paragraph repetition and duplicate sentences in generated blog posts.
* Enhanced technical depth with multi-dimensional facts and accurate citations.
* Clean, valid Gutenberg block syntax ready for WordPress publishing.
* Pipeline tests and containerized multi-agent workflows execute reliably under Java 25.

### Negative Consequences
* Gathering multi-dimensional research facts slightly increases initial research query latency.

## References
* ADR-2026-0009: Integrating TextHumanize for AI Blog Post Naturalization
* ADR-2026-0012: Java 25 Upgrade and Gutenberg Block Formatting Normalization
* ADR-2026-0013: Align Docker Base Images with Java 25
