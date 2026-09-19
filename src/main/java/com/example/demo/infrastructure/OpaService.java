package com.example.demo.infrastructure;

import com.example.demo.infrastructure.OpaClient;
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

        return opaClient.evaluatePolicy(opaClient.getOpaUrl(), input);
    }
}
