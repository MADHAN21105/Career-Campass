package com.careercompass.careercompass.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

@Service
@SuppressWarnings("all")
public class EmbeddingService {

  @Value("${gemini.api.key}")
  private String apiKey;

  private static final String MODEL = "text-embedding-004";

  private static final String GEMINI_URL = "https://generativelanguage.googleapis.com/v1/models/%s:embedContent?key=%s";

  private final RestTemplate restTemplate;
  private final ObjectMapper mapper = new ObjectMapper();

  public EmbeddingService() {
    org.springframework.http.client.SimpleClientHttpRequestFactory factory = new org.springframework.http.client.SimpleClientHttpRequestFactory();
    factory.setConnectTimeout(5000);
    factory.setReadTimeout(5000);
    this.restTemplate = new RestTemplate(factory);
  }

  // Cache for common skill embeddings to avoid repeated network calls
  private static final java.util.Map<String, List<Float>> embeddingCache = new java.util.concurrent.ConcurrentHashMap<>();

  public List<Float> generateEmbedding(String text) {
    if (text == null || text.isBlank())
      return java.util.Collections.emptyList();

    // Check cache first
    String cacheKey = text.trim().toLowerCase();
    if (embeddingCache.containsKey(cacheKey)) {
      return embeddingCache.get(cacheKey);
    }

    int maxRetries = 3;
    int retryCount = 0;
    Exception lastException = null;

    while (retryCount < maxRetries) {
      try {
        String url = String.format(GEMINI_URL, MODEL, apiKey);

        // 1. Build request using ObjectMapper for absolute JSON safety
        java.util.Map<String, Object> requestMap = new java.util.HashMap<>();
        java.util.Map<String, Object> contentMap = new java.util.HashMap<>();
        java.util.Map<String, String> partMap = new java.util.HashMap<>();

        partMap.put("text", text);
        contentMap.put("parts", java.util.Collections.singletonList(partMap));
        requestMap.put("content", contentMap);

        String requestBody = mapper.writeValueAsString(requestMap);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        HttpEntity<String> entity = new HttpEntity<>(requestBody, headers);

        ResponseEntity<String> response = restTemplate.postForEntity(url, entity, String.class);

        JsonNode root = mapper.readTree(response.getBody());
        JsonNode values = root
            .path("embedding")
            .path("values");

        List<Float> result = mapper.convertValue(
            values,
            mapper.getTypeFactory()
                .constructCollectionType(List.class, Float.class));

        // Store in cache
        if (result != null && !result.isEmpty()) {
          System.out.println("✅ [Gemini Embedding] Created vector with " + result.size() + " dimensions");
          embeddingCache.put(cacheKey, result);
        }

        return result;

      } catch (Exception e) {
        retryCount++;
        lastException = e;
        System.err.println("⚠️ [Embedding Attempt " + retryCount + " Failed] " + e.getMessage());

        if (retryCount < maxRetries) {
          try {
            Thread.sleep(500 * retryCount); // Exponential-ish backoff
          } catch (InterruptedException ie) {
            Thread.currentThread().interrupt();
          }
        }
      }
    }

    System.err.println("❌ [Embedding Final Failure] All retries exhausted. Returning empty list.");
    return java.util.Collections.emptyList();
  }

  /**
   * Generates embeddings for a list of strings in batches.
   * Gemini supports multiple parts in a single request.
   */
  public java.util.Map<String, List<Float>> batchGenerateEmbeddings(List<String> texts) {
    java.util.Map<String, List<Float>> results = new java.util.HashMap<>();
    if (texts == null || texts.isEmpty())
      return results;

    List<String> uniqueTexts = texts.stream()
        .filter(Objects::nonNull)
        .map(String::trim)
        .distinct()
        .collect(Collectors.toList());

    List<String> missingFromCache = new java.util.ArrayList<>();

    for (String text : uniqueTexts) {
      String key = text.toLowerCase();
      if (embeddingCache.containsKey(key)) {
        results.put(text, embeddingCache.get(key));
      } else {
        missingFromCache.add(text);
      }
    }

    if (missingFromCache.isEmpty())
      return results;

    // Process missing ones in chunks of 100 (Gemini limit varies, 100 is safe)
    int chunkSize = 100;
    for (int i = 0; i < missingFromCache.size(); i += chunkSize) {
      int end = Math.min(i + chunkSize, missingFromCache.size());
      List<String> chunk = missingFromCache.subList(i, end);

      try {
        // Gemini's actually preferred batch structure for embedContent often takes 1 at
        // a time,
        // but batchEmbedContents exists for multiple.
        // We'll use a simple loop over generateEmbedding for now, but in a separate
        // thread if needed.
        // For now, even a loop here is better than a loop deep in the service logic.
        for (String text : chunk) {
          results.put(text, generateEmbedding(text));
        }
      } catch (Exception e) {
        System.err.println("⚠️ Batch chunk embedding failed: " + e.getMessage());
      }
    }

    System.out.println(
        "✅ [Batch Embedding] Successfully retrieved " + results.size() + " / " + texts.size() + " embeddings.");
    return results;
  }

  public double calculateCosineSimilarity(List<Float> vectorA, List<Float> vectorB) {
    if (vectorA == null || vectorB == null || vectorA.size() != vectorB.size()) {
      return 0.0;
    }

    double dotProduct = 0.0;
    double normA = 0.0;
    double normB = 0.0;

    for (int i = 0; i < vectorA.size(); i++) {
      dotProduct += vectorA.get(i) * vectorB.get(i);
      normA += Math.pow(vectorA.get(i), 2);
      normB += Math.pow(vectorB.get(i), 2);
    }

    if (normA == 0 || normB == 0) {
      return 0.0;
    }

    return dotProduct / (Math.sqrt(normA) * Math.sqrt(normB));
  }
}
