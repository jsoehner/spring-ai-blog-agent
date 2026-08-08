# ADR-0004: Dependency Updates and Gradle Build Configuration Fixes

## Status
Accepted

## Date
2026-08-05

## Context
During maintenance and local environment testing of the Spring AI Blog Agent, several issues prevented successful compilation, dependency updates, and containerization:
1. **Broken Dependency Check Plugin ID**: The OWASP dependency check plugin was configured with the incorrect ID `org.owasp.dependency-check` instead of `org.owasp.dependencycheck` in `build.gradle`.
2. **Deprecated Dependency Check Properties**: The `build.gradle` configured `nvdData_path` and the nested `reports` DSL, which are deprecated and absent in version 9.0.9+, failing the build configuration phase.
3. **Missing Versions Plugin**: The Gradle versions plugin was not applied to `build.gradle`, preventing the local execution of `./gradlew dependencyUpdates` to dynamically evaluate new packages.
4. **Syntax/Compilation Errors**: 
   - [AutoDraftService.java](file:///Users/jsoehner/spring-ai-blog-agent/src/main/java/com/example/demo/AutoDraftService.java) had an extra closing brace `}` at the end of the file.
   - [OpaGuardrailAspect.java](file:///Users/jsoehner/spring-ai-blog-agent/src/main/java/com/example/demo/security/OpaGuardrailAspect.java) referenced the `request` Map in `input.put(...)` before declaring the `request` variable.
5. **Outdated Dependencies**: Recommended dependency updates from the report needed to be applied to ensure the system is up-to-date.

## Decision
1. **Gradle Build Hardening**: 
   - Corrected the plugin ID to `org.owasp.dependencycheck` in `build.gradle`.
   - Updated properties to be version 9.0.9+ compatible: replaced `nvdData_path` with `data.directory`, removed the `reports` DSL, and set `outputDirectory` and `format` directly.
   - Applied the `com.github.ben-manes.versions` plugin version `0.54.0` to `build.gradle`.
2. **Codebase Correction**:
   - Removed the extra closing brace at the end of [AutoDraftService.java](file:///Users/jsoehner/spring-ai-blog-agent/src/main/java/com/example/demo/AutoDraftService.java).
   - Rearranged variable declarations in [OpaGuardrailAspect.java](file:///Users/jsoehner/spring-ai-blog-agent/src/main/java/com/example/demo/security/OpaGuardrailAspect.java) to declare `request` prior to placing it in the `input` map.
3. **Dependency Upgrades**:
   - Executed `.venv/bin/python3 .github/scripts/update-dependencies.py` to automatically update `build.gradle`.
   - Upgraded Spring Boot to `4.1.0` (as recommended by the report), `jsoup` to `1.23.1`, and `metadata-extractor` to `2.21.0`.
4. **Docker Validation**:
   - Ran `./run-and-submit.sh --build "AI Security"` to verify that the build succeeds inside the multi-stage Docker environment and that the local containers orchestrate and process queued requests correctly.

## Alternatives Considered
* **Manual Dependency Tracking**: Highly error-prone and doesn't scale. Using the automated python update script in tandem with the versions plugin ensures consistency and accuracy.

## Consequences
* Compilation succeeds reliably.
* Gradle task builds and tests are fully functional on local environments and CI/CD pipelines.
* Dockerized microservices successfully coordinate and execute content generation.
