package com.careercompass.careercompass.dto;

import jakarta.validation.constraints.NotBlank;

public record AnalysisRequest(
        @NotBlank(message = "Job description cannot be empty") String jobDescription,

        @NotBlank(message = "Resume text cannot be empty") String resumeText) {
}
