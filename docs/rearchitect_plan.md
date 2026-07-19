# Application Re-Architecture & Documentation Plan

This plan leverages the **C4 Architecture Model** guidelines to document the current state of the Spring AI Blog Agent and outlines a roadmap for long-term re-architecting, addressing critical security boundaries.

---

## 1. C4 Architecture Overview (Proposed Documentation)
We plan to establish a structured documentation folder under `C4-Documentation/` mapping:
1. **Context Level**: The relationship between the Spring AI Agent, personas (e.g., users requesting drafts, scheduled cron trigger), and external endpoints (Wikipedia API, target security blogs, GitHub repository).
2. **Container Level**: The isolation between the `supervisor-agent`, `researcher-agent`, `image-agent`, and the Open Policy Agent (`opa`) sidecar.
3. **Component Level**: Specific Java classes (`AutoDraftService`, `WebCrawlerConfig`, `OpaGuardrailAspect`) and Python helper scripts (`tls_scanner.py`).

---

## 2. Issues Encountered & Resolution Tracker

Below is the track of identified architecture and security issues. During planning, we evaluated the relevance of each issue, closing the resolved ones and prioritizing the most critical architectural improvement:

### [Issue-01] Path Traversal Bypass via OPA Aspect
* **Status**: 🔴 CLOSED (Resolved)
* **Relevance**: Resolved via path normalization inside `OpaGuardrailAspect.java` using native Path APIs before OPA evaluations.

### [Issue-02] Option Injection in subprocess execution
* **Status**: 🔴 CLOSED (Resolved)
* **Relevance**: Resolved by forcing `--` option separators in `TlsScannerTool.java`.

### [Issue-03] Dockerfile Non-Root User Creation Sequence
* **Status**: 🔴 CLOSED (Resolved)
* **Relevance**: Resolved by creating the `spring` group and user *before* copying jar files with `--chown`.

### [Issue-04] DNS Rebinding SSRF in Web Crawler
* **Status**: 🔴 CLOSED (Resolved)
* **Relevance**: Resolved by forcing JVM DNS cache pinning (`networkaddress.cache.ttl = 30`) inside `WebCrawlerConfig.java`.

### [Issue-05] SSRF in Python TLS Scanner
* **Status**: 🔴 CLOSED (Resolved)
* **Relevance**: Resolved by resolving hostnames and validating IP addresses against private/loopback ranges in `tls_scanner.py`.

### [Issue-07] Ineffective Nightly Dependency Update Command
* **Status**: 🔴 CLOSED (Resolved)
* **Relevance**: Resolved by implementing a custom `update-dependencies.py` script that dynamically queries Maven Central for the latest stable versions of Spring Boot, Spring AI, and other libraries and edits `build.gradle` directly.

### [Issue-08] Multi-Agent Blocking Calls & Sequential Security Scans
* **Status**: 🔴 CLOSED (Resolved)
* **Relevance**: Resolved by parallelizing security scan jobs in GitHub Actions and making the Supervisor Agent's RabbitMQ message listener asynchronous via `CompletableFuture.runAsync()`.

### [Issue-06] Structural Egress Isolation for Web Crawlers (Most Relevant Architectural Target)
* **Status**: 🟢 ACTIVE (Prioritized)
* **Relevance**: **Highly Relevant.** Even with JVM DNS cache pinning and Python-level IP checks, the crawler code runs in the same container space as the agent orchestrator. A malicious website returning malformed text or exploit payloads could compromise the agent runtime environment.
* **Proposed Architecture Change**: Move the Web Crawler and HTTP request utilities out of the main JVM/Python containers into an isolated, egress-restricted sandbox microservice (e.g., a lightweight crawler container that communicates with the `researcher-agent` over a secure RPC interface, strictly prevented from accessing internal services or local files).

---

## 3. Implementation Steps for Isolated Crawling Container

1. **Decouple Crawler Component**: Extract JSoup crawling from `WebCrawlerConfig` into a standalone Node.js or Python microservice.
2. **Egress Network Restrictions**: Configure Docker network settings to ensure the Crawler container can only talk to public IPs and the incoming agent RPC, while blocking access to the internal network (`rabbitmq`, `supervisor-agent`, `opa`).
3. **Generate C4 Component Diagrams**: Document the new container and components in `C4-Documentation/` to visualize the new secure boundary.
