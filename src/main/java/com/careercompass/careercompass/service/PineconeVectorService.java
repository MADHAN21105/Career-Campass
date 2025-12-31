package com.careercompass.careercompass.service;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import okhttp3.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.*;

@Service
@SuppressWarnings("all")
public class PineconeVectorService {

    @Autowired
    private EmbeddingService embeddingService;

    @Value("${pinecone.index.url}")
    private String indexUrl;

    @Value("${pinecone.api.key}")
    private String apiKey;

    private final OkHttpClient httpClient = new OkHttpClient();
    private final Gson gson = new Gson();
    private static final MediaType JSON = MediaType.get("application/json; charset=utf-8");

    /**
     * Upsert a batch of snippets into Pinecone index (Optimized for bulk)
     * 
     * @param snippets Batch of snippets to store
     */
    public void upsertSnippetsBatch(List<CsvSnippetLoader.Snippets> snippets) {
        if (snippets.isEmpty())
            return;

        try {
            JsonObject requestBody = new JsonObject();
            JsonArray vectors = new JsonArray();

            for (CsvSnippetLoader.Snippets snippet : snippets) {
                // Generate embedding for the snippet's advice text
                String textToEmbed = snippet.getTopic() + " " + snippet.getAdviceText();
                List<Float> embedding = embeddingService.generateEmbedding(textToEmbed);

                // Create vector with metadata
                JsonObject vector = new JsonObject();
                vector.addProperty("id", snippet.getId());

                JsonArray values = new JsonArray();
                for (Float value : embedding) {
                    values.add(value);
                }
                vector.add("values", values);

                // Add metadata
                JsonObject metadata = new JsonObject();
                metadata.addProperty("topic", snippet.getTopic());
                metadata.addProperty("category", snippet.getCategory());
                metadata.addProperty("adviceText", snippet.getAdviceText());
                metadata.addProperty("keywords", String.join(",", snippet.getKeywords()));
                vector.add("metadata", metadata);

                vectors.add(vector);
            }

            requestBody.add("vectors", vectors);

            RequestBody body = RequestBody.create(gson.toJson(requestBody), JSON);

            Request request = new Request.Builder()
                    .url(indexUrl + "/vectors/upsert")
                    .addHeader("Api-Key", apiKey)
                    .addHeader("Content-Type", "application/json")
                    .addHeader("X-Pinecone-Api-Version", "2024-10")
                    .post(body)
                    .build();

            try (Response response = httpClient.newCall(request).execute()) {
                if (!response.isSuccessful()) {
                    String errorBody = response.body() != null ? response.body().string() : "No error body";
                    throw new IOException(
                            "Pinecone batch upsert failed: " + response.code() + " - " + response.message()
                                    + " - " + errorBody);
                }
            }

        } catch (Exception e) {
            System.err.println("❌ Error upserting batch: " + e.getMessage());
            e.printStackTrace();
            throw new RuntimeException("Failed to upsert batch", e);
        }
    }

    /**
     * Upsert a single snippet (Wrapper for batch)
     */
    public void upsertSnippets(CsvSnippetLoader.Snippets snippet) {
        upsertSnippetsBatch(Collections.singletonList(snippet));
    }

    /**
     * Perform semantic search in Pinecone index
     * 
     * @param query The search query
     * @param topK  Number of results to return
     * @return List of matching snippets with scores
     */
    public List<ScoredSnippet> semanticSearch(String query, int topK) {
        try {
            // Generate embedding for the query
            List<Float> queryEmbedding = embeddingService.generateEmbedding(query);
            if (queryEmbedding == null || queryEmbedding.isEmpty()) {
                System.err.println("⚠️ Skipping Pinecone query due to empty embedding.");
                return new ArrayList<>();
            }

            // Build query request
            JsonObject requestBody = new JsonObject();

            JsonArray values = new JsonArray();
            for (Float value : queryEmbedding) {
                values.add(value);
            }
            requestBody.add("vector", values);
            requestBody.addProperty("topK", topK);
            requestBody.addProperty("includeMetadata", true);

            RequestBody body = RequestBody.create(gson.toJson(requestBody), JSON);

            Request request = new Request.Builder()
                    .url(indexUrl + "/query")
                    .addHeader("Api-Key", apiKey)
                    .addHeader("Content-Type", "application/json")
                    .addHeader("X-Pinecone-Api-Version", "2024-10")
                    .post(body)
                    .build();

            try (Response response = httpClient.newCall(request).execute()) {
                if (!response.isSuccessful()) {
                    throw new IOException("Pinecone query failed: " + response.code() + " - " + response.message());
                }

                String responseBody = response.body().string();
                // DEBUG LOG: Print raw response from Pinecone (Removed for security)
                // System.out.println("🔎 RAW Pinecone Response: " + responseBody);

                JsonObject jsonResponse = gson.fromJson(responseBody, JsonObject.class);

                // Parse matches
                List<ScoredSnippet> results = new ArrayList<>();

                if (jsonResponse.has("matches")) {
                    JsonArray matches = jsonResponse.getAsJsonArray("matches");
                    System.out.println("📊 Matches found: " + matches.size());

                    for (int i = 0; i < matches.size(); i++) {
                        JsonObject match = matches.get(i).getAsJsonObject();

                        // DEBUG LOG: Print individual match structure if needed
                        // System.out.println(" Match " + i + ": " + match);

                        String id = match.get("id").getAsString();
                        double score = match.get("score").getAsDouble();
                        JsonObject metadata = match.getAsJsonObject("metadata");

                        if (metadata == null) {
                            System.out.println("   ⚠️ Warning: Match " + id + " has NO metadata!");
                            continue;
                        }

                        // Reconstruct snippet from metadata
                        // Handle potential missing fields gracefully
                        String topic = metadata.has("topic") ? metadata.get("topic").getAsString() : "Unknown";
                        String category = metadata.has("category") ? metadata.get("category").getAsString() : "General";
                        String adviceText = metadata.has("adviceText") ? metadata.get("adviceText").getAsString() : "";
                        String keywordsAndStr = metadata.has("keywords") ? metadata.get("keywords").getAsString() : "";

                        CsvSnippetLoader.Snippets snippet = new CsvSnippetLoader.Snippets(
                                id,
                                topic,
                                category,
                                Arrays.asList(keywordsAndStr.split(",")),
                                adviceText);

                        results.add(new ScoredSnippet(snippet, score));
                    }
                } else {
                    System.out.println("⚠️ JSON response has no 'matches' field.");
                }

                return results;
            }

        } catch (Exception e) {
            System.err.println("❌ Error performing semantic search: " + e.getMessage());
            if (e.getCause() != null) {
                System.err.println("   - Nested Cause: " + e.getCause().getMessage());
            }
            // Return empty list on error to allow fallback
            return new ArrayList<>();
        }
    }

    /**
     * Initialize Pinecone index with all snippets using Batching
     * 
     * @param snippets List of snippets to populate
     */
    public void initializeIndex(List<CsvSnippetLoader.Snippets> snippets) {
        System.out.println("🔄 Populating Pinecone index with " + snippets.size() + " snippets (Batch Mode)...");

        int processedCount = 0;
        int batchSize = 50; // Pinecone recommends smaller batches for complex metadata, 50-100 is safe
        List<CsvSnippetLoader.Snippets> batch = new ArrayList<>();

        for (CsvSnippetLoader.Snippets snippet : snippets) {
            batch.add(snippet);

            if (batch.size() >= batchSize) {
                try {
                    upsertSnippetsBatch(batch);
                    processedCount += batch.size();
                    System.out.println("   ✓ Upserted batch: " + processedCount + "/" + snippets.size());
                    batch.clear();
                    // Small delay to be polite to API
                    Thread.sleep(200);
                } catch (Exception e) {
                    System.err.println(
                            "   ✗ Failed to upsert batch near index " + processedCount + ": " + e.getMessage());
                    // Don't clear batch if we want retry logic, but for now we skip
                    batch.clear();
                }
            }
        }

        // Process remaining
        if (!batch.isEmpty()) {
            try {
                upsertSnippetsBatch(batch);
                processedCount += batch.size();
                System.out.println("   ✓ Upserted final batch: " + processedCount + "/" + snippets.size());
            } catch (Exception e) {
                System.err.println("   ✗ Failed to upsert final batch: " + e.getMessage());
            }
        }

        System.out.println("✅ Pinecone index initialization complete!");
        System.out.println("   Total Processed: " + processedCount);
    }

    /**
     * Check if index is empty
     * 
     * @return true if index has no vectors
     */
    public boolean isIndexEmpty() {
        try {
            Request request = new Request.Builder()
                    .url(indexUrl + "/describe_index_stats")
                    .addHeader("Api-Key", apiKey)
                    .addHeader("X-Pinecone-Api-Version", "2024-10")
                    .get()
                    .build();

            try (Response response = httpClient.newCall(request).execute()) {
                if (response.isSuccessful()) {
                    String responseBody = response.body().string();
                    System.out.println("📊 Pinecone Index Stats: " + responseBody);
                    JsonObject stats = gson.fromJson(responseBody, JsonObject.class);

                    if (stats.has("totalVectorCount")) {
                        int count = stats.get("totalVectorCount").getAsInt();
                        return count == 0;
                    }
                }
            }
        } catch (Exception e) {
            System.err.println("⚠️ Could not check index status: " + e.getMessage());
        }

        return true; // Assume empty if we can't check
    }

    /**
     * Log full index statistics for verification
     */
    public void logIndexStats() {
        isIndexEmpty(); // Re-uses the logic above which now prints the stats
    }

    /**
     * Calculate semantic similarity between two text blocks
     * 
     * @param text1 First text block (e.g., job description)
     * @param text2 Second text block (e.g., resume)
     * @return Similarity score (0.0 to 1.0)
     */
    public double calculateSemanticSimilarity(String text1, String text2) {
        if (text1 == null || text2 == null || text1.trim().isEmpty() || text2.trim().isEmpty()) {
            return 0.0;
        }

        try {
            List<Float> embedding1 = embeddingService.generateEmbedding(text1);
            List<Float> embedding2 = embeddingService.generateEmbedding(text2);

            return embeddingService.calculateCosineSimilarity(embedding1, embedding2);
        } catch (Exception e) {
            System.err.println("⚠️ Semantic similarity calculation failed: " + e.getMessage());
            return 0.0;
        }
    }

    /**
     * Helper class to hold snippet with similarity score
     */
    public static class ScoredSnippet {
        private final CsvSnippetLoader.Snippets snippet;
        private final double score;

        public ScoredSnippet(CsvSnippetLoader.Snippets snippet, double score) {
            this.snippet = snippet;
            this.score = score;
        }

        public CsvSnippetLoader.Snippets getSnippet() {
            return snippet;
        }

        public double getScore() {
            return score;
        }
    }
}
