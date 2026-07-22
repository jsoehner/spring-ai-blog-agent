package com.example.agent.skill;

import java.util.Map;
import java.util.Set;

/**
 * Defines the contract for agents to dynamically discover capabilities.
 * This interface allows the Supervisor Agent and other coordinating agents 
 * to know what external specialized capabilities are available in the ecosystem.
 */
public interface SkillRegistry {

    /**
     * Retrieves the set of all available specialized skills.
     * @return Set of skill IDs (e.g., "docker-expert", "review-team").
     */
    Set<String> getAllAvailableSkills();

    /**
     * Retrieves the description and function of a specific skill.
     * @param skillId The ID of the skill.
     * @return A descriptive map containing information like description and location.
     */
    Map<String, String> getSkillDetails(String skillId);

    /**
     * Checks if a skill is suitable for a given task description.
     * This function should use advanced semantic matching or keyword extraction 
     * on the skill descriptions to determine relevance.
     * @param skillId The skill to check.
     * @param taskDescription The high-level task description (e.g., "Optimize Docker setup").
     * @return true if the skill is deemed relevant to the task.
     */
    boolean isSkillRelevant(String skillId, String taskDescription);
}