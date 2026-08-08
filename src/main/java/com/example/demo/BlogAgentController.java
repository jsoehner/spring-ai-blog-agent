package com.example.demo;

import org.springframework.ai.chat.client.advisor.SimpleLoggerAdvisor;
import org.springframework.amqp.rabbit.annotation.Queue;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Profile;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.example.demo.config.RabbitConfig;
import com.example.demo.service.AgentOrchestrator;
import com.example.demo.service.BlogAgentService;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;

@RestController
@Profile("supervisor")
public class BlogAgentController {

    private final BlogAgentService blogAgentService;
    private final AgentOrchestrator agentOrchestrator;
    private final Executor agentExecutor;

    @Autowired
    public BlogAgentController(BlogAgentService blogAgentService, 
                               AgentOrchestrator agentOrchestrator,
                               @org.springframework.beans.factory.annotation.Qualifier("agentExecutor") Executor agentExecutor) {
        this.blogAgentService = blogAgentService;
        this.agentOrchestrator = agentOrchestrator;
        this.agentExecutor = agentExecutor;
    }

    @GetMapping(value = "/blog")
    public ResponseEntity<String> blog(
            @RequestParam(defaultValue = "Recent mobile security threats in Android") List<String> topics) {
        
        List<String> queued = blogAgentService.queueBlogTopics(topics);
        
        return ResponseEntity.accepted().body("Queued " + queued.size() + " topics for background processing.");
    }

    @RabbitListener(queuesToDeclare = @Queue(RabbitConfig.QUEUE_SUPERVISOR))
    public CompletableFuture<Void> processSupervisorTask(String jsonPayload) {
        return CompletableFuture.runAsync(() -> {
            agentOrchestrator.handleSupervisorTask(jsonPayload);
        }, agentExecutor);
    }
}
