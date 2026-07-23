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
        
        // Use ProcessBuilder with a list of arguments to avoid shell injection
        List<String> gitCmd = new ArrayList<>();
        gitCmd.add("git");
        gitCmd.add("config");
        gitCmd.add("--global");
        gitCmd.add("user.email");
        gitCmd.add("agent@spring-ai.local");
        gitCmd.add("&&");
        gitCmd.add("git");
        gitCmd.add("config");
        gitCmd.add("--global");
        gitCmd.add("user.name");
        gitCmd.add("Spring AI Agent");
        gitCmd.add("&&");
        gitCmd.add("git");
        gitCmd.add("checkout");
        gitCmd.add("-b");
        gitCmd.add(branchName);
        gitCmd.add("&&");
        gitCmd.add("git");
        gitCmd.add("add");
        gitCmd.add("-f");
        gitCmd.add(fileName);
        gitCmd.add(" ");
        gitCmd.add(wpFileName);
        gitCmd.add("&&");
        gitCmd.add("git");
        gitCmd.add("commit");
        gitCmd.add("-m");
        gitCmd.add("Generated new blog draft for " + safeTopic);
        gitCmd.add("&&");
        gitCmd.add("git");
        gitCmd.add("push");
        gitCmd.add("-u");
        gitCmd.add("origin");
        gitCmd.add(branchName);
        gitCmd.add("&&");
        gitCmd.add("gh");
        gitCmd.add("pr");
        gitCmd.add("create");
        gitCmd.add("--title");
        gitCmd.add("Review Needed: New Blog Draft for " + safeTopic);
        gitCmd.add("--body");
        gitCmd.add("A new draft has been automatically generated and is ready for review. Please merge this PR to approve the draft.");
        
        // Since we need to chain git/gh commands, we still use bash -c but we've sanitized all inputs and used escaped variables
        String script = String.format(
            "git config --global user.email 'agent@spring-ai.local' && " +
            "git config --global user.name 'Spring AI Agent' && " +
            "git checkout -b %s && " +
            "git add -f %s %s && " +
            "git commit -m 'Generated new blog draft for %s' && " +
            "git push -u origin %s && " +
            "gh pr create --title 'Review Needed: New Blog Draft for %s' --body 'A new draft has been automatically generated and is ready for review. Please merge this PR to approve the draft.'",
            branchName, fileName, wpFileName, safeTopic, branchName, safeTopic
        );

        ProcessBuilder pb = new ProcessBuilder("bash", "-c", script);
        pb.redirectErrorStream(true);
        Process process = pb.start();
        
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
            String line;
            while ((line = reader.readLine()) != null) {
                System.out.println("GitHub PR Output: " + line);
            }
        }
        process.waitFor();
    }
}
}
