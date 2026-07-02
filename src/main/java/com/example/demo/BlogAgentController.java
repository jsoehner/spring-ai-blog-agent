package com.example.demo;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.SimpleLoggerAdvisor;
import org.springframework.amqp.rabbit.annotation.Queue;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.context.annotation.Profile;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestTemplate;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.core.type.TypeReference;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.example.demo.security.OpaClient;

@RestController
@Profile("supervisor")
public class BlogAgentController {

    private final RabbitTemplate rabbitTemplate;
    private final OpaClient opaClient;
    private final ChatClient bloggerClient;
    private final WordPressTool wordPressTool;
    private final String imageAgentUrl;
    private final RestTemplate restTemplate = new RestTemplate();
    private final ObjectMapper objectMapper = new ObjectMapper();

    public BlogAgentController(RabbitTemplate rabbitTemplate, 
                               OpaClient opaClient,
                               ChatClient.Builder chatClientBuilder,
                               WordPressTool wordPressTool,
                               @Value("${IMAGE_AGENT_URL:http://localhost:8080/image}") String imageAgentUrl,
                               @Value("${BLOGGER_PROMPT:You are an entertaining, engaging technical writer. Take the provided bullet points and compose a detailed, interesting, and highly educational blog post. Structure the content so that each paragraph contains 3 or more sentences of similar subject matter and message. Focus on good sentence structure with an opening sentence starting a new paragraph. CRITICAL: Do NOT bold the first sentence of your paragraphs, and do NOT separate the opening sentence from the rest of the paragraph; integrate it naturally into the same paragraph block. Follow with several sentences that contain a noun and a verb and avoid too many colourful adverbs and adjectives that could make the sentence run-on. Pay close attention to your punctuation: use commas where the sentence flow changes, and don't be afraid to break one big sentence into two shorter, clearer sentences. Provide substantial content and aim to have between 750 and 1000 words in the blog post. Reserve summary points for the last paragraph. CRITICAL: Limit your output to a maximum of 5 headers. Do not include duplicate conclusions or repetitive closing statements. CRITICAL: Do NOT generate a main title or H1 heading for the article; start directly with the content or an introductory subheading. CRITICAL: You MUST format your entire output using WordPress Gutenberg block syntax. Wrap every heading in <!-- wp:heading --><h2>...</h2><!-- /wp:heading --> and every paragraph in <!-- wp:paragraph --><p>...</p><!-- /wp:paragraph -->. Ensure your HTML is well-formed and do NOT add broken closing tags like </. Do NOT wrap your response in ```html markdown blocks.}") String bloggerPrompt) {
        this.rabbitTemplate = rabbitTemplate;
        this.opaClient = opaClient;
        this.wordPressTool = wordPressTool;
        this.imageAgentUrl = imageAgentUrl;

        this.bloggerClient = chatClientBuilder.build().mutate()
                .defaultSystem(bloggerPrompt)
                .defaultAdvisors(new SimpleLoggerAdvisor())
                .build();
    }

    @GetMapping(value = "/blog")
    public ResponseEntity<String> blog(
            @RequestParam(defaultValue = "Recent mobile security threats in Android") List<String> topics) {
        
        for (String topic : topics) {
            Map<String, Object> input = new HashMap<>();
            input.put("topic", topic);

            String topicOpaUrl = opaClient.getOpaUrl().replace("/agent/main", "/blog");
            if (!opaClient.evaluatePolicy(topicOpaUrl, input)) {
                System.out.println("Topic rejected by OPA policy: " + topic);
                continue;
            }
            System.out.println("Processing topic for research: " + topic);
            rabbitTemplate.convertAndSend("research-tasks", topic);
        }
        
        return ResponseEntity.accepted().body("Queued " + topics.size() + " topics for background processing.");
    }

    @RabbitListener(queuesToDeclare = @Queue("supervisor-tasks"))
    public void processSupervisorTask(String jsonPayload) {
        try {
            Map<String, String> payload = objectMapper.readValue(jsonPayload, new TypeReference<Map<String, String>>() {});
            String topic = payload.get("topic");
            String facts = payload.get("facts");
            System.out.println("Supervisor Agent received compiled facts for topic: " + topic);

            // Pass 2: Write blog post
            System.out.println("Starting Pass 2 (Blog Writing/Grammar Check) for: " + topic);
            String htmlContent = bloggerClient.prompt()
                    .user("Here are the gathered facts:\n" + facts + "\n\nPlease perform grammatical corrections and organize the content into the HTML blog post.")
                    .advisors(a -> a.param("chat_memory_conversation_id", "pass2-" + topic))
                    .call()
                    .content();

            htmlContent = htmlContent.trim();
            if (htmlContent.startsWith("```html")) {
                htmlContent = htmlContent.substring(7);
            }
            if (htmlContent.endsWith("```")) {
                htmlContent = htmlContent.substring(0, htmlContent.length() - 3);
            }
            htmlContent = htmlContent.trim();

            // Delegate to Image Agent
            System.out.println("Delegating image generation to: " + imageAgentUrl);
            Map<String, String> imageRequest = new HashMap<>();
            imageRequest.put("topic", topic);
            imageRequest.put("content", htmlContent);

            String imageUrls = "";
            try {
                imageUrls = restTemplate.postForObject(imageAgentUrl, imageRequest, String.class);
            } catch (Exception e) {
                System.err.println("Failed to fetch images: " + e.getMessage());
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
            System.out.println("Uploading draft to WordPress for topic: " + topic);
            String result = wordPressTool.createDraftPost(new WordPressTool.DraftRequest(topic, contentWithImages));
            System.out.println(result);

            // Save to local file
            // Sanitize filename to prevent path traversal
            String safeBaseName = topic.replaceAll("[^a-zA-Z0-9\\s-]", "").strip();
            String filename = safeBaseName.replaceAll("\\s+", "-").toLowerCase() + ".html";
            try {
                java.nio.file.Path baseDir = java.nio.file.Paths.get("output").toAbsolutePath().normalize();
                java.nio.file.Path targetFile = baseDir.resolve(filename).normalize();
                
                if (!targetFile.startsWith(baseDir)) {
                    throw new SecurityException("Path traversal attempt detected!");
                }
                
                // Create output directory if it does not exist
                if (!java.nio.file.Files.exists(baseDir)) {
                    java.nio.file.Files.createDirectories(baseDir);
                }
                
                java.nio.file.Files.writeString(targetFile, contentWithImages);
                System.out.println("Saved blog post to local file: " + targetFile.toString());
            } catch (Exception e) {
                System.err.println("Failed to save local blog post file: " + e.getMessage());
            }

        } catch (Exception e) {
            System.err.println("Error processing supervisor task: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
