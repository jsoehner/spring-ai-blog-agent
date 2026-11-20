# Build & Functional Verification Report

- **Date:** 2026-11-20
- **Verified By:** Jeffs' Automated build and deploy agent
- **Repository:** `jsoehner/spring-ai-blog-agent`
- **Runtime / Toolchain:** OpenJDK `25.0.5` (`JavaLanguageVersion.of(25)`), Gradle `9.9.1`, Spring Boot `4.1.2`, Spring AI `2.0.2`
- **Overall Status:** VERIFIED

---

## 1. Trusted OSS Split-Horizon Configuration

- Configured split-horizon Trusted OSS Harbor registry (`virtual` mode):
  - **Local LAN:** `TRUSTED_OSS_CACHE_URL=http://oss.jsigroup.local:8080` (stored in local gitignored `.env`)
  - **GitHub Actions (WAN):** `TRUSTED_OSS_CACHE_URL=https://jsigroup-oss.duckdns.org`, `TRUSTED_OSS_MODE=virtual`, `TRUSTED_OSS_GPG_EMAIL=oss-signing@the-techy.com`, plus mTLS client certificate/key secrets (`OSS_MTLS_CLIENT_CERT`, `OSS_MTLS_CLIENT_KEY`).
- Verified `.gitignore` excludes `.env`, `.mvn/`, `uv.toml`, `mtls-pki/`, `*.key`, and `*.p12` so LAN-only URLs and credentials are never committed.
- **Gradle 9.9.1 Note:** `build.gradle` resolves dependencies via `${TRUSTED_OSS_CACHE_URL}/maven/${TRUSTED_OSS_MODE}`. Because Gradle 9.9.1 blocks `http://` Maven repositories unless explicitly opted in, local LAN HTTP builds used a temporary Gradle init script setting `allowInsecureProtocol = true` for `http://oss.jsigroup.local:8080`.

---

## 2. Build & Run Commands

### Clean Compile & Unit Tests
```bash
set -a && source .env && set +a
./gradlew clean test --no-daemon --init-script /tmp/gradle-allow-insecure.gradle
```
- **Outcome:** `BUILD SUCCESSFUL` (6 actionable tasks executed).

### Executable JAR Build
```bash
./gradlew bootJar --no-daemon --init-script /tmp/gradle-allow-insecure.gradle
```
- **Outcome:** `BUILD SUCCESSFUL` (`build/libs/demo-0.0.1-SNAPSHOT.jar`, ~61 MB).

### Runtime Service Verification
```bash
java --enable-native-access=ALL-UNNAMED -jar build/libs/demo-0.0.1-SNAPSHOT.jar \
  --server.port=18081 \
  --spring.profiles.active=supervisor \
  --spring.autoconfigure.exclude= \
  --spring.ai.ollama.base-url=http://192.168.100.190:11434 \
  --spring.ai.ollama.chat.options.model=qwen3.5:9b \
  --opa.url=http://localhost:8181/v1/data/blog \
  --spring.rabbitmq.listener.simple.auto-startup=false \
  --management.health.rabbit.enabled=false \
  --management.endpoints.web.exposure.include=health,info,prometheus
```
- **Outcome:** `SpringAiProjectApplication` started cleanly on Tomcat `11.0.29` (port `18081`, profile `supervisor`) and was cleanly stopped after verification.

---

## 3. Functional Verification Results

### Unit & Component Tests (`./gradlew test`)
- **Total:** `41` tests — **`40` passed**, **`0` failed**, **`0` errors**, **`1` skipped** (`RunLiveBlogGenerationTest`, live LLM integration test disabled by default).
- Verified test suites:
  - `BlogAgentControllerTest` (3/3)
  - `SpringAiProjectApplicationTests` (1/1)
  - `TlsScannerToolTest` (1/1)
  - `WordPressToolTest` (2/2)
  - `WebCrawlerConfigTest` (3/3)
  - `OpaGuardrailAcceptanceTest` (6/6)
  - `OpaGuardrailAspectTest` (5/5)
  - `AgentOrchestratorTest` (2/2)
  - `MarkdownSanitizerTest` (8/8)
  - `SentenceDeduplicatorTest` (3/3)
  - `StorageServiceTest` (3/3)
  - `TextHumanizerProcessorTest` (3/3)

### API / Actuator Verification (`curl` via `api_verify.py`)
| Endpoint | Method | HTTP Status | Content-Type | Response Preview |
| :--- | :--- | :---: | :--- | :--- |
| `/actuator/health` | `GET` | `200` | `application/vnd.spring-boot.actuator.v3+json` | `{"groups":["liveness","readiness"],"status":"UP"}` |
| `/actuator/info` | `GET` | `200` | `application/vnd.spring-boot.actuator.v3+json` | `{}` |
| `/actuator/prometheus` | `GET` | `200` | `text/plain;version=0.0.4;charset=utf-8` | Prometheus metrics (`application_ready_time_seconds`, etc.) |

### Web UI Verification
- Not applicable (headless multi-agent REST API & RabbitMQ microservice without static web UI assets).
