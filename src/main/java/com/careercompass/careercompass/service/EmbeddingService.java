package com.careercompass.careercompass.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.List;

@Service
public class EmbeddingService {

    @Value("${gemini.api.key}")
    private String apiKey;

    private static final String MODEL = "text-embedding-004";

    private static final String GEMINI_URL = "https://generativelanguage.googleapis.com/v1/models/%s:embedContent?key=%s";

    private final RestTemplate restTemplate = new RestTemplate();
    private final ObjectMapper mapper = new ObjectMapper();

    public List<Float> generateEmbedding(String text) {
        try {
            String url = String.format(GEMINI_URL, MODEL, apiKey);

            // Escape quotes and backslashes in the input text to prevent invalid JSON
            // Also sanitize newlines which can break JSON strings
            String sanitizedText = text.replace("\\", "\\\\")
                    .replace("\"", "\\\"")
                    .replace("\n", " ")
                    .replace("\r", " ");

            String requestBody = """
                    {
                      "content": {
                        "parts": [
                          { "text": "%s" }
                        ]
                      }
                    }
                    """.formatted(sanitizedText);

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);

            HttpEntity<String> entity = new HttpEntity<>(requestBody, headers);

            ResponseEntity<String> response = restTemplate.postForEntity(url, entity, String.class);

            JsonNode root = mapper.readTree(response.getBody());
            JsonNode values = root
                    .path("embedding")
                    .path("values");

            return mapper.convertValue(
                    values,
                    mapper.getTypeFactory()
                            .constructCollectionType(List.class, Float.class));

        } catch (Exception e) {
            throw new RuntimeException(
                    "Gemini embedding failed for text: " + (text.length() > 50 ? text.substring(0, 50) + "..." : text),
                    e);
        }
    }
}
