package com.example.demo;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.List;
import java.util.Random;

@Service
public class AutoDraftService {

    private final ChatClient chatClient;
    private final WordPressTool wordPressTool;
    private final List<String> topics = List.of("Cryptography", "Application Security", "AI Security", "Mobile Security");
    private final Random random = new Random();

    public AutoDraftService(ChatClient.Builder chatClientBuilder, WordPressTool wordPressTool, WebCrawlerConfig webCrawlerConfig) {
        this.wordPressTool = wordPressTool;
        this.chatClient = chatClientBuilder.build().mutate()
                .defaultSystem("You are an expert security analyst and blog poster agent. Your task is to research a given subject related to Mobile Security, Cryptography, Application Security, or AI Security, and compose a detailed and engaging blog post formatted using proper HTML. The blog post must contain at least 5 to 10 paragraphs, with each paragraph being 100+ words. CRITICAL: Do NOT bold the first sentence of your paragraphs, and do NOT separate the opening sentence from the rest of the paragraph; integrate it naturally into the same paragraph block.")
                .defaultTools(webCrawlerConfig)
                .build();
    }

    // Run every Monday and Thursday at 9 AM
    @Scheduled(cron = "0 0 9 * * MON,THU")
    public void generateDraftAndOpenPullRequest() {
        String randomTopic = topics.get(random.nextInt(topics.size()));
        System.out.println("AutoDraftService: Starting automated blog draft for topic: " + randomTopic);

        String prompt = "Research the top security sites for recent news on " + randomTopic + ". Write a massive 1,000+ word HTML draft blog post containing 5-10 paragraphs.";

        try {
            String content = this.chatClient.prompt()
                    .user(prompt)
                    .call()
                    .content();

            // Save the draft locally
            wordPressTool.createDraftPost(new WordPressTool.DraftRequest(randomTopic, content));

            // Execute git and gh commands to open a PR
            openPullRequest(randomTopic);

        } catch (Exception e) {
            System.err.println("Failed to generate draft: " + e.getMessage());
        }
    }

    private void openPullRequest(String topic) throws Exception {
        // Sanitize topic to prevent command injection and enforce length limits
        String safeTopic = topic.replaceAll("[^a-zA-Z0-9\\s-]", "").strip();
        if (safeTopic.length() > 100) {
            throw new IllegalArgumentException("Topic is too long. Maximum length is 100 characters.");
        }
        String baseName = safeTopic.replaceAll("\\s+", "-").toLowerCase();
        String fileName = "output/" + baseName + ".html";
        String wpFileName = "output/" + baseName + "_wp.html";
        String branchName = "draft-" + System.currentTimeMillis();
        
        java.nio.file.Path baseDir = java.nio.file.Paths.get("output").toAbsolutePath().normalize();
        java.nio.file.Path fileTarget = baseDir.resolve(fileName).normalize();
        java.nio.file.Path wpFileTarget = baseDir.resolve(wpFileName).normalize();
        
        if (!fileTarget.startsWith(baseDir) || !wpFileTarget.startsWith(baseDir)) {
            throw new SecurityException("Path traversal attempt detected!");
        }
        
        
        runCommand(List.of("git", "config", "--global", "user.email", "agent@spring-ai.local"));
        runCommand(List.of("git", "config", "--global", "user.name", "Spring AI Agent"));
        runCommand(List.of("git", "checkout", "-b", branchName));
        runCommand(List.of("git", "add", "-f", fileName, wpFileName));
        runCommand(List.of("git", "commit", "-m", "Generated new blog draft for " + safeTopic));
        runCommand(List.of("git", "push", "-u", "origin", branchName));
        runCommand(List.of("gh", "pr", "create", "--title", "Review Needed: New Blog Draft for " + safeTopic, "--body", "A new draft has been automatically generated and is ready for review. Please merge this PR to approve the draft."));
    }

    private void runCommand(List<String> cmd) throws Exception {
        ProcessBuilder pb = new ProcessBuilder(cmd);
        pb.redirectErrorStream(true);
        Process process = pb.start();
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
            String line;
            while ((line = reader.readLine()) != null) {
                System.out.println("GitHub PR Output: " + line);
            }
        }
        int exitCode = process.waitFor();
        if (exitCode != 0) {
            System.err.println("Command failed with exit code " + exitCode + ": " + String.join(" ", cmd));
        }
    }
}
}
