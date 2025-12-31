package com.careercompass.careercompass.service;

import com.careercompass.careercompass.dto.AiSkillProfile;
import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.concurrent.TimeUnit;

@Service
public class CacheService {

    // L1: Exact Question Cache (1 hour TTL)
    private final Cache<String, String> questionCache = CacheBuilder.newBuilder()
            .expireAfterWrite(1, TimeUnit.HOURS)
            .maximumSize(1000)
            .build();

    // L2: Intent Templates Cache (24 hours TTL)
    private final Cache<String, String> intentCache = CacheBuilder.newBuilder()
            .expireAfterWrite(24, TimeUnit.HOURS)
            .maximumSize(200)
            .build();

    // L4: Profile Cache (1 hour TTL) - Reuses skill analysis across chat turns
    private final Cache<String, AiSkillProfile> profileCache = CacheBuilder.newBuilder()
            .expireAfterWrite(1, TimeUnit.HOURS)
            .maximumSize(500)
            .build();

    // L3: RAG Context Cache (Skill -> Snippets, 12 hours TTL)
    private final Cache<String, List<CsvSnippetLoader.Snippets>> ragCache = CacheBuilder.newBuilder()
            .expireAfterWrite(12, TimeUnit.HOURS)
            .maximumSize(500)
            .build();

    public String getQuestion(String key) {
        return questionCache.getIfPresent(key);
    }

    public void putQuestion(String key, String value) {
        questionCache.put(key, value);
    }

    public String getIntent(String key) {
        return intentCache.getIfPresent(key);
    }

    public void putIntent(String key, String value) {
        intentCache.put(key, value);
    }

    public AiSkillProfile getProfile(String key) {
        return profileCache.getIfPresent(key);
    }

    public void putProfile(String key, AiSkillProfile value) {
        profileCache.put(key, value);
    }

    public List<CsvSnippetLoader.Snippets> getRagContext(String key) {
        return ragCache.getIfPresent(key);
    }

    public void putRagContext(String key, List<CsvSnippetLoader.Snippets> value) {
        ragCache.put(key, value);
    }

    public void clearAll() {
        questionCache.invalidateAll();
        intentCache.invalidateAll();
        profileCache.invalidateAll();
        ragCache.invalidateAll();
    }
}
