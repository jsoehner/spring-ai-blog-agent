package com.example.demo;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.MessageChatMemoryAdvisor;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.memory.InMemoryChatMemoryRepository;
import org.springframework.ai.chat.memory.MessageWindowChatMemory;
import org.springframework.ai.converter.BeanOutputConverter;
import org.springframework.amqp.rabbit.annotation.Queue;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import org.springframework.ai.chat.client.advisor.SimpleLoggerAdvisor;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Component
@Profile("researcher")
public class ResearcherController {

    public record Fact(String claim, String sourceUrl) {}
    public record ResearchReport(String summary, List<Fact> facts) {}

    private final ChatClient factGathererClient;
    private final RabbitTemplate rabbitTemplate;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public ResearcherController(ChatClient.Builder chatClientBuilder,
                                RabbitTemplate rabbitTemplate,
                                WebCrawlerConfig webCrawlerConfig,
                                @Value("${RESEARCHER_PROMPT:You are an expert security researcher specializing in IT Security. First, use your search tool to query for a list of topic-based URLs related to the requested topic. Then, begin parsing the content by crawling the URLs to capture key sentences that include the search terms from the topic. IMPORTANT: You must ONLY perform exactly ONE search. Do not search again even if the URLs seem generic. Do not wait for 10 URLs. Just crawl the ones you received and proceed immediately to summarize the findings as a detailed list of bulleted facts.}") String researcherPrompt) {
        this.rabbitTemplate = rabbitTemplate;

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
                .defaultTools(webCrawlerConfig)
                .build();
    }

    @RabbitListener(queuesToDeclare = @Queue("research-tasks"))
    public void processResearchTask(String topic) {
        System.out.println("Researcher Agent received topic: " + topic);

        try {
            // Pass 1: Gather facts
            System.out.println("Starting Pass 1 (Fact Gathering) for: " + topic);
            
            BeanOutputConverter<ResearchReport> converter = new BeanOutputConverter<>(ResearchReport.class);
            String formatInstructions = converter.getFormat();
            
            String promptText = "Please gather facts about the following topic and summarize them. Include the source URLs for each fact: " + topic + "\n\n" + formatInstructions;
            
            ResearchReport report = null;
            int maxAttempts = 3;
            for (int i = 1; i <= maxAttempts; i++) {
                try {
                    String response = factGathererClient.prompt()
                            .user(promptText)
                            .advisors(a -> a.param("chat_memory_conversation_id", "pass1-" + topic))
                            .call()
                            .content();
                    
                    report = converter.convert(response);
                    break; // Success!
                } catch (Exception e) {
                    System.err.println("Attempt " + i + " failed to parse JSON. Error: " + e.getMessage());
                    if (i == maxAttempts) {
                        throw e;
                    }
                    // Self-correction prompt
                    promptText = "Your previous response was not valid JSON or did not match the schema. " +
                                 "Error: " + e.getMessage() + "\n\n" +
                                 "Please try again. You must output valid JSON matching this schema:\n" + formatInstructions;
                }
            }

            String facts = "Summary: " + report.summary() + "\n\nFacts:\n";
            if (report.facts() != null) {
                for (Fact f : report.facts()) {
                    facts += "- " + f.claim() + " (" + f.sourceUrl() + ")\n";
                }
            }
            
            System.out.println("Gathered facts to be considered for the blog:\n" + facts);

            // Send compiled content to Supervisor Agent
            System.out.println("Sending compiled content to Supervisor Agent for topic: " + topic);
            Map<String, String> payload = new HashMap<>();
            payload.put("topic", topic);
            payload.put("facts", facts);
            String jsonPayload = objectMapper.writeValueAsString(payload);
            rabbitTemplate.convertAndSend("supervisor-tasks", jsonPayload);

        } catch (Exception e) {
            System.err.println("Error processing topic " + topic + ": " + e.getMessage());
            e.printStackTrace();
        }
    }
}
