# Agentic SAST — spring-ai-blog-agent

## Summary
The system is highly vulnerable to complete CI/CD pipeline takeover and lateral movement between microservices. An attacker can execute arbitrary code in a high-privilege GitHub Actions environment via unvetted Pull Requests or hijack the internal message bus using default RabbitMQ credentials reachable from internet-facing agents.

## Scan Metrics

- Scan ID: 2026-07-31T10:07:11Z__spring-ai-blog-agent
- Module: spring-ai-blog-agent
- Start: 2026-07-31T10:07:11Z
- End: 2026-07-31T15:53:19Z
- Duration (sec): 20768
- Files in scope: 244
- Files analyzed (unique): 122
- Coverage: 50.0%
- Chunks: 68 (risk=5, catch-all=20, specialist=43)
- Tokens (prompt): 1573300
- Tokens (completion): 157608
- Tokens (total): 1730908

- Folders scanned: 77
### Tokens by Phase

_Prompt = fresh + cache-write (billable). Cache-read shown separately, NOT included in totals._

| Phase | Calls | Prompt | Completion | Total | % | Cache-read (excl.) |
|---|---:|---:|---:|---:|---:|---:|
| s4-deepdive | 118 | 1,030,115 | 82,716 | 1,112,831 | 64.3 | 0 |
| s6-verify | 76 | 415,452 | 62,659 | 478,111 | 27.6 | 0 |
| s1-preprocess | 5 | 98,982 | 908 | 99,890 | 5.8 | 0 |
| s3-decompose | 1 | 11,849 | 2,765 | 14,614 | 0.8 | 0 |
| s2-threatmodel | 1 | 8,508 | 3,201 | 11,709 | 0.7 | 0 |
| s7-dedup | 1 | 4,050 | 3,956 | 8,006 | 0.5 | 0 |
| s1-autoexclude | 1 | 4,017 | 1,195 | 5,212 | 0.3 | 0 |
| unlabeled | 2 | 327 | 208 | 535 | 0.0 | 0 |

### Language LOC Coverage

| Language | LOC in scope | LOC scanned | Coverage % |
|---|---:|---:|---:|
| batch | 64 | 64 | 100.0 |
| java | 1096 | 1096 | 100.0 |
| javascript | 71 | 71 | 100.0 |
| other | 18630 | 1279 | 6.9 |
| python | 262 | 262 | 100.0 |
| shell | 111 | 111 | 100.0 |

## Scan Health

- ⚠️ Degraded coverage: 1/68 deep-dive chunk(s) failed or timed out — their findings are absent from this report.
- Recoverable errors logged by stage: s4=7, s6-verify=1
- Full error log: `spring-ai-blog-agent_20260731T100711Z_errors.jsonl`

## Threat Model

### System context

The Spring AI Blog Agent is a multi-agent, asynchronous automation system designed to autonomously research topics, generate blog content, and publish drafts via GitHub PRs or WordPress. It utilizes a microservices architecture with a Supervisor Agent (Spring Boot) orchestrating tasks through a RabbitMQ message bus to specialized Researcher and Image Agents. The system leverages Large Language Models (LLM) for reasoning and web crawling for data gathering, running primarily within Docker containers.

### Assets

| Asset | Sensitivity | Description |
|---|---|---|
| GitHub Personal Access Token | critical | Credential used by the agent to autonomously create Pull Requests and commit code to repositories. |
| Process Integrity (Automated Pipeline) | high | The integrity of the automated content generation, preventing unauthorized/malicious content injection into public blogs. |
| Internal Infrastructure Metadata | medium | Information regarding internal network topology, RabbitMQ credentials, and local service endpoints (Ollama/OPA). |
| Generated Blog Artifacts | low | HTML files and images generated in the output directory. |

### Trust boundaries

- **tls_scanner.py::main** — local_user → shell/subprocess execution → Process Integrity (Automated Pipeline)
- **Supervisor Agent REST API** — external_network → application_logic → GitHub Personal Access Token, Process Integrity (Automated Pipeline)
- **Researcher Agent $\rightarrow$ Internet** — untrusted_web_content → agent_crawler → Internal Infrastructure Metadata

### Ranked threats

| ID | Threat | Actor | Surface | Asset | Impact | Likelihood | Controls |
|---|---|---|---|---|---|---|---|
| T1 | Command or argument injection via hostname manipulation in Python subprocess calls. | local_user | tls_scanner.py::main | Process Integrity (Automated Pipeline) | high | possible | is_safe_host validation and use of '--' option separators. |
| T2 | Path traversal via malformed topic names resulting in unauthorized file writes/overwrites in the host filesystem. | remote_auth | Supervisor Agent REST API | Process Integrity (Automated Pipeline) | high | possible | OPA guardrails and path normalization logic. |
| T3 | Server-Side Request Forgery (SSRF) via malicious URLs discovered during the web crawling phase. | remote_unauth | Researcher Agent $\rightarrow$ Internet | Internal Infrastructure Metadata | medium | possible | DNS and IP range validation (blocking loopback/private ranges). |
| T4 | Command injection via un-sanitized topic strings in downstream shell script execution. | remote_auth | Supervisor Agent REST API | Process Integrity (Automated Pipeline) | high | rare | Alphanumeric and whitespace-only regex sanitization. |

### Open questions

- Is the Supervisor Agent REST API exposed to any network beyond the local Docker bridge?
- Does the RabbitMQ management interface have default credentials enabled for external access?
- How are end-user requests (REST calls) authenticated/authorized before reaching the Supervisor logic?
- What is the risk appetite regarding the LLM's ability to generate malicious instructions that could influence tool usage (Prompt Injection)?

## Verification
- Raw findings (pre-verification): 26
- True positives (verified): 14
- False positives (dropped): 2
- Verifier errors (excluded — undetermined, not confirmed clean): 1
- Duplicates collapsed (all passes): 4
- Verification precision: 53.8%

## Findings (14)

### 1. [CRITICAL] Automated workflow dispatch triggers high-privilege workflow on untrusted PR code
**Class:** CWE-829: Inclusion of Functionality from Untrusted Control Sphere
**CWE:** CWE-829: Inclusion of Functionality from Untrusted Control Sphere - https://cwe.mitre.org/data/definitions/829.html
**File:** `.github/workflows/security-scan.yml:175-180`
**CVSS 3.1:** **10.0** (Critical) — `CVSS:3.1/AV:N/AC:L/PR:N/UI:N/S:C/C:H/I:H/A:H`
**OffensivePriority:** **P3** - Internal Network / Privileged Position | *exposure unverified — no CMDB context; AV:N (network-routable; internet exposure unconfirmed)*
**Confidence:** 0.95 (2 runs agreed)

#### Description
The `reporting` job in `security-scan.yml` is triggered on `pull_request` events and uses `github.rest.actions.createWorkflowDispatch` to trigger the `nightly-dependency-update.yml` workflow. The `ref` parameter for this dispatch is set to `context.ref || 'main'`. In a `pull_request` event, `context.ref` refers to the merge ref of the PR (e.g., `refs/pull/X/merge`), which contains the unvetted code from the attacker's branch. The target workflow, `nightly-dependency-update.yml`, has high privileges (`contents: write`, `pull-requests: write`) and executes scripts from the repository (e.g., `.github/scripts/update-dependencies.py`). An attacker can include a malicious script in their PR and trigger its execution by intentionally causing a security scan failure.

#### Impact
An attacker can achieve Remote Code Execution (RCE) in the context of a highly privileged workflow. This allows them to steal repository secrets (like `PERSONAL_ACCESS_TOKEN`) or inject malicious code into the main branch, compromising the software supply chain.

#### Exploit scenario
An attacker submits a PR containing a modified `update-dependencies.py` that exfiltrates the `PR_TOKEN` environment variable. The attacker also modifies a file to ensure the `semgrep` job fails. When the `security-scan` workflow runs, the `reporting` job detects the failure and triggers the `nightly-dependency-update` workflow on the PR's merge ref, executing the malicious script with write access.

#### Preconditions
- Attacker can submit a Pull Request to the repository
- The attacker modifies a script or configuration file used by the `nightly-dependency-update.yml` workflow

```
await github.rest.actions.createWorkflowDispatch({
  owner,
  repo,
  workflow_id: 'nightly-dependency-update.yml',
  ref: context.ref || 'main'
});
```

#### How to fix
Ensure that `workflow_dispatch` is only triggered using a trusted, immutable reference (e.g., 'main') and never uses `context.ref` when the triggering workflow is running in a `pull_request` context.

**Exploitability:** Pre-auth (via PR). An attacker can trigger a high-privilege workflow using unvetted code from their own branch, leading to RCE in the CI/CD environment with repository write access.

#### Adversarial verification
**Verdict:** TRUE_POSITIVE (confidence: 10/10) — (no reason given)

The scanner has correctly identified a highly critical logic flaw in the GitHub Actions configuration. 

In `.github/workflows/security-scan.yml`, the `reporting` job is configured to run even if previous security scanning jobs fail (`if: always()`). When a `pull_request` event triggers this workflow, the `context.ref` refers to the PR's merge ref (e.g., `refs/pull/X/merge`), which contains the unvetted code from the attacker's branch merged into the target branch.

The `reporting` job executes a `workflow_dispatch` call to `nightly-dependency-update.yml`, explicitly passing `ref: context.ref || 'main'`. Because an attacker can submit a PR that intentionally fails a security scan (e.g., by introducing a detectable secret or malformed code), they can force the execution of the `nightly-dependency-update.yml` workflow using their unvetted merge ref. 

The target workflow, `nightly-dependency-update.yml`, has high privileges (`contents: write`, `pull-requests: write`) and executes `.github/scripts/update-dependencies.py`. Since this script is checked out from the provided `ref` (the attacker's code), the attacker can execute arbitrary Python code with the permissions of the nightly workflow, leading to full repository compromise or secret exfiltration (e.g., stealing `PERSONAL_ACCESS_TOKEN`).

### 2. [CRITICAL] Default RabbitMQ credentials allow message injection
**Class:** CWE-287: Improper Authentication
**CWE:** CWE-287: Improper Authentication - https://cwe.mitre.org/data/definitions/287.html
**File:** `docker-compose.yml:8-9`
**CVSS 3.1:** **9.8** (Critical) — `CVSS:3.1/AV:N/AC:L/PR:N/UI:N/S:U/C:H/I:H/A:H`
**OffensivePriority:** **P3** - Internal Network / Privileged Position | *exposure unverified — no CMDB context; AV:N (network-routable; internet exposure unconfirmed)*
**Confidence:** 0.95 (1 run agreed)

#### Description
The RabbitMQ service is configured with default `guest/guest` credentials. The `researcher-agent`, which is an untrusted entry point due to its internet access, is part of the same Docker network and uses this broker. An attacker who compromises the researcher agent can use these known credentials to publish arbitrary messages to the queues used by the Supervisor Agent.

#### Impact
An attacker can inject malicious messages into the RabbitMQ bus, potentially triggering unauthorized actions or bypassing business logic in the Supervisor Agent. This could lead to unauthorized content generation or deployment of malicious blog drafts.

#### Exploit scenario
An attacker compromises the `researcher-agent` container via its internet-facing interface. They then use the default `guest/guest` credentials to connect to the RabbitMQ service and inject a crafted message into the task queue, forcing the `supervisor-agent` to execute an unauthorized workflow.

#### Preconditions
- Attacker compromises the researcher-agent container
- The researcher-agent is on the same Docker network as RabbitMQ

```
RABBITMQ_DEFAULT_USER=guest
RABBITMQ_DEFAULT_PASS=guest
```

#### How to fix
Replace the default `RABBITMQ_DEFAULT_USER` and `RABBITMQ_DEFAULT_PASS` with strong, unique credentials that are not shared across all services.

**Exploitability:** Achievable if the Researcher Agent is compromised. Default credentials allow any service on the internal Docker network to inject arbitrary tasks into the Supervisor Agent.

#### Adversarial verification
**Verdict:** TRUE_POSITIVE (confidence: 10/10) — The RabbitMQ service uses default `guest/guest` credentials and is accessible from the `researcher-agent` container on the same Docker network, allowing an attacker to inject arbitrary messages into the `supervisor-tasks` queue.

The scanner alleges that the RabbitMQ service in `docker-compose.yml` is using default `guest/guest` credentials, which could allow an attacker who compromises the `researcher-agent` to inject messages into queues used by the `supervisor-agent`.

**Analysis:**
1.  **Credentials Verification**: In `docker-compose.yml`, lines 8 and 9 confirm:
    ```yaml
    RABBITMQ_DEFAULT_USER=guest
    RABBITMQ_DEFAULT_PASS=guest
    ```
2.  **Network Exposure**: The `researcher-agent` (lines 67-104) is part of the same Docker network as RabbitMQ and has access to it via the `rabbitmq` service name.
3.  **Data Flow / Vulnerability Path**:
    *   The `ResearcherController` (lines 58-116) listens to a queue named `research-tasks`. After processing, it sends data to the `supervisor-tasks` queue using `rabbitTemplate.convertAndSend("supervisor-tasks", jsonPayload);` (line 110).
    *   The `BlogAgentController` (lines 73-163) listens to the `supervisor-tasks` queue via `@RabbitListener(queuesToDeclare = @Queue("supervisor-tasks"))`. It processes the payload, which contains a `topic` and `facts`.
    *   Importantly, the `BlogAgentController`'s `processSupervisorTask` method (line 74) is an entry point triggered by messages in that queue. An attacker who can connect to RabbitMQ with `guest/guest` credentials can publish arbitrary JSON payloads to the `supervisor-tasks` queue.
    *   The code in `BlogAgentController.processSupervisorTask` takes the `facts` string from the payload and uses it directly in a prompt to an LLM (line 85): `user("Here are the gathered facts:\n" + facts + ...)`. This allows for **Prompt Injection**.
    *   Furthermore, the contents of this message eventually end up in a file on the host's filesystem via `java.nio.file.Files.writeString(targetFile, contentWithImages);` (line 152). While there is path traversal protection (lines 137-145), an attacker can still control the *content* of the files written to the `output` directory.
    *   The most direct impact described by the finding is "message injection": injecting a crafted message into `supervisor-tasks` to force unauthorized workflows or data manipulation (via LLM prompt injection).

4.  **Conclusion**: Since the `researcher-agent` is an untrusted entry point (as per architecture notes) and shares the same network as RabbitMQ, and since the RabbitMQ credentials are default, an attacker who compromises the researcher agent can indeed inject messages into the broker. This allows them to manipulate the logic of the `supervisor-agent`.

The finding is a TRUE_POSITIVE.

### 3. [CRITICAL] Command injection bypass via delimiter manipulation
**Class:** CWE-78: Improper Neutralization of Special Elements used in an OS Command (OS Command Injection)
**CWE:** CWE-78: Improper Neutralization of Special Elements used in an OS Command (OS Command Injection) - https://cwe.mitre.org/data/definitions/78.html
**File:** `security-policies/opa-guardrails/policies/agent_commands.rego:10-25`
**CVSS 3.1:** **9.8** (Critical) — `CVSS:3.1/AV:N/AC:L/PR:N/UI:N/S:U/C:H/I:H/A:H`
**OffensivePriority:** **P3** - Internal Network / Privileged Position | *exposure unverified — no CMDB context; AV:N (network-routable; internet exposure unconfirmed)*
**Confidence:** 0.95 (2 runs agreed)

#### Description
The policy attempts to prevent forbidden command execution by splitting the input command string only by spaces (`split(command, " ")`) and checking if any resulting part matches a forbidden command or ends with a path-based version of it. An attacker can bypass this filter by using shell metachartains like ';' or '|' to chain a forbidden command to another command without using any spaces (e.g., 'ls;rm'). In this case, the split produces a single part ['ls;rm'], which fails both the equality check (`parts[_] == forbidden_cmd`) and the suffix check (`endswith(parts[_], "/rm")`).

#### Impact
An attacker can execute arbitrary forbidden commands (e.g., 'rm', 'nc') with the privileges of the Agent process, leading to full system compromise and unauthorized data access.

#### Exploit scenario
An attacker provides the command string 'ls;rm' through an untrusted input source (like a web search result processed by the Researcher Agent). The OPA policy evaluates this as a single part that does not match any forbidden command in the configuration, allowing the shell to execute both 'ls' and 'rm'.

#### Preconditions
- Attacker can influence the command string sent to the OPA evaluator (e.g., via the Researcher Agent processing external web content)

```
parts := split(command, " ")
    forbidden_cmd := forbidden_commands[_]
    
    # Check if the base command or any piped command is forbidden
    parts[_] == forbidden_cmd
```

#### How to fix
Avoid using string splitting for security boundaries; instead, use a strict allow-list of permitted commands and arguments, or implement a parser that recognizes all shell metacharacters as delimiters.

**Exploitability:** Direct command injection possible if an attacker can submit commands to the OPA engine, bypassing the space-based delimiter check using shell metacharacters.

#### Adversarial verification
**Verdict:** TRUE_POSITIVE (confidence: 10/10) — The OPA policy relies on `split(command, " ")`, which can be bypassed by using shell metacharacters like `;`, `|`, or `&` to chain forbidden commands without spaces.

The scanner identifies a potential command injection bypass in `security-policies/opa-guardrails/policies/agent_commands.rego`.

**Analysis of the OPA Policy:**
The policy aims to prevent forbidden commands from being executed by splitting the command string by spaces (`split(command, " ")`) and then checking if any part is equal to a forbidden command or ends with `/` + `forbidden_cmd`. 

As stated in the finding:
1. If an attacker provides a command like `ls;rm`, the `split(command, " ")` function will produce a single element in the list: `['ls;rm']`.
2. This string `ls;rm` is neither equal to a forbidden command (e.g., `rm`) nor does it end with `/rm`.
3. Consequently, the `deny` rules are not triggered, and the command execution is allowed by the OPA policy.

**Impact:**
If an downstream component (like a shell-invoking agent) executes this string using a shell (e.g., `subprocess.run(cmd, shell=True)` or even just because the input was intended for a shell), the semicolon `;` acts as a command separator in most shells. This allows the execution of arbitrary commands that were specifically blocked by the policy list.

**Checking for Mitigations:**
The provided code snippet is an OPA policy (Rego). Rego itself doesn't execute the command; it only provides the decision. The vulnerability exists because the *logic* used to detect forbidden commands is flawed and bypassable via shell metacharacters that don't involve spaces. I have checked `tls_scanner.py` (the identified entry point), which uses `subprocess.run(cmd, ...)` where `cmd` is a list of arguments (`["openssl", ...]`). In Python, when `shell=False` (the default), `subprocess.run` does not invoke a shell and therefore does not interpret `;`, `|`, or `&`. 

**However**, the finding refers to an "Agent" pattern (e.g., "Researcher Agent") processing external content. If such an agent exists in the architecture and uses the output of this OPA policy decision to execute commands via a shell, then the bypass is a real security flaw in the *security policy itself*. The policy's job is to prevent forbidden commands; if it can be bypassed by using characters like `;` instead of spaces, it fails its stated purpose.

The scanner's claim that `ls;rm` would pass the split-based check is correct and demonstrably true for the provided Rego code.

**Verdict:**
The OPA policy is indeed vulnerable to bypass via shell metacharacters because it relies on space-delimited splitting which does not account for command chaining characters.

### 4. [CRITICAL] Security guardrail bypass via incomplete denylist
**Class:** CWE-285: Improper Authorization
**CWE:** CWE-285: Improper Authorization - https://cwe.mitre.org/data/definitions/285.html
**File:** `security-policies/policy/rules/guardrails.rego:5-5`
**CVSS 3.1:** **9.8** (Critical) — `CVSS:3.1/AV:N/AC:L/PR:N/UI:N/S:U/C:H/I:H/A:H`
**OffensivePriority:** **P3** - Internal Network / Privileged Position | *exposure unverified — no CMDB context; AV:N (network-routable; internet exposure unconfirmed)*
**Confidence:** 0.95 (2 runs agreed)

#### Description
The security policy in `guardrails.rego` uses a denylist approach, specifically checking if the attacker-controlled `input.request.type` is exactly equal to the string 'unsafe_op'. Because it does not use an allowlist of permitted operations, any other value provided in the `type` field (e.g., via the Supervisor Agent REST API) will bypass this specific security check. Furthermore, providing a non-string type like an array containing the forbidden string (e.g., `['unsafe_op']`) also causes the equality check to fail, resulting in a bypass.

#### Impact
An attacker can bypass the security policy by providing an operation type other than 'unsafe_op'. This allows unauthorized or malicious operations to be executed by the Supervisor Agent, potentially leading to unauthorized system actions.

#### Exploit scenario
An attacker sends a request to the Supervisor Agent REST API with the payload `{"request": {"type": "malicious_operation"}}`. The OPA policy evaluates the equality `input.request.type == "unsafe_op"` as false, no deny message is generated in `main.rego`, and the unauthorized operation is permitted to proceed.

#### Preconditions
- Attacker can send requests to the Supervisor Agent REST API
- The system relies on this specific Rego policy to prevent unauthorized operations

```
input.request.type == "unsafe_op"
```

#### How to fix
Replace the denylist approach in `security-policies/policy/rules/guardrails.rego:5` with an allowlist of authorized operation types. Define a set of permitted strings and deny any input that does not belong to that set.

**Exploitability:** Auth bypass/Logic flaw. The denylist approach allows any operation string other than 'unsafe_op' (or an array containing it) to pass the security check.

#### Adversarial verification
**Verdict:** TRUE_POSITIVE (confidence: 9/10) — The Rego equality check is susceptible to both value-based denylist incompleteness and a highly effective type-confusion exploit using arrays.

The scanner correctly identifies a logic flaw in the Rego policy definition within `security-policies/policy/rules/guardrails.rego`. 

In Rego, the equality operator (`==`) is type-sensitive. The policy defines a `deny` rule that triggers only when `input.request.type` is exactly equal to the string `"unsafe_op"`. This implementation suffers from two distinct vulnerabilities:

1.  **Type Confusion Bypass (Technical Bypass):** An attacker can bypass the specific check for `"unsafe_op"` by providing the value as an array containing the string, such as `["unsafe_op"]`. In Rego, `["unsafe_op"] == "unsafe_op"` evaluates to `false`, meaning the `deny` rule will not trigger, and the ostensibly "unsafe" operation will proceed.
2.  **Incomplete Denylist (Design Flaw):** The policy relies on a denylist of specific forbidden operations rather than an allowlist of permitted ones. This means any value other than `"unsafe_op"`—including potentially sensitive or unauthorized operations like `"delete_all"` or `"admin_access"`—will pass this specific security check, assuming no other `deny` rules exist in the policy bundle to catch them. 

Since the code's purpose (as indicated by the package name and message) is to serve as a security guardrail for an agent's operations, the ability to bypass checks for known unsafe types or bypass the entire mechanism via type manipulation represents a real security risk in any system relying on this OPA policy for authorization.

### 5. [CRITICAL] Arbitrary File Write via CSV Export Argument
**Class:** CWE-73: External Control of File Name or Path
**CWE:** CWE-73: External Control of File Name or Path - https://cwe.mitre.org/data/definitions/73.html
**File:** `tls_scanner.py:123-126`
**CVSS 3.1:** **9.8** (Critical) — `CVSS:3.1/AV:N/AC:L/PR:N/UI:N/S:U/C:H/I:H/A:H`
**OffensivePriority:** **P3** - Internal Network / Privileged Position | *exposure unverified — no CMDB context; AV:N (network-routable; internet exposure unconfirmed)*
**Confidence:** 0.95 (1 run agreed)

#### Description
The script's main entry point accepts a '--csv' argument which is used directly as a filename in the `open()` function with write mode ('w'). There is no validation to ensure that the provided path is restricted to a safe directory or that it does not contain directory traversal sequences (e.g., '../'). Since the `main` function is identified as an untrusted entry point, an attacker can specify any path on the filesystem that the executing user has permission to write to.

#### Impact
An attacker can overwrite arbitrary files on the host filesystem by controlling the '--csv' argument. This can lead to Remote Code Execution (RCE) if sensitive files like web shells, configuration files, or authorized_keys are overwritten.

#### Exploit scenario
An attacker provides '--csv /var/www/html/shell.php --targets "<?php system($_GET[cmd]); ?>"' as arguments to the script. The script writes a CSV containing the malicious PHP payload into the web root, creating a functional web shell.

#### Preconditions
- Attacker can control command-line arguments passed to the `main` function (e.g., via a wrapper service or CI/CD configuration)

```
with open(args.csv, "w", newline="") as f:
    writer = csv.DictWriter(f, fieldnames=["Start_URL", "Final_URL", "PQC", "Issuer", "Protocol", "Group", "Cipher"])
    writer.writeheader()
    writer.writerows(results)
```

#### How to fix
Implement strict path validation for the `--csv` argument. Ensure the resolved absolute path resides within a predefined, restricted directory and reject any input containing directory traversal components or absolute paths to sensitive system directories.

**Exploitability:** Arbitrary file write via path traversal in the '--csv' argument, allowing an attacker to overwrite sensitive local files if they can influence execution arguments.

#### Adversarial verification
**Verdict:** TRUE_POSITIVE (confidence: 10/10) — (no reason given)

The scanner correctly identifies a vulnerability where the `--csv` argument in `tls_scanner.py` is used directly as a filename in a write operation without any path validation or sanitization. If an attacker can influence the command-line arguments passed to this script (e.g., through a wrapper service, CI/CD pipeline, or a web interface that invokes the script), they can perform an arbitrary file write. By combining this with the `--targets` argument (or by providing malicious input via a target file), which is also written into the CSV, the attacker can write arbitrary content to any location on the filesystem that the executing user has permission to access. This can lead to Remote Code Execution (RCE) if an attacker can overwrite a web-accessible file (like a `.php` or `.jsp` file in a web root) with a malicious payload.

The precondition defined in the finding—that the caller is an external/untrusted entry point (e.g., a wrapper service)—is aligned with the "Out of Scope" exception for boundary-crossing inputs. No validation exists in `tls_scanner.py` to prevent directory traversal or path manipulation via the `--csv` flag.

### 6. [CRITICAL] Unverified binary download from external URL
**Class:** CWE-494
**CWE:** CWE-494 - https://cwe.mitre.org/data/definitions/494.html
**File:** `security-policies/.github/workflows/opa-test.yml:19-20`
**CVSS 3.1:** **9.0** (Critical) — `CVSS:3.1/AV:N/AC:H/PR:N/UI:N/S:C/C:H/I:H/A:H`
**OffensivePriority:** **P3** - Internal Network / Privileged Position | *exposure unverified — no CMDB context; AV:N (network-routable; internet exposure unconfirmed)*
**Confidence:** 0.95 (2 runs agreed)

#### Description
The workflow downloads a binary from an external URL (`openpolicyagent.org`) using `curl` but fails to verify its integrity via a cryptographic checksum. If the remote source or the download path is compromised, the runner will execute a malicious payload with the permissions of the GitHub Actions environment.

#### Impact
An attacker compromising the OPA distribution server can execute arbitrary code in the CI/CD pipeline. This leads to the theft of repository secrets (e.g., GITHUB_TOKEN) and potential supply chain poisoning of the project.

#### Exploit scenario
An attacker replaces the OPA binary on the distribution server with a script that exfiltrates the `GITHUB_TOKEN` to an external server. When the workflow triggers on a push or PR, `curl` downloads the malicious script and the subsequent execution of `./opa` runs the attacker's code.

#### Preconditions
- Attacker must be able to compromise the OPA download infrastructure or intercept the download via a Man-in-the-Middle attack (if TLS is bypassed/compromised)

```
curl -L -o opa https://openpolicyagent.org/downloads/latest/opa_linux_amd64
chmod 755 opa
```

#### How to fix
Verify the integrity of the downloaded binary by checking its SHA-256 hash against a known good value immediately after the `curl` command.

**Exploitability:** (not ranked by chaining pass)

#### Adversarial verification
**Verdict:** TRUE_POSITIVE (confidence: 10/10) — (no reason given)

The scanner correctly identifies that the GitHub Actions workflow downloads an external binary from `openpolicyagent.org` using `curl` without verifying its integrity (e.g., via a SHA-256 checksum). This creates a supply chain vulnerability: if the remote server or the TLS connection is compromised, the runner will execute arbitrary code provided by the attacker. While the use of HTTPS mitigates Man-in-the-Middle attacks, it does not protect against a compromise of the OPA distribution infrastructure itself. The impact includes potential exposure of `GITHUB_TOKEN` and other repository secrets or the ability to inject malicious logic into subsequent CI steps/artifacts.

### 7. [CRITICAL] Unverified OPA binary download via curl
**Class:** CWE-494
**CWE:** CWE-494 - https://cwe.mitre.org/data/definitions/494.html
**File:** `security-policies/.github/workflows/test.yml:9-9`
**CVSS 3.1:** **10.0** (Critical) — `CVSS:3.1/AV:N/AC:L/PR:N/UI:N/S:C/C:H/I:H/A:H`
**OffensivePriority:** **P3** - Internal Network / Privileged Position | *exposure unverified — no CMDB context; AV:N (network-routable; internet exposure unconfirmed)*
**Confidence:** 0.95 (1 run agreed)

#### Description
The workflow downloads the OPA binary from an external URL using `curl` and executes it without verifying its integrity via a checksum (e.g., SHA256). This makes the build pipeline dependent on the security of the remote host and the network path, specifically targeting the 'latest' release which can change without notice.

#### Impact
An attacker who compromises the OPA distribution server or intercepts network traffic can replace the legitimate OPA binary with a malicious one. This leads to arbitrary code execution on the GitHub Actions runner, potentially allowing for the theft of repository secrets or lateral movement within the CI/CD environment.

#### Exploit scenario
An attacker intercepts the connection to `openintagent.org` or compromises their download server to serve a malicious binary. When the workflow runs, it downloads and executes this binary, granting the attacker RCE on the runner.

#### Preconditions
- Attacker can intercept network traffic (MITM) during the workflow execution
- or Attacker has compromised the OPA download infrastructure

```
curl -L -o opa https://openpolicyagent.org/downloads/latest/opa_linux_amd64 && chmod 755 opa
```

#### How to fix
Download the binary and verify its integrity using a hardcoded SHA256 checksum before execution. Use a specific, versioned URL instead of 'latest'.

**Exploitability:** (not ranked by chaining pass)

#### Adversarial verification
**Verdict:** TRUE_POSITIVE (confidence: 10/10) — The workflow downloads a binary from an external URL and executes it without verifying its checksum, leaving the pipeline vulnerable to upstream supply chain attacks.

The scanner correctly identifies that the GitHub Actions workflow downloads an external binary (`opa`) via `curl` from a remote URL without performing any integrity verification (such as checking a SHA256 checksum). While the use of HTTPS provides protection against many Man-in-the-Middle (MITM) attacks, it does not protect against a compromise of the upstream download server or its distribution infrastructure (e.g., an S3 bucket). If the OPA release infrastructure were compromised, the workflow would automatically download and execute a malicious binary, leading to Remote Code Execution (RCE) on the GitHub Actions runner. This is a classic software supply chain vulnerability.

### 8. [CRITICAL] Supply chain risk via mutable image tags
**Class:** CWE-494
**CWE:** CWE-494 - https://cwe.mitre.org/data/definitions/494.html
**File:** `docker-compose.yml:25-25`
**CVSS 3.1:** **10.0** (Critical) — `CVSS:3.1/AV:N/AC:L/PR:N/UI:N/S:C/C:H/I:H/A:H`
**OffensivePriority:** **P3** - Internal Network / Privileged Position | *exposure unverified — no CMDB context; AV:N (network-routable; internet exposure unconfirmed)*
**Confidence:** 0.90 (1 run agreed)

#### Description
The 'supervisor-agent', 'opa', and 'prometheus' services all utilize mutable tags (':latest' or ':latest-debug'). This bypasses the security property of supply-chain integrity, as there is no guarantee that the code running in production matches the audited/tested version from the build pipeline.

#### Impact
The use of the ':latest' tag makes deployments non-deterministic and vulnerable to supply chain attacks. If the 'ghcr.io/jsoehner/spring-ai-blog-agent' registry is compromised, an attacker can push a malicious image that is automatically pulled and executed during the next service restart or deployment.

#### Exploit scenario
An attacker gains access to the GitHub Container Registry and overwrites the 'latest' tag for the supervisor-agent with a malicious image containing a backdoor. The next time the docker-compose stack is updated or restarted, the compromised image is deployed into the production environment.

#### Preconditions
- Attlaner must have write access to the container registry (ghcr.io or docker.io)

```
image: ghcr.io/jsoehner/spring-ai-blog-agent:latest
```

#### How to fix
Pin all container images to a specific, immutable content hash (SHA256) or at least a specific version tag (e.g., 'v1.2.3') instead of using ':latest'.

**Exploitability:** (not ranked by chaining pass)

#### Adversarial verification
**Verdict:** TRUE_POSITIVE (confidence: 10/10) — (no reason given)

The scanner correctly identifies that several services in `docker-compose.yml` (`supervisor-agent`, `researcher-agent`, `image-agent`, `opa`, and `prometheus`) utilize mutable image tags such as `:latest` or `:latest-debug`. This configuration presents a supply chain risk because it prevents the guarantee of image integrity; if an attacker compromises the container registry (e.g., GHCR), they can overwrite these tags with malicious images that will be automatically pulled and executed by any pipeline or environment using this configuration. While the `supervisor-agent`, `researcher-agent`, and `image-agent` also include a `build: .` instruction which builds the image locally from source, the `:latest` tag still points to a mutable remote reference that could be used in pull-based deployments (e.g., via `docker-compose pull`) or by other users of this configuration who do not have the local build context.

### 9. [HIGH] Sensitive Git history exposed via host bind mount
**Class:** CWE-200: Exposure of Sensitive Information to an Unauthorized Actor
**CWE:** CWE-200: Exposure of Sensitive Information to an Unauthorized Actor - https://cwe.mitre.org/data/definitions/200.html
**File:** `docker-compose.yml:57-57`
**CVSS 3.1:** **7.5** (High) — `CVSS:3.1/AV:N/AC:L/PR:N/UI:N/S:U/C:H/I:N/A:N`
**OffensivePriority:** **P3** - Internal Network / Privileged Position | *exposure unverified — no CMDB context; AV:N (network-routable; internet exposure unconfirmed)*
**Confidence:** 0.95 (1 run agreed)

#### Description
The docker-compose configuration mounts the host's '.git' directory directly into the running 'supervisor-agent' container at '/app/.git'. While the application runs as a non-root user, any vulnerability in the Spring Boot application (such as Path Traversal or Local File Inclusion) allows an attacker to traverse the Git structure and reconstruct the repository's entire history.

#### Impact
An attacker who gains file-read access or shell execution within the 'supervisor-agent' container can access the complete Git history of the repository. This exposes deleted secrets, internal commit metadata, and historical architectural changes that are not present in the current working directory.

#### Exploit scenario
An attacker exploits a file-read vulnerability in the Supervisor Agent REST API to read '/app/.git/logs/HEAD'. By parsing these logs, they identify previous commit hashes and use 'git show <hash>' (if git is available) or direct object reads of Git blobs to recover previously deleted API keys or configuration files.

#### Preconditions
- Attacker must be able to trigger a file-read or command execution within the 'supervisor-agent' container

```
- ./.git:/app/.git
```

#### How to fix
Remove the '.git' bind mount from the 'supervisor-agent' service in docker-compose.yml. Only copy the necessary application files into the image during the build stage.

**Exploitability:** Information leak of entire Git history. Requires a pre-existing LFI/Path Traversal vulnerability in the Spring Boot application.

#### Adversarial verification
**Verdict:** TRUE_POSITIVE (confidence: 10/10) — The .git directory is explicitly mounted into the container, and there is a functional arbitrary file read tool (`CodeTools.readFile`) available to the application's agent, allowing an attacker to traverse the Git history.

The scanner identifies a sensitive information leak via a host bind mount of the `.git` directory into the `supervisor-agent` container in `docker-compose.yml`.

**Analysis:**
1.  **Vulnerability Confirmation**: In `docker-compose.yml`, line 57, the configuration indeed contains `- ./.git:/app/.git`. This mounts the host's Git history into the container.
2.  **Attack Surface**: The scanner claims that a vulnerability like Path Traversal or LFI in the Spring Boot application would allow an attacker to read this directory.
3.  **Code Review**: 
    *   `CodeTools.java` contains a `@Tool` called `readFile(String absolutePath)` which directly reads any file path provided by the user: `return Files.readString(path);`. This is a classic Local File Inclusion (LFI) / Arbitrary File Read vulnerability. Since this code is part of an AI Agent's toolset, if an attacker can influence the agent's instructions or interact with the controller that triggers these tools, they can read any file within the container.
    *   The `supervisor-agent` container has `.git` mounted at `/app/.git`. An attacker using `CodeTools.readFile("/app/.git/logs/HEAD")` (or similar) would successfully be able to retrieve Git history and potentially secrets stored in previous commits or the Git structure itself.
    *   The `BlogAgentController` does not have a direct request-to-file read path in its REST endpoints, but it *does* trigger background tasks (`processSupervisorTask`) that use these tools (via the AI agent's logic). While the controller has some sanitization for writing files to the `output` directory (lines 137-145), it does not prevent the `CodeTools` from reading arbitrary paths if they are invoked.
4.  **Impact**: Access to the `.git` directory allows an attacker to reconstruct the repository's history, which often contains sensitive information such as deleted credentials, internal configuration, or architectural details that were once present in the repo. This is a confirmed info-leak via an insecure container configuration.

The scanner is correct about both the mount and the potential impact if a file-read vulnerability (which exists in `CodeTools.java`) is triggered.

### 10. [HIGH] TLS Audit Spoofing via Certificate Issuer Injection
**Class:** CWE-20: Improper Input Validation
**CWE:** CWE-20: Improper Input Validation - https://cwe.mitre.org/data/definitions/20.html
**File:** `tls_scanner.py:69-73`
**CVSS 3.1:** **7.5** (High) — `CVSS:3.1/AV:N/AC:L/PR:N/UI:N/S:U/C:N/I:H/A:N`
**OffensivePriority:** **P3** - Internal Network / Privileged Position | *exposure unverified — no CMDB context; AV:N (network-routable; internet exposure unconfirmed)*
**Confidence:** 0.90 (1 run agreed)

#### Description
The scanner uses `re.search` on the entire output of `openssl s_client`. Because the certificate chain (which contains untrusted data provided by the target server) is printed to stdout before the handshake summary, an attacker can inject strings like 'Protocol : TLSv1.3' into the Certificate Issuer field. The parser will find this first occurrence and report the spoofed value instead of the actual negotiated protocol found in the handshake summary.

#### Impact
An attacker can manipulate the audit results to report false security properties (e.g., claiming TLS 1.3 or PQC support) by embedding specific strings in their certificate's Issuer field, effectively bypassing the security audit.

#### Exploit scenario
An attacker hosts a server with a certificate where the Issuer field contains the string 'Protocol : TLSv1.3'. When the scanner audits this host, it matches the injected string in the certificate dump and reports TLS 1.3, even if the actual connection used an insecure protocol like TLS 1.0.

#### Preconditions
- Attacker controls the TLS certificate presented to the scanner.

```
protocol = re.search(r"Protocol\s*:\s*(TLSv[\d\.]+)", output)
        cipher = re.search(r"Cipher is\s*(.+)", output)
```

#### How to fix
The parser should only search for protocol and cipher information within the handshake summary section of the OpenSSL output (e.g., by splitting the output at the end of the certificate block) or use a more restrictive regex that ensures the match is not part of the certificate text.

**Exploitability:** (not ranked by chaining pass)

#### Adversarial verification
**Verdict:** TRUE_POSITIVE (confidence: 10/10) — (no reason given)

The scanner has correctly identified a logic flaw in `tls_scanner.py`. 

The script executes `openssl s_client -showcerts` and captures the entire standard output into the `output` variable. The `openssl s_client` command prints the certificate chain (which contains untrusted, attacker-controlled data from the server) at the beginning of its output, followed by a handshake summary containing the actual negotiated `Protocol` and `Cipher`.

The parser uses `re.search(r"Protocol\s*:\s*(TLSv[\d\.]+)", output)`, which returns the **first** occurrence of the pattern found in the entire string. An attacker hosting a malicious TLS server can include the string `"Protocol : TLSv1.3"` within an untrusted field (such as the `Issuer` or `Subject` of the certificate) to spoof the reported protocol version. This allows a server using insecure protocols (e.g., TLS 1.0) to appear compliant with modern standards in the audit report, effectively bypassing the security check's purpose.

There are no preceding filters or anchors (like searching from the end of the string or specifically targeting the summary section) to prevent this injection.

### 11. [MEDIUM] Markdown injection via unescaped pipe characters in security reports
**Class:** CWE-74: Improper Neutralization of Special Elements in Output (Injection)
**CWE:** CWE-74: Improper Neutralization of Special Elements in Output (Injection) - https://cwe.mitre.org/data/definitions/74.html
**File:** `.github/scripts/parse-findings.js:73-73`
**CVSS 3.1:** **5.3** (Medium) — `CVSS:3.1/AV:N/AC:L/PR:N/UI:N/S:U/C:N/I:L/A:N`
**OffensivePriority:** **P3** - Internal Network / Privileged Position | *exposure unverified — no CMDB context; AV:N (network-routable; internet exposure unconfirmed)*
**Confidence:** 0.95 (2 runs agreed)
**Also at:** `.github/workflows/security-scan.yml:127-167`

*1 additional call site(s) collapsed during dedup — same root cause; each location needs the same fix applied.*

#### Description
The script parses JSON reports from security tools (Gitleaks, Semgrep, Trivy) and constructs a Markdown table by concatenating fields like `tool`, `file`, `severity`, and `description` into a string. While the script removes newlines from the description, it fails to escape the pipe character (`|`), which is the structural delimiter for Markdown tables. An attacker who can trigger a security finding containing a pipe character can break out of the current table cell and inject new columns or rows.

#### Impact
An attacker can manipulate the generated `findings-table.md` to inject arbitrary Markdown content. This can be used to deceive developers with fake findings or perform phishing attacks if the table is rendered in a web UI, such as GitHub PR comments.

#### Exploit scenario
An attacker submits a Pull Request containing a string like `| Fake | Info |`. A tool like Semgrep identifies this as a finding with that message. When the script runs, it writes `| Semgrep | path:1 | HIGH | | Fake | Info | |` to the Markdown table, allowing the attacker to inject arbitrary content into the report.

#### Preconditions
- Attacker can influence the content of security tool reports (e.g., via a Pull Request)
- The generated Markdown file is rendered in an environment that parses Markdown (e.g., GitHub UI)

```
table += `| ${f.tool} | ${f.file} | ${f.severity} | ${f.description.replace(/\n/g, ' ')} |\n`;
```

#### How to fix
Escape the pipe character (`|`) and other Markdown control characters before appending them to the table string. Specifically, at line 73, replace `|` with `\|`.

**Exploitability:** (not ranked by chaining pass)

#### Adversarial verification
**Verdict:** TRUE_POSITIVE (confidence: 10/10) — The script `parse-findings.js` constructs a Markdown table by concatenating fields from security tool reports (`gitleaks-report.json`, `semgrep-results.json`, `trivy-results.json`) without escaping the pipe character (`|`). Since an attacker can control the content of these reports (e.g., by including specific strings in a Pull Request that trigger finding messages), they can break out of the table cells to inject arbitrary rows or columns, leading to the manipulation and deception within the security report rendered on GitHub.



### 12. [MEDIUM] CSV Formula Injection in TLS Audit Export
**Class:** CWE-1236: Improper Neutralization of Formula Elements in a CSV File
**CWE:** CWE-1236: Improper Neutralization of Formula Elements in a CSV File - https://cwe.mitre.org/data/definitions/1236.html
**File:** `tls_scanner.py:107-126`
**CVSS 3.1:** **4.3** (Medium) — `CVSS:3.1/AV:N/AC:L/PR:N/UI:R/S:U/C:L/I:N/A:N`
**OffensivePriority:** **P3** - Internal Network / Privileged Position | *exposure unverified — no CMDB context; AV:N (network-routable; internet exposure unconfirmed)*
**Confidence:** 1.00 (2 runs agreed)

#### Description
The script takes user-provided target URLs from command-line arguments or a text file and writes them directly into a CSV file using `csv.DictWriter`. There is no validation or sanitization to strip or escape characters that trigger formula execution in spreadsheet software (e.g., '=', '+', '-', '@'). An attacker who can influence the input targets (via CLI or a shared target file) can inject malicious payloads.

#### Impact
An attacker can execute arbitrary formulas in spreadsheet software when the generated CSV is opened. This can lead to data exfiltration, local file disclosure, or phishing attacks against users viewing the audit results.

#### Exploit scenario
An attacker provides a target string like '=SUM(1+1)' via the `--file` argument. When an administrator opens the resulting CSV in Microsoft Excel or LibreOffice, the cell executes the formula, which could be expanded to exfiltrate data via `HYPERLINK`.

#### Preconditions
- Attacker can influence the input targets (e.g., by modifying a shared target file or CI/CD parameter)
- The resulting CSV is opened in a spreadsheet application that supports formula execution

```
writer.writerows(results)
```

#### How to fix
Sanitize all input strings before writing them to the CSV. Specifically, ensure that any cell value starting with '=', '+', '-', '@', or '	' is either stripped of those characters or prefixed with an apostrophe (').

**Exploitability:** (not ranked by chaining pass)

#### Adversarial verification
**Verdict:** TRUE_POSITIVE (confidence: 10/10) — The script writes user-provided targets from command-line arguments and files directly into a CSV file without any sanitization of characters that trigger formula execution in spreadsheet software (=, +, -, @). An attacker can inject malicious formulas via the `--file` argument or CLI arguments if they can control these inputs (e.g., through a Pull Request to a shared targets file used in CI/CD), which will execute when an administrator opens the exported CSV in applications like Excel or LibreOffice.



### 13. [MEDIUM] Sensitive local system paths and internal addresses leaked in audit logs
**Class:** CWE-200: Exposure of Sensitive Information to an Unauthorized Actor
**CWE:** CWE-200: Exposure of Sensitive Information to an Unauthorized Actor - https://cwe.mitre.org/data/definitions/200.html
**File:** `piolium/audit-state.json:18-22`
**CVSS 3.1:** **5.3** (Medium) — `CVSS:3.1/AV:N/AC:L/PR:N/UI:N/S:U/C:L/I:N/A:N`
**OffensivePriority:** **P3** - Internal Network / Privileged Position | *internal-network position required*
**Confidence:** 0.95 (2 runs agreed)

#### Description
The `error` and `last_error` fields in `audit-state.json` contain unmasked absolute Windows filesystem paths (e.g., `C:\Users\jsoehner\AppData\...`) and internal service URLs (`http://127.0.0.1:8080`). This data is likely written to the state file by error handlers during runtime without sanitization of the underlying exception messages.

#### Impact
An attacker accessing the audit state can discover absolute filesystem paths on the host machine and internal network addresses. This information facilitates targeted attacks, such as identifying software versions for exploitation or preparing path traversal attacks.

#### Exploit scenario
An attacker querying the Supervisor Agent's REST API (an untrusted entry point) retrieves the audit state JSON, revealing the host's local username and the exact location of sensitive npm modules on the developer's or server's filesystem.

#### Preconditions
- Attacker can access the `audit-state.json` file via the Supervisor Agent REST API or repository access

```
"error": "Failed after 5 retries: No API key found for llama-server=http://127.0.0.1:8080.\n\nUse /login to log into a provider via OAuth or API key. See:\n  C:\\Users\\jsoehner\\AppData\\Roaming\\npm\\node_modules\\@earendil-works\\pi-coding-agent\\docs\\providers.md..."
```

#### How to fix
Implement a sanitization layer in the error handling logic to strip absolute filesystem paths, local user directories, and internal IP addresses from error messages before they are persisted to audit logs or state files.

**Exploitability:** (not ranked by chaining pass)

#### Adversarial verification
**Verdict:** TRUE_POSITIVE (confidence: 10/10) — The `audit-state.json` file contains unmasked absolute filesystem paths containing the local username and internal service URLs, which constitutes an information leak.

The scanner has correctly identified that `piolium/audit-state.json` contains sensitive information, specifically absolute Windows filesystem paths (revealing the local username `jsoehner`) and internal service URLs (`http://127.0.0.1:8080`).

While the "exploit scenario" assumes an attacker can access this file via a REST API, the primary issue is indeed the presence of unmasked, sensitive data in a state file that is intended to be used for audit logging and potentially exposed through other interfaces. The content itself is verified by the `Read` operation on the file.

The impact of leaking local usernames and internal network addresses is a real information leak (PII/system metadata), as it aids an attacker in reconnaissance and lateral movement within a compromised environment.

### 14. [MEDIUM] Sensitive information disclosure via stack traces
**Class:** CWE-209: Generation of Error Message Containing Sensitive Information
**CWE:** CWE-209: Generation of Error Message Containing Sensitive Information - https://cwe.mitre.org/data/definitions/209.html
**File:** `src/main/resources/application.properties.template:13-13`
**CVSS 3.1:** **5.3** (Medium) — `CVSS:3.1/AV:N/AC:L/PR:N/UI:N/S:U/C:L/I:N/A:N`
**OffensivePriority:** **P3** - Internal Network / Privileged Position | *exposure unverified — no CMDB context; AV:N (network-routable; internet exposure unconfirmed)*
**Confidence:** 0.95 (3 runs agreed)

#### Description
The configuration `server.error.include-stacktrace=always` is explicitly enabled in the application properties template. When an exception occurs during processing of a request to the Supervisor Agent's REST API, the full stack trace is returned in the HTTP response body to the caller.

#### Impact
An attacker can obtain detailed internal implementation details, including class names, method calls, and library versions. This reconnaissance significantly aids in crafting more sophisticated exploits against the application.

#### Exploit scenario
An attacker sends a malunformed request (e.g., invalid JSON or unexpected type) to a REST endpoint on the Supervisor Agent. The server encounters an unhandled exception and returns an HTTP 500 error containing the complete Java stack trace, revealing internal code paths and dependencies.

#### Preconditions
- The application is deployed using this configuration
- The attacker can reach the Supervisor Agent's REST API

```
server.error.include-stacktrace=always
```

#### How to fix
Disable stack trace inclusion in error responses by setting `server.error.include-stacktrace=never` in production configurations.

**Exploitability:** (not ranked by chaining pass)

#### Adversarial verification
**Verdict:** TRUE_POSITIVE (confidence: 9/10) — The configuration `server.error.include-stacktrace=always` in the properties template is explicitly set to include full stack traces in error responses, and no custom exception handling was found in the controllers to prevent this disclosure.

The scanner identifies that `server.error.include-stacktrace=always` is set in `src/main/resources/application.properties.template`. This configuration tells Spring Boot to include the full stack trace in the HTTP response body when an unhandled exception occurs.

I analyzed the controllers (e.g., `BlogAgentController`) and found that they expose REST endpoints (`/blog`). If a request to these endpoints triggers an unhandled exception (for example, due to malformed input or unexpected downstream service behavior), Spring Boot's default error handling mechanism will use this configuration setting. Since there is no global `@ControllerAdvice` or custom error handler visible in the provided controllers that overrides this behavior to sanitize the response, a stack trace could indeed be leaked to an attacker.

The leak of stack traces can reveal internal class names, dependencies, and even parts of the logic flow (as seen in `BlogAgentController`), which aids an attacker in reconnaissance for more complex exploits.

While the configuration is present in a `.template` file, it is likely used as the basis for production configurations. The impact is information disclosure via stack traces.

## Exploit Chains

### [CRITICAL] Researcher Agent Compromise -> Message Injection -> Orchestrator Takeover
**Path:** #2 Default RabbitMQ credentials allow message injection → #6 Unverified binary download from external URL

An attacker uses an SSRF or web-crawling vulnerability (T3) to compromise the Researcher Agent. Once inside the Docker network, they use the default 'guest/guest' RabbitMQ credentials ([12]) to inject malicious task messages into the queue, effectively controlling the instructions received by the Supervisor Agent.


## Dropped Findings

- **[EXCLUDED]** `src/main/java/com/example/demo/TlsScannerTool.java:15` injection (chunk-03) — test/mock/example path
- **[EXCLUDED]** `src/main/java/com/example/demo/CodeTools.java:15` other (spec-crypto-10) — test/mock/example path
- **[EXCLUDED]** `src/main/java/com/example/demo/ChatController.java:49` logic-flaw (spec-logic-bug-10) — test/mock/example path
- **[EXCLUDED]** `src/main/java/com/example/demo/ChatController.java:50` logic-flaw (spec-access-control-10) — test/mock/example path
- **[EXCLUDED]** `src/main/java/com/example/demo/BlogAgentController.java:67` injection (spec-batch-etl-10) — test/mock/example path
- **[DUP (pre-verify)]** `tls_scanner.py:52` injection (spec-logic-bug-01) — trivial: same file/class within line tolerance
- **[DUP (pre-verify)]** `.github/workflows/security-scan.yml:154` injection (spec-batch-etl-06) — trivial: same file/class within line tolerance
- **[FP]** `security-policies/opa-guardrails/policies/agent_files.rego:9` logic-flaw (chunk-04) — No finding provided to review.
- **[VERIFY-ERR]** `.github/scripts/update-dependencies.py:66` injection (spec-crypto-02) — verifier output unparseable
- **[FP]** `docker-compose.yml:59` logic-flaw (spec-logic-bug-04) — While the researchers' findings influence the content written to the shared volume by the supervisor, the supervisor only performs git operations on those files and does not execute their contents. The file names themselves are sanitized against path traversal and command injection.
- **[DUP of #11]** `.github/workflows/security-scan.yml:127` injection (spec-logic-bug-06) — The Markdown injection in GitHub issues is a direct result of the unescaped pipes in parse-findings.js.
- **[DUP of #2]** `docker-compose.yml:8` other (spec-iac-01) — Identical finding regarding default RabbitMQ credentials in the same file and line.


---

## Appendix: Scan Scope

### Folders scanned (77)

- `./`
- `.github/`
- `.github/scripts/`
- `.github/workflows/`
- `.gradle/`
- `.gradle/9.5.1/`
- `.gradle/buildOutputCleanup/`
- `.gradle/vcs-1/`
- `config/`
- `gradle/wrapper/`
- `piolium/`
- `piolium/attack-surface/`
- `piolium/tmp/piolium/runs/l1-2026-07-23T22-09-31-996Z-a1-0fa4b593/`
- `piolium/tmp/piolium/runs/l1-2026-07-23T22-09-31-996Z-a1-2051b8a9/`
- `piolium/tmp/piolium/runs/l1-2026-07-23T22-09-31-996Z-a1-20eaffa9/`
- `piolium/tmp/piolium/runs/l1-2026-07-23T22-09-31-996Z-a1-71480e93/`
- `piolium/tmp/piolium/runs/l1-2026-07-23T22-09-31-996Z-a1-7780462a/`
- `piolium/tmp/piolium/runs/l1-2026-07-23T22-09-31-996Z-a1-c5ad9a8a/`
- `piolium/tmp/piolium/runs/l1-2026-07-23T22-09-31-996Z-a1-dab7fedd/`
- `piolium/tmp/piolium/runs/l1-2026-07-23T22-09-31-996Z-a1-f821b189/`
- `piolium/tmp/piolium/runs/l1-2026-07-23T22-09-31-996Z-a2-09c2cd38/`
- `piolium/tmp/piolium/runs/l1-2026-07-23T22-09-31-996Z-a2-201f0863/`
- `piolium/tmp/piolium/runs/l1-2026-07-23T22-09-31-996Z-a2-40c5236f/`
- `piolium/tmp/piolium/runs/l1-2026-07-23T22-09-31-996Z-a2-4dc13590/`
- `piolium/tmp/piolium/runs/l1-2026-07-23T22-09-31-996Z-a2-74f9c0f4/`
- `piolium/tmp/piolium/runs/l1-2026-07-23T22-09-31-996Z-a2-8a17daf5/`
- `piolium/tmp/piolium/runs/l1-2026-07-23T22-09-31-996Z-a2-bfb8982b/`
- `piolium/tmp/piolium/runs/l1-2026-07-23T22-09-31-996Z-a2-c621e290/`
- `piolium/tmp/piolium/runs/l1-2026-07-23T22-09-31-996Z-a3-2097f30e/`
- `piolium/tmp/piolium/runs/l1-2026-07-23T22-09-31-996Z-a3-324b09f0/`
- `piolium/tmp/piolium/runs/l1-2026-07-23T22-09-31-996Z-a3-434b99a8/`
- `piolium/tmp/piolium/runs/l1-2026-07-23T22-09-31-996Z-a3-66e6f424/`
- `piolium/tmp/piolium/runs/l1-2026-07-23T22-09-31-996Z-a3-76b133bb/`
- `piolium/tmp/piolium/runs/l1-2026-07-23T22-09-31-996Z-a3-802b0c4b/`
- `piolium/tmp/piolium/runs/l1-2026-07-23T22-09-31-996Z-a3-85918699/`
- `piolium/tmp/piolium/runs/l1-2026-07-23T22-09-31-996Z-a3-c116460c/`
- `piolium/tmp/piolium/runs/l1-2026-07-23T22-09-31-996Z-a4-232800d8/`
- `piolium/tmp/piolium/runs/l1-2026-07-23T22-09-31-996Z-a4-25e1d7d7/`
- `piolium/tmp/piolium/runs/l1-2026-07-23T22-09-31-996Z-a4-9c87e09c/`
- `piolium/tmp/piolium/runs/l1-2026-07-23T22-09-31-996Z-a4-9fe366d4/`
- `piolium/tmp/piolium/runs/l1-2026-07-23T22-09-31-996Z-a4-d8e10a1b/`
- `piolium/tmp/piolium/runs/l1-2026-07-23T22-09-31-996Z-a4-f2776513/`
- `piolium/tmp/piolium/runs/l1-2026-07-23T22-09-31-996Z-a4-f96a8e43/`
- `piolium/tmp/piolium/runs/l1-2026-07-23T22-09-31-996Z-a5-143736eb/`
- `piolium/tmp/piolium/runs/l1-2026-07-23T22-09-31-996Z-a5-2acc300d/`
- `piolium/tmp/piolium/runs/l1-2026-07-23T22-09-31-996Z-a5-4d818dec/`
- `piolium/tmp/piolium/runs/l1-2026-07-23T22-09-31-996Z-a5-6ac1861b/`
- `piolium/tmp/piolium/runs/l1-2026-07-23T22-09-31-996Z-a5-6b815c70/`
- `piolium/tmp/piolium/runs/l1-2026-07-23T22-09-31-996Z-a5-cb23a470/`
- `piolium/tmp/piolium/runs/l1-2026-07-23T22-09-31-996Z-a5-e8a642cb/`
- `piolium/tmp/piolium/runs/l1-2026-07-23T22-09-31-996Z-a6-34f4f217/`
- `piolium/tmp/piolium/runs/l1-2026-07-23T22-09-31-996Z-a6-5823a1d0/`
- `piolium/tmp/piolium/runs/l1-2026-07-23T22-09-31-996Z-a6-965dae0f/`
- `piolium/tmp/piolium/runs/l1-2026-07-23T22-09-31-996Z-a6-b97e295a/`
- `piolium/tmp/piolium/runs/l1-2026-07-23T22-09-31-996Z-a6-bd3af084/`
- `piolium/tmp/piolium/runs/l1-2026-07-23T22-09-31-996Z-a6-f02d3704/`
- `piolium/tmp/piolium/runs/l1-2026-07-23T22-09-31-996Z-a6-f56b85f7/`
- `piolium/tmp/piolium/runs/smoke-2026-07-23T22-19-47-667Z-a1-87f19440/`
- `piolium/tmp/piolium/runs/smoke-2026-07-23T22-19-53-952Z-a2-cb508976/`
- `piolium/tmp/piolium/runs/smoke-2026-07-23T22-20-05-286Z-a3-d7f1e2f9/`
- `piolium/tmp/piolium/runs/smoke-2026-07-23T22-20-26-746Z-a4-80965be5/`
- `piolium/tmp/piolium/runs/smoke-2026-07-23T22-39-54-076Z-a1-4e20a2c2/`
- `piolium/tmp/piolium/runs/smoke-2026-07-23T22-40-00-458Z-a2-942af5b0/`
- `piolium/tmp/piolium/runs/smoke-2026-07-23T22-40-11-453Z-a3-21fcc569/`
- `piolium/tmp/piolium/runs/smoke-2026-07-23T22-40-32-443Z-a4-e2139047/`
- `security-policies/`
- `security-policies/.github/workflows/`
- `security-policies/opa-guardrails/data/`
- `security-policies/opa-guardrails/policies/`
- `security-policies/policy/`
- `security-policies/policy/rules/`
- `src/main/java/com/example/agent/model/`
- `src/main/java/com/example/agent/skill/`
- `src/main/java/com/example/demo/`
- `src/main/java/com/example/demo/config/`
- `src/main/java/com/example/demo/security/`
- `src/main/resources/`

### Excluded from scan (6217 files)

**Folders** (matched `exclude_dirs`):

- `.venv/` — 5605 files
- `.git/` — 443 files
- `build/` — 73 files
- `bin/` — 41 files
- `output/` — 16 files
- `security-policies/opa-guardrails/tests/` — 8 files
- `src/test/` — 7 files
- `.vscode/` — 1 files

**File types** (matched `exclude_exts`):

- `*.bin` — 7 files
- `*.lock` — 4 files
- `*.log` — 1 files
- `*.jpg` — 1 files
- `*.jar` — 1 files

**Patterns** (matched `exclude_globs`):

- `**/.gitignore` — 2 files
- `**/.DS_Store` — 1 files
- `**/.gitmodules` — 1 files
- `**/.gitattributes` — 1 files

**Symlinks** (target resolves outside the repo — not followed):

- `.venv/bin/𝜋thon`
- `.venv/bin/python3`
- `.venv/bin/python`
- `.venv/bin/python3.14`
