package com.careercompass.careercompass.service;

import com.fasterxml.jackson.core.json.JsonReadFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class GroqClient {
    private static final Logger log = LoggerFactory.getLogger(GroqClient.class);

    @Value("${groq.api.key}")
    private String groqApiKey;

    private static final String GROQ_URL = "https://api.groq.com/openai/v1/chat/completions";
    private static final String MODEL_POWERFUL = "llama-3.3-70b-versatile";
    private static final String MODEL_FAST = "llama-3.3-70b-versatile";
    private static final String MODEL_BALANCED = "llama-3.3-70b-versatile";

    private final RestTemplate restTemplate = new RestTemplate();
    private final ObjectMapper objectMapper;
    private final CacheService cacheService;

    public enum QueryComplexity {
        FAST, BALANCED, POWERFUL
    }

    public GroqClient(CacheService cacheService) {
        this.cacheService = cacheService;
        this.objectMapper = new ObjectMapper();
        this.objectMapper.configure(JsonReadFeature.ALLOW_UNESCAPED_CONTROL_CHARS.mappedFeature(), true);
    }

    public String callGroq(String prompt, QueryComplexity complexity) {
        if (prompt == null || prompt.isBlank())
            return null;

        String model = selectModel(complexity);
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(groqApiKey);
        headers.setContentType(MediaType.APPLICATION_JSON);

        Map<String, Object> body = new HashMap<>();
        body.put("model", model);
        body.put("messages", List.of(
                Map.of("role", "system", "content", "You are a professional career assistant."),
                Map.of("role", "user", "content", prompt)));

        // Adjust temperature based on complexity
        switch (complexity) {
            case FAST -> body.put("temperature", 0.2);
            case BALANCED -> body.put("temperature", 0.4);
            case POWERFUL -> body.put("temperature", 0.5);
        }

        HttpEntity<Map<String, Object>> entity = new HttpEntity<>(body, headers);

        int maxRetries = 3;
        for (int i = 0; i < maxRetries; i++) {
            try {
                ResponseEntity<Map> response = restTemplate.postForEntity(GROQ_URL, entity, Map.class);
                if (response.getStatusCode() == HttpStatus.OK && response.getBody() != null) {
                    Map<String, Object> responseBody = response.getBody();
                    List<Map<String, Object>> choices = (List<Map<String, Object>>) responseBody.get("choices");
                    if (choices != null && !choices.isEmpty()) {
                        Map<String, Object> message = (Map<String, Object>) choices.get(0).get("message");
                        return (String) message.get("content");
                    }
                }
            } catch (Exception e) {
                log.warn("⚠️ Groq API Error (Attempt {}/{}): {}", (i + 1), maxRetries, e.getMessage());
                if (i == maxRetries - 1)
                    return null;
                try {
                    Thread.sleep(1000L * (i + 1) * (i + 1));
                } catch (InterruptedException ignored) {
                }
            }
        }
        return null;
    }

    public String selectModel(QueryComplexity complexity) {
        return switch (complexity) {
            case FAST -> MODEL_FAST;
            case POWERFUL -> MODEL_POWERFUL;
            case BALANCED -> MODEL_BALANCED;
        };
    }

    public String getGroqUrl() {
        return GROQ_URL;
    }

    public String getApiKey() {
        return groqApiKey;
    }

    public ObjectMapper getObjectMapper() {
        return objectMapper;
    }

    public RestTemplate getRestTemplate() {
        return restTemplate;
    }
}
