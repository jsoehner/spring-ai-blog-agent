## 🛡️ Cryptographic Bill of Materials (CBOM) & PQC Migration Assessment

**Format**: CycloneDX (v1.6) | **First-Party Code Crypto Assets**: 0 | **Total Tracked Crypto Assets**: 6

### 📊 Post-Quantum Migration Scorecard

| Metric | Count | Migration Status |
|---|---|---|
| **Post-Quantum Ready (PQC)** | **0** | 🟢 Quantum-Resistant (NIST FIPS 203/204/205) |
| **Quantum-Vulnerable (Backlog)** | **5** | 🔴 At Risk of 'Harvest Now, Decrypt Later' |
| **Classical Symmetric / Hashing** | **1** | 🟡 Classical Security (Requires AES-256 / SHA-256+) |
| **Asymmetric PQC Migration Progress** | **0.0%** | (0 of 5 asymmetric primitives migrated) |

### 🎯 Cryptographic Supply Chain Coverage & Confidence

| Evaluation Layer | Coverage / Status | Audit Confidence Assessment |
|---|---|---|
| **First-Party Code (`src/`)** | **100% Audited** (0 Custom Primitives) | 🟢 **HIGH** (Direct AST & SAST verified clean) |
| **Third-Party Supply Chain** | **6.3%** (10 of 159 dependencies cataloged) | 🔴 LOW (Known profiles assimilated) |
| **Overall Audit Confidence Score** | **6.9%** | **🔴 LOW** (149 unassimilated supply chain dependencies) |

### ✅ Post-Quantum Cryptography Migrated Assets

> ⚠️ **No Post-Quantum Ready assets detected.** Immediate migration planning recommended for asymmetric key exchanges and digital signatures.

### ⚠️ Quantum-Vulnerable Assets & Remediation Plan

| Component / Algorithm | Type / Primitive | Key Length / Curve | Recommended Target | Provenance / Context |
|---|---|---|---|---|
| **`spring-web (Spring WebClient / RestTemplate HTTPS)`**<br><sub>HTTPS / TLS 1.3</sub> | assimilated-dependency-crypto / protocol | ECDH-X25519 / RSA-2048 | **Ensure target AI endpoints (e.g. Ollama/OpenAI) and client support hybrid post-quantum key encapsulation (ML-KEM-768)** | Assimilated Upstream CBOM: Spring Framework HTTP Client Security Specification<br>`Dependency: spring-web@7.0.9` |
| **`tomcat-embed-core (Tomcat TLS Transport Engine)`**<br><sub>TLS 1.2 / TLS 1.3</sub> | assimilated-dependency-crypto / protocol | RSA-2048 / ECDSA-P256 | **Upgrade to Post-Quantum hybrid key exchange (X25519+ML-KEM-768) via OpenSSL 3.4+ or Java 25 JSSE provider** | Assimilated Upstream CBOM: Apache Tomcat Security Advisory & JSSE Specification<br>`Dependency: tomcat-embed-core@11.0.26` |
| **`spring-webmvc (Spring WebClient / RestTemplate HTTPS)`**<br><sub>HTTPS / TLS 1.3</sub> | assimilated-dependency-crypto / protocol | ECDH-X25519 / RSA-2048 | **Ensure target AI endpoints (e.g. Ollama/OpenAI) and client support hybrid post-quantum key encapsulation (ML-KEM-768)** | Assimilated Upstream CBOM: Spring Framework HTTP Client Security Specification<br>`Dependency: spring-webmvc@7.0.9` |
| **`amqp-client (RabbitMQ AMQPS Transport)`**<br><sub>TLS 1.2 / TLS 1.3 (JSSE)</sub> | assimilated-dependency-crypto / protocol | RSA-2048 / ECDSA-P256 | **Configure TLS 1.3 hybrid post-quantum cipher suites in RabbitMQ broker & client connection factory** | Assimilated Upstream CBOM: RabbitMQ Transport Security Specification<br>`Dependency: amqp-client@5.37.0` |
| **`spring-webflux (Spring WebClient / RestTemplate HTTPS)`**<br><sub>HTTPS / TLS 1.3</sub> | assimilated-dependency-crypto / protocol | ECDH-X25519 / RSA-2048 | **Ensure target AI endpoints (e.g. Ollama/OpenAI) and client support hybrid post-quantum key encapsulation (ML-KEM-768)** | Assimilated Upstream CBOM: Spring Framework HTTP Client Security Specification<br>`Dependency: spring-webflux@7.0.9` |

### 🔒 Classical Symmetric & Digest Assets

| Component Name | Primitive | Key Length | Quantum Resistance Assessment | Provenance / Location(s) |
|---|---|---|---|---|
| `tomcat-embed-core (Tomcat TLS Symmetric Cipher Suites)` | block-cipher | 256 | Quantum-Resistant (Grover's proof) | Assimilated Upstream CBOM: Apache Tomcat Security Advisory & JSSE Specification<br>`Dependency: tomcat-embed-core@11.0.26` |

### ⚠️ Unassimilated Third-Party Binaries & Cryptographic Blind Spots

> ℹ️ *The following third-party dependencies do not have verified upstream CBOM attestations in the catalog. They lower the audit confidence score until explicit CBOMs or attestations are published.* 

| Dependency Name | Version | Package URL (purl) | Status |
|---|---|---|---|
| `actions/checkout` | v7 | `pkg:github/actions/checkout@v7` | 🟡 Unassimilated (No upstream CBOM) |
| `actions/checkout` | v7.0.1 | `pkg:github/actions/checkout@v7.0.1` | 🟡 Unassimilated (No upstream CBOM) |
| `actions/checkout` | v7.0.1 | `pkg:github/actions/checkout@v7.0.1` | 🟡 Unassimilated (No upstream CBOM) |
| `actions/checkout` | v7.0.1 | `pkg:github/actions/checkout@v7.0.1` | 🟡 Unassimilated (No upstream CBOM) |
| `actions/checkout` | v7.0.1 | `pkg:github/actions/checkout@v7.0.1` | 🟡 Unassimilated (No upstream CBOM) |
| `actions/github-script` | v9.0.0 | `pkg:github/actions/github-script@v9.0.0` | 🟡 Unassimilated (No upstream CBOM) |
| `actions/setup-java` | v6.0.1 | `pkg:github/actions/setup-java@v6.0.1` | 🟡 Unassimilated (No upstream CBOM) |
| `actions/setup-java` | v6.0.1 | `pkg:github/actions/setup-java@v6.0.1` | 🟡 Unassimilated (No upstream CBOM) |
| `actions/setup-python` | v5.6.0 | `pkg:github/actions/setup-python@v5.6.0` | 🟡 Unassimilated (No upstream CBOM) |
| `actions/setup-python` | v7.0.0 | `pkg:github/actions/setup-python@v7.0.0` | 🟡 Unassimilated (No upstream CBOM) |
| `actions/upload-artifact` | v4.6.2 | `pkg:github/actions/upload-artifact@v4.6.2` | 🟡 Unassimilated (No upstream CBOM) |
| `anchore/sbom-action` | v0.24.2 | `pkg:github/anchore/sbom-action@v0.24.2` | 🟡 Unassimilated (No upstream CBOM) |
| `aquasecurity/trivy-action` | v0.35.0 | `pkg:github/aquasecurity/trivy-action@v0.35.0` | 🟡 Unassimilated (No upstream CBOM) |
| `cbomkit/cbomkit-action` | v2.3.0 | `pkg:github/cbomkit/cbomkit-action@v2.3.0` | 🟡 Unassimilated (No upstream CBOM) |
| `docker/build-push-action` | v6.19.2 | `pkg:github/docker/build-push-action@v6.19.2` | 🟡 Unassimilated (No upstream CBOM) |
| *... and 134 more unassimilated dependencies* | | | |
