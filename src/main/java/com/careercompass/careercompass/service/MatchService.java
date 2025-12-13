package com.careercompass.careercompass.service;

import com.careercompass.careercompass.dto.AiSkillProfile;
import com.careercompass.careercompass.dto.AnalysisRequest;
import com.careercompass.careercompass.dto.AnalysisResponse;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Service
public class MatchService {

    // === Inject GroqAi (AI helper) via constructor injection ===
    private final GroqAi groqAi;

    public MatchService(GroqAi groqAi) {
        this.groqAi = groqAi;
    }

    // ---------------------------
    // Skill lists & heuristics
    // ---------------------------
    private static final List<String> SKILLS = Arrays.asList(
            "java","python","javascript","typescript",
            "c","c programming","c++","c#","go","ruby","kotlin","swift","php",
            "spring","spring boot","django","flask","node.js","node js",
            "html","css","react","angular","vue","bootstrap",
            "sql","mysql","postgresql","oracle","mongodb",
            "git","github","docker","aws","azure","gcp",
            "excel","power bi","tableau","data analysis","data analyst",
            "business analyst","business analysis","requirements gathering",
            "requirement gathering","stakeholder management",
            "ms office","microsoft office","word","powerpoint",
            "sales","marketing","crm","customer relationship",
            "business development","b2b","b2c",
            "oops","object oriented programming",
            "data structures","algorithms","dsa",
            "communication","teamwork","leadership","problem solving","analytical thinking","presentation"
    );

    private static final List<String> WEAK_SKILL_HINTS = Arrays.asList(
            "certification","course","introduction to","intro to","learning",
            "currently learning","basics","basic","beginner","foundation","training"
    );

    private static final Set<String> LANGUAGE_SKILLS = Set.of(
            "java","python","javascript","typescript","c","c++","c#","go","ruby","kotlin","swift","php"
    );

    private static final Set<String> KEY_IMPORTANT_SKILLS = Set.of(
            "java","python","javascript","typescript","c#","c++",
            "spring","spring boot","django","flask","node.js","node js",
            "react","angular","vue",
            "sql","mysql","postgresql","oracle","mongodb",
            "excel","power bi","tableau",
            "data analyst","business analyst","business analysis",
            "sales","marketing","crm","business development"
    );

    private static final Set<String> GENERAL_RELEVANT_SKILLS = Set.of(
            "oops","object oriented programming","data structures","algorithms","dsa",
            "git","github","html","css","javascript",
            "excel","ms office","microsoft office","communication","teamwork","leadership",
            "problem solving","analytical thinking","presentation",
            "requirements gathering","requirement gathering","stakeholder management","customer relationship"
    );

    // ---------------------------
    // Helper: weak mention detection (local window)
    // ---------------------------
    private boolean isWeakMention(String lowerText, String skill, int skillIndex) {
        int start = Math.max(0, skillIndex - 40);
        int end = Math.min(lowerText.length(), skillIndex + skill.length() + 40);
        String window = lowerText.substring(start, end);
        for (String weakKey : WEAK_SKILL_HINTS) {
            String wk = weakKey.toLowerCase();
            if (window.contains(wk + " " + skill) || window.contains(skill + " " + wk)) {
                return true;
            }
        }
        return false;
    }

    // ---------------------------
    // Extract skills deterministically from text
    // ---------------------------
    private List<String> extractSkills(String text) {
        if (text == null || text.isBlank()) return new ArrayList<>();
        String lower = text.toLowerCase();
        List<String> detected = new ArrayList<>();

        for (String rawSkill : SKILLS) {
            if (rawSkill == null || rawSkill.isBlank()) continue;
            String skill = rawSkill.toLowerCase();

            boolean foundStrong = false;

            if (skill.contains(" ") || skill.contains(".")) {
                int index = lower.indexOf(skill);
                while (index != -1) {
                    if (!isWeakMention(lower, skill, index)) {
                        foundStrong = true;
                        break;
                    }
                    index = lower.indexOf(skill, index + skill.length());
                }
            } else {
                String regex = "\\b" + Pattern.quote(skill) + "\\b";
                Pattern pattern = Pattern.compile(regex);
                Matcher matcher = pattern.matcher(lower);
                while (matcher.find()) {
                    int index = matcher.start();
                    if (!isWeakMention(lower, skill, index)) {
                        foundStrong = true;
                        break;
                    }
                }
            }

            if (foundStrong) detected.add(skill);
        }

        return detected.stream().distinct().collect(Collectors.toList());
    }

    // ---------------------------
    // Normalize skills collection
    // ---------------------------
    private Set<String> normalizeSkills(Collection<String> skills) {
        Set<String> out = new HashSet<>();
        if (skills == null) return out;
        for (String s : skills) {
            if (s == null) continue;
            String c = s.toLowerCase().trim();
            if (!c.isEmpty()) out.add(c);
        }
        return out;
    }

    // ---------------------------
    // Keep only skills actually present in resume (safety)
    // ---------------------------
    private List<String> filterSkillsByResumeEvidence(List<String> skills, String resumeText) {
        if (skills == null || skills.isEmpty() || resumeText == null) return new ArrayList<>();
        String lower = resumeText.toLowerCase();
        return skills.stream()
                .filter(Objects::nonNull)
                .map(String::trim)
                .map(String::toLowerCase)
                .filter(s -> !s.isEmpty() && lower.contains(s))
                .distinct()
                .collect(Collectors.toList());
    }

    // ---------------------------
    // Deterministic scoring (0-100)
    // ---------------------------
    private double calculateMatchScore(Set<String> jdSkillsRaw, Set<String> resumeSkillsRaw) {
        Set<String> jdSkills = normalizeSkills(jdSkillsRaw);
        Set<String> resumeSkills = normalizeSkills(resumeSkillsRaw);

        if (jdSkills.isEmpty()) return 0.0;

        Set<String> matched = new HashSet<>(jdSkills);
        matched.retainAll(resumeSkills);

        double matchedCount = matched.size();
        double totalJdSkills = jdSkills.size();

        double score0to10 = (matchedCount / totalJdSkills) * 10.0;

        boolean hasRelevantBackground = resumeSkills.stream()
                .anyMatch(s -> LANGUAGE_SKILLS.contains(s) || GENERAL_RELEVANT_SKILLS.contains(s));

        if (matchedCount == 0 && hasRelevantBackground) score0to10 = 2.0;
        if (matchedCount == 0 && !hasRelevantBackground) score0to10 = 0.0;

        // language cap
        Set<String> jdLangs = jdSkills.stream().filter(LANGUAGE_SKILLS::contains).collect(Collectors.toSet());
        boolean resumeHasJdLang = resumeSkills.stream().anyMatch(jdLangs::contains);

        if (!jdLangs.isEmpty() && !resumeHasJdLang && score0to10 > 6.5) score0to10 = 6.5;

        // key skill cap
        boolean hasKeySkill = jdSkills.stream().anyMatch(s -> KEY_IMPORTANT_SKILLS.contains(s) && resumeSkills.contains(s));
        if (!hasKeySkill && score0to10 > 6.5) score0to10 = 6.5;

        if (score0to10 > 9.0) score0to10 = 9.0;

        return Math.round(score0to10 * 10.0); // -> percentage 0..90 (capped)
    }

    // ---------------------------
    // Map score -> label
    // ---------------------------
    private String determineMatchLevel(double score) {
        if (score >= 75) return "Strong Match";
        if (score >= 40) return "Medium Match";
        return "Weak Match";
    }

    // ---------------------------
    // Calibrate with AI profile (light adjustments)
    // ---------------------------
    private double calibrateScoreWithProfile(AiSkillProfile profile, List<String> jdSkills, List<String> resumeSkills, double baseScore) {
        if (profile == null || jdSkills == null || jdSkills.isEmpty()) return baseScore;

        Set<String> jd = normalizeSkills(jdSkills);
        Set<String> strong = normalizeSkills(profile.getStrongSkills());
        Set<String> weak = normalizeSkills(profile.getWeakSkills());

        Set<String> matchedStrong = new HashSet<>(jd);
        matchedStrong.retainAll(strong);

        double strongRatio = jd.isEmpty() ? 0.0 : (double) matchedStrong.size() / (double) jd.size();
        double adjusted = baseScore;

        if (!profile.isGenerallyRelated()) adjusted = Math.min(adjusted, 35.0);
        if (strongRatio < 0.3 && adjusted > 75.0) adjusted = 75.0;

        if (adjusted < 0.0) adjusted = 0.0;
        if (adjusted > 100.0) adjusted = 100.0;

        return Math.round(adjusted);
    }

    // ===============================================================
    // RESYMATCH WEIGHTED SCORING ALGORITHM (ADD-ONLY)
    // ===============================================================
    private double applyResyMatchScore(
            Set<String> jdSkills,
            Set<String> resumeSkills,
            List<String> resumeWords
    ) {
        if (jdSkills.isEmpty()) return 0;

        // 1. Exact keyword match (50%)
        long exactMatches = jdSkills.stream()
                .filter(resumeSkills::contains)
                .count();
        double exactScore = ((double) exactMatches / jdSkills.size()) * 50.0;

        // 2. Hard skills (25%)
        long hardMatches = resumeSkills.stream()
                .filter(s -> LANGUAGE_SKILLS.contains(s) || KEY_IMPORTANT_SKILLS.contains(s))
                .count();
        double hardScore = Math.min(hardMatches * 5, 25);

        // 3. Soft skills (15%)
        long softMatches = resumeSkills.stream()
                .filter(GENERAL_RELEVANT_SKILLS::contains)
                .count();
        double softScore = Math.min(softMatches * 3, 15);

        // 4. Keyword Density (10%)
        long densityHits = jdSkills.stream()
                .filter(s -> Collections.frequency(resumeWords, s) > 1)
                .count();
        double densityScore = Math.min(densityHits * 2, 10);

        return exactScore + hardScore + softScore + densityScore; // 0–100
    }

    // ---------------------------
    // Main analyze() used by controller
    // ---------------------------
    public AnalysisResponse analyze(AnalysisRequest request) {

        String jdText = request.getJobDescription();
        String resumeText = request.getResumeText();

        System.out.println("=== /api/analyze called ===");

        // 1) Extract skills deterministically
        List<String> jdSkills = extractSkills(jdText);
        List<String> resumeSkills = extractSkills(resumeText);

        // 2) Optional AI profile (for calibration + RAG context only)
        AiSkillProfile aiProfile = groqAi.analyzeSkillsWithAi(jdText, resumeText);

        if (aiProfile != null) {
            List<String> verifiedStrong = filterSkillsByResumeEvidence(aiProfile.getStrongSkills(), resumeText);
            List<String> verifiedWeak = filterSkillsByResumeEvidence(aiProfile.getWeakSkills(), resumeText);
            aiProfile.setStrongSkills(verifiedStrong);
            aiProfile.setWeakSkills(verifiedWeak);
        }

        // 3) Matched + Missing (from deterministic lists)
        List<String> matched = jdSkills.stream().filter(resumeSkills::contains).collect(Collectors.toList());
        List<String> missing = jdSkills.stream().filter(s -> !resumeSkills.contains(s)).collect(Collectors.toList());

        // 4) Base deterministic score
        Set<String> jdSet = new HashSet<>(jdSkills);
        Set<String> resumeSet = new HashSet<>(resumeSkills);

        double baseScore = calculateMatchScore(jdSet, resumeSet);

        // ResyMatch weighted scoring
        List<String> resumeWords = Arrays.asList(resumeText.toLowerCase().split("\\W+"));
        double resyMatchScore = applyResyMatchScore(jdSet, resumeSet, resumeWords);

        // Combined score (non-destructive)
        double score = Math.round((resyMatchScore * 0.7) + (baseScore * 0.3));

        // 5) Calibrate AI profile
        if (aiProfile != null) {
            score = calibrateScoreWithProfile(aiProfile, jdSkills, resumeSkills, score);
        }

        String matchLevel = determineMatchLevel(score);

        // 6) Build fallback tip
        String tip;
        if (!missing.isEmpty()) {
            tip = "You are missing: " + missing;
        } else if (score == 0.0) {
            tip = "This job does not match your current skills.";
        } else {
            tip = "Great match! Emphasize your strengths.";
        }

        // 7) Construct response
        AnalysisResponse response = new AnalysisResponse();
        response.setScore(score);
        response.setMatchLevel(matchLevel);
        response.setJdSkills(jdSkills);
        response.setResumeSkills(resumeSkills);
        response.setMatchedSkills(matched);
        response.setMissingSkills(missing);
        response.setTip(tip);

        // 8) AI refinement
        String improvedTip = groqAi.generateImprovedTip(request, response);
        response.setTip(improvedTip);

        // 9) Structured ATS insights
        groqAi.enrichWithInsights(request, response);

        return response;
    }
}
