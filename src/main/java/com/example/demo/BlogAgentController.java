package com.example.demo;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.MessageChatMemoryAdvisor;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.memory.InMemoryChatMemoryRepository;
import org.springframework.ai.chat.memory.MessageWindowChatMemory;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.http.MediaType;
import reactor.core.publisher.Flux;

@RestController
public class BlogAgentController {

    private final ChatClient chatClient;
    private final WordPressTool wordPressTool;

    public BlogAgentController(ChatClient.Builder chatClientBuilder, WordPressTool wordPressTool) {
        ChatMemory chatMemory = MessageWindowChatMemory.builder()
            .chatMemoryRepository(new InMemoryChatMemoryRepository())
            .maxMessages(100)
            .build();
        
        this.wordPressTool = wordPressTool;
        this.chatClient = chatClientBuilder
                .defaultSystem("You are an expert security analyst and blog poster agent. Your task is to research a given subject related to Mobile Security, Cryptography, Application Security, or AI Security, and compose a detailed and engaging blog post formatted using proper HTML. The blog post must contain at least 5 to 10 paragraphs, with each paragraph being 100+ words.")
                .defaultAdvisors(MessageChatMemoryAdvisor.builder(chatMemory).build())
                .defaultTools(new WebCrawlerConfig())
                .build();
    }

    @GetMapping(value = "/blog", produces = MediaType.TEXT_PLAIN_VALUE)
    public String blog(
            @RequestParam(defaultValue = "Recent mobile security threats in Android") String topic,
            @RequestParam(defaultValue = "blog-1") String chatId) {
        
        String fullContent = chatClient.prompt()
                .user("Please research and draft a blog post on the following topic: " + topic)
                .advisors(a -> a.param("chat_memory_conversation_id", chatId))
                .call()
                .content();
                
        String wordpressResult = wordPressTool.createDraftPost(new WordPressTool.DraftRequest(topic, fullContent));
        
        return "==========================================\n" +
               "WORDPRESS UPLOAD STATUS:\n" + 
               wordpressResult + "\n" +
               "==========================================\n\n" +
               "Generated Blog Content:\n\n" + 
               fullContent;
    }
}
