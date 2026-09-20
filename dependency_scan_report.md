# Dependency Vulnerability Scan Report

## Summary
The project uses several key libraries for Spring AI integration, web serving, and data extraction.

## Findings
| Library | Version | Status | Notes |
| :--- | :--- | :--- | :--- |
| `jackson-databind` | 2.22.2 | OK | Recent version, no known critical CVEs in this minor version. |
| `jsoup` | 1.23.2 | OK | Recent version. |
| `spring-boot-starter-actuator` | (Default) | WARNING | Ensure `management.endpoints.web.exposure.include` is restricted to `health` and `info` in production. |
| `spring-ai-starter-model-ollama` | 2.0.1 | OK | Newer version. |
| `aspectjweaver` | 1.9.25.1 | OK | Standard version. |

## Recommendations
- **Actuator Security**: Audit the `application.properties` to ensure only necessary Actuator endpoints are exposed.
- **Dependency Updates**: Implement a periodic automated check (e.g., Dependabot or Renovate) to keep libraries updated.
- **SBOM**: Generate a Software Bill of Materials (SBOM) for production deployments.
