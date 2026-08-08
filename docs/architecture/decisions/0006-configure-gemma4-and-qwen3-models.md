---
adr_id: ADR-2026-0006
title: "Configure Gemma 4 and Qwen 3 Models"
status: Accepted
risk_tier: Tier 1
control_domains:
  - Architecture
  - Operations
  - AI/ML
created_date: 2026-08-05
proposed_date: 2026-08-05
accepted_date: 2026-08-05
implemented_date: 2026-08-05
validated_date: 2026-08-05
next_review_date: 2027-08-05
review_triggers:
  - "Annual architectural review"
  - "Major LLM provider change"
  - "Model performance degradation"
adr_owner:
  name: "jsoehner"
  role: "Repository Maintainer"
decision_owner:
  name: "jsoehner"
  role: "Repository Maintainer"
accountable_role_or_forum: "Architecture Review Board"
acceptors:
  - name: "jsoehner"
    role: "Repository Maintainer"
affected_systems:
  - "Inference Engine"
  - "Agent Orchestrator"
affected_repositories:
  - "spring-ai-blog-agent"
affected_services:
  - "Researcher Agent"
  - "Image Agent"
data_classification: Internal
external_exposure: false
third_party_dependency: true
model_or_ai_impact: "High (Defines the core reasoning and vision capabilities of the agents)"
residual_risk_owner:
  name: "jsoehner"
  role: "Repository Maintainer"
exceptions_or_risk_acceptances: []
technical_debt_items: []
technical_debt_assessment: "Low - Standardized model configuration"
traceability:
  issues: []
  pull_requests: []
  git_commits: []
supersedes: null
superseded_by: null
retention_classification: Permanent Governance
legal_hold: false
---

# ADR-2026-0006: Configure Gemma 4 and Qwen 3 Models

## 1. Context and Problem Statement
To deliver high-quality research and image generation, the agent needs models that balance reasoning capabilities, context window size, and inference speed. The project requires:
1. A primary reasoning model for complex research and HTML drafting.
2. A vision-capable model for analyzing image metadata and generating visual content.
3. Support for local inference to maintain data privacy and reduce costs.

## 2. Decision Drivers
* **Reasoning Quality**: High-quality synthesis and fact-checking for blog posts.
* **Vision Capabilities**: Accurate extraction of EXIF data and visual analysis.
* **Privacy**: Ability to run locally using Ollama or similar engines.
* **Cost Efficiency**: Optimizing the use of tokens for high-volume research tasks.

## 3. Considered Options
* **Option 1**: Use only high-end proprietary models (e.g., GPT-4o, Claude 3.5 Sonnet).
  - **Pros**: Best-in-class reasoning and vision.
  - **Cons**: High cost, data privacy concerns, potential rate limits.
* **Option 2**: Use only small, local models (e.g., Llama 3 8B).
  - **Pros**: Zero cost, full privacy, low latency.
  - **Cons**: Insufficient reasoning for complex research, limited vision capabilities.
* **Option 3**: Hybrid approach using Gemma 4 and Qwen 3 via Ollama (chosen).

## 4. Decision Outcome
Chosen Option: **Option 3**. The project adopts a multi-model strategy:
1. **Gemma 4** as the primary reasoning model for the Researcher Agent (handling complex analysis and HTML generation).
2. **Qwen 3** (specifically vision-enabled variants) for the Image Agent (analyzing and cataloging visual assets).
3. **Ollama** as the primary inference engine to support local deployment and privacy-centric workflows.

## 5. Architecture & Governance Alignment
This decision aligns with the "Hybrid Model" strategy, allowing the project to scale by switching between local and cloud providers while maintaining a consistent API via Spring AI's model-agnostic abstractions.

## 6. Security & Control Domain Mapping
* **AI/ML Security**: Ensures that models are run in a controlled environment (Ollama) with clear input/output boundaries.
* **Privacy**: Prioritizes local inference to keep research data within the organization's infrastructure.

## 7. Risk Assessment & Mitigations

| Threat / Hazard ID | Risk Description | Pre-Mitigation Level | Designed Architectural Mitigation | Residual Risk Level |
| :--- | :--- | :--- | :--- | :--- |
| `THREAT-06` | Model hallucinations in research content. | **Medium** | Use of curated web search and fact-checking prompts to ground the model. | **Low (Accepted)** |
| `HAZARD-06` | Inconsistent output formats from different models. | **Low** | Standardized prompt templates and HTML schemas to enforce output structure. | **Low (Accepted)** |

## 8. Financial & Operational Impact
* Significantly reduces operational costs by enabling local inference.
* Provides high-quality results by using state-of-the-art open-weight models.

## 9. Implementation & Migration Strategy
1. Configure Ollama with Gemma 4 and Qwen 3 models.
2. Update `application.properties` to point to the local Ollama instance.
3. Update prompt templates to optimize for the specific strengths of each model.
4. Verify output quality through manual review of generated blog drafts.

## 10. Verification & Quality Assurance
* Verified that Gemma 4 successfully generates coherent HTML blog posts.
* Verified that Qwen 3 correctly extracts metadata from image files.
* Verified that the system correctly switches between models based on the task type.

## 11. Technical Debt & Residual Risk
* **Technical Debt**: None.
* **Residual Risk**: Model performance may vary based on the underlying hardware (GPU/CPU) used for Ollama.

## 12. Operational & Day-2 Considerations
* Monitor model performance and periodically evaluate newer versions of Gemma and Qwen.
* Maintain a local model registry to manage versions and deployment.

## 13. Compliance & Audit Evidence
* Model selection and configuration are documented in this ADR for architectural transparency.

## 14. Review Schedule & Triggers
* **Schedule**: Annual review.
* **Triggers**: Release of superior open-weight models or significant changes in inference costs.

## 15. Related ADRs & References
* ADR-2026-0003: Workflow and Agent Coordination Optimization
* ADR-2026-0008: OPA Client Hardening and Fail-Closed Behavior
