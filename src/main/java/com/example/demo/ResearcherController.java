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

import org.springframework.ai.chat.client.advisor.SimpleLoggerAdvisor;

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
                                @Value("${IMAGE_AGENT_URL:http://localhost:8080/image}") String imageAgentUrl,
                                @Value("${RESEARCHER_PROMPT:You are an expert security researcher specializing in IT Security. First, use your search tool to query for a list of topic-based URLs related to the requested topic. Then, begin parsing the content by crawling the URLs to capture key sentences that include the search terms from the topic. Iterate through until you have successfully parsed content from 10 URLs and captured a large number of key sentences to use in the blog post. You must list any/all of the URLs being used and count until 10 before proceeding to the next phase. Gather a diverse, well-educated perspective and summarize the findings as a detailed list of bulleted facts.}") String researcherPrompt,
                                @Value("${BLOGGER_PROMPT:You are an entertaining, engaging technical writer. Take the provided bullet points and compose a detailed, interesting, and highly educational blog post. Structure the content so that each paragraph contains 3 or more sentences of similar subject matter and message. Focus on good sentence structure with an opening sentence starting a new paragraph. Follow with several sentences that contain a noun and a verb and avoid too many colourful adverbs and adjectives that could make the sentence run-on. Pay close attention to your punctuation: use commas where the sentence flow changes, and don't be afraid to break one big sentence into two shorter, clearer sentences. Provide substantial content and aim to have between 750 and 1000 words in the blog post. Reserve summary points for the last paragraph. CRITICAL: Do NOT generate a main title or H1 heading for the article; start directly with the content or an introductory subheading. CRITICAL: You MUST format your entire output using WordPress Gutenberg block syntax. Wrap every heading in <!-- wp:heading --><h2>...</h2><!-- /wp:heading --> and every paragraph in <!-- wp:paragraph --><p>...</p><!-- /wp:paragraph -->. Ensure your HTML is well-formed and do NOT add broken closing tags like </. Do NOT wrap your response in ```html markdown blocks.}") String bloggerPrompt) {
        this.wordPressTool = wordPressTool;
        this.imageAgentUrl = imageAgentUrl;

        ChatMemory chatMemory = MessageWindowChatMemory.builder()
                .chatMemoryRepository(new InMemoryChatMemoryRepository())
                .maxMessages(100)
                .build();

        ChatClient baseClient = chatClientBuilder.build();

        // Pass 1 Client
        this.factGathererClient = baseClient.mutate()
                .defaultSystem(researcherPrompt)
                .defaultAdvisors(
                        MessageChatMemoryAdvisor.builder(chatMemory).build(),
                        new SimpleLoggerAdvisor()
                )
                .defaultTools(new WebCrawlerConfig())
                .build();

        // Pass 2 Client
        this.bloggerClient = baseClient.mutate()
                .defaultSystem(bloggerPrompt)
                .defaultAdvisors(new SimpleLoggerAdvisor())
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
                contentWithImages = "<!-- wp:image --><figure class=\"wp-block-image\"><img src=\"" + safeHeaderImage + "\" alt=\"Header Image\"/></figure><!-- /wp:image -->" + contentWithImages;
            }
            if (!inlineImage.isEmpty()) {
                String safeInlineImage = inlineImage.replace("&", "&amp;");
                contentWithImages = contentWithImages + "<!-- wp:image --><figure class=\"wp-block-image\"><img src=\"" + safeInlineImage + "\" alt=\"Inline Image\"/></figure><!-- /wp:image -->";
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
