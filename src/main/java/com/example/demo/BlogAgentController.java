package com.example.demo;

import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.context.annotation.Profile;
import org.springframework.http.ResponseEntity;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.example.demo.security.OpaClient;
@RestController
@Profile("supervisor")
public class BlogAgentController {

    private final RabbitTemplate rabbitTemplate;
    private final OpaClient opaClient;

    public BlogAgentController(RabbitTemplate rabbitTemplate, OpaClient opaClient) {
        this.rabbitTemplate = rabbitTemplate;
        this.opaClient = opaClient;
    }

    @GetMapping(value = "/blog")
    public ResponseEntity<String> blog(
            @RequestParam(defaultValue = "Recent mobile security threats in Android") List<String> topics) {
        
        for (String topic : topics) {
            Map<String, Object> input = new HashMap<>();
            input.put("resource_type", "topic");
            Map<String, Object> request = new HashMap<>();
            request.put("topic", topic);
            input.put("request", request);

            if (!opaClient.evaluatePolicy(input)) {
                System.out.println("Topic rejected by OPA policy: " + topic);
                continue;
            }
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
