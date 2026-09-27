# ADR-0030: Standardize Dual-Engine BOM Pipeline and Eliminate Redundant Security Scan Workflow

- **Status**: Accepted
- **Date**: 2026-09-27
- **Security Classification**: Internal
- **Deciders**: jsoehner

---

## 1. Context and Problem Statement

To adhere to unified repository governance and avoid workflow duplication, CI/CD pipelines must ensure non-overlapping execution paths while providing comprehensive Software Bill of Materials (SBOM) and Cryptographic Bill of Materials (CBOM) capabilities.

An un-unified generic workflow (`.github/workflows/security-scan.yml`) was still present alongside `.github/workflows/security-governance.yml`. In addition, the legacy `.github/workflows/sbom-cbom.yml` required modernization to the Node 24 SHA-pinned `sbom.yml` specification with multi-language semantic AST crypto discovery (`scan_crypto_ast.py`), automated CI test verification (`test_boms.sh`), and PQC readiness scoring (`analyze_cbom.py`).

---

## 2. Decision Outcome

1. **Eliminated Duplicate Security Workflow**: Permanently removed `.github/workflows/security-scan.yml`, preserving `.github/workflows/security-governance.yml` as the sole authoritative security workflow.
2. **Standardized Dual-Engine BOM Pipeline**: Replaced `sbom-cbom.yml` with `.github/workflows/sbom.yml` and deployed the complete BOM script suite (`generate_boms.sh`, `scan_crypto_ast.py`, `analyze_cbom.py`, `test_boms.sh`) into `scripts/`.
3. **Automated Verification Harness**: Enabled pre-flight validation of SBOM and CBOM artifacts in CI via `scripts/test_boms.sh`.
4. **Developer Experience Standardization**: Added `commit-lint.yml` and `changelog.yml` workflows.

---

## 3. Consequences

### Positive Consequences
- **Zero Workflow Redundancy**: Confirmed `security-governance.yml` as the sole scanner for Gitleaks, Semgrep, and Trivy.
- **Cryptographic Visibility**: Post-Quantum Cryptography (PQC) readiness scoring and AST-level discovery are integrated into every release and PR.
- **Validation**: Local BOM generation and validation test harness executed with all checks passing (4/4).

### Operational Considerations
- None.
