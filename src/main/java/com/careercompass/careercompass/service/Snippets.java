package com.careercompass.careercompass.service;

import java.util.List;

/**
 * Snippets
 * 
 * Represents a piece of knowledge snippet (RAG data, Skill, Job Role, etc.)
 * used for Pinecone ingestion and search.
 */
public class Snippets {
    private String id;
    private String topic;
    private String category;
    private List<String> keywords;
    private String adviceText;

    public Snippets() {
    }

    public Snippets(String id, String topic, String category, List<String> keywords, String adviceText) {
        this.id = id;
        this.topic = topic;
        this.category = category;
        this.keywords = keywords;
        this.adviceText = adviceText;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getTopic() {
        return topic;
    }

    public void setTopic(String topic) {
        this.topic = topic;
    }

    public String getCategory() {
        return category;
        // return category != null ? category : "general";
    }

    public void setCategory(String category) {
        this.category = category;
    }

    public List<String> getKeywords() {
        return keywords;
    }

    public void setKeywords(List<String> keywords) {
        this.keywords = keywords;
    }

    public String getAdviceText() {
        return adviceText;
    }

    public void setAdviceText(String adviceText) {
        this.adviceText = adviceText;
    }

    @Override
    public String toString() {
        return "Snippets{" +
                "id='" + id + '\'' +
                ", topic='" + topic + '\'' +
                ", category='" + category + '\'' +
                ", keywords=" + keywords +
                ", adviceText='" + adviceText + '\'' +
                '}';
    }
}
