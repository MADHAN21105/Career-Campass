package com.careercompass.careercompass.service;

import org.springframework.stereotype.Service;

/**
 * CsvExporter
 * 
 * Exports RAG data to CSV files for easy data management and Pinecone
 * ingestion.
 * Legacy library files (SkillLibrary, JobRoleLibrary) have been removed in
 * favor of CSV-based architecture.
 */
@Service
@SuppressWarnings("all")
public class CsvExporter {

    // Dependencies removed as part of migration to CSV-only architecture.
    // This service is now deprecated and preserved only to prevent compilation
    // errors.

    public void exportAllToCsv() {
        System.out.println("⚠️ CsvExporter is disabled. Use direct CSV editing in 'src/main/resources/data'.");
    }
}
