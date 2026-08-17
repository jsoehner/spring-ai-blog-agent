package com.example.demo.service;

import com.example.demo.tools.WordPressTool;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Service
public class AgentOrchestrator {

    private final ContentPipeline contentPipeline;

    private final ChatClient bloggerClient;
    private final WordPressTool wordPressTool;
    private final RestTemplate restTemplate = new RestTemplate();
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final String imageAgentUrl;

    private final StorageService storageService;
    private final ToolRegistry toolRegistry;
    private final MessageService messageService;
    private final VersionControlService versionControlService;

    private final Map<String, WorkflowState> workflowStates = new ConcurrentHashMap<>();

    public enum WorkflowState {
        RESEARCHING,
        FACT_CHECKING,
        WRITING,
        ASSEMBLING,
        PUBLISHING,
        COMPLETED,
        FAILED
    }

    private final TextHumanizerProcessor textHumanizerProcessor;

    public AgentOrchestrator(ChatClient.Builder chatClientBuilder,
                            WordPressTool wordPressTool,
                            @org.springframework.beans.factory.annotation.Value("${IMAGE_AGENT_URL:http://localhost:8080/image}") String imageAgentUrl,
                            @org.springframework.beans.factory.annotation.Value("${blogger.prompt.path:prompts/blogger-prompt.txt}") org.springframework.core.io.Resource bloggerPromptResource,
                            StorageService storageService,
                            ToolRegistry toolRegistry,
                            MessageService messageService,
                            VersionControlService versionControlService,
                            ContentPipeline contentPipeline,
                            TextHumanizerProcessor textHumanizerProcessor) {
        this.wordPressTool = wordPressTool;
        this.imageAgentUrl = imageAgentUrl;
        this.storageService = storageService;
        this.toolRegistry = toolRegistry;
        this.messageService = messageService;
        this.versionControlService = versionControlService;
        this.contentPipeline = contentPipeline;
        this.textHumanizerProcessor = textHumanizerProcessor;

        this.bloggerClient = chatClientBuilder.build().mutate()
                .defaultSystem(new String(bloggerPromptResource.getInputStream().readAllBytes(), java.nio.charset.StandardCharsets.UTF_8))
                .build();
    }

    public void startWorkflow(String topic) {
        log.info("Starting new blog generation workflow for topic: {}", topic);
        workflowStates.put(topic, WorkflowState.RESEARCHING);
        
        // Trigger initial research task
        messageService.sendResearchTask(topic);
    }

    public void handleSupervisorTask(String jsonPayload) {
        try {
            Map<String, String> payload = objectMapper.readValue(jsonPayload, new com.fasterxml.jackson.core.type.TypeReference<Map<String, String>>() {});
            String topic = payload.get("topic");
            String rawFacts = payload.get("facts");
            log.info("Supervisor Agent received compiled facts for topic: {}", topic);

            // Humanize research output before Pass 2 blog writing
            String facts = textHumanizerProcessor.process(rawFacts);

            workflowStates.put(topic, WorkflowState.WRITING);
            log.info("Starting Pass 2 (Blog Writing/Grammar Check) for: {}", topic);
            
            String htmlContent = bloggerClient.prompt()
                    .user("Here are the gathered facts:\n" + facts + "\n\nPlease perform grammatical corrections and organize the content into the HTML blog post.")
                    .call()
                    .content();

            htmlContent = contentPipeline.process(htmlContent);

            // Delegate to Image Agent
            log.info("Delegating image generation to: {}", imageAgentUrl);
            Map<String, String> imageRequest = new HashMap<>();
            imageRequest.put("topic", topic);
            imageRequest.put("content", htmlContent);

            String imageUrls = "";
            try {
                imageUrls = restTemplate.postForObject(imageAgentUrl, imageRequest, String.class);
            } catch (Exception e) {
                log.error("Failed to fetch images: {}", e.getMessage());
            }

            String headerImage = "";
            String inlineImage = "";
            if (imageUrls != null && !imageUrls.isEmpty()) {
                String[] urls = imageUrls.split("\n");
                headerImage = urls.length > 0 ? urls[0] : "";
                inlineImage = urls.length > 1 ? urls[1] : "";
            }

            String contentWithImages = htmlContent;
            if (!headerImage.isEmpty()) {
                String safeHeaderImage = headerImage.replace("&", "&amp;");
                contentWithImages = "<!-- wp:image --><figure class=\"wp-block-image\"><img src=\"" + safeHeaderImage + "\" alt=\"Header Image\"/></figure><!-- /wp:image -->\n" + contentWithImages;
            }
            if (!inlineImage.isEmpty()) {
                String safeInlineImage = inlineImage.replace("&", "&amp;");
                contentWithImages = contentWithImages + "\n<!-- wp:image --><figure class=\"wp-block-image\"><img src=\"" + safeInlineImage + "\" alt=\"Inline Image\"/></figure><!-- /wp:image -->";
            }

            // Upload to WordPress (Local draft)
            log.info("Uploading draft to WordPress for topic: {}", topic);
            String result = wordPressTool.createDraftPost(new WordPressTool.DraftRequest(topic, contentWithImages));
            log.info("WordPress upload result: {}", result);

            // Save to local file
            storageService.saveBlogPost(topic, contentWithImages);

            // Open Pull Request
            openPullRequest(topic);

            workflowStates.put(topic, WorkflowState.COMPLETED);

        } catch (Exception e) {
            log.error("Error processing supervisor task: {}", e.getMessage(), e);
            workflowStates.put(topic, WorkflowState.FAILED);
        }
    }

    private void openPullRequest(String topic) throws Exception {
        // Sanitize topic to prevent command injection and enforce length limits
        String safeTopic = topic.replaceAll("[^a-zA-Z0-9\\s-]", "").strip();
        if (safeTopic.length() > 100) {
            throw new IllegalArgumentException("Topic is too long. Maximum length is 100 characters.");
        }
        
        java.nio.file.Path fileTarget = storageService.getSafePath(topic, ".html");
        java.nio.file.Path wpFileTarget = storageService.getSafePath(topic, "_wp.html");
        
        String branchName = "draft-" + System.currentTimeMillis();
        
        versionControlService.createBranch(branchName);
        versionControlService.addFiles(List.of(fileTarget.toString(), wpFileTarget.toString()));
        versionControlService.commit("Generated new blog draft for " + topic);
        versionControlService.push(branchName);
        versionControlService.createPullRequest("Review Needed: New Blog Draft for " + topic, 
                "A new draft has been automatically generated and is ready for review. Please merge this PR to approve the draft.");
    }

    public WorkflowState getState(String topic) {
        return workflowStates.getOrDefault(topic, WorkflowState.FAILED);
    }
}
