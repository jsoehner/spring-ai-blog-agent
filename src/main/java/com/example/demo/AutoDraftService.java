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
        this.chatClient = chatClientBuilder
                .defaultSystem("You are an expert security analyst and blog poster agent. Your task is to research a given subject related to Mobile Security, Cryptography, Application Security, or AI Security, and compose a detailed and engaging blog post formatted using proper HTML. The blog post must contain at least 5 to 10 paragraphs, with each paragraph being 100+ words.")
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
            wordPressTool.createDraftPost(new WordPressTool.DraftRequest("New Draft: " + randomTopic, content));

            // Execute git and gh commands to open a PR
            openPullRequest(randomTopic);

        } catch (Exception e) {
            System.err.println("Failed to generate draft: " + e.getMessage());
        }
    }

    private void openPullRequest(String topic) throws Exception {
        String branchName = "draft-" + System.currentTimeMillis();
        String script = String.format(
            "git config --global user.email 'agent@spring-ai.local' && " +
            "git config --global user.name 'Spring AI Agent' && " +
            "git checkout -b %s && " +
            "git add blog_draft.html && " +
            "git commit -m 'Generated new blog draft for %s' && " +
            "git push -u origin %s && " +
            "gh pr create --title 'Review Needed: New Blog Draft for %s' --body 'A new draft has been automatically generated and is ready for review. Please merge this PR to approve the draft.'",
            branchName, topic, branchName, topic
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
