package com.careercompass.careercompass.service;

import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Rag_Data
 * 
 * Provides RAG (Retrieval-Augmented Generation) data for career advice.
 * Contains knowledge snippets about skills, job roles, and career guidance.
 */
@Service
public class Rag_Data {

    private final List<Snippets> allSnippets = new ArrayList<>();

    public Rag_Data() {
        loadSnippets();
    }

    public List<Snippets> getAllSnippets() {
        return allSnippets;
    }

    private void loadSnippets() {
        // Resume Writing Tips
        allSnippets.add(new Snippets(
                "resume-001",
                "Resume Writing Best Practices",
                "Resume",
                Arrays.asList("resume", "cv", "writing", "tips"),
                "Use strong action verbs, quantify achievements with numbers, tailor your resume to the job description, keep formatting ATS-friendly, and remove unnecessary details."));

        // Java Development
        allSnippets.add(new Snippets(
                "skill-java-001",
                "Java Developer Skills",
                "Skills",
                Arrays.asList("java", "programming", "development"),
                "To become a Java developer: Learn Core Java (OOP, Collections, Exception Handling), Spring Boot framework, SQL databases, REST APIs, Git version control, and Docker containerization."));

        // Frontend Development
        allSnippets.add(new Snippets(
                "skill-frontend-001",
                "Frontend Developer Skills",
                "Skills",
                Arrays.asList("frontend", "web", "html", "css", "javascript", "react"),
                "To become a Frontend developer: Learn HTML5, CSS3, JavaScript ES6+, React or Angular framework, responsive design, Git, and REST API integration."));

        // Data Analyst
        allSnippets.add(new Snippets(
                "role-dataanalyst-001",
                "Data Analyst Career Path",
                "Career",
                Arrays.asList("data analyst", "analytics", "excel", "sql"),
                "Data Analysts need: Excel (advanced formulas, pivot tables), SQL for data querying, Power BI or Tableau for visualization, basic statistics, and business domain knowledge."));

        // Interview Preparation
        allSnippets.add(new Snippets(
                "interview-001",
                "Interview Preparation Tips",
                "Interview",
                Arrays.asList("interview", "preparation", "questions"),
                "Prepare for interviews by: researching the company, practicing common technical questions, preparing STAR method examples for behavioral questions, and having questions ready for the interviewer."));

        // Python Skills
        allSnippets.add(new Snippets(
                "skill-python-001",
                "Python Developer Skills",
                "Skills",
                Arrays.asList("python", "programming", "development"),
                "Python developers should know: Core Python syntax, Django or Flask frameworks, data structures and algorithms, SQL databases, REST APIs, and version control with Git."));

        // Mobile Development
        allSnippets.add(new Snippets(
                "skill-mobile-001",
                "Mobile App Developer Skills",
                "Skills",
                Arrays.asList("mobile", "android", "ios", "flutter"),
                "Mobile developers need: Java/Kotlin for Android or Swift for iOS, understanding of mobile UI/UX patterns, API integration, Git, and knowledge of app deployment processes."));

        // Career Transition
        allSnippets.add(new Snippets(
                "career-transition-001",
                "Career Transition Advice",
                "Career",
                Arrays.asList("career change", "transition", "switch"),
                "When transitioning careers: identify transferable skills, take relevant online courses or certifications, build projects to demonstrate new skills, network in the target industry, and update your resume to highlight relevant experience."));

        // Soft Skills
        allSnippets.add(new Snippets(
                "soft-skills-001",
                "Important Soft Skills",
                "Skills",
                Arrays.asList("soft skills", "communication", "teamwork", "leadership"),
                "Essential soft skills include: effective communication, teamwork and collaboration, problem-solving, adaptability, time management, and leadership potential."));

        // Fresher Advice
        allSnippets.add(new Snippets(
                "fresher-001",
                "Advice for Freshers",
                "Career",
                Arrays.asList("fresher", "entry level", "beginner", "no experience"),
                "For freshers without experience: focus on academic projects, internships, certifications, hackathons, open-source contributions, and personal projects. Highlight skills and potential rather than just job titles."));
    }
}
