package com.careercompass.careercompass.service;

import com.careercompass.careercompass.dto.*;
import com.fasterxml.jackson.databind.ObjectMapper;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

@Service
public class GroqAi {

    // ======================================================================
    // CONFIG
    // ======================================================================

    @Value("${groq.api.key}")
    private String groqApiKey;

    private static final String GROQ_URL = "https://api.groq.com/openai/v1/chat/completions";
    private static final String MODEL = "llama-3.3-70b-versatile";

    private final RestTemplate restTemplate = new RestTemplate();
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final PineconeVectorService pineconeVectorService;

    // Small in-memory cache to avoid repeated Groq calls during same runtime
    private final Map<String, String> simpleCache = new ConcurrentHashMap<>();

    public GroqAi(PineconeVectorService pineconeVectorService) {
        this.pineconeVectorService = pineconeVectorService;
    }

    private String safe(String s) {
        return s == null ? "" : s.trim();
    }

    // ======================================================================
    // BAD WORD FILTER (IGNORE CASE) - English + Tamil-ish list
    // ======================================================================
    // ======================================================================
    // BAD WORD FILTER (IGNORE CASE) - English only
    // ======================================================================
    private String cleanBadWords(String text) {
        if (text == null)
            return "";

        List<String> badWords = Arrays.asList(
                "fuck", "fucking", "shit", "bullshit",
                "bitch", "bastard", "asshole",
                "dick", "cock", "pussy",
                "slut", "whore",
                "motherfucker", "mf",
                "jerk", "crap",
                "damn", "bloody");

        String lower = text.toLowerCase();

        for (String word : badWords) {
            if (lower.matches(".*\\b" + PatternEscape(word) + "\\b.*")) {
                return "__BAD_WORD__";
            }
        }
        return text.trim();
    }

    private String PatternEscape(String s) {
        return java.util.regex.Pattern.quote(s);
    }

    // ======================================================================
    // FUN-TOPIC BLOCKER (BLOCK NON-CAREER QUESTIONS)
    // ======================================================================
    private boolean isFunTopic(String q) {
        if (q == null || q.isBlank())
            return false;

        String lower = q.toLowerCase();

        List<String> funWords = Arrays.asList(

                // 🎬 Movies & Entertainment
                "movie", "movies", "film", "cinema", "trailer", "review",
                "actor", "actress", "hollywood", "bollywood", "web series",
                "netflix", "series", "dialogue", "comedy", "romantic",
                "horror", "action", "animated",

                // 🎮 Games & Gaming
                "game", "games", "gaming", "play", "player",
                "bgmi", "pubg", "free fire", "minecraft", "valorant",
                "pc games", "mobile games", "multiplayer", "esports",

                // 🏏 Sports & Players
                "cricket", "football", "match", "ipl", "sports",
                "athlete", "kohli", "dhoni", "messi", "ronaldo",
                "club", "team", "tournament",

                // 🎵 Music & Songs
                "song", "songs", "music", "playlist", "album",
                "singer", "lyrics", "tamil songs", "english songs",

                // 😄 Fun / Casual / Timepass
                "joke", "jokes", "funny", "meme", "memes",
                "roast", "laugh", "fun", "timepass", "bored",
                "chill", "entertainment", "quiz",

                // ❤️ Relationships / Casual Chat
                "love", "crush", "relationship", "dating",
                "breakup", "romance", "flirt", "friendship",
                "chat", "talk", "story", "gossip");

        return funWords.stream().anyMatch(lower::contains);
    }

    // ======================================================================
    // QUICK EXPLAIN + JOB + ROADMAP + INTERVIEW + CAREER SUGGESTIONS
    // ======================================================================

    // Use cacheKey prefix to avoid collision between types
    private String cacheGet(String prefix, String key) {
        return simpleCache.get(prefix + "::" + key.toLowerCase());
    }

    private void cachePut(String prefix, String key, String value) {
        if (value == null)
            return;
        simpleCache.put(prefix + "::" + key.toLowerCase(), value);
    }

    // ----------------------------------------------------------------------
    // Quick explain for a skill (4-5 lines)
    // ----------------------------------------------------------------------
    private String quickExplainSkill(String skill) {
        if (skill == null || skill.isBlank())
            return "";

        String cache = cacheGet("skillExplain", skill);
        if (cache != null)
            return cache;

        String prompt = """
                Give a short, clear explanation of the skill: %s

                Rules:
                - 4 to 5 lines only
                - No resume or job description references
                - No over-explaining
                - Speak practically as a career coach
                - No markdown formatting
                """.formatted(skill);

        String out = callGroq(prompt);
        if (out == null || out.isBlank())
            out = "Brief: " + skill + " — core concept and practical use.";
        cachePut("skillExplain", skill, out);
        return out;
    }

    // ----------------------------------------------------------------------
    // Auto career suggestions for a skill
    // ----------------------------------------------------------------------
    private String suggestCareersForSkill(String skill) {
        if (skill == null || skill.isBlank())
            return "";
        String cache = cacheGet("careerSuggest", skill);
        if (cache != null)
            return cache;

        String prompt = """
                Suggest 3–5 realistic career paths for someone who knows or is learning the skill: %s

                Rules:
                - Keep it short (one line per career).
                - Mention the most common entry-level and mid-level roles.
                - No resume or JD analysis.
                - No long paragraphs.
                """.formatted(skill);

        String out = callGroq(prompt);
        if (out == null || out.isBlank())
            out = "• " + skill + " - multiple career paths available.";
        cachePut("careerSuggest", skill, out);
        return out;
    }

    // ----------------------------------------------------------------------
    // Interview Q&A generator for a skill
    // ----------------------------------------------------------------------
    private String interviewQAForSkill(String skill) {
        if (skill == null || skill.isBlank())
            return "";
        String cache = cacheGet("interviewQA", skill);
        if (cache != null)
            return cache;

        String prompt = """
                Generate 3 interview questions and very short answers for the skill: %s

                Rules:
                - Each Q/A should be one line for the question and one short line for the answer.
                - Keep answers concise and actionable for freshers.
                - No long paragraphs.
                """.formatted(skill);

        String out = callGroq(prompt);
        if (out == null || out.isBlank())
            out = "Q1: Basic question? A1: Short answer.";
        cachePut("interviewQA", skill, out);
        return out;
    }

    // ----------------------------------------------------------------------
    // Quick explain for a job role (short guide)
    // ----------------------------------------------------------------------
    private String quickExplainJobRole(String role) {
        if (role == null || role.isBlank())
            return "";
        String cache = cacheGet("jobExplain", role);
        if (cache != null)
            return cache;

        String prompt = """
                Give a short professional career guide for the role: %s

                Include:
                - What the role is (2 lines)
                - Required skills (one line)
                - Learning roadmap (short)
                - Interview questions (3 brief)
                - Salary range for freshers (India)

                Rules:
                - 8–10 lines only
                - No markdown
                - Keep it practical
                """.formatted(role);

        String out = callGroq(prompt);
        if (out == null || out.isBlank())
            out = "Short career guide for: " + role;
        cachePut("jobExplain", role, out);
        return out;
    }

    // ----------------------------------------------------------------------
    // Quick roadmap generator for a role
    // ----------------------------------------------------------------------
    private String quickRoadmap(String role) {
        if (role == null || role.isBlank())
            return "";
        String cache = cacheGet("roadmap", role);
        if (cache != null)
            return cache;

        String prompt = """
                Create a clear, beginner-friendly learning roadmap for: %s

                Include:
                1. Fundamentals a beginner must learn
                2. Tools / Technologies required
                3. Projects to build (very practical)
                4. Recommended certifications (optional)
                5. Estimated timeline (India freshers)

                Rules:
                - 8 to 12 lines maximum
                - No markdown formatting
                - Keep it simple, practical, actionable
                - Avoid long paragraphs
                """.formatted(role);

        String out = callGroq(prompt);
        if (out == null || out.isBlank())
            out = "Roadmap: Learn basics → Tools → Projects → Deploy.";
        cachePut("roadmap", role, out);
        return out;
    }

    // ----------------------------------------------------------------------
    // Suggest career roles for a user with multiple skills (basic)
    // ----------------------------------------------------------------------
    private String autoSuggestCareersFromSkills(Collection<String> skills) {
        if (skills == null || skills.isEmpty())
            return "";
        String joined = String.join(", ", skills);
        String cache = cacheGet("autoCareerFromSkills", joined);
        if (cache != null)
            return cache;

        String prompt = """
                The candidate knows these skills: %s
                Suggest 4 suitable job roles (entry-level to mid-level) and one short reason each.
                Rules:
                - 4 lines only, one role per line.
                - No long paragraphs.
                """.formatted(joined);

        String out = callGroq(prompt);
        if (out == null || out.isBlank())
            out = "Suggested roles based on: " + joined;
        cachePut("autoCareerFromSkills", joined, out);
        return out;
    }

    // ======================================================================
    // GROQ API CALL (core)
    // ======================================================================
    @SuppressWarnings("unchecked")
    private String callGroq(String prompt) {
        if (prompt == null || prompt.isBlank())
            return null;

        // small cache to avoid repeated calls for identical prompts across same runtime
        String promptKey = Integer.toHexString(prompt.hashCode());
        String cached = simpleCache.get("rawPrompt::" + promptKey);
        if (cached != null)
            return cached;

        try {
            Map<String, Object> body = new HashMap<>();
            body.put("model", MODEL);
            body.put("temperature", 0.3);
            body.put("messages", List.of(Map.of(
                    "role", "user",
                    "content", prompt)));

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.set("Authorization", "Bearer " + groqApiKey);

            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(body, headers);

            ResponseEntity<Map> response = restTemplate.exchange(
                    GROQ_URL,
                    HttpMethod.POST,
                    entity,
                    Map.class);

            if (!response.getStatusCode().is2xxSuccessful() || response.getBody() == null) {
                return null;
            }

            Map<String, Object> json = response.getBody();
            List<Map<String, Object>> choices = (List<Map<String, Object>>) json.get("choices");

            if (choices == null || choices.isEmpty())
                return null;

            Map<String, Object> msg = (Map<String, Object>) choices.get(0).get("message");

            if (msg == null || msg.get("content") == null)
                return null;

            String out = msg.get("content").toString().trim();
            // cache the raw prompt short-term
            simpleCache.put("rawPrompt::" + promptKey, out);
            return out;

        } catch (Exception e) {
            System.out.println("🔥 Groq API Error: " + e.getMessage());
            return null;
        }
    }

    // ======================================================================
    // COVER LETTER GENERATOR (unchanged)
    // ======================================================================
    public String generateCoverLetter(CoverLetterRequest request) {
        String prompt = buildCoverLetterPrompt(request);
        String out = callGroq(prompt);
        return out != null ? out : "Unable to generate cover letter right now.";
    }

    private String buildCoverLetterPrompt(CoverLetterRequest request) {

        String name = safe(request.getFullName());
        String email = safe(request.getEmail());
        String phone = safe(request.getPhone());
        String company = safe(request.getCompanyName());
        String job = safe(request.getJobTitle());
        String hr = safe(request.getHiringManager());
        String jd = safe(request.getJobDescription());

        if (hr.isEmpty()) {
            hr = "Hiring Manager";
        }

        String today = LocalDate.now()
                .format(DateTimeFormatter.ofPattern("MMMM dd, yyyy"));

        // ------------------------------------------------------------------
        // COVER LETTER STRUCTURE (NO %s — EVER)
        // ------------------------------------------------------------------
        String coverLetterTemplate = """
{{NAME}}
Phone: {{PHONE}}
Email: {{EMAIL}}

{{DATE}}

{{HIRING_MANAGER}}
{{COMPANY_NAME}}

Dear {{SALUTATION_NAME}},

{{OPENING_PARAGRAPH}}

{{BODY_PARAGRAPH_1}}

{{BODY_PARAGRAPH_2}}

{{CALL_TO_ACTION}}

Sincerely,
{{NAME}}
""";

        // ------------------------------------------------------------------
        // AI PROMPT (NO FORMATTING TOKENS)
        // ------------------------------------------------------------------
        return """
You are an expert Java Developer cover letter writer.

GOAL:
Generate a polished, professional cover letter using standard business
letter formatting.

STRICT RULES:
- Replace ALL {{PLACEHOLDERS}} in the template.
- Do NOT leave {{ or }} in the final output.
- Do NOT change the structure.
- Focus on Java, Spring Boot, OOP, REST APIs, and backend development.
- Quantify achievements where possible.
- Keep it professional and ATS-friendly.
- Do NOT mention AI or explanations.

TEMPLATE:
""" + coverLetterTemplate + """

CANDIDATE INFORMATION:
Name: """ + name + """
Phone: """ + phone + """
Email: """ + email + """

JOB INFORMATION:
Job Title: """ + job + """
Company Name: """ + company + """
Hiring Manager: """ + hr + """
Date: """ + today + """

JOB DESCRIPTION:
""" + jd;
    }


    // ======================================================================
    // AI SKILL EXTRACTION (unchanged)
    // ======================================================================
    private static final String SKILL_PARSE_TEMPLATE = """
            Extract skills from JD + Resume. Return ONLY JSON.

            Job Description:
            %s

            Resume:
            %s

            JSON Format Example:
            {
              "jdRequiredSkills": [],
              "strongSkills": [],
              "weakSkills": [],
              "roleFocus": "",
              "generallyRelated": true
            }
            """;

    public AiSkillProfile analyzeSkillsWithAi(String jd, String resume) {

        String prompt = SKILL_PARSE_TEMPLATE.formatted(safe(jd), safe(resume));
        String raw = callGroq(prompt);

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
            p.setJdRequiredSkills(toList(map.get("jdRequiredSkills")));
            p.setStrongSkills(toList(map.get("strongSkills")));
            p.setWeakSkills(toList(map.get("weakSkills")));

            if (map.get("roleFocus") != null)
                p.setRoleFocus(map.get("roleFocus").toString());

            if (map.get("generallyRelated") instanceof Boolean b2)
                p.setGenerallyRelated(b2);

            return p;

        } catch (Exception e) {
            System.out.println("❌ Skill Parse Error: " + e.getMessage());
            return null;
        }
    }

    private List<String> toList(Object obj) {
        if (obj instanceof List<?> list) {
            return list.stream().filter(Objects::nonNull)
                    .map(o -> o.toString().trim().toLowerCase())
                    .collect(Collectors.toList());
        }
        return List.of();
    }

    // ======================================================================
    // SINGLE TIP GENERATOR (MatchService uses this)
    // ======================================================================
    public String generateImprovedTip(AnalysisRequest req, AnalysisResponse res) {

        String prompt = """
                Give ONE improvement tip (2–3 sentences).

                JD Skills: %s
                Resume Skills: %s
                Matched: %s
                Missing: %s

                Rules:
                - No bullets.
                - Speak directly to the user.
                """.formatted(
                res.getJdSkills(), res.getResumeSkills(),
                res.getMatchedSkills(), res.getMissingSkills());

        String out = callGroq(prompt);
        return out != null ? out : res.getTip();
    }

    // ======================================================================
    // CAREER CHAT (Main method with all detection + RAG fallback)
    // ======================================================================
    public String answerCareerQuestion(String question, String resume, String jd) {

        // 0) sanitize & normalize
        question = cleanBadWords(question);

        // 🚫 HARD STOP — DO NOT CONTINUE
        if ("__BAD_WORD__".equals(question)) {
            return """
                    ⚠️ Please do not use offensive or inappropriate language.

                    I'm here to help you with:
                    • Resume improvement
                    • Skills to learn
                    • Interview preparation
                    • Career guidance

                    Please ask your question respectfully.
                    """;
        }

        String q = safe(question).toLowerCase();

        // 1) Roadmap mode (look for roadmap/path/how to become)
        if (q.contains("roadmap") || q.contains("career path") || q.contains("how to become")
                || q.contains("how to become a") || q.contains("how do i become")) {
            for (String role : JobRoleLibrary.JOB_ROLES) {
                if (q.contains(role.toLowerCase())) {
                    String out = quickRoadmap(role);
                    if (out != null && !out.isBlank())
                        return out;
                    return "Here is a simple roadmap to become a " + role;
                }
            }
        }

        // 2) Interview questions explicit request (e.g. "interview questions for data
        // analyst")
        if (q.contains("interview") || q.contains("interview questions") || q.contains("questions for")) {
            for (String role : JobRoleLibrary.JOB_ROLES) {
                if (q.contains(role.toLowerCase())) {
                    // Provide interview questions and answers for the role
                    String cached = cacheGet("roleInterview", role);
                    if (cached != null)
                        return cached;

                    String prompt = """
                            Generate 5 interview questions and short answers tailored for the role: %s
                            Rules:
                            - Keep questions realistic for freshers
                            - Provide short answers (one or two lines)
                            - No long paragraphs
                            """.formatted(role);

                    String out = callGroq(prompt);
                    if (out == null || out.isBlank())
                        out = "Prepare fundamentals, projects, and problem-solving demonstration for " + role;
                    cachePut("roleInterview", role, out);
                    return out;
                }
            }
        }

        // 3) Job-role quick-guide (if user mentions a job role)
        for (String role : JobRoleLibrary.JOB_ROLES) {
            if (q.contains(role.toLowerCase())) {
                String cached = cacheGet("jobRoleFull", role);
                if (cached != null)
                    return cached;

                // Build a combined output: quick explain, career suggestions, interview Q&A,
                // roadmap
                StringBuilder sb = new StringBuilder();

                String explain = quickExplainJobRole(role);
                if (explain != null && !explain.isBlank())
                    sb.append(explain.trim()).append("\n\n");

                String roadmap = quickRoadmap(role);
                if (roadmap != null && !roadmap.isBlank())
                    sb.append("Roadmap:\n").append(roadmap.trim()).append("\n\n");

                String interview = cacheGet("roleInterview", role);
                if (interview == null) {
                    String prompt = """
                            Generate 3 short interview questions and concise answers for the role: %s
                            Rules: short & practical.
                            """.formatted(role);
                    interview = callGroq(prompt);
                    if (interview == null || interview.isBlank())
                        interview = "Practice explaining your projects and basics for " + role;
                    cachePut("roleInterview", role, interview);
                }
                sb.append("Interview Q&A:\n").append(interview.trim()).append("\n\n");

                String careers = suggestCareersForSkill(role); // re-using skill career suggest for role
                if (careers != null && !careers.isBlank())
                    sb.append("Related Roles:\n").append(careers.trim());

                String finalOut = sb.toString().trim();
                cachePut("jobRoleFull", role, finalOut);
                return finalOut;
            }
        }

        // 4) Skill quick-explain + career
        for (String skill : SkillLibrary.UNIVERSAL_SKILLS) {
            String s = skill.toLowerCase();
            if (q.contains(s)) {
                // Compose combined skill output
                StringBuilder sb = new StringBuilder();

                String explanation = quickExplainSkill(skill);
                if (explanation != null && !explanation.isBlank())
                    sb.append(explanation.trim()).append("\n\n");
                else
                    sb.append("Short: ").append(skill).append("\n\n");

                String careers = suggestCareersForSkill(skill);
                if (careers != null && !careers.isBlank())
                    sb.append("Career Paths:\n").append(careers.trim()).append("\n\n");
                else
                    sb.append("Career Paths: Explore roles related to ").append(skill).append("\n\n");

                String interview = interviewQAForSkill(skill);
                if (interview != null && !interview.isBlank())
                    sb.append("Interview Q&A:\n").append(interview.trim());

                // Auto career suggestion for multiple resume skills - if resume contains list
                if (resume != null && !resume.isBlank()) {
                    List<String> resumeSkills = extractSkills(resume);
                    if (!resumeSkills.isEmpty()) {
                        String autoCareers = autoSuggestCareersFromSkills(resumeSkills);
                        if (autoCareers != null && !autoCareers.isBlank()) {
                            sb.append("\n\nSuggested Roles from your resume skills:\n").append(autoCareers.trim());
                        }
                    }
                }

                return sb.toString().trim();
            }
        }

        // 5) Fun-topic block
        if (isFunTopic(q)) {
            return """
                    😄 Hey! I'm here as your Career Assistant.

                    I don't handle fun, movie, gaming, or casual chat topics.
                    But I CAN help you with things that matter for your future:

                    ✔ Resume & CV improvement
                    ✔ Job matching & ATS score
                    ✔ Skills to learn for your career
                    ✔ Interview preparation
                    ✔ Career guidance & growth paths

                    💡 Ask me a career-related question and let's level up your future!
                    """;
        }

        // 6) FAQ OVERRIDES (existing)
        if (q.contains("not able to upload") || q.contains("cannot upload") || q.contains("upload issue")) {
            return "If you're unable to upload your resume, try refreshing the page, disabling browser plugins like ad-blockers, or switch to Chrome. Ensure file is PDF or DOCX.";
        }

        if (q.contains("contact") || q.contains("support")) {
            return "You can reach support through the Contact section or via email.";
        }

        if (q.contains("resume not read") || q.contains("parsing error")) {
            return "Ensure your resume is a text-based PDF or DOCX. Avoid tables and images.";
        }

        if (q.contains("cancel subscription") || q.contains("unsubscribe")) {
            return "To cancel your subscription, go to Account → Subscription Details → Cancel. Your plan remains active until billing period ends.";
        }

        if (q.contains("no experience") || q.contains("fresher") || q.contains("don't have experience")) {
            return """
                    If you don't have experience, focus on projects, internships, certifications, and academic work.
                    Add skills, tools you learned, and volunteer work. Recruiters care about potential, not only job titles.
                    """;
        }

        // 7) RAG + AI skill context fallback (original behavior)
        AiSkillProfile profile = analyzeSkillsWithAi(jd, resume);
        String context = buildRagContext(question, resume, jd, profile);

        String prompt = """
                You are a friendly career coach.

                User Question:
                %s

                Resume:
                %s

                Job Description:
                %s

                Context:
                %s

                Rules:
                - Be simple.
                - Be practical.
                - No markdown.
                """.formatted(safe(question), safe(resume), safe(jd), context);

        String out = callGroq(prompt);
        return out != null ? out : "I'm here to help, but I couldn't generate the answer right now.";
    }

    // ======================================================================
    // RAG CONTEXT BUILDER (unchanged)
    // ======================================================================
    private String buildRagContext(String question, String resume, String jd, AiSkillProfile p) {

        if (p == null)
            return "No skill profile.";

        List<String> jdSkills = p.getJdRequiredSkills();
        List<String> strong = p.getStrongSkills();
        List<String> weak = p.getWeakSkills();

        List<String> matched = jdSkills.stream()
                .filter(s -> strong.contains(s) || weak.contains(s))
                .toList();

        List<String> missing = jdSkills.stream()
                .filter(s -> !matched.contains(s))
                .toList();

        List<Snippets> snips = retrieveRelevantSnippets(question, missing, p.getRoleFocus());

        StringBuilder sb = new StringBuilder();

        sb.append("JD Skills: ").append(jdSkills).append("\n");
        sb.append("Strong Skills: ").append(strong).append("\n");
        sb.append("Weak Skills: ").append(weak).append("\n");
        sb.append("Matched Skills: ").append(matched).append("\n");
        sb.append("Missing Skills: ").append(missing).append("\n");

        if (!snips.isEmpty()) {
            sb.append("\nRelevant Knowledge:\n");
            for (Snippets s : snips) {
                sb.append("- ").append(s.getTopic()).append(":\n");
                sb.append(s.getAdviceText()).append("\n");
            }
        }

        return sb.toString();
    }

    // ======================================================================
    // RAG SNIPPET RETRIEVER (Pinecone Semantic Search)
    // ======================================================================
    private List<Snippets> retrieveRelevantSnippets(
            String question,
            List<String> missing,
            String roleFocus) {

        try {
            // Build enhanced query with context
            StringBuilder queryBuilder = new StringBuilder(safe(question));

            // Add missing skills to query for better context
            if (missing != null && !missing.isEmpty()) {
                queryBuilder.append(" ").append(String.join(" ", missing));
            }

            // Add role focus to query
            if (roleFocus != null && !roleFocus.isBlank()) {
                queryBuilder.append(" ").append(roleFocus);
            }

            String enhancedQuery = queryBuilder.toString();

            // DEBUG: Log Pinecone usage
            System.out.println("🔍 Using Pinecone semantic search for query: " + enhancedQuery);

            // Use Pinecone semantic search (AI-powered vector similarity)
            List<PineconeVectorService.ScoredSnippet> scoredResults = pineconeVectorService
                    .semanticSearch(enhancedQuery, 3);

            // DEBUG: Log results
            System.out.println("✅ Pinecone returned " + scoredResults.size() + " results");

            // Extract snippets from scored results
            return scoredResults.stream()
                    .map(PineconeVectorService.ScoredSnippet::getSnippet)
                    .collect(Collectors.toList());

        } catch (Exception e) {
            System.err.println("⚠️ Pinecone search failed, returning empty context: " + e.getMessage());
            return Collections.emptyList();
        }
    }

    // ======================================================================
    // ATS INSIGHT GENERATOR (MatchService uses this) - unchanged signature
    // ======================================================================
    public void enrichWithInsights(AnalysisRequest req, AnalysisResponse res) {
        try {
            String prompt = """
                    You are an ATS & Resume Optimization expert.

                    Generate 3 advice sections using these tag formats:

                    INPUT:
                    JD Skills: %s
                    Resume Skills: %s
                    Matched Skills: %s
                    Missing Skills: %s

                    OUTPUT FORMAT:

                    [PRO_TIP_START]
                    • ⭐ Summary: ...
                    • 💪 Strength: ...
                    • ⚠️ Improvement Area: ...
                    • 🎯 Recommendation: ...
                    [PRO_TIP_END]

                    [RESUME_TIPS_START]
                    - Tip 1...
                    - Tip 2...
                    - Tip 3...
                    [RESUME_TIPS_END]

                    [SKILL_TIPS_START]
                    - Tip 1...
                    - Tip 2...
                    - Tip 3...
                    [SKILL_TIPS_END]

                    Do NOT add anything outside these tags.
                    """
                    .formatted(
                            res.getJdSkills(),
                            res.getResumeSkills(),
                            res.getMatchedSkills(),
                            res.getMissingSkills());

            String out = callGroq(prompt);
            if (out == null || out.isBlank())
                return;

            // Extract tagged sections
            String proTip = extractSection(out, "[PRO_TIP_START]", "[PRO_TIP_END]");
            String resumeTipsRaw = extractSection(out, "[RESUME_TIPS_START]", "[RESUME_TIPS_END]");
            String skillTipsRaw = extractSection(out, "[SKILL_TIPS_START]", "[SKILL_TIPS_END]");

            if (!proTip.isBlank())
                res.setTip(proTip.trim());
            if (!resumeTipsRaw.isBlank())
                res.setResumeImprovementTips(parseBulletedList(resumeTipsRaw));
            if (!skillTipsRaw.isBlank())
                res.setSkillImprovementTips(parseBulletedList(skillTipsRaw));

        } catch (Exception e) {
            System.out.println("❌ enrichWithInsights ERROR: " + e.getMessage());
        }
    }

    // ======================================================================
    // HELPERS - small helpers used across methods
    // ======================================================================
    private List<String> parseBulletedList(String raw) {
        return Arrays.stream(raw.split("\\n"))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .map(s -> s.replaceAll("^[-*•]\\s*", ""))
                .collect(Collectors.toList());
    }

    private String extractSection(String text, String startMarker, String endMarker) {
        try {
            int start = text.indexOf(startMarker);
            if (start == -1)
                return "";
            start += startMarker.length();

            int end = text.indexOf(endMarker, start);
            if (end == -1)
                end = text.length();

            return text.substring(start, end).trim();

        } catch (Exception e) {
            return "";
        }
    }

    // ---------------------------
    // Deterministic skill extract (used by MatchService fallback)
    // ---------------------------
    private static final List<String> DEFAULT_SKILLS = Arrays.asList(); // placeholder if needed

    // Simple deterministic extract used when trying to infer resume skills for
    // suggestions
    private List<String> extractSkills(String text) {
        if (text == null || text.isBlank())
            return new ArrayList<>();
        String lower = text.toLowerCase();
        Set<String> found = new LinkedHashSet<>();
        // check SkillLibrary contents
        for (String s : SkillLibrary.UNIVERSAL_SKILLS) {
            String lw = s.toLowerCase();
            if (lower.contains(lw))
                found.add(s);
        }
        // fallback: check defaults
        for (String s : DEFAULT_SKILLS) {
            if (lower.contains(s.toLowerCase()))
                found.add(s);
        }
        return new ArrayList<>(found);
    }

}
