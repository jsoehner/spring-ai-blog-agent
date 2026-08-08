package com.example.demo.service;

import com.example.demo.tools.WordPressTool;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.*;

@Slf4j
@Service
public class BlogAgentService {

    private final PromptManager promptManager;

    private final ChatClient bloggerClient;
    private final WordPressTool wordPressTool;
    private final RestTemplate restTemplate = new RestTemplate();
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final String imageAgentUrl;

    private final OpaService opaService;
    private final MessageService messageService;
    private final StorageService storageService;

    public BlogAgentService(ChatClient.Builder chatClientBuilder,
                            WordPressTool wordPressTool,
                            @org.springframework.beans.factory.annotation.Value("${IMAGE_AGENT_URL:http://localhost:8080/image}") String imageAgentUrl,
                            @org.springframework.beans.factory.annotation.Value("${blogger.prompt.path:prompts/blogger-prompt.txt}") org.springframework.core.io.Resource bloggerPromptResource,
                            OpaService opaService,
                            MessageService messageService,
                            StorageService storageService,
                            PromptManager promptManager) throws IOException {
        this.wordPressTool = wordPressTool;
        this.imageAgentUrl = imageAgentUrl;
        this.opaService = opaService;
        this.messageService = messageService;
        this.storageService = storageService;
        this.promptManager = promptManager;

        this.bloggerClient = chatClientBuilder.build().mutate()
                .defaultSystem(promptManager.getLatestPrompt("blogger-prompt"))
                .build();
    }

    public List<String> queueBlogTopics(List<String> topics) {
        List<String> queuedTopics = new ArrayList<>();
        for (String topic : topics) {
            if (!opaService.isTopicAllowed(topic)) {
                log.info("Topic rejected by OPA policy: {}", topic);
                continue;
            }
            
            log.info("Processing topic for research: {}", topic);
            messageService.sendResearchTask(topic);
            queuedTopics.add(topic);
        }
        return queuedTopics;
    }

    public void queueResearchTask(String topic) {
        log.info("Queuing research task for topic: {}", topic);
        messageService.sendResearchTask(topic);
    }
}
