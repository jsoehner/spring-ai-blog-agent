# Finding H1: Tool Schema Fragility

**Severity:** HIGH
**Status:** VALID

## Description
The `OpaGuardrailAspect` class in `com.example.demo.security` uses a manual mapping strategy to flatten tool arguments for OPA evaluation. Because it uses an `if-else` chain to map specific objects (like `WriteRequest` or `MoveRequest`), any other object type provided by the LLM will be mapped to a generic `argN` key. The OPA policy, which looks for `writeRequest.absolutePath`, will find no such key in the `argN` input and may default to "Allow" if not explicitly denied.

## Exploit Scenario
An attacker provides a tool call with a custom object key:
`writeFile(custom_payload={"absolutePath": "/etc/shadow", "content": "..."})`

The `flattenArguments` method maps this to `arg0`. The OPA policy, looking for `writeRequest.absolutePath`, will see nothing and may default to ALLOW.

## Remediation
Replace the manual mapping in `flattenArguments` with a schema-based validation that ensures all tool arguments are correctly mapped to their expected types and constraints.
