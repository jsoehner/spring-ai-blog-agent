# Finding H2: Researcher SSRF

**Severity:** HIGH
**Status:** VALID

## Description
The `Researcher` agent uses `Jsoup` to fetch content but lacks any egress filtering. This allows an attacker to use the agent to probe internal network ranges or fetch sensitive cloud metadata.

## Exploit Scenario
A user prompts the researcher: "Research the status of the internal metadata service at `http://169.254.169.254/latest/meta-data/`."
The agent successfully fetches the metadata from the cloud provider and returns it to the user, potentially exposing IAM credentials or other sensitive instance data.

## Remediation
Implement an application-level blocklist for private IP ranges (RFC 1918) and cloud metadata service IPs (e.g., 169.254.169.254) in the `Researcher` agent's request handler.
