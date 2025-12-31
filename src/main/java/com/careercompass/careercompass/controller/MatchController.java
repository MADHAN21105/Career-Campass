package com.careercompass.careercompass.controller;

import com.careercompass.careercompass.dto.AnalysisRequest;
import com.careercompass.careercompass.dto.AnalysisResponse;
import com.careercompass.careercompass.dto.QuestionRequest;
import com.careercompass.careercompass.dto.QuestionResponse;
import com.careercompass.careercompass.dto.ResumeExtractResponse;
import com.careercompass.careercompass.exception.InvalidInputException;
import com.careercompass.careercompass.service.CareerChatService;
import com.careercompass.careercompass.service.MatchService;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.validation.annotation.Validated;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api")
@CrossOrigin
@Validated
public class MatchController {

    @Autowired
    private MatchService matchService;

    @Autowired
    private CareerChatService chatService;

    @PostMapping("/analyze")
    public AnalysisResponse analyze(@Valid @RequestBody AnalysisRequest request) {
        return matchService.analyze(request);
    }

    @PostMapping("/ask")
    public QuestionResponse ask(@Valid @RequestBody QuestionRequest request) {

        String answer = chatService.answerCareerQuestion(request);

        QuestionResponse response = new QuestionResponse();
        response.setAnswer(answer);

        return response;
    }

    @PostMapping("/upload-resume")
    public ResponseEntity<ResumeExtractResponse> uploadResumePdf(
            @RequestParam("file") MultipartFile file) {

        if (file == null || file.isEmpty()) {
            throw new InvalidInputException("Resume file cannot be empty");
        }

        String extractedText;
        try {
            extractedText = extractTextFromPdf(file);
        } catch (Exception e) {
            throw new InvalidInputException("Unable to read PDF file: " + e.getMessage());
        }

        return ResponseEntity.ok(new ResumeExtractResponse(extractedText));
    }

    // Helper method to extract text from PDF using PDFBox
    private String extractTextFromPdf(MultipartFile file) throws Exception {
        try (PDDocument document = PDDocument.load(file.getInputStream())) {
            PDFTextStripper stripper = new PDFTextStripper();
            return stripper.getText(document);
        }
    }
}
