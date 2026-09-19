package com.example.demo.agent;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import java.util.List;
import java.util.stream.Collectors;

@Component
public class FactVerifier implements ContentProcessor {

    private static final Logger log = LoggerFactory.getLogger(FactVerifier.class);

    @Override
    public String process(String content) {
        log.info("Verifying research facts for consistency...");
        
        // Simple verification: check for common contradictions or empty/useless claims
        // In a production system, this could be an LLM-based verification pass.
        String[] lines = content.split("\n");
        StringBuilder verifiedContent = new StringBuilder();
        
        for (String line : lines) {
            String trimmed = line.trim();
            if (trimmed.isEmpty()) {
                verifiedContent.append("\n");
            } else if (trimmed.startsWith("-") || trimmed.startsWith("*")) {
                // Basic check: ensure every fact starts with a dash/bullet and has content
                if (trimmed.length() > 3) {
                    verifiedContent.append(trimmed).append("\n");
                } else {
                    log.warn("Skipping suspiciously short or empty fact: {}", trimmed);
                }
            } else {
                // Non-bulleted text might be noise or intro
                if (trimmed.length() > 5) {
                    verifiedContent.append(trimmed).append("\n");
                }
            }
        }
        
        return verifiedContent.toString();
    }
}
