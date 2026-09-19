# ADR-005: File System & Storage Strategy

## Status
Proposed

## Context
The current `StorageService` implementation directly interacts with the local filesystem using `java.nio.file`. This tight coupling makes it difficult to:
1. Support cloud storage (e.g., AWS S3, Google Cloud Storage) without modifying the business logic.
2. Implement advanced storage features like content-addressable storage or automatic cleanup policies.
3. Abstract away file system differences between local development and production environments.

## Decision
We will abstract all storage operations behind a `StorageService` interface. This interface will define common operations such as saving files, reading files, and listing directory contents.

Implementations will include:
- `LocalFileSystemStorage`: For local development and simple file storage.
- `S3StorageService`: For cloud-based storage using AWS S3.
- `GCSStorageService`: For cloud-based storage using Google Cloud Storage.

All storage paths will be managed through a configuration-driven base directory system.

## Consequences
- **Pros**:
  - **Cloud Portability**: Easily switch between local and cloud storage by changing the implementation in the configuration.
  - **Testability**: Allows for mocking storage interactions in unit tests.
  - **Scalability**: Provides a clear path for implementing distributed storage solutions.
- **Cons**:
  - **Abstraction Overhead**: Some cloud-specific features may be harder to expose through a generic interface.
  - **Configuration Complexity**: Requires careful management of credentials and region-specific configurations for cloud providers.
