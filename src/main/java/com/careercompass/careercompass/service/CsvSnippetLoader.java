package com.careercompass.careercompass.service;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * CsvSnippetLoader
 * 
 * Loads snippets from CSV files for Pinecone ingestion.
 * Supports batch processing for large files (10,000+ records).
 */
@Service
@SuppressWarnings("all")
public class CsvSnippetLoader {

    /**
     * Load snippets from a CSV file with progress tracking
     * 
     * @param filePath Path to the CSV file
     * @return List of Snippets loaded from the file
     * @throws IOException If file reading fails
     */
    public List<Snippets> loadFromCsv(String filePath) throws IOException {
        return loadFromCsv(filePath, true);
    }

    /**
     * Load snippets from a CSV file
     * 
     * @param filePath     Path to the CSV file
     * @param showProgress Whether to show progress messages
     * @return List of Snippets loaded from the file
     * @throws IOException If file reading fails
     */
    public List<Snippets> loadFromCsv(String filePath, boolean showProgress) throws IOException {
        List<Snippets> snippets = new ArrayList<>();
        int successCount = 0;
        int errorCount = 0;
        int totalRecords = 0;

        if (showProgress) {
            System.out.println("📂 Attempting to load CSV: " + filePath);
        }

        InputStream inputStream = null;

        // 1. Try treating filePath as a relative path to project root (Filesystem)
        try {
            java.io.File file = new java.io.File(filePath);
            if (file.exists()) {
                inputStream = new java.io.FileInputStream(file);
                if (showProgress)
                    System.out.println("   ✓ Found in filesystem: " + file.getAbsolutePath());
            }
        } catch (Exception ignored) {
        }

        // 2. If not found, try Classpath
        if (inputStream == null) {
            try {
                inputStream = new ClassPathResource(filePath).getInputStream();
                if (showProgress)
                    System.out.println("   ✓ Found in classpath: " + filePath);
            } catch (Exception e) {
                // Both failed
                throw new IOException("CSV file not found in filesystem or classpath: " + filePath);
            }
        }

        try (Reader reader = new InputStreamReader(inputStream);
                CSVParser parser = CSVFormat.DEFAULT
                        .withFirstRecordAsHeader()
                        .withIgnoreEmptyLines()
                        .withTrim()
                        .parse(reader)) {

            for (CSVRecord record : parser) {
                totalRecords++;
                try {
                    // Robust ID generation: Use "id" column if present, else fallback to
                    // filename-row
                    String fileName = new java.io.File(filePath).getName().replace(".csv", "");
                    String id = record.isMapped("id") && !record.get("id").isBlank()
                            ? record.get("id")
                            : fileName + "-" + totalRecords;

                    String topic;
                    String category;
                    String keywordsStr;
                    String adviceText;

                    // 1. Check for SPECIAL SCHEMA (Soft Skills: skill, situation, advice, example)
                    if (record.isMapped("skill") && record.isMapped("situation")) {
                        String skill = record.get("skill");
                        String situation = record.get("situation");
                        String coreAdvice = record.get("advice");
                        String example = record.get("example");

                        topic = skill;
                        category = "Soft Skills & Workplace Behavior";
                        keywordsStr = skill + "|" + situation + "|soft skills";

                        // Combine into a ROADMAP-STYLE Advice Text
                        adviceText = String.format(
                                "To handle situations involving **%s**:\n" +
                                        "1. **Understand the Context**: %s\n" +
                                        "2. **Action Plan**: %s\n" +
                                        "3. **Real World Example**: %s",
                                skill, situation, coreAdvice, example);

                    } else if (record.isMapped("resumeTopic") && record.isMapped("bestPractice")) {
                        // 2. CHECK FOR RESUME & ATS SCHEMA
                        String rTopic = record.get("resumeTopic");
                        String rCategory = record.get("category");
                        String rKeywords = record.get("keywords");
                        String bestPractice = record.get("bestPractice");
                        String mistake = record.get("commonMistake");
                        String example = record.get("example");

                        topic = rTopic;
                        category = rCategory;
                        keywordsStr = rKeywords;

                        // Combine into a ROADMAP-STYLE Advice Text
                        adviceText = String.format(
                                "To optimize your **%s**:\n" +
                                        "1. **Best Practice**: %s\n" +
                                        "2. **Avoid this Mistake**: %s\n" +
                                        "3. **Example**: %s",
                                rTopic, bestPractice, mistake, example);

                    } else if (record.isMapped("fromRole") && record.isMapped("toRole")) {
                        // 3. CHECK FOR CAREER TRANSITION SCHEMA
                        String fromRole = record.get("fromRole");
                        String toRole = record.get("toRole");
                        String reqSkills = record.get("requiredSkills");
                        String steps = record.get("transitionSteps");
                        String time = record.get("timeEstimate");

                        topic = "Transition from " + fromRole + " to " + toRole;
                        category = "Career Transition";
                        keywordsStr = fromRole + "|" + toRole + "|" + reqSkills.replace(",", "|");

                        // Combine into a ROADMAP-STYLE Advice Text
                        adviceText = String.format(
                                "Transition Strategy: **%s** to **%s**\n" +
                                        "1. **Required Skills**: %s\n" +
                                        "2. **Steps**: %s\n" +
                                        "3. **Timeline**: %s",
                                fromRole, toRole, reqSkills, steps, time);

                    } else if (record.isMapped("topic") && record.isMapped("adviceText")
                            && record.isMapped("importance")) {
                        // 4. CHECK FOR SKILLS TAXONOMY SCHEMA (skills.csv)
                        String sTopic = record.get("topic");
                        String sCategory = record.get("category");
                        String sKeywords = record.get("keywords");
                        String sAdvice = record.get("adviceText");
                        String sImportance = record.get("importance");

                        // Clean up "Skill: " prefix if present in topic
                        topic = sTopic.toLowerCase().startsWith("skill: ") ? sTopic.substring(7) : sTopic;
                        category = (sCategory != null && !sCategory.isEmpty()) ? sCategory : "Technical Skill";
                        keywordsStr = sKeywords;

                        // Combine into a structured Advice Text
                        adviceText = String.format(
                                "Skill: **%s** (%s)\n" +
                                        "Importance: %s\n\n" +
                                        "💡 **Expert Advice**:\n%s",
                                topic, category, sImportance, sAdvice);

                    } else if (record.isMapped("platform")) {
                        // 5. CHECK FOR JOB SEARCH STRATEGY SCHEMA
                        String platform = record.get("platform");
                        String advice = record.get("adviceText");

                        topic = record.get("topic");
                        category = "Job Search Strategy";
                        keywordsStr = topic + "|" + platform;

                        // Combine into a ROADMAP-STYLE Advice Text
                        adviceText = String.format(
                                "Job Search Strategy for **%s**:\n" +
                                        "**Platform**: %s\n" +
                                        "**Advice**: %s",
                                topic, platform, advice);

                    } else {
                        // 5. STANDARD SCHEMA (id, topic, category, keywords, adviceText)
                        topic = record.isMapped("topic") ? record.get("topic") : "General";
                        category = record.isMapped("category") ? record.get("category") : "Uncategorized";
                        keywordsStr = record.isMapped("keywords") ? record.get("keywords") : "";
                        adviceText = record.isMapped("adviceText") ? record.get("adviceText") : "";
                    }

                    // Parse keywords (pipe-separated or space-separated fallback)
                    List<String> keywords = new ArrayList<>(Arrays.asList(keywordsStr.split("[|,]")));

                    snippets.add(new Snippets(id, topic, category, keywords, adviceText));
                    successCount++;

                    // Show progress every 100 records
                    if (showProgress && successCount % 100 == 0) {
                        System.out.println("   ✓ Loaded " + successCount + " records...");
                    }

                } catch (Exception e) {
                    errorCount++;
                    if (showProgress && errorCount <= 5) {
                        System.err.println("⚠️ Error parsing CSV record #" + totalRecords + ": " + e.getMessage());
                    }
                    // Continue with next record
                }
            }
        }

        if (showProgress) {
            System.out.println("✅ CSV loading complete:");
            System.out.println("   Total records: " + totalRecords);
            System.out.println("   Successfully loaded: " + successCount);
            System.out.println("   Errors: " + errorCount);
        }

        return snippets;
    }

    /**
     * Load snippets from multiple CSV files
     * 
     * @param filePaths List of CSV file paths
     * @return Combined list of snippets from all files
     */
    public List<Snippets> loadFromMultipleCsv(List<String> filePaths) {
        List<Snippets> allSnippets = new ArrayList<>();

        for (String filePath : filePaths) {
            try {
                List<Snippets> snippets = loadFromCsv(filePath);
                allSnippets.addAll(snippets);
            } catch (IOException e) {
                System.err.println("   ✗ Failed to load " + filePath + ": " + e.getMessage());
            }
        }

        return allSnippets;
    }

    /**
     * Load the unified knowledge CSV file
     * 
     * @return List of all snippets from unified file
     */
    public List<Snippets> loadUnifiedKnowledge() {
        try {
            return loadFromCsv("data/unified_knowledge.csv");
        } catch (IOException e) {
            System.err.println("❌ Failed to load unified knowledge CSV: " + e.getMessage());
            return new ArrayList<>();
        }
    }

    /**
     * Load custom CSV file (for user's 10,000 records)
     * 
     * @param fileName Name of the CSV file in data directory
     * @return List of snippets from the file
     */
    public List<Snippets> loadCustomCsv(String fileName) {
        try {
            String filePath = "data/" + fileName;
            return loadFromCsv(filePath, true);
        } catch (IOException e) {
            System.err.println("❌ Failed to load custom CSV: " + e.getMessage());
            e.printStackTrace();
            return new ArrayList<>();
        }
    }

    /**
     * Snippets
     * 
     * Represents a piece of knowledge snippet (RAG data, Skill, Job Role, etc.)
     * used for Pinecone ingestion and search.
     */
    public static class Snippets {
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
}
