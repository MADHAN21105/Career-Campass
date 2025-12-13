package com.careercompass.careercompass.controller;

import com.careercompass.careercompass.dto.AnalysisRequest;
import com.careercompass.careercompass.dto.AnalysisResponse;
import com.careercompass.careercompass.dto.QuestionRequest;
import com.careercompass.careercompass.dto.QuestionResponse;
import com.careercompass.careercompass.dto.ResumeExtractResponse;
import com.careercompass.careercompass.service.GroqAi;
import com.careercompass.careercompass.service.MatchService;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api")
@CrossOrigin
public class MatchController {

    @Autowired
    private MatchService matchService;

    @Autowired
    private GroqAi groqAi;

    @PostMapping("/analyze")
    public AnalysisResponse analyze(@RequestBody AnalysisRequest request) {
        return matchService.analyze(request);
    }

    @PostMapping("/ask")
    public QuestionResponse ask(@RequestBody QuestionRequest request) {

        String answer = groqAi.answerCareerQuestion(
                request.getQuestion(),
                request.getResumeText(),
                request.getJobDescription() // ✅ pass JD too
        );

        QuestionResponse response = new QuestionResponse();
        response.setAnswer(answer);

        return response;
    }

    @PostMapping("/upload-resume")
    public ResponseEntity<ResumeExtractResponse> uploadResumePdf(
            @RequestParam("file") MultipartFile file) {

        if (file == null || file.isEmpty()) {
            return ResponseEntity.badRequest()
                    .body(new ResumeExtractResponse(""));
        }

        String extractedText;
        try {
            extractedText = extractTextFromPdf(file);
        } catch (Exception e) {
            return ResponseEntity.internalServerError()
                    .body(new ResumeExtractResponse("ERROR: Unable to read PDF file."));
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
