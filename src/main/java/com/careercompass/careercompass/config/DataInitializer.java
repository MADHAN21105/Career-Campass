package com.careercompass.careercompass.config;

import com.careercompass.careercompass.service.*;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.beans.factory.annotation.Value;

import java.util.List;

@Component
public class DataInitializer {

    @Autowired
    private PineconeVectorService pineconeVectorService;

    @Autowired
    private CsvSnippetLoader csvSnippetLoader;

    // ⚠️ CONFIGURATION OPTIONS
    // Set these flags to control initialization behavior
    private static final boolean EXPORT_CSV = false; // Disabled as data source classes are removed
    private static final boolean INGEST_TO_PINECONE = true; // Populate Pinecone index
    private static final boolean USE_CSV_SOURCE = true; // Always load from CSV now

    // 📁 CSV FILE CONFIGURATION
    // Specify your CSV file name here (must be in 'data/' directory)
    private static final String CUSTOM_CSV_FILE = "career_jobs.csv";

    // Use unified knowledge (generated) or custom CSV file
    private static final boolean USE_CUSTOM_CSV = false; // false = use unified_knowledge.csv (default)

    // ⚠️ IMPORTANT: Uncomment @PostConstruct ONLY for first-time setup
    // After successful ingestion, comment it out to prevent duplicate vectors

    @Value("${pinecone.ingest.enabled:false}")
    private boolean ingestEnabled;

    @PostConstruct
    public void initializePineconeIndex() {
        // ALWAYS check specific stats on startup so the user knows what's in the DB
        System.out.println("🔍 Checking Pinecone Index Stats...");
        pineconeVectorService.logIndexStats();

        if (!ingestEnabled) {
            System.out.println("🛑 Auto-ingestion DISABLED via properties. Skipping Pinecone upsert.");
            System.out.println("👉 To ingest data, make a POST request to: /admin/ingest");
            return;
        }
        ingestAllData();
    }

    public synchronized void ingestAllData() {
        try {
            System.out.println("🚀 Starting MANUAL data ingestion...");

            // Step 2: Ingest to Pinecone if enabled
            if (INGEST_TO_PINECONE) {
                System.out.println("\n🔍 Checking Pinecone index status...");

                // Check if index is empty OR if this manual call forces it
                if (pineconeVectorService.isIndexEmpty() || ingestEnabled) {
                    System.out.println("📦 Pinecone index check passed. Starting data ingestion...");

                    List<Snippets> snippets;

                    // Load snippets from MULTIPLE CSV files
                    // 1. unified_knowledge.csv (Core data)
                    // 2. interview_qa.csv (Your new 10,000+ dataset)
                    List<String> filesToLoad = List.of(
                            "data/unified_knowledge.csv",
                            "data/interview_qa.csv",
                            "data/career_guidance.csv",
                            "data/soft_skills.csv",
                            "data/resume_ats.csv",
                            "data/career_transition.csv",
                            "data/job_search.csv");

                    System.out.println("📂 Loading snippets from files: " + filesToLoad);
                    snippets = csvSnippetLoader.loadFromMultipleCsv(filesToLoad);

                    System.out.println("📊 Total snippets to ingest: " + snippets.size());

                    pineconeVectorService.initializeIndex(snippets);

                    System.out.println("\n✅ Data ingestion complete!");

                } else {
                    System.out.println("✅ Pinecone index already populated. Skipping initialization.");
                }
            }

            // Verify final state
            System.out.println("\n🔍 Final Pinecone Status Check:");
            pineconeVectorService.logIndexStats();

        } catch (Exception e) {
            System.err.println("⚠️ Warning: Could not initialize data: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
