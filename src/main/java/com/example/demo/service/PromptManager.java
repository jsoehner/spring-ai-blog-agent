package com.example.demo.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.*;

@Service
public class PromptManager {

    private static final Logger log = LoggerFactory.getLogger(PromptManager.class);

    private final Map<String, Map<String, PromptTemplate>> promptRegistry = new HashMap<>();

    public PromptManager(List<PromptTemplate> templates) {
        for (PromptTemplate template : templates) {
            if (template != null && template.getName() != null) {
                promptRegistry.computeIfAbsent(template.getName(), k -> new HashMap<>())
                        .put(template.getVersion(), template);
            }
        }
    }

    public String getPrompt(String name, String version) {
        Map<String, PromptTemplate> versions = promptRegistry.get(name);
        if (versions == null) {
            throw new IllegalArgumentException("Prompt not found: " + name);
        }
        PromptTemplate template = versions.get(version);
        if (template == null) {
            throw new IllegalArgumentException("Version not found for prompt: " + name + " (version: " + version + ")");
        }
        return template.getContent();
    }

    public String getLatestPrompt(String name) {
        Map<String, PromptTemplate> versions = promptRegistry.get(name);
        if (versions == null) {
            throw new IllegalArgumentException("Prompt not found: " + name);
        }
        return versions.values().stream()
                .max(Comparator.comparing(PromptTemplate::getVersion))
                .map(PromptTemplate::getContent)
                .orElseThrow(() -> new RuntimeException("No versions found for prompt: " + name));
    }
}
