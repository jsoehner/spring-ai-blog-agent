package com.example.demo;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.MessageChatMemoryAdvisor;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.memory.InMemoryChatMemoryRepository;
import org.springframework.ai.chat.memory.MessageWindowChatMemory;
import org.springframework.amqp.rabbit.annotation.Queue;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

@Component
@Profile("researcher")
public class ResearcherController {

    private final ChatClient factGathererClient;
    private final ChatClient bloggerClient;
    private final WordPressTool wordPressTool;
    private final String imageAgentUrl;
    private final RestTemplate restTemplate = new RestTemplate();

    public ResearcherController(ChatClient.Builder chatClientBuilder,
                                WordPressTool wordPressTool,
                                WebCrawlerConfig webCrawlerConfig,
                                @Value("${IMAGE_AGENT_URL:http://localhost:8080/image}") String imageAgentUrl) {
        this.wordPressTool = wordPressTool;
        this.imageAgentUrl = imageAgentUrl;

        ChatMemory chatMemory = MessageWindowChatMemory.builder()
                .chatMemoryRepository(new InMemoryChatMemoryRepository())
                .maxMessages(100)
                .build();

        ChatClient baseClient = chatClientBuilder.build();

        // Pass 1 Client
        this.factGathererClient = baseClient.mutate()
                .defaultSystem("You are an expert security researcher specializing in AI Security. " +
                        "CRITICAL INSTRUCTIONS:\n" +
                        "1. You MUST use your web crawler tool to cross-reference at least 10 separate articles about the requested topic.\n" +
                        "2. Do not stop or proceed to summarize until you have successfully crawled and read from 10 distinct links.\n" +
                        "3. Parse the returned content and specifically look for sentences that include most of the context provided in the research topic. Extract these highly relevant facts.\n" +
                        "4. Your final response MUST ONLY be a detailed list of bulleted facts based on your findings.\n" +
                        "CRITICAL: You MUST invoke the 'crawl' tool using the native tool calling API. Do NOT print raw JSON tool call blocks (e.g. {\"name\": \"crawl\"...}) in your text output. Wait for the tool to return results before summarizing.")
                .defaultAdvisors(MessageChatMemoryAdvisor.builder(chatMemory).build())
                .defaultTools(webCrawlerConfig)
                .build();

        // Pass 2 Client
        this.bloggerClient = baseClient.mutate()
                .defaultSystem("You are an entertaining, engaging technical writer. " +
                        "Take the provided bullet points and compose a detailed, interesting, and highly educational blog post. " +
                        "Structure the paragraphs closer together and create an opening sentence to begin a new thought. " +
                        "Group similar thoughts or subjects together in a tight paragraph. Make an effort to create more paragraphs with 3 or more sentences that share the same subject matter. " +
                        "Create no less than 3 and no more than 5 paragraphs and reserve summary points for the last paragraph. " +
                        "CRITICAL: Do NOT generate a main title or H1 heading for the article; start directly with the content or an introductory subheading. " +
                        "CRITICAL: You MUST format your entire output using WordPress Gutenberg block syntax. Wrap every heading in <!-- wp:heading -->\n<h2>...</h2>\n<!-- /wp:heading --> and every paragraph in <!-- wp:paragraph -->\n<p>...</p>\n<!-- /wp:paragraph -->. Ensure your HTML is well-formed and do NOT add broken closing tags like </. Do NOT wrap your response in ```html markdown blocks. " +
                        "IMPORTANT: If the provided facts contain any raw JSON, tool calls (like '{\"name\": \"crawl\"}'), or system error messages, IGNORE them completely and do not include them in the blog post.")
                .build();
    }

    @RabbitListener(queuesToDeclare = @Queue("research-tasks"))
    public void processResearchTask(String topic) {
        System.out.println("Researcher Agent received topic: " + topic);

        try {
            // Pass 1: Gather facts
            System.out.println("Starting Pass 1 (Fact Gathering) for: " + topic);
            String facts = factGathererClient.prompt()
                    .user("Please gather facts about the following topic and summarize them as bullet points: " + topic + 
                          "\\n\\nRemember to filter the crawled content for sentences that include most of the context of this topic, and output ONLY bullet points. If you need to use the crawl tool, use the native tool calling API. Do NOT output raw JSON.")
                    .advisors(a -> a.param("chat_memory_conversation_id", "pass1-" + topic))
                    .call()
                    .content();
            
            System.out.println("Gathered facts to be considered for the blog:\n" + facts);

            // Pass 2: Write blog post
            System.out.println("Starting Pass 2 (Blog Writing) for: " + topic);
            String htmlContent = bloggerClient.prompt()
                    .user("Here are the gathered facts:\n" + facts + "\n\nPlease write the HTML blog post.")
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
            java.util.Map<String, String> imageRequest = new java.util.HashMap<>();
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
                contentWithImages = "<!-- wp:image -->\n<figure class=\"wp-block-image\"><img src=\"" + safeHeaderImage + "\" alt=\"Header Image\"/></figure>\n<!-- /wp:image -->\n\n" + contentWithImages;
            }
            if (!inlineImage.isEmpty()) {
                String safeInlineImage = inlineImage.replace("&", "&amp;");
                contentWithImages = contentWithImages + "\n\n<!-- wp:image -->\n<figure class=\"wp-block-image\"><img src=\"" + safeInlineImage + "\" alt=\"Inline Image\"/></figure>\n<!-- /wp:image -->";
            }

            // Upload to WordPress (Local draft)
            System.out.println("Uploading draft to WordPress for topic: " + topic);
            String result = wordPressTool.createDraftPost(new WordPressTool.DraftRequest(topic, contentWithImages));
            System.out.println(result);

        } catch (Exception e) {
            System.err.println("Error processing topic " + topic + ": " + e.getMessage());
            e.printStackTrace();
        }
    }
}
