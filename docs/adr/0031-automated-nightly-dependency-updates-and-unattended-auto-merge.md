# ADR 0031: Automated Nightly Dependency Updates and Unattended Auto-Merge Governance

* **Status:** Accepted
* **Deciders:** Spring AI Blog Agent Engineering Team
* **Date:** 2026-09-27

---

## 1. Context & Business Problem Statement

The repository runs an automated nightly dependency update workflow (`.github/workflows/nightly-dependency-update.yml`) scheduled at 02:00 UTC (~03:00 AM local time). The workflow executes `./gradlew clean build`, updates dependencies, and creates a pull request via `peter-evans/create-pull-request`.

Previously, pull requests were created using `token: ${{ secrets.GITHUB_TOKEN }}` and explicitly required human review via `reviewers: "jsoehner"`. This caused two significant issues:
1. Under GitHub Actions security rules, pull requests created by the default `GITHUB_TOKEN` do not trigger secondary `pull_request` workflows or fail during workflow registration.
2. The mandatory reviewer assignment blocked automated merging, requiring human manual review at midday (~12:00 PM) rather than merging unattended during the 3:00 AM window.

---

## 2. Decision Drivers

1. **Unattended Execution**: Verified automated dependency updates must be automatically approved and squash-merged at 3:00 AM without human intervention.
2. **Reliable Token Resolution**: Dynamically verify `PERSONAL_ACCESS_TOKEN` against the GitHub API and fallback to `GITHUB_TOKEN` if invalid or unset.
3. **Reviewer Decoupling**: Remove `reviewers: "jsoehner"` to eliminate reviewer lock, while retaining `assignees: "jsoehner"` for visibility.
4. **Immediate Auto-Merge Trigger**: Invoke `gh pr merge --auto --squash --delete-branch` immediately upon PR creation.

---

## 3. Decision Outcome

1. Added token resolution step (`Resolve PR creation token`) to dynamically validate `PERSONAL_ACCESS_TOKEN` before creating PRs.
2. Removed `reviewers: "jsoehner"` from `.github/workflows/nightly-dependency-update.yml`.
3. Added `Auto-merge Pull Request` step in `nightly-dependency-update.yml` executing `gh pr merge "$PR_URL" --auto --squash --delete-branch || gh pr merge "$PR_URL" --squash --delete-branch`.

---

## 4. Consequences & Verification

- **Positive**: Nightly dependency updates automatically merge upon passing test suites at 3:00 AM without waiting until noon.
- **Positive**: PR creation avoids recursive workflow suppression.
- **Verification**: Verified YAML syntax and workflow compatibility.
