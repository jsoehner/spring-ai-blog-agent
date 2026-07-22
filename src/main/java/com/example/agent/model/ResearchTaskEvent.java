package com.example.agent.model;

import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Canonical message payload for all inter-agent communication via RabbitMQ.
 * Ensures traceability and strongly typed context.
 */
public class ResearchTaskEvent {

    private UUID taskId;
    private String topic;
    private String sourceAgent; 
    private String currentPass; 
    private String targetFormat; 

    // Structured metadata specifying what the task requires
    private Map<String, String> requiredExpertise; 

    // Payload containing the raw data or context for the current pass
    private Map<String, Object> payload; 
    
    // List of IDs for dependencies (if this task relies on a previous output)
    private List<UUID> dependencyTaskIds; 

    // --- Constructors ---
    public ResearchTaskEvent() {
        this.taskId = UUID.randomUUID();
    }
    
    // --- Getters and Setters (Standard Lombok/Java Bean approach assumed) ---

    public UUID getTaskId() { return taskId; }
    public void setTaskId(UUID taskId) { this.taskId = taskId; }
    public String getTopic() { return topic; }
    public void setTopic(String topic) { this.topic = topic; }
    public String getSourceAgent() { return sourceAgent; }
    public void setSourceAgent(String sourceAgent) { this.sourceAgent = sourceAgent; }
    public String getCurrentPass() { return currentPass; }
    public void setCurrentPass(String currentPass) { this.currentPass = currentPass; }
    public String getTargetFormat() { return targetFormat; }
    public void setTargetFormat(String targetFormat) { this.targetFormat = targetFormat; }
    public Map<String, String> getRequiredExpertise() { return requiredExpertise; }
    public void setRequiredExpertise(Map<String, String> requiredExpertise) { this.requiredExpertise = requiredExpertise; }
    public Map<String, Object> getPayload() { return payload; }
    public void setPayload(Map<String, Object> payload) { this.payload = payload; }
    public List<UUID> getDependencyTaskIds() { return dependencyTaskIds; }
    public void setDependencyTaskIds(List<UUID> dependencyTaskIds) { this.dependencyTaskIds = dependencyTaskIds; }
}