# 🤖 Spring AI Autonomous Blog Agent

![License](https://img.shields.io/badge/License-MIT-blue.svg) ![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.2.x-brightgreen.svg) ![Docker](https://img.shields.io/badge/Docker-Ready-blue.svg) ![RabbitMQ](https://img.shields.io/badge/RabbitMQ-Event--Driven-orange.svg)

**A highly robust, multi-agent AI system built to run complex asynchronous tasks utilizing Spring Boot and local/private Large Language Models (LLMs).**

This project demonstrates the true power of scaling robust Java application logic (Spring Boot) and asynchronous event-driven queues (RabbitMQ) with LLMs. By decoupling HTTP requests from long-running inference tasks, it achieves incredible resilience, making it perfect for pairing with powerful frontier models or private, locally-hosted LLMs (like `qwen3.5:9b`).

---

## 🚀 The Power of the Architecture

When working with LLMs, inference takes time—especially when executing a multi-pass reasoning chain that involves deep-dive web crawling, fact synthesis, and image generation. Traditional synchronous REST APIs often time out or lock up valuable threads during these operations.

**This agent solves that problem.**

By utilizing a **RabbitMQ message bus**, the system instantly accepts a large batch of research topics and frees up the HTTP thread. The specialized agents then process the tasks sequentially or in parallel without any risk of protocol timeouts.

### 🧠 Multi-Agent Microservices Workflow

```mermaid
graph TD
    %% Define Styles
    classDef user fill:#3b82f6,stroke:#1d4ed8,stroke-width:2px,color:#fff,rx:5px,ry:5px;
    classDef api fill:#10b981,stroke:#047857,stroke-width:2px,color:#fff,rx:5px,ry:5px;
    classDef queue fill:#f59e0b,stroke:#b45309,stroke-width:2px,color:#fff,rx:5px,ry:5px;
    classDef agent fill:#8b5cf6,stroke:#6d28d9,stroke-width:2px,color:#fff,rx:5px,ry:5px;
    classDef action fill:#ef4444,stroke:#b91c1c,stroke-width:2px,color:#fff,rx:5px,ry:5px;

    %% Nodes
    User(("User / REST Call")):::user
    Script["./run-and-submit.sh"]:::user
    Cron["Scheduled Cron (MON/THU)"]:::user

    Supervisor["Supervisor Agent\n(Spring Boot REST API)"]:::api
    RabbitMQ[("RabbitMQ Bus\n(Async Decoupling)")]:::queue

    Researcher["Researcher Agent\n(Language & Logic Model)"]:::agent
    Crawler["Curated Web Crawler\n(Top 25 Security Sites)"]:::action
    
    ImageAgent["Image Agent\n(Vision Model API)"]:::agent
    
    Draft["blog_draft.html\n(Local Save)"]:::action
    WP_Draft["blog_draft_wp.html\n(WordPress Save)"]:::action
    GitHub["Auto GitHub PR\n(Review & Merge)"]:::action

    %% Edges
    User -->|Submit Topic| Supervisor
    Script -->|Pull Image & Submit| Supervisor
    Cron -->|Auto-Generate Topic| Supervisor

    Supervisor -->|Publish to Queue| RabbitMQ
    RabbitMQ -->|Consume Task| Researcher

    Researcher <-->|Pass 1: Gather Facts| Crawler
    Researcher <-->|Pass 2: Synthesize HTML| Researcher
    
    Researcher -->|Request Contextual Assets| ImageAgent
    ImageAgent -->|Return Images| Researcher

    Researcher -->|Save| Draft
    Researcher -->|Save| WP_Draft
    Researcher -->|Commit & Push| GitHub
```

### Why Specialized Agents?
Complex visual work is delegated to a separate, dedicated **Image Agent** running specific vision models (e.g., `qwen3-vl:latest`). This keeps the **Researcher Agent** focused strictly on language, analysis, and HTML drafting, drastically reducing hallucinations and formatting errors.

---

## ✨ Features
- **Asynchronous Decoupling:** Never drop a request. Send as many topics as you want; the agent works through them at its own pace.
- **Curated Web Crawling:** Pre-configured to search the top industry sites for Mobile Security, Cryptography, AppSec, and AI Security. The researcher is strictly instructed to cross-reference at least 10 distinct articles before drafting to ensure comprehensive coverage and reduce hallucination.
- **Autonomous Scheduling:** Uses Spring's `@Scheduled` annotation to run completely independently on a strict cron schedule (e.g., every Mon/Thu).
- **Auto-Pull Requests:** The agent practically contributes to itself! It executes CLI commands to create its own Git branch, commits the generated `.html` files, and opens a GitHub Pull Request for your review.
- **WordPress Ready:** Generates both a raw local draft (`blog_draft.html`) and a WordPress-optimized draft (`blog_draft_wp.html`).

---

## 🛠️ Setup & Installation

### 1. Configuration
Ensure your `.env` or local environment holds your `GITHUB_TOKEN` (required for the agent to open PRs automatically). 

If you are using private LLMs, ensure they are accessible on your network (e.g., via Ollama).

### 2. The Single Command Execution
We've bundled the entire lifecycle into a single, easy-to-use script. This script will build the agent image locally from your source code, start the entire multi-agent Docker Compose stack, wait for the APIs to initialize, and submit your topic:

```bash
./run-and-submit.sh "AI code tech debt"
```

### 3. Watching it Work
Because the system is decoupled, your script will return a success message instantly once the topic is queued. The script will then automatically tail the logs of all containers in real-time so you can watch the AI "think" as it gathers facts and drafts the HTML:

```bash
docker-compose logs -f
```

---

## 🤝 Human in the Loop (Contributing)
While the agent is designed to be highly autonomous—opening its own Pull Requests with finished drafts—human contributions to the core Java architecture or prompts are always welcome. Just branch off, make your tweaks to the agents, and open a PR!
