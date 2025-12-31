package com.careercompass.careercompass.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

@Service
public class RAGService {
    private static final Logger log = LoggerFactory.getLogger(RAGService.class);

    private final PineconeVectorService pineconeVectorService;
    private final CacheService cacheService;

    public RAGService(PineconeVectorService pineconeVectorService, CacheService cacheService) {
        this.pineconeVectorService = pineconeVectorService;
        this.cacheService = cacheService;
    }

    public List<CsvSnippetLoader.Snippets> getRagContext(String query, int limit) {
        if (query == null || query.isBlank())
            return new ArrayList<>();

        log.info("🔍 [Pinecone RAG Search] Query: {}", query);
        try {
            List<PineconeVectorService.ScoredSnippet> scored = pineconeVectorService.semanticSearch(query, limit);
            return scored.stream()
                    .map(PineconeVectorService.ScoredSnippet::getSnippet)
                    .collect(Collectors.toList());
        } catch (Exception e) {
            log.error("❌ RAG Search failed: {}", e.getMessage());
            return new ArrayList<>();
        }
    }

    public List<CsvSnippetLoader.Snippets> getRagContextOptimized(Set<String> skills, String rawQuery) {
        List<CsvSnippetLoader.Snippets> context = new ArrayList<>();
        Set<String> uncached = new HashSet<>();

        // 1. Skill-Based RAG (Specific IDs)
        if (skills != null && !skills.isEmpty()) {
            for (String skill : skills) {
                List<CsvSnippetLoader.Snippets> cached = cacheService.getRagContext(skill.toLowerCase());
                if (cached != null) {
                    context.addAll(cached);
                } else {
                    uncached.add(skill);
                }
            }

            if (!uncached.isEmpty()) {
                String batchQuery = String.join(" ", uncached);
                try {
                    List<PineconeVectorService.ScoredSnippet> results = pineconeVectorService.semanticSearch(batchQuery,
                            12);
                    for (String skill : uncached) {
                        String sLower = skill.toLowerCase();
                        List<CsvSnippetLoader.Snippets> skillSnippets = results.stream()
                                .filter(r -> r.getScore() > 0.72)
                                .filter(r -> r.getSnippet() != null &&
                                        (r.getSnippet().getTopic().toLowerCase().contains(sLower) ||
                                                r.getSnippet().getAdviceText().toLowerCase().contains(sLower)))
                                .limit(2)
                                .map(PineconeVectorService.ScoredSnippet::getSnippet)
                                .collect(Collectors.toList());

                        if (!skillSnippets.isEmpty()) {
                            cacheService.putRagContext(sLower, skillSnippets);
                            context.addAll(skillSnippets);
                        }
                    }
                } catch (Exception e) {
                    log.error("Skill RAG error: {}", e.getMessage());
                }
            }
        }

        // 2. Query-Based RAG (Broad Context)
        if (rawQuery != null && !rawQuery.trim().isEmpty()) {
            try {
                log.info("🔍 Broad RAG search for: '{}'", rawQuery);
                List<PineconeVectorService.ScoredSnippet> broadResults = pineconeVectorService.semanticSearch(rawQuery,
                        15);

                List<CsvSnippetLoader.Snippets> broadSnippets = broadResults.stream()
                        .filter(r -> r.getScore() > 0.65)
                        .map(PineconeVectorService.ScoredSnippet::getSnippet)
                        .filter(Objects::nonNull)
                        .limit(8)
                        .collect(Collectors.toList());

                context.addAll(broadSnippets);
            } catch (Exception e) {
                log.error("Broad RAG error: {}", e.getMessage());
            }
        }

        // De-duplicate snippets by topic
        return context.stream()
                .filter(Objects::nonNull)
                .collect(Collectors.toMap(
                        CsvSnippetLoader.Snippets::getTopic,
                        s -> s,
                        (s1, s2) -> s1))
                .values().stream()
                .collect(Collectors.toList());
    }

    public List<CsvSnippetLoader.Snippets> retrieveRelevantSnippets(String question, List<String> missing,
            String roleFocus) {
        try {
            StringBuilder queryBuilder = new StringBuilder(question != null ? question.trim() : "");
            if (missing != null && !missing.isEmpty()) {
                queryBuilder.append(" ").append(String.join(" ", missing));
            }
            if (roleFocus != null && !roleFocus.isBlank()) {
                queryBuilder.append(" ").append(roleFocus);
            }

            String enhancedQuery = queryBuilder.toString();
            log.info("🔍 Using Pinecone semantic search for query: {}", enhancedQuery);

            List<PineconeVectorService.ScoredSnippet> scoredResults = pineconeVectorService
                    .semanticSearch(enhancedQuery, 8);
            return scoredResults.stream()
                    .map(PineconeVectorService.ScoredSnippet::getSnippet)
                    .filter(Objects::nonNull)
                    .collect(Collectors.toMap(
                            CsvSnippetLoader.Snippets::getTopic,
                            s -> s,
                            (s1, s2) -> s1))
                    .values().stream()
                    .collect(Collectors.toList());

        } catch (Exception e) {
            log.error("⚠️ Pinecone search failed: {}", e.getMessage());
            return Collections.emptyList();
        }
    }
}
