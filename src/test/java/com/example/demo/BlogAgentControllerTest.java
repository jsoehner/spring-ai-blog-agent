package com.example.demo;

import com.example.demo.security.OpaClient;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.ai.chat.client.ChatClient;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.function.Consumer;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;

class BlogAgentControllerTest {

    private MockMvc mockMvc;
    private RabbitTemplate rabbitTemplate;
    private OpaClient opaClient;
    private ChatClient.Builder chatClientBuilder;
    private WordPressTool wordPressTool;
    private BlogAgentController blogAgentController;
    private ChatClient chatClient;

    @BeforeEach
    void setUp() {
        rabbitTemplate = mock(RabbitTemplate.class);
        opaClient = mock(OpaClient.class);
        chatClientBuilder = mock(ChatClient.Builder.class);
        wordPressTool = mock(WordPressTool.class);
        chatClient = mock(ChatClient.class);
        
        ChatClient.Builder builder = mock(ChatClient.Builder.class);
        
        when(chatClientBuilder.build()).thenReturn(chatClient);
        when(chatClient.mutate()).thenReturn(builder);
        when(builder.defaultSystem(anyString())).thenReturn(builder);
        when(builder.defaultAdvisors(any(org.springframework.ai.chat.client.advisor.SimpleLoggerAdvisor.class))).thenReturn(builder);
        when(builder.build()).thenReturn(chatClient);

        blogAgentController = new BlogAgentController(
                rabbitTemplate,
                opaClient,
                chatClientBuilder,
                wordPressTool,
                "http://localhost:8080/image",
                "Blogger System Prompt"
        );

        mockMvc = MockMvcBuilders.standaloneSetup(blogAgentController).build();
    }

    @Test
    void testBlogEndpointWithAllowedTopic() throws Exception {
        when(opaClient.getOpaUrl()).thenReturn("http://localhost:8181/v1/data/agent/main");
        when(opaClient.evaluatePolicy(anyString(), any())).thenReturn(true);

        mockMvc.perform(get("/blog").param("topics", "AI Security"))
                .andExpect(status().isAccepted())
                .andExpect(content().string("Queued 1 topics for background processing."));

        verify(rabbitTemplate, times(1)).convertAndSend(eq("research-tasks"), eq("AI Security"));
    }

    @Test
    void testBlogEndpointWithBannedTopic() throws Exception {
        when(opaClient.getOpaUrl()).thenReturn("http://localhost:8181/v1/data/agent/main");
        when(opaClient.evaluatePolicy(anyString(), any())).thenReturn(false);

        mockMvc.perform(get("/blog").param("topics", "bomb explosive"))
                .andExpect(status().isAccepted())
                .andExpect(content().string("Queued 1 topics for background processing."));

        verify(rabbitTemplate, never()).convertAndSend(anyString(), anyString());
    }

    @Test
    void testProcessSupervisorTaskPathTraversalPrevention() throws Exception {
        ChatClient.ChatClientRequestSpec requestSpec = mock(ChatClient.ChatClientRequestSpec.class);
        ChatClient.CallResponseSpec responseSpec = mock(ChatClient.CallResponseSpec.class);
        
        when(chatClient.prompt()).thenReturn(requestSpec);
        when(requestSpec.user(anyString())).thenReturn(requestSpec);
        when(requestSpec.advisors(any(Consumer.class))).thenReturn(requestSpec);
        when(requestSpec.call()).thenReturn(responseSpec);
        when(responseSpec.content()).thenReturn("<html>Sample Content</html>");

        when(wordPressTool.createDraftPost(any())).thenReturn("Draft uploaded");

        String payload = "{\"topic\":\"../../traversal-test\",\"facts\":\"Some facts here\"}";
        
        blogAgentController.processSupervisorTask(payload);

        Path resolvedPath = Paths.get("output", "traversal-test.html").toAbsolutePath().normalize();
        assertTrue(Files.exists(resolvedPath), "File should be saved in normalized path under output directory");
        
        Files.deleteIfExists(resolvedPath);
    }
}
