package com.example.demo;

import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.context.annotation.Profile;
import org.springframework.http.ResponseEntity;

import java.util.List;

@RestController
@Profile("supervisor")
public class BlogAgentController {

    private final RabbitTemplate rabbitTemplate;

    public BlogAgentController(RabbitTemplate rabbitTemplate) {
        this.rabbitTemplate = rabbitTemplate;
    }

    @GetMapping(value = "/blog")
    public ResponseEntity<String> blog(
            @RequestParam(defaultValue = "Recent mobile security threats in Android") List<String> topics) {
        
        for (String topic : topics) {
            System.out.println("Processing topic for research: " + topic);
            rabbitTemplate.convertAndSend("research-tasks", topic);
        }
        
        return ResponseEntity.accepted().body("Queued " + topics.size() + " topics for background processing.");
    }

    @GetMapping(value = "/health")
    public ResponseEntity<String> health() {
        return ResponseEntity.ok("OK");
    }
}
