package com.careercompass.careercompass.controller;

import com.careercompass.careercompass.dto.CoverLetterRequest;
import com.careercompass.careercompass.service.CareerChatService;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/cover-letter")
@CrossOrigin(origins = "*")
@SuppressWarnings("all")
public class CoverLetterController {

    private final CareerChatService chatService;

    public CoverLetterController(CareerChatService chatService) {
        this.chatService = chatService;
    }

    @PostMapping("/generate")
    public Map<String, String> generateCoverLetter(@RequestBody CoverLetterRequest request) {

        String generatedLetter = chatService.generateCoverLetter(request);

        Map<String, String> response = new HashMap<>();
        response.put("coverLetter", generatedLetter); // ✔ REQUIRED BY FRONTEND

        return response;
    }
}
