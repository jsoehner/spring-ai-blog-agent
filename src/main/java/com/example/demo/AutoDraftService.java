package com.example.demo;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import com.example.demo.service.AgentOrchestrator;
import com.example.demo.service.VersionControlService;
import com.example.demo.service.StorageService;
import com.example.demo.service.ToolRegistry;

import java.util.List;
import java.util.Random;

@Service
public class AutoDraftService {

    private static final Logger log = LoggerFactory.getLogger(AutoDraftService.class);

    private final ChatClient chatClient;
    private final AgentOrchestrator agentOrchestrator;
    private final List<String> topics;
    private final Random random = new Random();

    public AutoDraftService(ChatClient.Builder chatClientBuilder, 
                            WebCrawlerConfig webCrawlerConfig,
                            @org.springframework.beans.factory.annotation.Value("${auto.draft.topics:Cryptography,Application Security,AI Security,Mobile Security}") List<String> topics,
                            VersionControlService versionControlService,
                            StorageService storageService,
                            ToolRegistry toolRegistry,
                            AgentOrchestrator agentOrchestrator) {
        this.agentOrchestrator = agentOrchestrator;
        this.topics = topics;
        this.chatClient = chatClientBuilder.build().mutate()
                .defaultSystem("You are an expert security analyst and blog poster agent. Your task is to research a given subject related to Mobile Security, Cryptography, Application Security, or AI Security, and compose a detailed and engaging blog post formatted using proper HTML. The blog post must contain at least 5 to 10 paragraphs, with each paragraph being 100+ words. CRITICAL: Do NOT bold the first sentence of your paragraphs, and do NOT separate the opening sentence from the rest of the paragraph; integrate it naturally into the same paragraph block.")
                .defaultTools(webCrawlerConfig)
                .build();
    }

    @Scheduled(cron = "${auto.draft.cron:0 0 9 * * MON,THU}")
    public void generateDraftAndOpenPullRequest() {
        String randomTopic = topics.get(random.nextInt(topics.size()));
        log.info("AutoDraftService: Starting automated blog draft workflow for topic: {}", randomTopic);

        agentOrchestrator.startWorkflow(randomTopic);
    }
}
