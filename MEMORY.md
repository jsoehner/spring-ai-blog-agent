# Project Memory

## 📖 Project Context
This project builds an offline EJBCA root Certificate Authority (CA) backed by a Hardware Security Module (HSM). It issues subordinate CA certificates to Microsoft Active Directory Certificate Services (AD CS) and a secondary EJBCA instance for ACME (Automated Certificate Management Environment) for automatic certificate enrollment in the JSIGROUP environment.

## 🎯 Current Objectives
- [ ] Setup and test the ACME subCA.
- [ ] Ensure ACME CA is successfully orchestrated and validated for certificate issuance.

## 🧠 Key Decisions & Architecture
- **2026-06-10 - Project Memory**: Initialized MEMORY.md to maintain project context and user preferences over time.
- **Phase 2 Completion**: EJBCA SubCA profiles were configured, and ACME SubCA was orchestrated via offline root CA.

## 👤 User Preferences
- Follow established bash scripts for environment setup and lifecycle management.
- Keep MEMORY.md updated after significant architecture decisions and milestones.

## 📝 Unresolved Issues / Gotchas
- The ACME instance setup can take time to deploy `ejbca.ear` and bind the management interface.
- **EJBCA 9.x Community Edition vs ACME**: EJBCA CE removed native ACME support. A third-party proxy named **Serles** is required to bridge ACME requests to the EJBCA SOAP/REST API.
- **WildFly 30 + EJBCA 9.3.7 JSF 4.0 Bug**: The EJBCA "Edit End Entity Profile" Admin UI crashes (`ClassCastException: String to Integer` at `MenuRenderer`) on WildFly 30 due to JSF 4.0 strictness. We must use the default `EMPTY` and `ENDUSER` profiles instead of creating new ones.
- **Serles/Zeep WSDL Bug**: WildFly rewrites the EJBCA WSDL endpoint address to `localhost:8080`. Python's Zeep strictness requires patching `self.ejbca_service = self.client.create_service(...)` in Serles to forcefully override the WSDL address and bind to `https://127.0.0.1:8443`.
- **Systemd SELinux Policies**: Systemd cannot execute binaries from user home directories (`user_home_t`). The Serles `venv` must be installed in `/opt/serles-venv` to run successfully as a service.
- **EJBCA CLI End Entity Workaround**: When cross-signing SubCAs with strict `OU` or `O` DN fields, `ejbca.sh ra addendentity` will fail because `EMPTY` requires authorization and the custom profiles (e.g. `ADCS2025_SubCA_EE_Profile`) restrict `OU`. EJBCA web interface `Edit End Entity Profile` crashes (`WELD-000049` `postConstruct`) on WildFly 30.
- **Native HSM Signing Workaround**: To bypass EJBCA's rigid End Entity profile DN parsing, we sign the CSR natively using `certtool` interfacing with the Nitrokey HSM PKCS#11 module (`--provider /usr/lib64/opensc-pkcs11.so --load-ca-privkey "pkcs11:token=SmartCard-HSM;object=root-ca-key-prod-v2;type=private"`), and then import the generated certificate into EJBCA using `ejbca.sh ca importcert` under the `EMPTY`/`ENDUSER` profiles.
