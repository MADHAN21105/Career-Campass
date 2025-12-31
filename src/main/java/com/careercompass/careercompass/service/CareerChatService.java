package com.careercompass.careercompass.service;

import com.careercompass.careercompass.dto.AiSkillProfile;
import com.careercompass.careercompass.dto.CoverLetterRequest;
import com.careercompass.careercompass.dto.QuestionRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.function.Consumer;
import java.util.stream.Collectors;

@Service
@SuppressWarnings("all")
public class CareerChatService {
    private static final Logger log = LoggerFactory.getLogger(CareerChatService.class);

    private final GroqClient groqClient;
    private final RAGService ragService;
    private final PromptBuilder promptBuilder;
    private final SkillAnalysisService skillService;
    private final CacheService cacheService;

    public enum CareerQueryIntent {
        SKILL_EXPLANATION, LEARNING_ROADMAP, SKILL_GAP_ANALYSIS,
        INTERVIEW_PREP, PROJECT_IDEAS, CAREER_SWITCH,
        JOB_REQUIREMENTS, GENERAL_ADVICE, FAQ, LIST_ONLY,
        GRATITUDE, UNKNOWN
    }

    public CareerChatService(GroqClient groqClient, RAGService ragService,
            PromptBuilder promptBuilder, SkillAnalysisService skillService,
            CacheService cacheService) {
        this.groqClient = groqClient;
        this.ragService = ragService;
        this.promptBuilder = promptBuilder;
        this.skillService = skillService;
        this.cacheService = cacheService;
    }

    public String answerCareerQuestion(String question, String resume, String jd, double atsScore) {
        String cacheKey = java.util.UUID.nameUUIDFromBytes((question + resume + jd + atsScore).getBytes()).toString();
        String cached = cacheService.getQuestion(cacheKey);
        if (cached != null)
            return cached;

        CareerQueryIntent intent = detectIntent(question);
        if (intent == CareerQueryIntent.GRATITUDE)
            return getRandomGratitudeResponse();

        AiSkillProfile profile = skillService.getCachedProfile(jd, resume);
        if (profile != null && atsScore > 0)
            profile.setAtsScore(atsScore);

        String ragContext = getRagContext(question, profile, intent);
        String prompt = promptBuilder.buildCareerPrompt(question, resume, jd, profile, intent.name(), ragContext);

        GroqClient.QueryComplexity complexity = (intent == CareerQueryIntent.SKILL_GAP_ANALYSIS
                || intent == CareerQueryIntent.CAREER_SWITCH)
                        ? GroqClient.QueryComplexity.POWERFUL
                        : GroqClient.QueryComplexity.BALANCED;

        String response = groqClient.callGroq(prompt, complexity);
        if (response != null) {
            response = skillService.professionalize(response);
            cacheService.putQuestion(cacheKey, response);
        }
        return response;
    }

    public String answerCareerQuestion(QuestionRequest request) {
        // AI profile construction for the controller call
        AiSkillProfile profile = new AiSkillProfile();
        profile.setMatchedSkills(request.matchedSkills() != null ? request.matchedSkills() : new ArrayList<>());
        profile.setMissingSkills(request.missingSkills() != null ? request.missingSkills() : new ArrayList<>());
        profile.setSummary(request.summary());
        profile.setStrength(request.strength());
        profile.setImprovementArea(request.improvementArea());
        profile.setRecommendation(request.recommendation());
        profile.setAtsScore(request.atsScore() != null ? request.atsScore() : 0.0);
        profile.setJdRole(request.jobTitle());
        profile.setResumeBio(request.resumeBio());

        log.info("📧 [Chat Request] Question: {}", request.question());
        log.info("📊 [Chat Context] Score: {}%, Role: {}, Bio present: {}",
                profile.getAtsScore(), profile.getJdRole(),
                (profile.getResumeBio() != null && !profile.getResumeBio().isEmpty()));
        log.info("🛠️ [Chat Skills] Matched: {}, Missing: {}",
                profile.getMatchedSkills().size(), profile.getMissingSkills().size());

        CareerQueryIntent intent = detectIntent(request.question());
        if (intent == CareerQueryIntent.GRATITUDE)
            return getRandomGratitudeResponse();

        List<CsvSnippetLoader.Snippets> snippets = ragService.retrieveRelevantSnippets(request.question(),
                request.missingSkills(), null);
        String ragContext = snippets.stream()
                .map(s -> "### " + s.getTopic() + " (" + s.getCategory() + ")\n" + s.getAdviceText())
                .collect(Collectors.joining("\n\n"));

        String prompt = promptBuilder.buildCareerPrompt(request.question(), request.resumeText(),
                request.jobDescription(), profile, intent.name(),
                ragContext);
        String response = groqClient.callGroq(prompt, GroqClient.QueryComplexity.POWERFUL);

        return response != null ? skillService.professionalize(response) : null;
    }

    private String getRagContext(String question, AiSkillProfile profile, CareerQueryIntent intent) {
        Set<String> searchSkills = new HashSet<>();
        if (profile != null) {
            if (intent == CareerQueryIntent.SKILL_GAP_ANALYSIS || intent == CareerQueryIntent.LEARNING_ROADMAP) {
                searchSkills.addAll(profile.getMissingSkills());
            } else if (intent == CareerQueryIntent.INTERVIEW_PREP) {
                searchSkills.addAll(profile.getMatchedSkills());
            } else {
                searchSkills.addAll(profile.getJdRequiredSkills());
            }
        }

        List<CsvSnippetLoader.Snippets> snippets = ragService.getRagContextOptimized(searchSkills, question);
        return snippets.stream()
                .map(s -> "topic: " + s.getTopic() + "\nadvice: " + s.getAdviceText())
                .collect(Collectors.joining("\n---\n"));
    }

    public CareerQueryIntent detectIntent(String q) {
        String lower = q.toLowerCase();
        if ((lower.contains("list") || lower.contains("only")) && (lower.contains("skill") || lower.contains("gap")))
            return CareerQueryIntent.LIST_ONLY;
        if (lower.contains("roadmap") || lower.contains("path") || lower.contains("learn"))
            return CareerQueryIntent.LEARNING_ROADMAP;
        if (lower.contains("interview") || lower.contains("question") || lower.contains("answer"))
            return CareerQueryIntent.INTERVIEW_PREP;
        if (lower.contains("gap") || lower.contains("missing") || lower.contains("analysis"))
            return CareerQueryIntent.SKILL_GAP_ANALYSIS;
        if (lower.contains("project") || lower.contains("idea") || lower.contains("portfolio"))
            return CareerQueryIntent.PROJECT_IDEAS;
        if (lower.contains("switch") || lower.contains("change") || lower.contains("career"))
            return CareerQueryIntent.CAREER_SWITCH;
        if ((lower.contains("requirement") || lower.contains("skill")) &&
                (lower.contains("job") || lower.contains("role") || lower.contains("need") || lower.contains("for")))
            return CareerQueryIntent.JOB_REQUIREMENTS;
        if (lower.contains("explain") || lower.contains("what is") || lower.contains("how does"))
            return CareerQueryIntent.SKILL_EXPLANATION;
        if (lower.matches(".*\\b(thanks|thank|tq|thx|gracias|cheers)\\b.*"))
            return CareerQueryIntent.GRATITUDE;
        return CareerQueryIntent.GENERAL_ADVICE;
    }

    private String getRandomGratitudeResponse() {
        List<String> responses = Arrays.asList(
                "You're welcome! 😊 If you need help with resume improvement, skill gaps, or job preparation, feel free to ask.",
                "You're welcome! 👍 Keep learning and building projects — you're on the right track. Let me know if you want help with your next step.",
                "You're welcome. I'm here whenever you need help with resume analysis, job matching, or career guidance.",
                "Happy to help! Let me know if you have another question.",
                "You're welcome! 🚀 Keep improving your skills — you're doing great so far.");
        return responses.get(new Random().nextInt(responses.size()));
    }

    public String generateCoverLetter(CoverLetterRequest request) {
        String prompt = promptBuilder.buildCoverLetterPrompt(request);
        String out = groqClient.callGroq(prompt, GroqClient.QueryComplexity.POWERFUL);
        return out != null ? out : "Unable to generate cover letter right now.";
    }
}
