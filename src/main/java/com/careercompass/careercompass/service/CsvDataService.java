package com.careercompass.careercompass.service;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;

import jakarta.annotation.PostConstruct;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.util.*;

@Service
@SuppressWarnings("all")
public class CsvDataService {

    private Map<String, String> skillImportance = new HashMap<>();
    private Map<String, List<String>> jobRequiredSkills = new HashMap<>();
    private Map<String, String> skillIdToName = new HashMap<>();
    private Map<String, String> skillNameToId = new HashMap<>();
    private Map<String, String> skillAdvice = new HashMap<>();
    private Map<String, String> skillCategory = new HashMap<>();
    // ✅ Store original display names for UI
    private Map<String, String> skillDisplayNames = new HashMap<>();
    private Map<String, String> keywordToId = new HashMap<>();

    @PostConstruct
    public void init() {
        loadSkills();
        loadJobRoles();
    }

    private void loadSkills() {
        try (Reader reader = new InputStreamReader(new ClassPathResource("data/skills.csv").getInputStream());
                CSVParser parser = CSVFormat.DEFAULT.withFirstRecordAsHeader().parse(reader)) {

            for (CSVRecord record : parser) {
                String id = record.get("id");

                // Original display name (e.g., "Java", "Spring Boot")
                String displayName = record.get("topic");
                if (displayName.startsWith("Skill: ")) {
                    displayName = displayName.substring(7).trim();
                }

                // Lowercase for matching logic
                String name = displayName.toLowerCase();

                String importance = record.get("importance");
                String advice = record.get("adviceText");
                String category = record.isMapped("category") ? record.get("category") : "";
                String keywords = record.isMapped("keywords") ? record.get("keywords") : "";

                skillIdToName.put(id, name);
                skillNameToId.put(name, id);
                skillDisplayNames.put(id, displayName); // Store proper casing
                skillImportance.put(id, importance);
                skillAdvice.put(id, advice);
                skillCategory.put(id, category);

                // Map keywords to ID
                if (!keywords.isEmpty()) {
                    String[] kwArray = keywords.split("\\|");
                    for (String kw : kwArray) {
                        keywordToId.put(kw.trim().toLowerCase(), id);
                    }
                }
            }
        } catch (IOException e) {
            System.err.println("Error loading skills.csv: " + e.getMessage());
        }
    }

    private void loadJobRoles() {
        try (Reader reader = new InputStreamReader(new ClassPathResource("data/job_roles.csv").getInputStream());
                CSVParser parser = CSVFormat.DEFAULT.withFirstRecordAsHeader().parse(reader)) {

            for (CSVRecord record : parser) {
                String id = record.get("id");
                String requiredSkillsStr = record.get("required_skills");

                if (requiredSkillsStr != null && !requiredSkillsStr.isEmpty()) {
                    List<String> skills = Arrays.asList(requiredSkillsStr.split("\\|"));
                    jobRequiredSkills.put(id, skills);
                }
            }
        } catch (IOException e) {
            System.err.println("Error loading job_roles.csv: " + e.getMessage());
        }
    }

    public String findJobRole(String text) {
        if (text == null)
            return null;
        String lower = text.toLowerCase();

        // Simple keyword matching for job roles
        for (String roleId : jobRequiredSkills.keySet()) {
            String roleName = roleId.replace("job-", "").replace("-", " ");
            if (lower.contains(roleName)) {
                return roleId;
            }
        }
        return null;
    }

    public String getImportance(String skillId) {
        return skillImportance.getOrDefault(skillId, "Medium");
    }

    public List<String> getRequiredSkills(String jobRoleId) {
        return jobRequiredSkills.getOrDefault(jobRoleId, new ArrayList<>());
    }

    public String getSkillIdByName(String name) {
        return skillNameToId.get(name.toLowerCase());
    }

    public Set<String> getAllSkillNames() {
        return skillNameToId.keySet();
    }

    public Set<String> getAllKeywords() {
        return keywordToId.keySet();
    }

    public String getAdvice(String skillName) {
        String id = findSkillId(skillName);
        return id != null ? skillAdvice.get(id) : null;
    }

    public String getSkillCategory(String skillName) {
        String id = findSkillId(skillName);
        return id != null ? skillCategory.get(id) : null;
    }

    /**
     * Get the properly cased display name for a skill (e.g., "java development" ->
     * "Java")
     * robustly resolves via keywords if direct match fails.
     */
    public String getSkillDisplayName(String skillName) {
        String id = findSkillId(skillName);
        return id != null ? skillDisplayNames.get(id) : skillName; // Fallback to input if not found
    }

    /**
     * Robust skill finding:
     * 1. Exact name match
     * 2. Keyword match
     * 3. Containment match (skill contains name or name contains keyword)
     */
    public String findSkillId(String name) {
        if (name == null)
            return null;
        String lower = name.toLowerCase().trim();

        // 1. Direct match
        if (skillNameToId.containsKey(lower))
            return skillNameToId.get(lower);

        // 2. Keyword match
        if (keywordToId.containsKey(lower))
            return keywordToId.get(lower);

        // 3. Reverse Keyword match (e.g. "java development" contains "java")
        // Check if any keyword is contained in the input name
        for (String kw : keywordToId.keySet()) {
            if (lower.equals(kw) || (kw.length() > 3 && lower.contains(kw))) {
                // Prioritize exact keyword matches if multiple? For now just return first.
                // Ideally we pick longest match.
                return keywordToId.get(kw);
            }
        }

        return null;
    }
}
