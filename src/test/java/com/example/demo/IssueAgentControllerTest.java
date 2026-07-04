package com.example.demo;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.ai.chat.client.ChatClient;

import java.nio.file.Path;
import java.nio.file.Paths;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;

class IssueAgentControllerTest {

    private MockMvc mockMvc;
    private ChatClient.Builder chatClientBuilder;
    private ChatClient chatClient;
    private IssueAgentTools issueAgentTools;
    private IssueAgentController issueAgentController;

    @BeforeEach
    void setUp() {
        chatClientBuilder = mock(ChatClient.Builder.class);
        chatClient = mock(ChatClient.class);
        issueAgentTools = mock(IssueAgentTools.class);

        ChatClient.Builder builder = mock(ChatClient.Builder.class);

        when(chatClientBuilder.build()).thenReturn(chatClient);
        when(chatClient.mutate()).thenReturn(builder);
        when(builder.defaultSystem(anyString())).thenReturn(builder);
        when(builder.defaultAdvisors(any(org.springframework.ai.chat.client.advisor.SimpleLoggerAdvisor.class))).thenReturn(builder);
        when(builder.defaultTools(any())).thenReturn(builder);
        when(builder.build()).thenReturn(chatClient);

        issueAgentController = new IssueAgentController(
                chatClientBuilder,
                issueAgentTools,
                "Prompt"
        );

        mockMvc = MockMvcBuilders.standaloneSetup(issueAgentController).build();
    }

    @Test
    void testCreatePrEndpoint() throws Exception {
        ChatClient.ChatClientRequestSpec requestSpec = mock(ChatClient.ChatClientRequestSpec.class);
        ChatClient.CallResponseSpec responseSpec = mock(ChatClient.CallResponseSpec.class);

        when(chatClient.prompt()).thenReturn(requestSpec);
        when(requestSpec.user(anyString())).thenReturn(requestSpec);
        when(requestSpec.call()).thenReturn(responseSpec);
        when(responseSpec.content()).thenReturn("PR successfully created and changes merged.");

        when(issueAgentTools.setupRepository(anyString(), anyString(), anyString()))
                .thenReturn(Paths.get("build/cloned-repos/owner/repo"));

        mockMvc.perform(get("/issue/create-pr")
                        .param("issueRef", "owner/repo/issues/123")
                        .param("baseBranch", "main"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("success"))
                .andExpect(jsonPath("$.message").value("PR successfully created and changes merged."));

        verify(issueAgentTools, times(1)).setupRepository("owner", "repo", "main");
        verify(issueAgentTools, times(1)).setCurrentRepoRoot(any(Path.class));
        verify(issueAgentTools, times(1)).clearCurrentRepoRoot();
    }
}
