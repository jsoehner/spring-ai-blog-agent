# Security Audit Summary

This project underwent a comprehensive security audit using the `piolium` audit framework.

## Summary of Findings
The audit identified two high-severity vulnerabilities:

### H1: Tool Schema Fragility (High)
- **Description:** The `OpaGuardrailAspect` uses manual mapping for tool arguments, allowing attackers to bypass path-based OPA checks by using non-standard JSON keys.
- **Status:** Validated.
- **Remediation:** Implement a schema-driven argument mapper to ensure all tool inputs are explicitly validated against a contract before reaching the policy engine.

### H2: Researcher SSRF (High)
- **Description:** The `Researcher` agent lacks egress filtering, allowing it to be used to probe internal network ranges or cloud metadata services.
- **Status:** Validated.
- **Remediation:** Implement an application-level blocklist for private IP ranges (RFC 1918) and cloud-provider metadata IPs.

## Audit Artifacts
The full audit report and detailed findings can be found in the `piolium/` directory.
