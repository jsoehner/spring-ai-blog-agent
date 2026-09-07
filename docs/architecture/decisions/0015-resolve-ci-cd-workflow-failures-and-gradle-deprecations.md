# 0015. Resolve CI/CD Workflow Failures, Gradle 10 Deprecations, and Dependency Upgrades

* Status: Accepted
* Deciders: Development Team
* Date: 2026-09-07

## Technical Story
* Issue: The repository experienced recurring GitHub Actions workflow failures and deprecation warnings:
  1. `nightly-dependency-update.yml` failed every night during PR creation with Git error exit code 128 (`could not read Username for 'https://github.com': No such device or address`) due to an invalid/expired `PERSONAL_ACCESS_TOKEN` in `PR_TOKEN`, causing false alarm issue alerts (#131, #132) while leaving the origin branch stale.
  2. `nightly-dependency-update.yml` used Java 21 (`java-version: '21'`) and `actions/setup-java@v5` instead of Java 25 and Node 24 native action versions.
  3. `security-scan.yml` raised Node 20 deprecation warnings on `actions/setup-python@v5` and failed to download the secret scanning artifact (`Artifact not found for name: gitleaks-report`) because `gitleaks-action@v3` produces SARIF format (`gitleaks-results.sarif`).
  4. `build.gradle` triggered Gradle 10 deprecation warnings due to Groovy space syntax on `maven { url '...' }`.
  5. Spring Boot had a verified patch update from 4.1.0 to 4.1.1.
  6. Python bytecode cache directories (`__pycache__/`, `*.pyc`) were not ignored in `.gitignore`.

## Context and Problem Statement
Routine repository maintenance identified recurring open issues and CI/CD workflow warnings:
* **Nightly PR Failures:** `peter-evans/create-pull-request` failed during `git push` because `token` was evaluating `env.PR_TOKEN || github.token` where `PR_TOKEN` referenced an expired PAT repository secret. Consequently, the automated update branch was never pushed, and fallback issues were created nightly with stale links.
* **Java and Runner Version Drift:** The repository had moved to Java 25 (`JavaLanguageVersion.of(25)`), but the nightly workflow still configured Java 21. Furthermore, GitHub Actions runners enforcing Node 24 flagged deprecated action versions.
* **Security Scan Artifact Mismatch:** The gitleaks scan job attempted to generate and upload `gitleaks-report.json` via unsupported `GITLEAKS_ARGS`, while `gitleaks-action@v3` outputs `gitleaks-results.sarif` by default. Downstream reporting failed to download `gitleaks-report` and `parse-findings.js` could not parse SARIF.
* **Gradle 10 Incompatibility:** Space assignment `url 'https://repo.spring.io/milestone'` triggered deprecation warnings.

## Decision Drivers
* Clean, green CI/CD runs with zero deprecation warnings under Node 24 and Gradle 10.
* Ensure automated dependency updates reliably push the update branch and authenticate using workflow permissions.
* Seamless secret scan reporting supporting both SARIF and JSON format.
* Maintain parity across Java 25 toolchains in CI, Docker, and local Gradle.

## Decision Outcome
1. **GitHub Workflows Hardening:**
   - Updated `nightly-dependency-update.yml` to use `actions/setup-java@dd06d9cba3e5552c54d9f8ea23572deb30010f7c # v6.0.0` with `java-version: '25'`.
   - Replaced fragile PAT environment token fallback in `create-pull-request` with `token: ${{ secrets.GITHUB_TOKEN }}`, leveraging the workflow's explicit `contents: write` and `pull-requests: write` permissions.
   - Upgraded `actions/setup-python` in `security-scan.yml` to `actions/setup-python@5fda3b95a4ea91299a34e894583c3862153e4b97 # v7.0.0` to run natively on Node 24.
   - Updated artifact naming in `security-scan.yml` to download `gitleaks-results.sarif` uploaded by `gitleaks-action@v3` and removed the redundant json upload step.
   - Updated `parse-findings.js` to parse both SARIF (`results.sarif` / `gitleaks-results.sarif`) and legacy JSON reports.
2. **Gradle & Build Configuration:**
   - Corrected property assignment in `build.gradle` to `maven { url = 'https://repo.spring.io/milestone' }`, eliminating all Gradle deprecation warnings.
   - Upgraded `org.springframework.boot` plugin to `4.1.1`.
3. **Repository Cleanliness:**
   - Added `__pycache__/` and `*.pyc` to `.gitignore`.

### Positive Consequences
* All Gradle builds and test suites pass cleanly with 0 deprecations.
* GitHub Actions security scan and nightly dependency workflows execute with full Node 24 compliance and reliable artifact transfer.
* Automated dependency update branches can be pushed and verified without requiring external PAT secret renewal.
