package com.careercompass.careercompass.service;

import com.careercompass.careercompass.dto.AiSkillProfile;
import com.careercompass.careercompass.dto.AnalysisResponse;
import com.careercompass.careercompass.dto.JobRequirements;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

@Service
public class SkillAnalysisService {
    private static final Logger log = LoggerFactory.getLogger(SkillAnalysisService.class);

    private final GroqClient groqClient;
    private final RAGService ragService;
    private final PromptBuilder promptBuilder;
    private final CsvDataService csvDataService;
    private final CacheService cacheService;
    private final ObjectMapper objectMapper;

    public SkillAnalysisService(GroqClient groqClient, RAGService ragService,
            PromptBuilder promptBuilder, CsvDataService csvDataService,
            CacheService cacheService) {
        this.groqClient = groqClient;
        this.ragService = ragService;
        this.promptBuilder = promptBuilder;
        this.csvDataService = csvDataService;
        this.cacheService = cacheService;
        this.objectMapper = groqClient.getObjectMapper();
    }

    public AiSkillProfile getCachedProfile(String jd, String resume) {
        String hash = java.util.UUID.nameUUIDFromBytes((jd + resume).getBytes()).toString();
        AiSkillProfile cached = cacheService.getProfile(hash);
        if (cached != null) {
            log.info("🎯 Cache hit for AiSkillProfile");
            return cached;
        }

        AiSkillProfile fresh = analyzeSkillsWithAi(jd, resume);
        if (fresh != null) {
            cacheService.putProfile(hash, fresh);
        }
        return fresh;
    }

    public AiSkillProfile analyzeSkillsWithAi(String jd, String resume) {
        List<String> extractedJdSkills = extractSkills(jd);
        List<CsvSnippetLoader.Snippets> snippets = ragService.getRagContextOptimized(new HashSet<>(extractedJdSkills),
                jd);

        String context = snippets.stream()
                .map(s -> "### " + s.getTopic() + " (" + s.getCategory() + ")\n" + s.getAdviceText())
                .distinct()
                .collect(Collectors.joining("\n\n"));

        String prompt = promptBuilder.buildSkillAnalysisPrompt(context, jd, resume);
        String raw = groqClient.callGroq(prompt, GroqClient.QueryComplexity.POWERFUL);

        if (raw == null)
            return null;

        try {
            int a = raw.indexOf("{");
            int b = raw.lastIndexOf("}");
            if (a == -1 || b == -1)
                return null;

            String json = raw.substring(a, b + 1);
            Map<String, Object> map = objectMapper.readValue(json, Map.class);

            AiSkillProfile p = new AiSkillProfile();
            p.setJdRequiredSkills(standardizeAndFilter(toList(map.get("jdRequiredSkills"))));
            p.setStrongSkills(standardizeAndFilter(toList(map.get("strongSkills"))));
            p.setMatchedSkills(standardizeAndFilter(toList(map.get("matchedSkills"))));
            p.setMissingSkills(standardizeAndFilter(toList(map.get("missingSkills"))));
            p.setMandatorySkills(standardizeAndFilter(toList(map.get("mandatorySkills"))));
            p.setPreferredSkills(standardizeAndFilter(toList(map.get("preferredSkills"))));

            p.setJdRole(getString(map, "jdRole"));
            p.setResumeTitle(getString(map, "resumeTitle"));
            p.setEducationRequirement(getString(map, "education"));

            p.setSummary(professionalize(getString(map, "summary")));
            p.setStrength(professionalize(getString(map, "strength")));
            p.setImprovementArea(professionalize(getString(map, "improvementArea")));
            p.setRecommendation(professionalize(getString(map, "recommendation")));
            p.setResumeImprovementTips(professionalizeList(toList(map.get("resumeTips"))));
            p.setSkillImprovementTips(professionalizeList(toList(map.get("skillTips"))));
            p.setProTips(professionalizeList(toList(map.get("proTips"))));
            p.setCareerGrowthSkills(standardizeAndFilter(toList(map.get("careerGrowthSkills"))));

            return p;
        } catch (Exception e) {
            log.error("❌ Unified Analysis Error: {}", e.getMessage());
            return null;
        }
    }

    public List<String> extractSkills(String text) {
        if (text == null || text.isBlank())
            return new ArrayList<>();
        String prompt = promptBuilder.buildSkillExtractionPrompt(text);
        String raw = groqClient.callGroq(prompt, GroqClient.QueryComplexity.BALANCED);

        if (raw == null || raw.isBlank())
            return new ArrayList<>();

        return Arrays.stream(raw.split(","))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .map(s -> csvDataService.getSkillDisplayName(s))
                .filter(s -> csvDataService.findSkillId(s) != null)
                .filter(this::isNotNoise)
                .distinct()
                .collect(Collectors.toList());
    }

    public List<String> standardizeAndFilter(List<String> rawSkills) {
        if (rawSkills == null)
            return new ArrayList<>();
        return rawSkills.stream()
                .filter(s -> csvDataService.findSkillId(s) != null)
                .map(s -> csvDataService.getSkillDisplayName(s))
                .filter(s -> {
                    String cat = csvDataService.getSkillCategory(s);
                    boolean isSoft = cat != null && cat.toLowerCase().contains("soft");
                    return !isSoft && isNotNoise(s);
                })
                .distinct()
                .collect(Collectors.toList());
    }

    private boolean isNotNoise(String s) {
        String lower = s.toLowerCase();
        return !lower.contains("backend development") &&
                !lower.contains("backend developer") &&
                !lower.contains("frontend development") &&
                !lower.contains("frontend developer") &&
                !lower.contains("software engineer") &&
                !lower.contains("software engineering") &&
                !lower.contains("junior developer") &&
                !lower.contains("senior developer") &&
                !lower.contains("entry level") &&
                !lower.contains("full stack") &&
                !lower.contains("web development") &&
                !lower.contains("analyze different job") &&
                !lower.contains("cover letter generator") &&
                !lower.contains("resume guide") &&
                !lower.contains("roadmap") &&
                !lower.contains("bakenddevloper") &&
                !lower.contains("job description") &&
                !lower.contains("resume analysis") &&
                !lower.contains("ats score") &&
                !lower.contains("skill gaps") &&
                !lower.contains("matched skills") &&
                !lower.contains("pro tips") &&
                !lower.contains("resume") &&
                !lower.contains("candidate") &&
                !lower.contains("learning tips") &&
                !lower.contains("formatting tips") &&
                !lower.contains("industry standard") &&
                !lower.contains("overall job fit") &&
                !lower.contains("analysis complete") &&
                !lower.contains("match summary") &&
                !lower.contains("critical gap") &&
                !lower.contains("strategic recommendation") &&
                !lower.contains("score") &&
                !lower.contains("summary") &&
                !lower.contains("strength") &&
                !lower.contains("goal") &&
                !lower.contains("expert tip") &&
                lower.length() >= 2;
    }

    public String professionalize(String text) {
        if (text == null || text.isBlank())
            return "";
        String cleaned = text.replaceAll(
                "(?i)(job description|resume analysis|ats score|skill gaps|matched skills|pro tips|learning tips|formatting tips|industry standard|overall job fit|analysis complete|match summary|critical gap|strategic recommendation|expert tip|tip \\d+:?|summary:?|strength:?|goal:?|roadmap|bakenddevloper)",
                "")
                .replaceAll("(?m)[ \\t]{2,}", " ")
                .replaceAll("\\n{3,}", "\n\n")
                .trim();

        if (cleaned.isEmpty())
            return "";

        char[] chars = cleaned.toCharArray();
        for (int i = 0; i < chars.length; i++) {
            if (Character.isLetter(chars[i])) {
                chars[i] = Character.toUpperCase(chars[i]);
                break;
            }
        }
        cleaned = new String(chars);

        if (!cleaned.endsWith(".") && !cleaned.endsWith("!") && !cleaned.endsWith("?")) {
            cleaned += ".";
        }
        return cleaned;
    }

    public List<String> professionalizeList(List<String> list) {
        if (list == null)
            return new ArrayList<>();
        return list.stream()
                .map(this::professionalize)
                .filter(s -> !s.isEmpty())
                .collect(Collectors.toList());
    }

    public String getString(Map<String, Object> map, String... keys) {
        for (String key : keys) {
            if (map.containsKey(key) && map.get(key) != null) {
                return map.get(key).toString().trim();
            }
        }
        return "";
    }

    public List<String> toList(Object obj) {
        if (obj instanceof List<?> list) {
            return list.stream().filter(Objects::nonNull)
                    .map(o -> o.toString().trim().toLowerCase())
                    .collect(Collectors.toList());
        }
        return List.of();
    }

    public JobRequirements parseJobRequirements(String jdText) {
        if (jdText == null || jdText.isBlank())
            return new JobRequirements();
        String prompt = promptBuilder.buildJobRequirementsPrompt(jdText);
        String raw = groqClient.callGroq(prompt, GroqClient.QueryComplexity.BALANCED);
        if (raw == null)
            return new JobRequirements();

        try {
            int a = raw.indexOf("{");
            int b = raw.lastIndexOf("}");
            if (a == -1 || b == -1)
                return new JobRequirements();
            return objectMapper.readValue(raw.substring(a, b + 1), JobRequirements.class);
        } catch (Exception e) {
            log.error("❌ Error parsing JobRequirements: {}", e.getMessage());
            return new JobRequirements();
        }
    }
}
