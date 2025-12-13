package com.careercompass.careercompass.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class PineconeConfig {

    @Value("${pinecone.api.key}")
    private String apiKey;

    @Value("${pinecone.index.url}")
    private String indexUrl;

    @Bean
    public String pineconeIndexUrl() {
        System.out.println("✅ Pinecone configuration loaded");
        System.out.println("📍 Index URL: " + indexUrl);
        return indexUrl;
    }

    @Bean
    public String pineconeApiKey() {
        return apiKey;
    }
}
