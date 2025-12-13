package com.careercompass.careercompass.service;

import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * DataTransformer
 * 
 * Transforms JobRoleLibrary and SkillLibrary data into Snippets format
 * for unified storage in Pinecone index.
 */
@Service
public class DataTransformer {

    // Dependencies removed as part of migration to CSV-only architecture.
    // This service is now deprecated and preserved only to prevent compilation
    // errors if referenced.

    public List<Snippets> transformJobRolesToSnippets(List<String> jobRoles) {
        return new ArrayList<>();
    }

    public List<Snippets> transformJobRolesToRoadmapSnippets(List<String> jobRoles) {
        return new ArrayList<>();
    }

    public List<Snippets> transformSkillsToSnippets(Map<String, List<String>> skillMap) {
        return new ArrayList<>();
    }

    /*
     * public List<Snippets> getAllTransformedSnippets(Rag_Data ragData) {
     * return new ArrayList<>();
     * }
     */
}
