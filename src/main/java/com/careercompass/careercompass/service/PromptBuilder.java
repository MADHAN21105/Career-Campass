package com.careercompass.careercompass.service;

import com.careercompass.careercompass.dto.AiSkillProfile;
import com.careercompass.careercompass.dto.AnalysisResponse;
import com.careercompass.careercompass.dto.CoverLetterRequest;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

@Component
public class PromptBuilder {

        // ======================================================================
        // TEMPLATES
        // ======================================================================

        public static final String SKILL_PARSE_TEMPLATE = """
                        You are a Technical Skill extraction and Match Analysis Engine.
                        Analyze the JD and Resume to extract technical skills and provide a detailed fit analysis.

                        KNOWLEDGE BASE (CONTEXT):
                        %s

                        STRICT RULES:
                        1. SOLE AUTHORITY: You MUST ONLY extract skills that are explicitly listed in the KNOWLEDGE BASE (CONTEXT).
                        2. DISCARD: If a skill in the JD or Resume is NOT found in the KNOWLEDGE BASE, you MUST ignore it completely.
                        3. STRICT EXCLUSION: Do NOT include broad roles (e.g. "Backend Developer") or UI noise (e.g. "Analyze Different Job").
                        4. SKILL EQUIVALENCE: Respect the "Note: Proves competency" instructions in the KNOWLEDGE BASE. If a candidate has a child skill (e.g., Spring Boot), they automatically satisfy the parent requirement (e.g., Spring). Use this to mark skills as 'matchedSkills' even if the exact JD keyword is missing, as long as an equivalent 'proving' skill is present in the Resume.
                        5. Focus ONLY on specific technical tools, languages, and methodologies from the Knowledge Base.

                        JD:
                        %s

                        Resume:
                        %s

                        JSON Format (Return ONLY valid JSON):
                        {
                          "jdRequiredSkills": ["skill1", "skill2"],
                          "strongSkills": ["skillA", "skillB"],
                          "matchedSkills": ["skill1"],
                          "missingSkills": ["skill2"],
                          "mandatorySkills": ["skill1"],
                          "preferredSkills": ["skill2"],
                          "jdRole": "Extracted Job Title (from JD)",
                          "resumeTitle": "Extracted Candidate Title (from Resume)",
                          "education": "Specify EXACT degree/field required by JD (e.g. B.Tech / B.E. in CS, IT or related field)",
                          "summary": "Comprehensive, multi-paragraph technical analysis of the candidate's fit, alignment with the JD, and overall potential for this specific role.",
                          "strength": "Extremely detailed breakdown of the candidate's top technical strength and how it directly applies to the JD's core responsibilities.",
                          "improvementArea": "In-depth analysis of the most critical skill or experience gap, explaining precisely why it hinders their match for this role.",
                          "recommendation": "Strategic, long-term career growth plan specifically tailored to bridging the identified gaps and excelling in this position.",
                          "resumeTips": ["💡 [Emoji-Start Tip for Resume, 4-5 lines]", "🚀 [Emoji-Start Strategic Tip, 4-5 lines]"],
                          "skillTips": ["🛠️ [Emoji-Start Skill Master Tip, 4-5 lines]", "📚 [Emoji-Start Practical Learning Path, 4-5 lines]"],
                          "proTips": ["🎯 High-impact career strategy.", "📈 Industry insight."],
                          "careerGrowthSkills": ["Skills suggested by knowledge base but not required in JD"]
                        }

                        CRITICAL CONTENT RULES:
                        1. ROLE ALIGNMENT: Analyze fit STRICTLY for the Job Title in the JD.
                        2. SCAN EVERYTHING: You MUST check 'SKILLS', 'Tools', 'Projects', and 'Education'. Every mentioned skill counts.
                        3. JD SKILL COMPLETENESS: 'jdRequiredSkills' MUST contain the union of ALL skills found in the JD (both Mandatory and Preferred).
                        4. SKILL EQUIVALENCE: Use the KNOWLEDGE BASE notes.
                           - CRITICAL: GitHub or GitLab usage confirms Git proficiency. If the candidate has GitHub, Mark Git as MATCHED.
                           - SQL proficiency confirms MySQL/PostgreSQL.
                           - BUT: Do NOT assume 'JavaScript' if the candidate only has 'Java' or 'HTML/CSS'. They are DISTINCT.
                        4. VERBOSITY: All Summary, Strength, Gap, and Recommendation sections MUST be exactly ONE dense, high-impact paragraph. Do NOT use multiple paragraphs.
                        5. TIPS LIMIT: Provide exactly TWO high-impact points for 'resumeTips' and 'skillTips'. Each point MUST be exactly 4-5 lines long (approx 70-100 words) and MUST include one relevant emoji at the start.
                        6. NATURAL LANGUAGE: Every tip and summary MUST be a natural, professional sentence. Do NOT use prefixes like "Tip 1:", "Expert Tip:", or specific numbering.
                        7. EMOJI RULE: Do NOT include emojis in Summary, Strength, or Gap sections. HOWEVER, 'resumeTips' and 'skillTips' MUST include exactly one relevant emoji at the start of each point.
                        8. NO HALLUCINATIONS: Do NOT assume role types or mention skills not in the text or Knowledge Base.
                        9. JAVA vs JAVASCRIPT: They are COMPLETELY DIFFERENT. If a JD requires 'JavaScript' and the resume only says 'Java', it is a MISSING skill. Never match them.
                        10. EXACT MATCH: Only match skills if they are explicitly mentioned or satisfy the specific EQUIVALENCE rules above. Remove 'JavaScript' from matchedSkills if it's not explicitly in the resume.
                        11. JSON SAFETY: You MUST escape all newlines as '\\n' inside JSON strings. Ensure the final output is a SINGLE VALID JSON block. Do NOT include any text before or after the JSON.
                        """;

        private static final String COVER_LETTER_TEMPLATE = """
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

        // ======================================================================
        // BUILDERS
        // ======================================================================

        public String buildSkillAnalysisPrompt(String context, String jd, String resume) {
                return SKILL_PARSE_TEMPLATE.formatted(context, safe(jd), safe(resume));
        }

        public String buildCoverLetterPrompt(CoverLetterRequest req) {
                String today = LocalDate.now().format(DateTimeFormatter.ofPattern("MMMM dd, yyyy"));
                String hr = (req.getHiringManager() == null || req.getHiringManager().isBlank()) ? "Hiring Manager"
                                : req.getHiringManager();

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
                                """ + COVER_LETTER_TEMPLATE + """

                                CANDIDATE INFORMATION:
                                Name: """ + req.getFullName() + """
                                Phone: """ + req.getPhone() + """
                                Email: """ + req.getEmail() + """

                                JOB INFORMATION:
                                Job Title: """ + req.getJobTitle() + """
                                Company Name: """ + req.getCompanyName() + """
                                Hiring Manager: """ + hr + """
                                Date: """ + today + """

                                JOB DESCRIPTION:
                                """ + req.getJobDescription();
        }

        public String buildCareerPrompt(String question, String resume, String jd, AiSkillProfile profile,
                        String intent,
                        String ragContext) {
                String base = """
                                You are an expert career coach providing ultra-personalized, ChatGPT-Pro-level advice.

                                CANDIDATE PROFILE:
                                Current Bio/Skills: %s
                                Matched Skills: %s (These are STRENGTHS)
                                Target Role: %s
                                ATS Match Score: %s%%

                                JOB REQUIREMENTS:
                                Missing Key Skills: %s (These are MANDATORY GAPS)
                                Career Growth Suggestions: %s (These are OPTIONAL/BONUS)

                                KNOWLEDGE BASE CONTEXT:
                                %s

                                USER'S QUESTION:
                                %s
                                """;

                String strengths = formatSkillList(profile.getMatchedSkills(), "STRENGTH");
                String gaps = formatSkillList(profile.getMissingSkills(), "GAP");
                String growth = formatSkillList(profile.getCareerGrowthSkills(), "GROWTH");

                String mainPrompt = base.formatted(
                                (profile.getResumeBio() == null || profile.getResumeBio().isEmpty())
                                                ? "Candidate Analysis"
                                                : profile.getResumeBio(),
                                strengths,
                                (profile.getJdRole() == null || profile.getJdRole().isEmpty()) ? "Target Role"
                                                : profile.getJdRole(),
                                String.format("%.0f", profile.getAtsScore()),
                                gaps,
                                growth,
                                ragContext,
                                safe(question));

                if ("LIST_ONLY".equals(intent)) {
                        return mainPrompt
                                        + """
                                                        CRITICAL INSTRUCTION:
                                                        The user wants ONLY a list. Do NOT provide introductions, explanations, or empathetic sections.
                                                        Respond with a clean, bulleted list of the requested data (e.g. gaps or skills) and NOTHING ELSE.
                                                        """;
                }

                String intentGuideline = switch (intent) {
                        case "SKILL_GAP_ANALYSIS" ->
                                "Focus heavily on the 'Missing Key Skills'. For every gap, look into the KNOWLEDGE BASE CONTEXT and explain EXACTLY how that skill is defined and how to start learning it.";
                        case "LEARNING_ROADMAP" ->
                                "Create a structured technical roadmap. You MUST use the 'advice' from the KNOWLEDGE BASE CONTEXT as the foundation for every learning stop. Do not suggest resources or paths that contradict the provided expert data.";
                        case "INTERVIEW_PREP" ->
                                "Focus on 'Matched Skills'. Use the KNOWLEDGE BASE CONTEXT to find interview tips or standard requirements for those skills to generate realistic practice questions.";
                        case "CAREER_SWITCH" ->
                                "Bridge the gap between current 'Matched Skills' and the 'Target Role'. Use the KNOWLEDGE BASE context to explain how transferable those skills are and what specific technical hurdles (from the RAG data) must be cleared first.";
                        case "JOB_REQUIREMENTS" ->
                                "The user is asking about what it takes to be a [Role]. Use the KNOWLEDGE BASE CONTEXT to list the core technical skills, why they matter, and the standard expert advice provided for those topics.";
                        default ->
                                "Provide well-rounded career advice. Always prioritize information found in the KNOWLEDGE BASE CONTEXT (RAG Data) over general AI training.";
                };

                return mainPrompt
                                + """
                                                CRITICAL INSTRUCTIONS:
                                                1. **Intent-Specific Focus**: %s
                                                2. **Direct Answer First**: Always answer the USER'S QUESTION directly in the first paragraph.
                                                3. **Dynamic Empathy**: Be encouraging but vary your language. Avoid scripted phrases like "You are a strong match". Instead, describe their unique fit or potential.
                                                4. **Knowledge-Base Authority**: Use the provided context to explain skills, but do not repeat the context headers verbatim.
                                                5. **Actionable Roadmap**: Include a timeline-based action plan (e.g., "Step 1: Next 10 days...") but keep it concise and relevant to the question.
                                                6. **Markdown Formatting**: Use ### headers, bolding, and bullet points for high readability.
                                                7. **No Verbatim Repetition**: Do not copy the "CANDIDATE PROFILE" data back to the user as a report. Use it to inform your unique advice.
                                                """
                                                .formatted(intentGuideline);
        }

        private String formatSkillList(List<String> skills, String type) {
                if (skills == null || skills.isEmpty())
                        return "None";
                return skills.stream()
                                .map(s -> {
                                        String tag = "[CORE]";
                                        if ("GROWTH".equals(type))
                                                tag = "[OPTIONAL]";
                                        // Note: Category-based tagging (Cloud/DevOps -> [BONUS]) can be added here if
                                        // needed,
                                        // but it might require CsvDataService injection which we want to avoid if
                                        // possible
                                        // or pass it in as a pre-mapped list. For now, let's keep it simple.
                                        return s + " " + tag;
                                })
                                .collect(Collectors.joining(", "));
        }

        public String buildQuickExplainSkillPrompt(String skill) {
                return """
                                Give a short, clear explanation of the skill: %s

                                Rules:
                                - 4 to 5 lines only
                                - No resume or job description references
                                - No over-explaining
                                - Speak practically as a career coach
                                - No markdown formatting
                                """.formatted(skill);
        }

        public String buildSuggestCareersPrompt(String skill) {
                return """
                                Suggest 3–5 realistic career paths for someone who knows or is learning the skill: %s

                                Rules:
                                - Keep it short (one line per career).
                                - Mention the most common entry-level and mid-level roles.
                                - No resume or JD analysis.
                                - No long paragraphs.
                                """.formatted(skill);
        }

        public String buildInterviewQAPrompt(String skill) {
                return """
                                Generate 3 interview questions and very short answers for the skill: %s

                                Rules:
                                - Each Q/A should be one line for the question and one short line for the answer.
                                - Keep answers concise and actionable for freshers.
                                - No long paragraphs.
                                """.formatted(skill);
        }

        public String buildJobGuidePrompt(String role) {
                return """
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
        }

        public String buildRoadmapPrompt(String role) {
                return """
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
        }

        public String buildAutoSuggestCareersPrompt(Collection<String> skills) {
                String joined = String.join(", ", skills);
                return """
                                The candidate knows these skills: %s
                                Suggest 4 suitable job roles (entry-level to mid-level) and one short reason each.
                                Rules:
                                - 4 lines only, one role per line.
                                - No long paragraphs.
                                """.formatted(joined);
        }

        public String buildImprovementTipPrompt(AnalysisResponse res) {
                return """
                                Give ONE improvement tip (2–3 sentences).

                                JD Skills: %s
                                Resume Skills: %s
                                Matched: %s
                                Missing: %s

                                Rules:
                                - No bullets.
                                - Speak directly to the user.
                                """.formatted(res.getJdSkills(), res.getResumeSkills(), res.getMatchedSkills(),
                                res.getMissingSkills());
        }

        public String buildJobRequirementsPrompt(String jd) {
                return """
                                Extract structured job requirements from this Job Description.
                                Return ONLY JSON.

                                JD:
                                %s

                                JSON Format Example:
                                {
                                  "jobTitle": "Senior Java Developer",
                                  "mandatorySkills": ["Java", "Spring Boot", "SQL"],
                                  "preferredSkills": ["AWS", "Docker", "Kubernetes"],
                                  "responsibilities": ["Develop scalable APIs", "Code reviews", "Mentoring junior devs"],
                                  "educationLevel": "Bachelor's in Computer Science",
                                  "experienceRange": "3-5 years"
                                }
                                """
                                .formatted(jd);
        }

        public String buildSkillExtractionPrompt(String text) {
                return """
                                Extract a flat list of technical skills, tools, and methodologies from this text.
                                Include programming languages, frameworks, design tools (Figma, Adobe XD), and domain-specific concepts (Wireframing, Prototyping, UX Research).
                                Return ONLY a comma-separated list of skill names.

                                Text:
                                %s
                                """
                                .formatted(text);
        }

        private String safe(String s) {
                return s == null ? "" : s.replace("\"", "\\\"");
        }
}
