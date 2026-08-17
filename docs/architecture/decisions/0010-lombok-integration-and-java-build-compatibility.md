# 0010. Lombok Integration and Java Build Compatibility

* Status: Accepted
* Deciders: Development Team
* Date: 2026-08-17

## Technical Story
* Issue: Build failure during compilation when running Gradle on host JDK 25 and inside container build step (`RUN ./gradlew build -x test --no-daemon`).

## Context and Problem Statement
When running Gradle builds across host development environments (which may run newer JDK versions like JDK 25) and Docker multi-stage build containers (`eclipse-temurin:21-jdk-alpine`), several compilation and annotation-processing errors were encountered:
1. Lombok annotations (`@Slf4j`, `@Data`, `@NoArgsConstructor`, `@AllArgsConstructor`) failed due to Lombok missing from `build.gradle` annotation processor configurations, resulting in Lombok internal errors when compiled under JDK 25 (`java.lang.NoSuchFieldException: com.sun.tools.javac.code.TypeTag :: UNKNOWN`).
2. Package import errors existed where `WordPressTool` was imported from `com.example.demo.tools` instead of `com.example.demo`.
3. `StorageService` declared cyclic inheritance by implementing itself (`public class StorageService implements StorageService`).
4. Missing imports (`RabbitConfig`, `RestTemplate`, `List`) and missing constructors in `PromptTemplate` caused build failures.
5. Unhandled checked `IOException` when reading prompt resource byte streams in `AgentOrchestrator`.

## Decision Drivers
* Build Reliability: Ensure reproducible, clean builds in both containerized environments (`eclipse-temurin:21-jdk-alpine`) and host developer environments.
* Lombok Compatibility: Enable full Lombok code-generation support via official Gradle plugin configuration.

## Considered Options
* Option 1: Manually write all getters/setters/loggers and remove Lombok.
* Option 2: Add `io.freefair.lombok` plugin and explicit Lombok dependencies to `build.gradle`, enforce JDK 21 toolchain for Gradle, and fix source-level import/type errors.

## Decision Outcome
Chosen option: Option 2, because it preserves developer productivity with Lombok while resolving build errors cleanly across containerized and host environments.

### Positive Consequences
* Gradle builds succeed cleanly in both Docker multi-stage container builds and local execution.
* Lombok annotation processing is properly configured via `io.freefair.lombok` plugin (`version 8.12.2`).
* Explicit source imports and constructor generation prevent compilation breakages.

### Negative Consequences
* Build configuration depends on the Freefair Lombok Gradle plugin for annotation processor wiring.

## Pros and Cons of Options

### Option 2 (Chosen)
* Good, because `io.freefair.lombok` ensures annotation processors run seamlessly during javac invocation.
* Good, because strict Java 21 toolchain specification guarantees compilation consistency across host and container environments.
* Bad, because adding a plugin introduces a minor extra Gradle plugin dependency.
