package com.careercompass.careercompass.service;

import com.careercompass.careercompass.dto.AnalysisRequest;
import com.careercompass.careercompass.dto.AnalysisResponse;
import com.careercompass.careercompass.exception.InvalidInputException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MatchServiceTest {

    @Mock
    private GroqAi groqAi;

    @Mock
    private CsvDataService dataService;

    @Mock
    private EmbeddingService embeddingService;

    @InjectMocks
    private MatchService matchService;

    private AnalysisRequest validRequest;

    @BeforeEach
    void setUp() {
        validRequest = new AnalysisRequest(
                "We are looking for a Java Developer with Spring Boot and AWS experience.",
                "I am a Java Developer with experience in Spring Boot and cloud platforms.");

        // Default mock behaviors for scoring components
        org.mockito.Mockito.lenient().when(embeddingService.calculateCosineSimilarity(any(), any())).thenReturn(0.8);
        org.mockito.Mockito.lenient().when(embeddingService.batchGenerateEmbeddings(any()))
                .thenReturn(new java.util.HashMap<>());

        // Mock dataService for standardization
        org.mockito.Mockito.lenient().when(dataService.getSkillCategory(anyString())).thenReturn("Technical");
        org.mockito.Mockito.lenient().when(dataService.getSkillDisplayName(anyString()))
                .thenAnswer(invocation -> invocation.getArgument(0));

        // Mock fullAnalysis as the primary entry point
        org.mockito.Mockito.lenient().doAnswer(invocation -> {
            AnalysisResponse res = invocation.getArgument(1);
            res.setJdSkills(List.of("java", "spring boot", "aws"));
            res.setResumeSkills(List.of("java", "spring boot"));
            res.setMandatorySkills(List.of("java"));
            res.setJobTitle("Java Developer");
            res.setResumeTitle("Java Developer");
            return null;
        }).when(groqAi).fullAnalysis(any(), any());
    }

    @Test
    void testAnalyze_Success() {
        AnalysisResponse response = matchService.analyze(validRequest);

        assertNotNull(response);
        assertTrue(response.getScore() > 0, "Score should be positive");
    }

    @Test
    void testAnalyze_PerfectMatch() {
        validRequest = new AnalysisRequest(
                "We are looking for a Java Developer with Spring Boot and AWS experience.",
                "Java Developer Spring Boot AWS");
        // Mock GroqAi response

        // when(groqAi.generateImprovedTip(any(), any())).thenReturn("Great job!"); //
        // Optional

        AnalysisResponse response = matchService.analyze(validRequest);

        assertNotNull(response);
        // Score should be high for exact match
        assertTrue(response.getScore() > 50, "Score should be > 50 for perfect match");
    }

    @Test
    void testAnalyze_NoSkillsMatch() {
        validRequest = new AnalysisRequest(
                "We are looking for a Java Developer.",
                "I range cows and drive tractors." // No match
        );

        // Mocking return with NO matching skills
        org.mockito.Mockito.doAnswer(invocation -> {
            AnalysisResponse res = invocation.getArgument(1);
            res.setMandatorySkills(List.of("java"));
            res.setResumeSkills(List.of("farming"));
            return null;
        }).when(groqAi).fullAnalysis(any(), any());

        AnalysisResponse response = matchService.analyze(validRequest);

        assertNotNull(response);
        // With penalty for missing mandatory 'java', score should be significantly
        // lowered
        assertTrue(response.getScore() < 50,
                "Score should be < 50 for no mandatory match. Got: " + response.getScore());
    }

    @Test
    void testAnalyze_EmptyJobDescription_ShouldThrowException() {
        AnalysisRequest invalidReq = new AnalysisRequest("", "Resume");
        assertThrows(InvalidInputException.class, () -> matchService.analyze(invalidReq));
    }

    @Test
    void testAnalyze_EmptyResume_ShouldThrowException() {
        AnalysisRequest invalidReq = new AnalysisRequest("JD", "");
        assertThrows(InvalidInputException.class, () -> matchService.analyze(invalidReq));
    }

    @Test
    void testAnalyze_CorrelatedSkills() {
        // JD asks for "Unit Testing", Resume has "JUnit"
        validRequest = new AnalysisRequest(
                "Must have experience with Unit Testing.",
                "Proficient in JUnit.");

        // Mock semantic overlap (JUnit is semantically similar to Unit Testing)
        org.mockito.Mockito.lenient().when(embeddingService.calculateCosineSimilarity(any(), any()))
                .thenReturn(0.9);

        AnalysisResponse response = matchService.analyze(validRequest);

        // JUnit should count for Unit Testing via vector similarity
        assertTrue(response.getScore() > 0);
        assertFalse(response.getMissingSkills().contains("unit testing"),
                "Should have matched Unit Testing via JUnit similarity");
    }

    @Test
    void testAnalyze_WeightedScoring() {
        // Test case 1: Soft Skills match only
        validRequest = new AnalysisRequest("Need Tech + Soft", "Have Soft");
        org.mockito.Mockito.doAnswer(invocation -> {
            AnalysisResponse res = invocation.getArgument(1);
            res.setMandatorySkills(List.of("java")); // Mandatory Tech
            res.setMatchedSkills(List.of("communication")); // Matched Soft
            return null;
        }).when(groqAi).fullAnalysis(any(), any());

        AnalysisResponse responseSoftOnly = matchService.analyze(validRequest);

        // Test case 2: Tech Skills match only
        validRequest = new AnalysisRequest("Need Tech + Soft", "Have Tech");
        org.mockito.Mockito.doAnswer(invocation -> {
            AnalysisResponse res = invocation.getArgument(1);
            res.setMandatorySkills(List.of("java"));
            res.setMatchedSkills(List.of("java")); // Matched Mandatory Tech
            res.setResumeSkills(List.of("java"));
            return null;
        }).when(groqAi).fullAnalysis(any(), any());

        AnalysisResponse responseTechOnly = matchService.analyze(validRequest);

        // Tech skills should carry more weight than soft skills
        assertTrue(responseTechOnly.getScore() > responseSoftOnly.getScore(),
                "Technical skills should carry more weight. TechScore: " +
                        responseTechOnly.getScore() + ", SoftScore: " + responseSoftOnly.getScore());
    }

    @Test
    void testAnalyze_PenaltyLogic_MissingPrimaryLanguage() {
        validRequest = new AnalysisRequest("Need Java", "Python only");

        org.mockito.Mockito.doAnswer(invocation -> {
            AnalysisResponse res = invocation.getArgument(1);
            res.setMandatorySkills(List.of("java"));
            res.setResumeSkills(List.of("python"));
            return null;
        }).when(groqAi).fullAnalysis(any(), any());

        AnalysisResponse response = matchService.analyze(validRequest);

        assertTrue(response.getScore() < 60.0,
                "Score should be heavily penalized when mandatory skill 'java' is missing");
    }
}
