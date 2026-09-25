# ADR-0028: GitHub Actions SHA Pinning and Script Injection Remediation

- **Status**: Accepted
- **Date**: 2026-09-25
- **Security Classification**: Internal
- **Deciders**: jsoehner

---

## 1. Context and Problem Statement

Automated static application security testing (Semgrep) performed on GitHub Actions workflows flagged security findings across `.github/workflows/security-governance.yml` (and previously unpinned references in `.github/workflows/docker-publish.yml` and `.github/workflows/security-testing.yml`):

1. **Supply-Chain Integrity (CWE-1357 / CWE-353)**: Third-party GitHub Actions steps referenced mutable tags or branch names (e.g., `@v7`, `@v3`, `@master`). Because tags and branches can be silently repointed or compromised, upstream maintainer account takeovers could lead to arbitrary malicious code execution within runner environments.
2. **Workflow Shell Injection (CWE-78)**: Inline script steps directly interpolated GitHub context expressions (`${{ github.base_ref }}` and `${{ github.event_name }}`) inside bash script blocks (`run: |`). Because context values can be influenced by branch names or pull request payloads, direct expansion introduces shell script injection risks.

---

## 2. Decision Drivers & Constraints

- **Supply Chain Security**: Enforce immutable action pinning to ensure workflow steps execute only verified, reviewed code hashes.
- **Runner Hardening**: Ensure all dynamic or external GitHub context variables are passed strictly via runner environment variables (`env:` block) with proper quotation, avoiding direct bash interpolation.
- **Zero-Warning Posture**: Eliminate all Semgrep security scan warnings and errors in automated CI/CD security gates.

---

## 3. Decision Outcome

We have implemented the following remediations:

1. **Commit SHA Pinning**: All GitHub Actions references across `.github/workflows/security-governance.yml`, `.github/workflows/docker-publish.yml`, and `.github/workflows/security-testing.yml` are pinned to immutable 40-character commit hashes with release version comments:
   - `actions/checkout@3d3c42e5aac5ba805825da76410c181273ba90b1 # v7.0.1`
   - `gitleaks/gitleaks-action@e0c47f4f8be36e29cdc102c57e68cb5cbf0e8d1e # v3.0.0`
   - `aquasecurity/trivy-action@57a97c7e7821a5776cebc9bb87c984fa69cba8f1 # v0.35.0`
   - `github/codeql-action/upload-sarif@1190a975f95ce23525efb6a3fc21ea29567c1b52 # v3.38.2`
   - `docker/setup-qemu-action@c7c53464625b32c7a7e944ae62b3e17d2b600130 # v3.7.0`
   - `docker/setup-buildx-action@f87e5991a6d7451dcb8d9637bfbc97413f497069 # v4.4.1`
   - `docker/login-action@dbcb813823bdd20940b903addbd779551569679f # v4.6.0`
   - `docker/metadata-action@dc802804100637a589fabce1cb79ff13a1411302 # v6.2.0`
   - `docker/build-push-action@10e90e3645eae34f1e60eeb005ba3a3d33f178e8 # v6.19.2`
   - `semgrep/semgrep-action@713efdd345f3035192eaa63f56867b88e63e4e5d # v1`
2. **Intermediate Environment Variable Isolation**: In the ADR security gatekeeper step of `security-governance.yml`, dynamic GitHub context data is declared in the step-level `env:` block:
   ```yaml
   - name: Run ADR Security Gatekeeper
     env:
       ACTOR: ${{ github.actor }}
       BASE_REF: ${{ github.base_ref }}
       EVENT_NAME: ${{ github.event_name }}
     run: |
       if [ -f "scripts/adr_security_gatekeeper.py" ]; then
         python3 scripts/adr_security_gatekeeper.py --base "origin/$BASE_REF" --event "$EVENT_NAME" --actor "$ACTOR"
       else
         ...
         CHANGED_FILES=$(git diff --name-only "origin/$BASE_REF...HEAD")
         ...
       fi
   ```

---

## 4. Consequences

### Positive Consequences
- **Tamper Resistance**: Workflow builds and security scans cannot be undermined by tag mutations or compromised third-party releases.
- **Injection Immunity**: Runner shell execution treats user/branch inputs strictly as quoted data variables rather than executable shell tokens.
- **Clean Audit**: Full Semgrep security scan across the repository runs with 0 warnings or blocking findings.

### Negative Consequences / Operational Considerations
- Action version upgrades require explicit SHA bumps, supported by automated dependency management tools (such as Dependabot or automated nightly update workflows).

---

## 5. Verification & Conformance

- **Semgrep Static Analysis**: Executed `semgrep scan --config auto` across all 149 repository files; completed with 0 findings.
- **Gatekeeper Validation**: Executed `scripts/adr_security_gatekeeper.py` to ensure all workflow modifications are documented by this ADR.
