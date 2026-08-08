package com.example.demo.service;

import com.example.demo.security.OpaClient;
import org.springframework.stereotype.Service;
import java.util.Map;

@Service
public class OpaService {

    private final OpaClient opaClient;

    public OpaService(OpaClient opaClient) {
        this.opaClient = opaClient;
    }

    public boolean isTopicAllowed(String topic) {
        Map<String, Object> input = new java.util.HashMap<>();
        input.put("topic", topic);

        String topicOpaUrl = opaClient.getOpaUrl().replace("/agent/main", "/blog");
        return opaClient.evaluatePolicy(topicOpaUrl, input);
    }
}
