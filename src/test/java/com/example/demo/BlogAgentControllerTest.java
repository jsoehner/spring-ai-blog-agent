package com.example.demo;

import com.example.demo.service.AgentOrchestrator;
import com.example.demo.service.BlogAgentService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.Collections;
import java.util.List;
import java.util.concurrent.Executor;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;

class BlogAgentControllerTest {

    private MockMvc mockMvc;
    private BlogAgentService blogAgentService;
    private AgentOrchestrator agentOrchestrator;
    private BlogAgentController blogAgentController;

    @BeforeEach
    void setUp() {
        blogAgentService = mock(BlogAgentService.class);
        agentOrchestrator = mock(AgentOrchestrator.class);
        Executor agentExecutor = Runnable::run; // direct executor for testing

        blogAgentController = new BlogAgentController(
                blogAgentService,
                agentOrchestrator,
                agentExecutor
        );

        mockMvc = MockMvcBuilders.standaloneSetup(blogAgentController).build();
    }

    @Test
    void testBlogEndpointWithAllowedTopic() throws Exception {
        when(blogAgentService.queueBlogTopics(List.of("AI Security"))).thenReturn(List.of("AI Security"));

        mockMvc.perform(get("/blog").param("topics", "AI Security"))
                .andExpect(status().isAccepted())
                .andExpect(content().string("Queued 1 topics for background processing."));

        verify(blogAgentService, times(1)).queueBlogTopics(List.of("AI Security"));
    }

    @Test
    void testBlogEndpointWithBannedTopic() throws Exception {
        when(blogAgentService.queueBlogTopics(List.of("bomb explosive"))).thenReturn(Collections.emptyList());

        mockMvc.perform(get("/blog").param("topics", "bomb explosive"))
                .andExpect(status().isAccepted())
                .andExpect(content().string("Queued 0 topics for background processing."));

        verify(blogAgentService, times(1)).queueBlogTopics(List.of("bomb explosive"));
    }

    @Test
    void testProcessSupervisorTaskDelegatesToOrchestrator() {
        String payload = "{\"topic\":\"test-topic\",\"facts\":\"Some facts here\"}";

        blogAgentController.processSupervisorTask(payload).join();

        verify(agentOrchestrator, times(1)).handleSupervisorTask(payload);
    }
}

