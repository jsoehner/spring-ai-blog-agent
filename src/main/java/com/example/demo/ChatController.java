package com.example.demo;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.MessageChatMemoryAdvisor;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.memory.InMemoryChatMemoryRepository;
import org.springframework.ai.chat.memory.MessageWindowChatMemory;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class ChatController {

    private final ChatClient chatClient;
    private final WebCrawlerConfig webCrawlerConfig;
    private final StockPriceTool stockPriceTool;
    private final ImageTools imageTools;
    private final CodeTools codeTools;
    private final TlsScannerTool tlsScannerTool;

    public ChatController(ChatClient.Builder chatClientBuilder,
                          WebCrawlerConfig webCrawlerConfig,
                          StockPriceTool stockPriceTool,
                          ImageTools imageTools,
                          CodeTools codeTools,
                          TlsScannerTool tlsScannerTool) {
        this.webCrawlerConfig = webCrawlerConfig;
        this.stockPriceTool = stockPriceTool;
        this.imageTools = imageTools;
        this.codeTools = codeTools;
        this.tlsScannerTool = tlsScannerTool;

        // 1. Initialize an in-memory repository and window memory strategy
        ChatMemory chatMemory = MessageWindowChatMemory.builder()
            .chatMemoryRepository(new InMemoryChatMemoryRepository())
            .maxMessages(100)
            .build();
        
        // 2. Build the ChatClient and register the Memory Advisor using the builder
        this.chatClient = chatClientBuilder.build().mutate()
                .defaultSystem("You are an expert code reviewer. You review code and provide feedback. You support Java, Python, and Go.")
                .defaultAdvisors(MessageChatMemoryAdvisor.builder(chatMemory).build())
                .build();
    }

    @GetMapping("/chat")
    public String chat(
            @RequestParam(defaultValue = "Tell me a short joke") String message,
            @RequestParam(defaultValue = "user-1") String chatId) {
        
        return chatClient.prompt()
                .user(message)
                // 3. Pass the chatId so the advisor knows which conversation history to use
                .advisors(a -> a.param("chat_memory_conversation_id", chatId))
                .tools(webCrawlerConfig, stockPriceTool, imageTools, codeTools, tlsScannerTool)
                .call()
                .content();
    }
}
