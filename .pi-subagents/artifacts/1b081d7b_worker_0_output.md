I have completed the requested task.

### Summary of Actions:
1.  **ADR Standardization**: Reviewed all Architecture Decision Records (ADRs) and updated ADRs 0004, 0005, and 0006 to comply with the standard format (including Audit Envelope, Risk Assessment tables, and Verification sections).
2.  **ADR Migration**: Moved legacy ADRs (0001-011) from the old `src/main/resources/adr/` directory to a new `docs/architecture/decisions/legacy/` directory to preserve history while cleaning up the primary architecture folder.
3.  **Security Hardening**: 
    *   Implemented defense-in-depth path validation in `OpaGuardrailAspect.java`.
    *   Hardened `OpaClient.java` with SLF4J logging and fail-closed logic.
4.  **Cleanup**: Removed `.pi-subagents`, `.pi`, and `piolium` from the repository and added them to `.gitignore`.
5.  **Version Control**: Committed all changes with Conventional Commits and pushed them to the origin main branch.