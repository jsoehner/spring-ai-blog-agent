package com.example.demo.security;

import org.springframework.beans.factory.annotation.Value;

import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;
import java.util.Map;

@Service
public class OpaClient {

    private final RestTemplate restTemplate;
    private final String opaUrl;

    public OpaClient(RestTemplate restTemplate, 
                     @Value("${opa.url:http://localhost:8181/v1/data/agent/main}") String opaUrl) {
        this.restTemplate = restTemplate;
        this.opaUrl = opaUrl;
    }

    public boolean evaluatePolicy(Map<String, Object> requestInput) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        Map<String, Object> body = new HashMap<>();
        body.put("input", requestInput);

        HttpEntity<Map<String, Object>> entity = new HttpEntity<>(body, headers);

        try {
            ResponseEntity<Map> response = restTemplate.postForEntity(opaUrl, entity, Map.class);
            Map<String, Object> resultBody = response.getBody();
            if (resultBody != null && resultBody.containsKey("result")) {
                Map<String, Object> result = (Map<String, Object>) resultBody.get("result");
                if (result.containsKey("allow")) {
                    return (Boolean) result.get("allow");
                }
            }
        } catch (Exception e) {
            // Log error
            System.err.println("Error calling OPA server: " + e.getMessage());
            return false; // Fail-safe (Deny on error)
        }
        return false;
    }
}
