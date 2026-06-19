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
                                @Value("${IMAGE_AGENT_URL:http://localhost:8080/image}") String imageAgentUrl) {
        this.wordPressTool = wordPressTool;
        this.imageAgentUrl = imageAgentUrl;

        ChatMemory chatMemory = MessageWindowChatMemory.builder()
                .chatMemoryRepository(new InMemoryChatMemoryRepository())
                .maxMessages(100)
                .build();

        // Pass 1 Client
        this.factGathererClient = chatClientBuilder
                .defaultSystem("You are an expert security researcher specializing in AI Security. " +
                        "Use your web crawler tool to cross reference 5 separate articles about the requested topic. " +
                        "Gather a diverse, well-educated perspective and summarize the findings as a detailed list of bulleted facts.")
                .defaultAdvisors(MessageChatMemoryAdvisor.builder(chatMemory).build())
                .defaultTools(new WebCrawlerConfig())
                .build();

        // Pass 2 Client
        this.bloggerClient = chatClientBuilder
                .defaultSystem("You are an entertaining, engaging technical writer. " +
                        "Take the provided bullet points and compose a detailed, interesting, and highly educational blog post formatted using proper HTML. " +
                        "Structure the paragraphs closer together and create an opening sentence to begin a new thought. " +
                        "Group similar thoughts or subjects together in a tight paragraph. " +
                        "Create no less than 3 and no more than 5 paragraphs and reserve summary points for the last paragraph. " +
                        "Do NOT wrap your response in ```html or ``` markdown blocks. Output only raw HTML.")
                .build();
    }

    @RabbitListener(queuesToDeclare = @Queue("research-tasks"))
    public void processResearchTask(String topic) {
        System.out.println("Researcher Agent received topic: " + topic);

        try {
            // Pass 1: Gather facts
            System.out.println("Starting Pass 1 (Fact Gathering) for: " + topic);
            String facts = factGathererClient.prompt()
                    .user("Please gather facts about the following topic and summarize them as bullet points: " + topic)
                    .advisors(a -> a.param("chat_memory_conversation_id", "pass1-" + topic))
                    .call()
                    .content();

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
                contentWithImages = "<img src=\"" + headerImage + "\" alt=\"Header Image\" style=\"width:100%;max-width:800px;\"/><br/><br/>\n" + contentWithImages;
            }
            if (!inlineImage.isEmpty()) {
                contentWithImages = contentWithImages + "\n<br/><br/><img src=\"" + inlineImage + "\" alt=\"Inline Image\" style=\"width:100%;max-width:800px;\"/>";
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
