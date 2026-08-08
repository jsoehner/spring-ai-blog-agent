package com.example.demo.security;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;

@Service
public class OpaClient {
    private static final Logger log = LoggerFactory.getLogger(OpaClient.class);

    private final RestTemplate restTemplate;
    private final String opaUrl;

    public OpaClient(RestTemplate restTemplate, 
                     @Value("${opa.url}") String opaUrl) {
        this.restTemplate = restTemplate;
        this.opaUrl = opaUrl;
    }

    public static class OpaResponse {
        private OpaResult result;

        public OpaResult getResult() {
            return result;
        }

        public void setResult(OpaResult result) {
            this.result = result;
        }
    }

    public static class OpaResult {
        private boolean allow;

        public boolean isAllow() {
            return allow;
        }

        public void setAllow(boolean allow) {
            this.allow = allow;
        }
    }

    public String getOpaUrl() {
        return opaUrl;
    }

    public boolean evaluatePolicy(Map<String, Object> requestInput) {
        return evaluatePolicy(this.opaUrl, requestInput);
    }

    public boolean evaluatePolicy(String url, Map<String, Object> requestInput) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        Map<String, Object> body = new HashMap<>();
        body.put("input", requestInput);

        HttpEntity<Map<String, Object>> entity = new HttpEntity<>(body, headers);

        try {
            ResponseEntity<OpaResponse> response = restTemplate.postForEntity(url, entity, OpaResponse.class);
            OpaResponse resultBody = response.getBody();
            if (resultBody != null && resultBody.getResult() != null) {
                return resultBody.getResult().isAllow();
            }
        } catch (Exception e) {
            log.error("Error calling OPA server at {}: {}", url, e.getMessage());
            return false; // Fail-safe (Deny on error)
        }
        return false;
    }
}
