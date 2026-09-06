package com.example.demo.service;

import com.example.demo.tools.WordPressTool;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import static com.example.demo.util.RetryUtils.executeWithRetry;

@Service
public class AgentOrchestrator {

    private static final Logger log = LoggerFactory.getLogger(AgentOrchestrator.class);

    public enum WorkflowState {
        RESEARCHING,
        FACT_CHECKING,
        WRITING,
        ASSEMBLING,
        PUBLISHING,
        COMPLETED,
        FAILED
    }

    private final ContentPipeline contentPipeline;
    private final ChatClient bloggerClient;
    private final WordPressTool wordPressTool;
    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final String imageAgentUrl;

    private final StorageService storageService;
    private final ToolRegistry toolRegistry;
    private final MessageService messageService;
    private final VersionControlService versionControlService;
    private final TextHumanizerProcessor textHumanizerProcessor;

    public AgentOrchestrator(ChatClient.Builder chatClientBuilder,
                            WordPressTool wordPressTool,
                            RestTemplate restTemplate,
                            @org.springframework.beans.factory.annotation.Value("${IMAGE_AGENT_URL:http://localhost:8080/image}") String imageAgentUrl,
                            @org.springframework.beans.factory.annotation.Value("${blogger.prompt.path:classpath:prompts/blogger-prompt.txt}") org.springframework.core.io.Resource bloggerPromptResource,
                            StorageService storageService,
                            ToolRegistry toolRegistry,
                            MessageService messageService,
                            VersionControlService versionControlService,
                            ContentPipeline contentPipeline,
                            TextHumanizerProcessor textHumanizerProcessor) {
        this.wordPressTool = wordPressTool;
        this.restTemplate = restTemplate;
        this.imageAgentUrl = imageAgentUrl;
        this.storageService = storageService;
        this.toolRegistry = toolRegistry;
        this.messageService = messageService;
        this.versionControlService = versionControlService;
        this.contentPipeline = contentPipeline;
        this.textHumanizerProcessor = textHumanizerProcessor;

        try {
            this.bloggerClient = chatClientBuilder.build().mutate()
                    .defaultSystem(new String(bloggerPromptResource.getInputStream().readAllBytes(), java.nio.charset.StandardCharsets.UTF_8))
                    .build();
        } catch (java.io.IOException e) {
            throw new RuntimeException("Failed to load blogger prompt resource", e);
        }
    }

    private final Map<String, WorkflowState> workflowStates = new ConcurrentHashMap<>();

    public void startWorkflow(String topic) {
        log.info("Starting new blog generation workflow for topic: {}", topic);
        workflowStates.put(topic, WorkflowState.RESEARCHING);
        
        // Trigger initial research task
        messageService.sendResearchTask(topic);
    }

    public void handleSupervisorTask(String jsonPayload) {
        String topic = null;
        try {
            Map<String, String> payload = objectMapper.readValue(jsonPayload, new com.fasterxml.jackson.core.type.TypeReference<Map<String, String>>() {});
            topic = payload.get("topic");
            if (topic == null) {
                log.error("Topic is missing in payload");
                return;
            }

            // Humanize research output before Pass 2 blog writing
            String rawFacts = payload.get("facts");
            String facts = (rawFacts != null) ? textHumanizerProcessor.process(rawFacts) : "";

            workflowStates.put(topic, WorkflowState.WRITING);
            log.info("Starting Pass 2 (Blog Writing/Grammar Check) for: {}", topic);
            
            // Step 1: Generate Content with Retries
            final String finalFacts = facts;
            String rawHtmlContent = executeWithRetry("Blogger Content Generation", () -> 
                bloggerClient.prompt().user("Here are the gathered facts:\n" + finalFacts + "\n\nPlease perform grammatical corrections and organize the content into the HTML blog post.").call().content()
            );

            // Step 2: Delegate to Image Agent with Retries
            log.info("Delegating image generation to: {}", imageAgentUrl);
            Map<String, String> imageRequest = new HashMap<>();
            imageRequest.put("topic", topic);
            imageRequest.put("content", rawHtmlContent);

            String imageUrls = "";
            try {
                imageUrls = restTemplate.postForObject(imageAgentUrl, imageRequest, String.class);
            } catch (Exception e) {
                log.error("Non-critical failure fetching images: {}", e.getMessage());
            }

            String headerImage = "";
            String inlineImage = "";
            if (imageUrls != null && !imageUrls.isEmpty()) {
                String[] urls = imageUrls.split("\\n");
                headerImage = urls.length > 0 ? urls[0] : "";
                inlineImage = urls.length > 1 ? urls[1] : "";
            }

            String wpContent = rawHtmlContent;
            if (!headerImage.isEmpty()) {
                String safeHeaderImage = headerImage.replace("&", "&amp;");
                wpContent = "<!-- wp:image --><figure class=\"wp-block-image\"><img src=\"" + safeHeaderImage + "\" alt=\"Header Image\"/></figure><!-- /wp:image -->\n" + wpContent;
            }
            if (!inlineImage.isEmpty()) {
                String safeInlineImage = inlineImage.replace("&", "&amp;");
                wpContent = wpContent + "\n<!-- wp:image --><figure class=\"wp-block-image\"><img src=\"" + safeInlineImage + "\" alt=\"Inline Image\"/></figure><!-- /wp:image -->";
            }

            // Sanitize WordPress Gutenberg content (compact Gutenberg blocks, no extra spacing, no blank lines)
            MarkdownSanitizer sanitizer = new MarkdownSanitizer();
            String cleanWpContent = sanitizer.process(wpContent);

            // Deduplicate redundant sentences / repeated paragraphs
            SentenceDeduplicator deduplicator = new SentenceDeduplicator();
            cleanWpContent = deduplicator.process(cleanWpContent);

            // Generate standalone HTML blog post via content pipeline (sanitizes, validates HTML, injects SEO meta)
            String standaloneHtml = contentPipeline.process(cleanWpContent);

            // Step 3: Upload to WordPress (Local draft with clean Gutenberg content)
            log.info("Uploading draft to WordPress for topic: {}", topic);
            final String finalWpContent = cleanWpContent;
            final String finalTopic = topic;
            String result = executeWithRetry("WordPress Upload", () -> 
                wordPressTool.createDraftPost(new WordPressTool.DraftRequest(finalTopic, finalWpContent))
            );
            log.info("WordPress upload result: {}", result);

            // Step 4: Save to local files (both standalone HTML and WordPress Gutenberg HTML)
            storageService.saveBlogPost(topic, standaloneHtml);
            storageService.saveWordPressPost(topic, cleanWpContent);

            // Step 5: Open Pull Request with Retries (safely caught if git is not initialized)
            try {
                openPullRequest(topic);
            } catch (Exception e) {
                log.warn("Git/PR creation skipped or failed: {}", e.getMessage());
            }

            workflowStates.put(topic, WorkflowState.COMPLETED);

        } catch (Exception e) {
            log.error("Error processing supervisor task: {}", e.getMessage(), e);
            if (topic != null) {
                workflowStates.put(topic, WorkflowState.FAILED);
            }
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
