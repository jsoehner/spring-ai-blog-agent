package com.example.demo;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.MessageChatMemoryAdvisor;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.memory.InMemoryChatMemoryRepository;
import org.springframework.ai.chat.memory.MessageWindowChatMemory;
import org.springframework.amqp.rabbit.annotation.Queue;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import org.springframework.ai.chat.client.advisor.SimpleLoggerAdvisor;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.util.HashMap;
import java.util.Map;

@Component
@Profile("researcher")
public class ResearcherController {

    private final ChatClient factGathererClient;
    private final RabbitTemplate rabbitTemplate;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public ResearcherController(ChatClient.Builder chatClientBuilder,
                                RabbitTemplate rabbitTemplate,
                                WebCrawlerConfig webCrawlerConfig,
                                @Value("${RESEARCHER_PROMPT:You are an expert security researcher specializing in IT Security. First, use your search tool to query for a list of topic-based URLs related to the requested topic. Then, begin parsing the content by crawling the URLs to capture key sentences that include the search terms from the topic. Iterate through until you have successfully parsed content from 10 URLs and captured a large number of key sentences to use in the blog post. You must list any/all of the URLs being used and count until 10 before proceeding to the next phase. Gather a diverse, well-educated perspective and summarize the findings as a detailed list of bulleted facts.}") String researcherPrompt) {
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
            String facts = factGathererClient.prompt()
                    .user("Please gather facts about the following topic and summarize them as bullet points: " + topic)
                    .advisors(a -> a.param("chat_memory_conversation_id", "pass1-" + topic))
                    .call()
                    .content();
            
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
