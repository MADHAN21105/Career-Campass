package com.careercompass.careercompass.service;

import org.springframework.stereotype.Service;

/**
 * CsvExporter
 * 
 * Exports RAG data, JobRoleLibrary, and SkillLibrary to CSV files
 * for easy data management and Pinecone ingestion.
 */
@Service
public class CsvExporter {

    // Dependencies removed as part of migration to CSV-only architecture.
    // This service is now deprecated and preserved only to prevent compilation
    // errors.

    public void exportAllToCsv() {
        System.out.println("⚠️ CsvExporter is disabled. Use direct CSV editing in 'src/main/resources/data'.");
    }
}
