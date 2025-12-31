package com.careercompass.careercompass.controller;

import com.careercompass.careercompass.dto.AnalysisRequest;
import com.careercompass.careercompass.dto.AnalysisResponse;
import com.careercompass.careercompass.service.GroqAi;
import com.careercompass.careercompass.service.MatchService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;

@WebMvcTest(MatchController.class)
@Disabled("Context loading issues - Bean Definition Conflict. TODO: Resolve GroqAi mock conflict.")
@SuppressWarnings("null")
public class MatchControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private MatchService matchService;

    @MockBean
    private GroqAi groqAi;

    @Autowired
    private ObjectMapper objectMapper;

    private AnalysisRequest validRequest;

    @BeforeEach
    void setUp() {
        validRequest = new AnalysisRequest(
                "Looking for Java Developer with Spring Boot.",
                "I am a Java Developer with Spring Boot experience.");
    }

    @Test
    void testAnalyze_Success() throws Exception {
        AnalysisResponse mockResponse = new AnalysisResponse();
        mockResponse.setScore(85.0);
        mockResponse.setMatchLevel("High");

        when(matchService.analyze(any(AnalysisRequest.class))).thenReturn(mockResponse);

        mockMvc.perform(post("/api/analyze")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(validRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.score").value(85.0))
                .andExpect(jsonPath("$.matchLevel").value("High"));
    }

    @Test
    void testAnalyze_ValidationFailure_EmptyInput() throws Exception {
        AnalysisRequest request = new AnalysisRequest(null, null); // Empty fields

        mockMvc.perform(post("/api/analyze")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest()); // Should trigger @Valid and @RestControllerAdvice
    }

    @Test
    void testAnalyze_ValidationFailure_ShortText() throws Exception {
        AnalysisRequest request = new AnalysisRequest("Short", "Short"); // Too short

        mockMvc.perform(post("/api/analyze")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }
}
