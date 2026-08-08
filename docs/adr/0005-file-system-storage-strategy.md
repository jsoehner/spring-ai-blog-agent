# ADR-005: File System & Storage Strategy

## Status
Proposed

## Context
Currently, file system operations (saving blog posts, path sanitization) are handled directly within the services using `java.nio.file.Files` and `java.nio.file.Paths`. This leads to:
- **Code Duplication**: Path sanitization and directory checking are repeated in multiple places.
- **Hardcoded Paths**: The "output" directory is hardcoded.
- **Lack of Flexibility**: It's difficult to switch from local file storage to cloud storage (e.g., AWS S3, Google Cloud Storage) without refactoring multiple services.
- **Security Risks**: Manual path traversal checks are error-prone.

## Decision
We will introduce a `StorageService` to abstract all file system and storage operations. This service will provide a unified API for saving files, checking existence, and ensuring safe paths.

The implementation will initially support local file system storage but will be designed with an interface that allows for easy integration of cloud storage providers in the future.

## Implementation Plan
1. **Define StorageService Interface**: Create a `StorageService` interface with methods for saving content, retrieving content, and listing files.
2. **Implement LocalStorageService**: Create a `LocalStorageService` implementation that handles local file system operations, including path sanitization and directory creation.
3. **Refactor Services**: Replace direct `java.nio.file` calls in `BlogAgentService` and `AutoDraftService` with calls to `StorageService`.
4. **Configuration**: Externalize the base storage path.

## Consequences
- **Pros**:
    - **Centralized Logic**: Path sanitization and directory management are handled in one place.
    9. **Easier Migration**: Switching to cloud storage becomes a matter of swapping the implementation.
    - **Better Security**: Centralized and robust path traversal protection.
- **Cons**:
    - **Abstraction Overhead**: Adds a layer of indirection.
    - **Initial Effort**: Requires refactoring existing code to use the new service.
