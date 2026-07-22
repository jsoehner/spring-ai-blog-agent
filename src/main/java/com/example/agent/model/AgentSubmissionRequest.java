package com.example.agent.model;

import java.util.List;

/**
 * Structured data transfer object (DTO) for client submission requests.
 * This replaces simple string parameters to provide rich context to the Supervisor Agent.
 */
public record AgentSubmissionRequest(
    String topic,
    String intent, // e.g., "BLOG_GENERATION", "SECURITY_AUDIT", "RESEARCH"
    List<String> requiredExpertise, // e.g., ["CODE_QUALITY", "ARCHITECTURE"]
    String targetFormat // e.g., "LOCAL_HTML", "WORDPRESS_OPTIMIZED"
) {}