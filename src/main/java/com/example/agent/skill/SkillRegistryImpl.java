package com.example.agent.skill;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * Concrete implementation of SkillRegistry. It serves as the central catalog 
 * for all available specialized agent capabilities.
 * 
 * In a production system, this would dynamically pull skill metadata from a centralized 
 * repository or API endpoint. Here, we simulate loading metadata from a predefined set.
 */
public class SkillRegistryImpl implements SkillRegistry {

    private final Map<String, Map<String, String>> skillCatalog;

    public SkillRegistryImpl() {
        this.skillCatalog = new HashMap<>();
        loadSkills();
    }

    /**
     * Simulates loading metadata for all available skills from configuration or files.
     * This replaces a dynamic file system scan for simplicity.
     */
    private void loadSkills() {
        // --- Core Review Skills ---
        Map<String, String> reviewTeam = new HashMap<>();
        reviewTeam.put("name", "Review Team");
        reviewTeam.put("description", "Runs a fleet of specialized reviewer subagents against code changes (single/iterate modes). Focuses on correctness, security, architecture, and quality.");
        skillCatalog.put("review-team", reviewTeam);
        
        // --- Core DevOps Skills ---
        Map<String, String> dockerExpert = new HashMap<>();
        dockerExpert.put("name", "Docker Expert");
        dockerExpert.put("description", "Advanced Docker containerization expert, covering optimization, security hardening, multi-stage builds, and production deployment.");
        skillCatalog.put("docker-expert", dockerExpert);
        
        // --- General Purpose Skills ---
        Map<String, String> braveSearch = new HashMap<>();
        braveSearch.put("name", "Brave Search");
        braveSearch.put("description", "Web search and content extraction via Brave Search API. Use for searching documentation, facts, or any web content.");
        skillCatalog.put("brave-search", braveSearch);

        Map<String, String> frontendDesign = new HashMap<>();
        frontendDesign.put("name", "Frontend Design");
        frontendDesign.put("description", "Creates distinctive, production-grade frontend interfaces with high design quality, generating polished web components.");
        skillCatalog.put("frontend-design", frontendDesign);
        
        // Add more skills here...
    }

    @Override
    public Set<String> getAllAvailableSkills() {
        return skillCatalog.keySet();
    }

    @Override
    public Map<String, String> getSkillDetails(String skillId) {
        return skillCatalog.getOrDefault(skillId, Map.of("description", "Skill not found."));
    }

    @Override
    public boolean isSkillRelevant(String skillId, String taskDescription) {
        Map<String, String> details = getSkillDetails(skillId);
        String description = details.get("description").toLowerCase();
        String task = taskDescription.toLowerCase();

        // Simple keyword matching for initial relevance check
        if (skillId.equals("review-team")) {
            return task.contains("review") || task.contains("code") || task.contains("fix") || task.contains("improve");
        } else if (skillId.equals("docker-expert")) {
            return task.contains("docker") || task.contains("container") || task.contains("deployment") || task.contains("optimize");
        } else if (skillId.equals("brave-search")) {
            return task.contains("research") || task.contains("find facts") || task.contains("documentation");
        } else if (skillId.equals("frontend-design")) {
            return task.contains("ui") || task.contains("design") || task.contains("frontend") || task.contains("web component");
        }
        return false; // Default to irrelevant
    }
}