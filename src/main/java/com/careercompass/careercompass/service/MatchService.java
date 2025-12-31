package com.careercompass.careercompass.service;

import com.careercompass.careercompass.dto.AiSkillProfile;
import com.careercompass.careercompass.dto.AnalysisRequest;
import com.careercompass.careercompass.dto.AnalysisResponse;
import com.careercompass.careercompass.dto.SkillEvidence;
import com.careercompass.careercompass.exception.InvalidInputException;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

@Service
public class MatchService {

    private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(MatchService.class);

    private final SkillAnalysisService skillService;
    private final CsvDataService dataService;
    private final EmbeddingService embeddingService;

    // 🧠 INDUSTRY SEMANTIC BRIDGE: Handled dynamically via
    // Pinecone/EmbeddingService

    public MatchService(SkillAnalysisService skillService, CsvDataService dataService,
            EmbeddingService embeddingService) {
        this.skillService = skillService;
        this.dataService = dataService;
        this.embeddingService = embeddingService;
    }

    // ============================================
    // ADVANCED SKILL EXTRACTION WITH FALSE POSITIVE PREVENTION
    // ============================================

    // ============================================
    // ADVANCED SCORING ALGORITHM WITH GROUPING
    // ============================================

    public double calculatePerfectMatchScore(
            Map<String, SkillEvidence> jdSkills,
            Map<String, SkillEvidence> resumeSkills,
            AnalysisResponse fullRes,
            String jdText,
            String resumeText,
            Map<String, List<Float>> embeddingContext) {

        log.info("📊 Calculating ATS-Style Weighted Scoring (REVISED)...");

        // ✅ Clean & Standardize Skill Lists (Filter broad roles/UI noise/Soft skills)
        standardizeSkills(fullRes);

        // Pillar 1: Hard Skills Match (60%) - Weighted Mandatory vs Preferred
        double hardSkillsScore = calculateHardSkillsMatch(fullRes, jdText, jdSkills, resumeSkills, embeddingContext);

        // Pillar 2: Job Title Alignment (15%)
        double titleScore = calculateJobTitleScore(fullRes.getJobTitle(), fullRes.getResumeTitle(), embeddingContext);

        // Pillar 3: Education & Requirements Match (15%) - Hierarchical
        double educationScore = calculateEducationScore(fullRes.getEducationRequirement(), resumeText);

        // Pillar 4: Semantic Context Fit (10%)
        List<Float> jdEmb = embeddingContext.get(jdText);
        List<Float> resEmb = embeddingContext.get(resumeText);
        double semanticScore = (jdEmb == null || resEmb == null) ? 50.0
                : embeddingService.calculateCosineSimilarity(jdEmb, resEmb) * 100.0;

        // Pillar 5: Soft Skills Match (Calculated for logging/insights but not in final
        // weighted score per plan)
        calculateSoftSkillsMatch(jdSkills, resumeSkills, embeddingContext, jdText);

        // Weighted Aggregation (Total 100%)
        double totalScore = (hardSkillsScore * 0.60) +
                (titleScore * 0.15) +
                (educationScore * 0.15) +
                (semanticScore * 0.10);

        // Set breakdown scores in response
        fullRes.setHardSkillsScore(hardSkillsScore);
        fullRes.setTitleScore(titleScore);
        fullRes.setEducationScore(educationScore);
        fullRes.setSemanticScore(semanticScore);
        // Note: Soft skills currently included in overall but we focus on Hard skills
        // as primary.
        // User's recommended weights sum to 0.60 + 0.15 + 0.15 + 0.10 = 1.0.
        // Soft skills seems to be replaced or merged into Hard.
        // I'll stick to the user's recommended 60/15/15/10 breakdown.

        log.info("   - Hard Skills: {}% (Weight 60%)", String.format("%.2f", hardSkillsScore));
        log.info("   - Job Title: {}% (Weight 15%)", String.format("%.2f", titleScore));
        log.info("   - Education: {}% (Weight 15%)", String.format("%.2f", educationScore));
        log.info("   - Semantic: {}% (Weight 10%)", String.format("%.2f", semanticScore));

        // Apply aggressive critical penalties for missing mandatory skills
        double penaltyFactor = applyRefinedPenaltiesMultiplier(jdText, resumeSkills, embeddingContext,
                fullRes.getMandatorySkills(), fullRes.getMatchedSkills());
        totalScore = totalScore * penaltyFactor;

        double finalScore = Math.min(100.0, Math.max(0.0, Math.round(totalScore)));
        log.info("🚀 Final ATS Match Score: {}% (Penalty Applied: {}%)",
                finalScore, Math.round((1.0 - penaltyFactor) * 100));

        return finalScore;
    }

    /**
     * Standardizes all skill lists to their canonical display names
     * defined in skills.csv (e.g. "java coding" -> "Java").
     * Also filters out broad roles, domain names, UI noise, and soft skills from
     * technical lists.
     */
    private void standardizeSkills(AnalysisResponse response) {
        if (response == null)
            return;

        // We process JDSkills, ResumeSkills, MatchedSkills, and MissingSkills
        response.setJdSkills(filterAndStandardizeList(response.getJdSkills()));
        response.setResumeSkills(filterAndStandardizeList(response.getResumeSkills()));
        response.setMatchedSkills(filterAndStandardizeList(response.getMatchedSkills()));
        response.setMissingSkills(filterAndStandardizeList(response.getMissingSkills()));
    }

    private List<String> filterAndStandardizeList(List<String> original) {
        if (original == null)
            return new java.util.ArrayList<>();
        return original.stream()
                .filter(s -> dataService.findSkillId(s) != null) // Authority: Only skills from skills.csv
                .filter(s -> {
                    // 1. Filter out Soft Skills
                    String category = dataService.getSkillCategory(s);
                    boolean isSoft = category != null && category.toLowerCase().contains("soft");

                    // 2. Filter out Broad Roles/Domains & UI Noise (Comprehensive Sync)
                    String lower = s.toLowerCase();
                    boolean isBroadRoleOrNoise = lower.contains("backend development") ||
                            lower.contains("backend developer") ||
                            lower.contains("frontend development") ||
                            lower.contains("frontend developer") ||
                            lower.contains("software engineer") ||
                            lower.contains("software engineering") ||
                            lower.contains("junior developer") ||
                            lower.contains("senior developer") ||
                            lower.contains("entry level") ||
                            lower.contains("full stack") ||
                            lower.contains("web development") ||
                            lower.contains("analyze different job") ||
                            lower.contains("cover letter generator") ||
                            lower.contains("resume guide") ||
                            lower.contains("roadmap") ||
                            lower.contains("bakenddevloper") ||
                            lower.contains("job description") ||
                            lower.contains("resume analysis") ||
                            lower.contains("ats score") ||
                            lower.contains("skill gaps") ||
                            lower.contains("matched skills") ||
                            lower.contains("pro tips") ||
                            lower.contains("resume") ||
                            lower.contains("candidate") ||
                            lower.contains("learning tips") ||
                            lower.contains("formatting tips") ||
                            lower.contains("industry standard") ||
                            lower.contains("overall job fit") ||
                            lower.contains("analysis complete") ||
                            lower.contains("match summary") ||
                            lower.contains("critical gap") ||
                            lower.contains("strategic recommendation") ||
                            lower.contains("score") ||
                            lower.contains("summary") ||
                            lower.contains("strength") ||
                            lower.contains("goal") ||
                            lower.contains("expert tip") ||
                            lower.length() < 2;

                    return !isSoft && !isBroadRoleOrNoise;
                })
                .map(s -> dataService.getSkillDisplayName(s))
                .distinct()
                .collect(Collectors.toList());
    }

    private double calculateJobTitleScore(String targetTitle, String resumeTitle,
            Map<String, List<Float>> embeddingContext) {
        if (targetTitle == null || targetTitle.isBlank() || resumeTitle == null || resumeTitle.isBlank())
            return 50.0;

        String t = targetTitle.toLowerCase();
        String r = resumeTitle.toLowerCase();

        // Exact match
        if (t.contains(r) || r.contains(t))
            return 100.0;

        // Semantic similarity match
        List<Float> tEmb = embeddingContext.getOrDefault(t, embeddingService.generateEmbedding(t));
        List<Float> rEmb = embeddingContext.getOrDefault(r, embeddingService.generateEmbedding(r));
        double sim = embeddingService.calculateCosineSimilarity(tEmb, rEmb);

        return sim * 100.0;
    }

    private double calculateEducationScore(String requirement, String resumeText) {
        if (requirement == null || requirement.isBlank() ||
                requirement.equalsIgnoreCase("None") ||
                requirement.equalsIgnoreCase("Not Required")) {
            return 100.0; // No requirement = full credit
        }

        String req = requirement.toLowerCase();
        String res = resumeText.toLowerCase();

        // Level-based matching
        if (req.contains("phd") || req.contains("doctorate")) {
            if (res.contains("phd") || res.contains("doctorate"))
                return 100.0;
            if (res.contains("master") || res.contains("m.sc") || res.contains("mtech"))
                return 70.0;
            return 30.0;
        }

        if (req.contains("master") || req.contains("mba")) {
            if (res.contains("master") || res.contains("m.sc") || res.contains("mtech") || res.contains("mba"))
                return 100.0;
            if (res.contains("bachelor") || res.contains("b.sc") || res.contains("btech") || res.contains("be"))
                return 65.0;
            return 20.0;
        }

        if (req.contains("bachelor") || req.contains("b.sc") || req.contains("degree")) {
            if (res.contains("bachelor") || res.contains("b.sc") || res.contains("btech") ||
                    res.contains("be") || res.contains("degree") || res.contains("master"))
                return 100.0;
            if (res.contains("diploma") || res.contains("associate"))
                return 50.0;
            return 10.0;
        }

        // Fallback: some degree mentioned
        if (res.contains("degree") || res.contains("university") || res.contains("college")) {
            return 55.0;
        }

        return 5.0; // Very low if no education found
    }

    private double calculateHardSkillsMatch(AnalysisResponse fullRes, String jdText,
            Map<String, SkillEvidence> jdSkills,
            Map<String, SkillEvidence> resumeSkills,
            Map<String, List<Float>> embeddingContext) {

        log.info("🔍 Calculating Fair-Weighted Hard Skills...");

        List<String> mandatorySkills = fullRes.getMandatorySkills() != null
                ? fullRes.getMandatorySkills()
                : new ArrayList<>();
        List<String> preferredSkills = fullRes.getPreferredSkills() != null
                ? fullRes.getPreferredSkills()
                : new ArrayList<>();
        List<String> allJdSkills = fullRes.getJdSkills() != null
                ? fullRes.getJdSkills()
                : new ArrayList<>();
        List<String> aiMatchedList = fullRes.getMatchedSkills() != null
                ? fullRes.getMatchedSkills()
                : new ArrayList<>();

        if (allJdSkills.isEmpty()) {
            allJdSkills = extractSkillsFromTextManual(jdText);
            fullRes.setJdSkills(allJdSkills);
        }

        // FORCE ALIGNMENT: Ensure allJdSkills is the union of mandatory and preferred.
        // This prevents "6/6 (100%)" when it should be "6/8 (75%)" if AI missed the
        // master JD list.
        Set<String> masterJdSet = new HashSet<>(allJdSkills);
        masterJdSet.addAll(mandatorySkills);
        masterJdSet.addAll(preferredSkills);
        allJdSkills = new ArrayList<>(masterJdSet);
        fullRes.setJdSkills(allJdSkills);

        // Track which skills were ACTUALLY matched during this granular check
        Set<String> actuallyMatched = new HashSet<>(aiMatchedList);

        // 1. Mandatory Skills Match (Critical)
        int mandatoryMatched = 0;
        int mandatoryTotal = mandatorySkills.size();
        List<String> missingMandatory = new ArrayList<>();
        for (String skill : mandatorySkills) {
            if (isSkillPresent(skill, resumeSkills, embeddingContext, aiMatchedList)) {
                mandatoryMatched++;
                actuallyMatched.add(skill);
            } else {
                missingMandatory.add(skill);
            }
        }

        // 2. Preferred Skills Match (Bonus)
        int preferredMatched = 0;
        int preferredTotal = preferredSkills.size();
        List<String> missingPreferred = new ArrayList<>();
        for (String skill : preferredSkills) {
            if (isSkillPresent(skill, resumeSkills, embeddingContext, aiMatchedList)) {
                preferredMatched++;
                actuallyMatched.add(skill);
            } else {
                missingPreferred.add(skill);
            }
        }

        // 3. Overall JD Skill Coverage (Fairness check)
        int overallMatchedCount = 0;
        for (String skill : allJdSkills) {
            if (isSkillPresent(skill, resumeSkills, embeddingContext, aiMatchedList)) {
                overallMatchedCount++;
                actuallyMatched.add(skill);
            }
        }

        // Sync the master lists for UI consistency
        List<String> currentMissing = fullRes.getMissingSkills() != null ? new ArrayList<>(fullRes.getMissingSkills())
                : new ArrayList<>();

        // Remove anything that was actually matched from missing
        currentMissing.removeIf(m -> actuallyMatched.stream().anyMatch(a -> a.equalsIgnoreCase(m)));

        // Add genuinely missing skills
        for (String m : missingMandatory)
            if (!currentMissing.stream().anyMatch(cm -> cm.equalsIgnoreCase(m)))
                currentMissing.add(m);
        for (String p : missingPreferred)
            if (!currentMissing.stream().anyMatch(cm -> cm.equalsIgnoreCase(p)))
                currentMissing.add(p);

        fullRes.setMissingSkills(currentMissing);

        // Update matched skills to include everything we found
        fullRes.setMatchedSkills(new ArrayList<>(actuallyMatched));

        // Weighted calculation
        double mandatoryScore = mandatoryTotal > 0
                ? ((double) mandatoryMatched / mandatoryTotal) * 100.0
                : 100.0;

        double preferredScore = preferredTotal > 0
                ? ((double) preferredMatched / preferredTotal) * 100.0
                : 100.0;

        double overallCoverageScore = !allJdSkills.isEmpty()
                ? ((double) overallMatchedCount / allJdSkills.size()) * 100.0
                : 100.0;

        // Set counts and individual match percentages in response
        fullRes.setMandatoryMatchedCount(mandatoryMatched);
        fullRes.setMandatoryTotalCount(mandatoryTotal);
        fullRes.setPreferredMatchedCount(preferredMatched);
        fullRes.setPreferredTotalCount(preferredTotal);
        fullRes.setOverallMatchedCount(overallMatchedCount);
        fullRes.setOverallTotalCount(allJdSkills.size());

        // NEW BALANCED WEIGHTS:
        double totalHardSkillScore = (mandatoryScore * 0.50) +
                (preferredScore * 0.20) +
                (overallCoverageScore * 0.30);

        log.info("   - Mandatory Skills: {}/{} ({}%)",
                mandatoryMatched, mandatoryTotal, String.format("%.2f", mandatoryScore));
        log.info("   - Preferred Skills: {}/{} ({}%)",
                preferredMatched, preferredTotal, String.format("%.2f", preferredScore));
        log.info("   - Overall Coverage: {}/{} ({}%)",
                overallMatchedCount, allJdSkills.size(), String.format("%.2f", overallCoverageScore));

        return Math.min(100.0, totalHardSkillScore);
    }

    private boolean isSkillPresent(String skill, Map<String, SkillEvidence> resumeSkills,
            Map<String, List<Float>> embeddingContext, List<String> aiMatchedList) {
        if (skill == null)
            return false;
        String skillLower = skill.toLowerCase().trim();

        // 1. Direct match in maps
        if (resumeSkills.containsKey(skillLower)) {
            return true;
        }

        // 2. Trust the AI: If AI already said it's matched in fullAnalysis, it's
        // matched.
        // This handles cases where LLM matching is smarter than our local map check.
        if (aiMatchedList != null && aiMatchedList.stream().anyMatch(s -> s.equalsIgnoreCase(skillLower))) {
            return true;
        }

        // 3. Semantic match (0.80+ similarity)
        return checkCorrelatedSkillMatch(skill, resumeSkills, embeddingContext);
    }

    private double calculateSoftSkillsMatch(Map<String, SkillEvidence> jdSkills,
            Map<String, SkillEvidence> resumeSkills,
            Map<String, List<Float>> embeddingContext,
            String jdText) {

        Set<String> csvSoftSkills = dataService.getAllSkillNames().stream()
                .filter(skill -> {
                    String category = dataService.getSkillCategory(skill);
                    return category != null && category.toLowerCase().contains("soft");
                })
                .collect(Collectors.toSet());

        Set<String> commonSoftSkills = csvSoftSkills.isEmpty()
                ? Set.of("communication", "leadership", "teamwork", "problem solving",
                        "time management", "adaptability", "creativity", "collaboration",
                        "critical thinking", "resilience", "empathy", "integrity")
                : csvSoftSkills;

        // ONLY score soft skills explicitly mentioned in JD
        Set<String> jdMentionedSoftSkills = new HashSet<>();
        String jdLower = jdText.toLowerCase();

        for (String skill : commonSoftSkills) {
            if (jdLower.contains(skill.toLowerCase())) {
                jdMentionedSoftSkills.add(skill);
            }
        }

        // If JD doesn't mention soft skills, don't reward/penalize
        if (jdMentionedSoftSkills.isEmpty()) {
            return 50.0; // Neutral, not 80
        }

        int matched = 0;
        for (String skill : jdMentionedSoftSkills) {
            if (resumeSkills.containsKey(skill.toLowerCase()) ||
                    checkCorrelatedSkillMatch(skill, resumeSkills, embeddingContext)) {
                matched++;
            }
        }

        return ((double) matched / jdMentionedSoftSkills.size()) * 100.0;
    }

    private double applyRefinedPenaltiesMultiplier(String jdText,
            Map<String, SkillEvidence> resumeSkills,
            Map<String, List<Float>> embeddingContext,
            List<String> mandatorySkills,
            List<String> aiMatchedList) {

        if (mandatorySkills == null || mandatorySkills.isEmpty()) {
            return 1.0; // No mandatory skills = no penalty
        }

        int missingMandatory = 0;
        List<String> missing = new ArrayList<>();

        for (String skill : mandatorySkills) {
            if (!isSkillPresent(skill, resumeSkills, embeddingContext, aiMatchedList)) {
                // Still allow "learning in progress" to mitigate penalty slightly?
                // The prompt says "NO grace period", so I'll follow that.
                missingMandatory++;
                missing.add(skill);
            }
        }

        if (missingMandatory == 0) {
            return 1.0; // All mandatory present, no penalty
        }

        // FAIR PENALTY: Each missing mandatory skill = 8% deduction (down from 15%)
        // This acknowledges that missing a primary skill is bad, but doesn't destroy
        // the whole score.
        double penaltyFactor = 1.0 - (0.08 * missingMandatory);
        penaltyFactor = Math.max(0.40, penaltyFactor); // Floor at 40%

        log.info("   - CRITICAL: Missing {} mandatory skill(s): {}",
                missingMandatory, String.join(", ", missing));

        return penaltyFactor;
    }

    private boolean checkCorrelatedSkillMatch(String jdSkill, Map<String, SkillEvidence> resumeSkills,
            Map<String, List<Float>> embeddingContext) {
        String jdLower = jdSkill.toLowerCase().trim();

        // Vector Similarity fallback (The Semantic AI Matcher)
        List<Float> targetEmb = embeddingContext.get(jdLower);
        if (targetEmb == null)
            return false;

        for (String resSkill : resumeSkills.keySet()) {
            List<Float> resEmb = embeddingContext.get(resSkill.toLowerCase());
            if (resEmb == null)
                continue;

            double similarity = embeddingService.calculateCosineSimilarity(targetEmb, resEmb);
            if (similarity >= 0.80) { // Slightly more relaxed for concept grouping
                return true;
            }
        }
        return false;
    }

    // ============================================
    // MATCH LEVEL & RECOMMENDATIONS
    // ============================================

    private String determineMatchLevel(double score) {
        if (score >= 80)
            return "Excellent Match";
        if (score >= 65)
            return "Strong Match";
        if (score >= 45)
            return "Good Match";
        if (score >= 25)
            return "Fair Match";
        return "Weak Match";
    }

    // ============================================
    // MAIN ANALYSIS METHOD
    // ============================================

    /**
     * Analyzes a resume against a job description using a weighted scoring
     * algorithm.
     * Considers context, specific skills, and correlation between improved groups.
     *
     * @param request The analysis request containing JD and Resume text.
     * @return The comprehensive analysis response including score, match level, and
     *         tips.
     * @throws InvalidInputException if inputs are null or empty.
     */
    public AnalysisResponse analyze(AnalysisRequest request) {
        if (request == null) {
            throw new InvalidInputException("Analysis request cannot be null");
        }
        if (request.jobDescription() == null || request.jobDescription().trim().isEmpty()) {
            throw new InvalidInputException("Job description cannot be empty");
        }
        if (request.resumeText() == null || request.resumeText().trim().isEmpty()) {
            throw new InvalidInputException("Resume text cannot be empty");
        }

        String jdText = request.jobDescription().trim();
        String resumeText = request.resumeText().trim();

        System.out.println("\n=== 🧠 Deep Analysis Engine Started ===");
        System.out.println("📄 JD Length: " + jdText.length() + " chars");
        System.out.println("📄 Resume Length: " + resumeText.length() + " chars");

        log.info("=== Optimized Analysis with Batch Embeddings Started ===");

        log.info("=== Optimized ATS Engine: Starting Deep Analysis ===");

        // 1. CONSOLIDATED CACHED ANALYSIS (Single Source of Truth)
        AiSkillProfile profile = skillService.getCachedProfile(jdText, resumeText);
        if (profile == null) {
            log.error("Failed to analyze skills with AI");
            return new AnalysisResponse();
        }

        // 2. PRE-CALCULATE ALL EMBEDDINGS (Single Batch)
        Set<String> stringsToEmbed = new HashSet<>();
        stringsToEmbed.add(jdText);
        stringsToEmbed.add(resumeText);

        if (profile.getJdRequiredSkills() != null)
            stringsToEmbed
                    .addAll(profile.getJdRequiredSkills().stream().map(String::toLowerCase)
                            .collect(Collectors.toSet()));
        if (profile.getStrongSkills() != null)
            stringsToEmbed
                    .addAll(profile.getStrongSkills().stream().map(String::toLowerCase).collect(Collectors.toSet()));
        if (profile.getMandatorySkills() != null)
            stringsToEmbed
                    .addAll(profile.getMandatorySkills().stream().map(String::toLowerCase).collect(Collectors.toSet()));
        if (profile.getJdRole() != null)
            stringsToEmbed.add(profile.getJdRole().toLowerCase());
        if (profile.getResumeTitle() != null)
            stringsToEmbed.add(profile.getResumeTitle().toLowerCase());
        if (profile.getMatchedSkills() != null)
            stringsToEmbed
                    .addAll(profile.getMatchedSkills().stream().map(String::toLowerCase).collect(Collectors.toSet()));
        if (profile.getMissingSkills() != null)
            stringsToEmbed
                    .addAll(profile.getMissingSkills().stream().map(String::toLowerCase).collect(Collectors.toSet()));

        log.info("🚀 Pre-calculating embeddings for {} priority keywords...", stringsToEmbed.size());
        Map<String, List<Float>> embeddingContext = embeddingService
                .batchGenerateEmbeddings(new ArrayList<>(stringsToEmbed));

        // 3. PREPARE FINAL SKILL MAPS
        Map<String, SkillEvidence> aiJdSkills = buildSkillMap(profile.getJdRequiredSkills());

        // Supplement AI resume skills with manual extraction to ensure 100% authority
        // check
        List<String> combinedResumeSkills = new ArrayList<>(
                profile.getStrongSkills() != null ? profile.getStrongSkills() : new ArrayList<>());
        List<String> manualResumeSkills = extractSkillsFromTextManual(resumeText);
        for (String ms : manualResumeSkills) {
            String disp = dataService.getSkillDisplayName(ms);
            if (!combinedResumeSkills.contains(disp))
                combinedResumeSkills.add(disp);
        }
        Map<String, SkillEvidence> aiResumeSkills = buildSkillMap(combinedResumeSkills);

        // 5. CORE SCORING (using high-quality AI maps)
        AnalysisResponse response = new AnalysisResponse();

        // Feed profile data into calculatePerfectMatchScore (which also cleans lists)
        AnalysisResponse tempRes = new AnalysisResponse();
        tempRes.setJdSkills(profile.getJdRequiredSkills());
        tempRes.setResumeSkills(profile.getStrongSkills());
        tempRes.setMatchedSkills(profile.getMatchedSkills());
        tempRes.setMissingSkills(profile.getMissingSkills());
        tempRes.setMandatorySkills(profile.getMandatorySkills());
        tempRes.setPreferredSkills(profile.getPreferredSkills());
        tempRes.setJobTitle(profile.getJdRole());
        tempRes.setResumeTitle(profile.getResumeTitle());

        double score = calculatePerfectMatchScore(
                aiJdSkills,
                aiResumeSkills,
                tempRes,
                jdText,
                resumeText,
                embeddingContext);
        response.setScore(score);
        response.setMatchLevel(determineMatchLevel(score));

        // Sync score back to profile for Chatbot reuse
        profile.setAtsScore(score);

        // Reuse Unified Analysis results for final output
        response.setJdSkills(tempRes.getJdSkills());
        response.setResumeSkills(tempRes.getResumeSkills());
        response.setMandatorySkills(tempRes.getMandatorySkills());
        response.setPreferredSkills(tempRes.getPreferredSkills());

        // Sync breakdown metrics
        response.setMandatoryMatchedCount(tempRes.getMandatoryMatchedCount());
        response.setMandatoryTotalCount(tempRes.getMandatoryTotalCount());
        response.setPreferredMatchedCount(tempRes.getPreferredMatchedCount());
        response.setPreferredTotalCount(tempRes.getPreferredTotalCount());
        response.setOverallMatchedCount(tempRes.getOverallMatchedCount());
        response.setOverallTotalCount(tempRes.getOverallTotalCount());
        response.setHardSkillsScore(tempRes.getHardSkillsScore());
        response.setTitleScore(tempRes.getTitleScore());
        response.setEducationScore(tempRes.getEducationScore());
        response.setSemanticScore(tempRes.getSemanticScore());

        response.setSummary(profile.getSummary());
        response.setStrength(profile.getStrength());
        response.setImprovementArea(profile.getImprovementArea());
        response.setRecommendation(profile.getRecommendation());
        response.setResumeImprovementTips(profile.getResumeImprovementTips());
        response.setSkillImprovementTips(profile.getSkillImprovementTips());
        response.setTip(profile.getSummary() + " " + profile.getRecommendation());
        response.setJobTitle(profile.getJdRole());
        response.setResumeTitle(profile.getResumeTitle());
        response.setEducationRequirement(profile.getEducationRequirement());
        response.setCareerGrowthSkills(profile.getCareerGrowthSkills());

        // 6. Final Skill Matching Logic
        List<String> matched = tempRes.getMatchedSkills();
        List<String> missing = tempRes.getMissingSkills();

        // Fallback if AI didn't provide lists (Safety)
        if (matched == null || matched.isEmpty()) {
            List<String> jdSkills = response.getJdSkills();
            if (jdSkills != null) {
                matched = jdSkills.stream()
                        .filter(skill -> aiResumeSkills.containsKey(skill.toLowerCase()) ||
                                checkCorrelatedSkillMatch(skill, aiResumeSkills, embeddingContext))
                        .collect(Collectors.toList());
            } else {
                matched = new java.util.ArrayList<>();
            }
        }

        if (missing == null || missing.isEmpty()) {
            List<String> finalMatched = matched;
            List<String> jdSkills = response.getJdSkills();
            if (jdSkills != null) {
                missing = jdSkills.stream()
                        .filter(skill -> !finalMatched.contains(skill))
                        .collect(Collectors.toList());
            } else {
                missing = new java.util.ArrayList<>();
            }
        }

        // 7. Dynamic Reconcile (Bidirectional):
        // Uses Pinecone Semantic Embeddings to bridge gaps (e.g. React -> Frontend)
        completeBidirectionalReconcile(matched, missing, embeddingContext);

        // 8. FINAL SAFETY FILTER (Authority Check + Noise Removal)
        matched = filterAndStandardizeList(matched);
        missing = filterAndStandardizeList(missing);

        // 9. Data Authority: Supplement AI tips with advice from the local skills.csv
        response.setMatchedSkills(matched);
        response.setMissingSkills(missing);
        enrichTipsWithLocalData(response);

        // Final filter for Career Growth skills
        response.setCareerGrowthSkills(filterAndStandardizeList(response.getCareerGrowthSkills()));

        // SYNC BACK TO CACHE: Ensure Chatbot sees the exact same filtered lists
        profile.setMatchedSkills(matched);
        profile.setMissingSkills(missing);
        profile.setCareerGrowthSkills(response.getCareerGrowthSkills());

        log.info("=== Analysis Complete: Score = {} ===", score);
        return response;
    }

    /**
     * Supplemental Advice: Pulls authoritative adviceText from skills.csv
     * for identified gaps to ensure results are grounded in the local dataset.
     */
    private void enrichTipsWithLocalData(AnalysisResponse res) {
        if (res.getMissingSkills() == null || res.getMissingSkills().isEmpty())
            return;

        List<String> localTips = new ArrayList<>();
        int count = 0;
        for (String skill : res.getMissingSkills()) {
            String advice = dataService.getAdvice(skill);
            if (advice != null && !advice.isEmpty()) {
                // Professional formatting for local database tips
                String formatted = Character.toUpperCase(advice.charAt(0)) + advice.substring(1);
                if (!formatted.endsWith("."))
                    formatted += ".";

                localTips.add("💡 Expert Tip: " + formatted);
                count++;
                if (count >= 2)
                    break;
            }
        }

        if (!localTips.isEmpty()) {
            List<String> combined = new ArrayList<>(localTips);
            if (res.getSkillImprovementTips() != null) {
                combined.addAll(res.getSkillImprovementTips());
            }
            // Cap at 2 high-impact items
            if (combined.size() > 2) {
                combined = combined.subList(0, 2);
            }
            res.setSkillImprovementTips(combined);
        } else if (res.getSkillImprovementTips() != null && res.getSkillImprovementTips().size() > 2) {
            res.setSkillImprovementTips(res.getSkillImprovementTips().subList(0, 2));
        }

        // Also cap Resume Tips at 2
        if (res.getResumeImprovementTips() != null && res.getResumeImprovementTips().size() > 2) {
            res.setResumeImprovementTips(res.getResumeImprovementTips().subList(0, 2));
        }
    }

    /**
     * SEMANTIC RECONCILE (Dynamic Pinecone Logic):
     * Checks if missing skills are semantically covered by matched skills.
     * Replaces hardcoded SKILL_GROUPS with vector similarity.
     */
    private void completeBidirectionalReconcile(List<String> matched, List<String> missing,
            Map<String, List<Float>> embeddingContext) {
        if (matched == null || missing == null || embeddingContext == null)
            return;

        List<String> toAddToMatched = new ArrayList<>();
        List<String> toRemoveFromMissing = new ArrayList<>();

        for (String gap : missing) {
            String gapLower = gap.toLowerCase();
            List<Float> gapEmb = embeddingContext.get(gapLower);

            // If embedding missing, try generating it on the fly (fallback)
            if (gapEmb == null) {
                // We typically expect them in context, but purely for robustness:
                continue;
            }

            // Check if any matched skill covers this gap
            for (String have : matched) {
                String haveLower = have.toLowerCase();
                List<Float> haveEmb = embeddingContext.get(haveLower);
                if (haveEmb == null)
                    continue;

                double sim = embeddingService.calculateCosineSimilarity(gapEmb, haveEmb);

                // Threshold 0.82 covers implicit relationships (e.g. React -> Frontend)
                if (sim >= 0.82) {
                    toRemoveFromMissing.add(gap);
                    // Optional: Add implied skill to matched if you want to show credit
                    // toAddToMatched.add(gap + " (Implied)");
                    // For now, just removing the false gap is sufficient.
                    log.info("   - Semantic Bridge: '{}' covers missing '{}' (Sim: {})", have, gap,
                            String.format("%.2f", sim));
                    break;
                }
            }
        }

        // Apply changes
        missing.removeAll(toRemoveFromMissing);
        matched.addAll(toAddToMatched);
    }

    private List<String> extractSkillsFromTextManual(String text) {
        if (text == null || text.isBlank())
            return new ArrayList<>();

        Set<String> foundSkills = new HashSet<>();
        String lower = text.toLowerCase();

        // 1. Scan Topic Names
        for (String skillName : dataService.getAllSkillNames()) {
            String regex = "\\b" + java.util.regex.Pattern.quote(skillName.toLowerCase()) + "\\b";
            if (java.util.regex.Pattern.compile(regex).matcher(lower).find()) {
                foundSkills.add(dataService.getSkillDisplayName(skillName));
            }
        }

        // 2. Scan Keywords (e.g. OOPS, JS, DSA)
        for (String keyword : dataService.getAllKeywords()) {
            String regex = "\\b" + java.util.regex.Pattern.quote(keyword.toLowerCase()) + "\\b";
            if (java.util.regex.Pattern.compile(regex).matcher(lower).find()) {
                foundSkills.add(dataService.getSkillDisplayName(keyword));
            }
        }

        return new ArrayList<>(foundSkills);
    }

    private Map<String, SkillEvidence> buildSkillMap(List<String> skills) {
        Map<String, SkillEvidence> map = new HashMap<>();
        if (skills == null)
            return map;
        for (String s : skills) {
            map.put(s.toLowerCase(), new SkillEvidence(s, true, 1, "moderate", false));
        }
        return map;
    }

    // The extractSkillsWithContext method is no longer directly used in the analyze
    // flow
    // as skills are now extracted by skillService.getCachedProfile and then
    // processed by buildSkillMap.
    // If it's used elsewhere, it might need to be adapted or removed if obsolete.
    // For this change, we assume it's no longer needed in the main analysis path.
}