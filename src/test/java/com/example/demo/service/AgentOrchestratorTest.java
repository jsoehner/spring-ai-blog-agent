package com.example.demo.service;

import com.example.demo.tools.WordPressTool;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Answers;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.web.client.RestTemplate;

import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class AgentOrchestratorTest {

    private AgentOrchestrator orchestrator;

    @Mock private ChatClient.Builder chatClientBuilder;
    @Mock(answer = Answers.RETURNS_DEEP_STUBS) private ChatClient chatClient;
    @Mock private WordPressTool wordPressTool;
    @Mock private RestTemplate restTemplate;
    @Mock private StorageService storageService;
    @Mock private ToolRegistry toolRegistry;
    @Mock private MessageService messageService;
    @Mock private VersionControlService versionControlService;
    @Mock private ContentPipeline contentPipeline;
    @Mock private TextHumanizerProcessor textHumanizerProcessor;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);

        when(chatClientBuilder.build()).thenReturn(chatClient);
        when(chatClient.mutate()).thenReturn(chatClientBuilder);
        when(chatClientBuilder.defaultSystem(anyString())).thenReturn(chatClientBuilder);

        orchestrator = new AgentOrchestrator(
            chatClientBuilder,
            wordPressTool,
            restTemplate,
            "http://mock-image-agent:8080/image",
            new ByteArrayResource("You are a blogger prompt".getBytes(StandardCharsets.UTF_8)),
            storageService,
            toolRegistry,
            messageService,
            versionControlService,
            contentPipeline,
            textHumanizerProcessor
        );
    }

    @Test
    void testHandleSupervisorTask_SuccessWithRetries() throws Exception {
        String payload = "{\"topic\": \"test-topic\", \"facts\": \"test-facts\"}";
        
        when(textHumanizerProcessor.process(anyString())).thenReturn("humanized-facts");
        when(chatClient.prompt().user(anyString()).call().content()).thenReturn("<html>Content</html>");
        when(contentPipeline.process(anyString())).thenReturn("<html>Processed</html>");
        when(restTemplate.postForObject(anyString(), anyMap(), eq(String.class)))
            .thenReturn("http://image.com/1.jpg");
        
        when(wordPressTool.createDraftPost(any())).thenReturn("Success");
        
        orchestrator.handleSupervisorTask(payload);

        verify(chatClient, atLeastOnce()).prompt();
        verify(restTemplate, times(1)).postForObject(anyString(), anyMap(), eq(String.class));
        verify(wordPressTool, times(1)).createDraftPost(any());
        verify(storageService, times(1)).saveBlogPost(eq("test-topic"), anyString());
        verify(storageService, times(1)).saveWordPressPost(eq("test-topic"), anyString());
        assertEquals(AgentOrchestrator.WorkflowState.COMPLETED, orchestrator.getState("test-topic"));
    }

    @Test
    void testHandleSupervisorTask_FailAfterMaxRetries() {
        String payload = "{\"topic\": \"fail-topic\", \"facts\": \"test-facts\"}";
        
        when(textHumanizerProcessor.process(anyString())).thenReturn("humanized-facts");
        when(chatClient.prompt().user(anyString()).call().content())
            .thenThrow(new RuntimeException("Persistent Failure"));

        orchestrator.handleSupervisorTask(payload);

        assertEquals(AgentOrchestrator.WorkflowState.FAILED, orchestrator.getState("fail-topic"));
    }
}
