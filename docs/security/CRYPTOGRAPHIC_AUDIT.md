## 🛡️ Cryptographic Bill of Materials (CBOM) & PQC Migration Assessment

**Format**: CycloneDX (v1.7) | **First-Party Code Crypto Assets**: 0 | **Total Tracked Crypto Assets**: 6

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
| **Third-Party Supply Chain** | **7.8%** (10 of 129 dependencies cataloged) | 🔴 LOW (Known profiles assimilated) |
| **Overall Audit Confidence Score** | **8.5%** | **🔴 LOW** (119 unassimilated supply chain dependencies) |

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
| `spring-boot-starter-jackson` | 4.1.1 | `pkg:maven/org.springframework.boot/spring-boot-starter-jackson@4.1.1` | 🟡 Unassimilated (No upstream CBOM) |
| `spring-boot-starter` | 4.1.1 | `pkg:maven/org.springframework.boot/spring-boot-starter@4.1.1` | 🟡 Unassimilated (No upstream CBOM) |
| `spring-boot-starter-logging` | 4.1.1 | `pkg:maven/org.springframework.boot/spring-boot-starter-logging@4.1.1` | 🟡 Unassimilated (No upstream CBOM) |
| `logback-classic` | 1.5.38 | `pkg:maven/ch.qos.logback/logback-classic@1.5.38` | 🟡 Unassimilated (No upstream CBOM) |
| `logback-core` | 1.5.38 | `pkg:maven/ch.qos.logback/logback-core@1.5.38` | 🟡 Unassimilated (No upstream CBOM) |
| `slf4j-api` | 2.0.18 | `pkg:maven/org.slf4j/slf4j-api@2.0.18` | 🟡 Unassimilated (No upstream CBOM) |
| `log4j-to-slf4j` | 2.25.5 | `pkg:maven/org.apache.logging.log4j/log4j-to-slf4j@2.25.5` | 🟡 Unassimilated (No upstream CBOM) |
| `log4j-api` | 2.25.5 | `pkg:maven/org.apache.logging.log4j/log4j-api@2.25.5` | 🟡 Unassimilated (No upstream CBOM) |
| `jul-to-slf4j` | 2.0.18 | `pkg:maven/org.slf4j/jul-to-slf4j@2.0.18` | 🟡 Unassimilated (No upstream CBOM) |
| `spring-boot-autoconfigure` | 4.1.1 | `pkg:maven/org.springframework.boot/spring-boot-autoconfigure@4.1.1` | 🟡 Unassimilated (No upstream CBOM) |
| `spring-boot` | 4.1.1 | `pkg:maven/org.springframework.boot/spring-boot@4.1.1` | 🟡 Unassimilated (No upstream CBOM) |
| `spring-core` | 7.0.9 | `pkg:maven/org.springframework/spring-core@7.0.9` | 🟡 Unassimilated (No upstream CBOM) |
| `commons-logging` | 1.3.6 | `pkg:maven/commons-logging/commons-logging@1.3.6` | 🟡 Unassimilated (No upstream CBOM) |
| `jspecify` | 1.0.1 | `pkg:maven/org.jspecify/jspecify@1.0.1` | 🟡 Unassimilated (No upstream CBOM) |
| `spring-context` | 7.0.9 | `pkg:maven/org.springframework/spring-context@7.0.9` | 🟡 Unassimilated (No upstream CBOM) |
| *... and 104 more unassimilated dependencies* | | | |
