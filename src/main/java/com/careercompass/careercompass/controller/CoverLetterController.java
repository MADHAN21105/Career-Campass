package com.careercompass.careercompass.controller;

import com.careercompass.careercompass.dto.CoverLetterRequest;
import com.careercompass.careercompass.service.GroqAi;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/cover-letter")
@CrossOrigin(origins = "*")
public class CoverLetterController {

    private final GroqAi groqAi;

    public CoverLetterController(GroqAi groqAi) {
        this.groqAi = groqAi;
    }

    @PostMapping("/generate")
    public Map<String, String> generateCoverLetter(@RequestBody CoverLetterRequest request) {

        String generatedLetter = groqAi.generateCoverLetter(request);

        Map<String, String> response = new HashMap<>();
        response.put("coverLetter", generatedLetter); // ✔ REQUIRED BY FRONTEND

        return response;
    }
}
