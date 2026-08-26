# 0013. Align Docker Base Images with Java 25

* Status: Accepted
* Deciders: Development Team
* Date: 2026-08-26

## Technical Story
* Issue: Following the upgrade to Java 25 in `build.gradle`, GitHub Actions container builds failed during `docker/build-push-action` (`./gradlew build -x test --no-daemon`) with the error:
  `Cannot find a Java installation on your machine ... matching: {languageVersion=25, vendor=any vendor, implementation=vendor-specific, nativeImageCapable=false}. Toolchain download repositories have not been configured.`

## Context and Problem Statement
When Gradle compiles the project with `toolchain { languageVersion = JavaLanguageVersion.of(25) }`, it expects a JDK 25 environment or auto-provisioned toolchain repositories. The `Dockerfile` multi-stage build previously used `eclipse-temurin:21-jdk-alpine` as the builder and `eclipse-temurin:21-jre-alpine` as the runtime.

Because toolchain auto-provisioning was not configured inside the isolated Docker build context, Gradle failed during task `:compileJava`. Furthermore, running a Java 25 compiled application on a Java 21 JRE runtime base image would result in `UnsupportedClassVersionError`.

## Decision Drivers
* Build Reliability: Ensure containerized Gradle builds succeed consistently across local environments and CI/CD pipelines (GitHub Actions buildx).
* Toolchain Consistency: Guarantee that both the builder JDK image and runtime JRE image match the configured Java 25 language specification.
* Container Footprint: Maintain lightweight Alpine-based container images for both build and runtime stages.

## Decision Outcome
* Updated `Dockerfile` builder image to `eclipse-temurin:25-jdk-alpine`.
* Updated `Dockerfile` runtime image to `eclipse-temurin:25-jre-alpine`.
* Verified that multi-stage builds and packaging pass cleanly inside Docker.

### Positive Consequences
* GitHub Actions `docker/build-push-action` workflow runs successfully without missing toolchain errors.
* Container runtime executes Java 25 bytecode natively without runtime class version mismatch.
* Preserves Alpine-based security hardening, non-root user setup, and Python integration inside the final image.

### Negative Consequences
* None.
