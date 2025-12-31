package com.careercompass.careercompass.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.util.List;

public record QuestionRequest(
                @NotBlank(message = "Question cannot be empty") @Size(max = 1000, message = "Question cannot exceed 1000 characters") String question,

                @Size(max = 50000, message = "Resume text cannot exceed 50000 characters") String resumeText,

                @Size(max = 50000, message = "Job description cannot exceed 50000 characters") String jobDescription,

                Double atsScore,

                List<String> matchedSkills,

                List<String> missingSkills,

                String summary,

                String strength,

                String improvementArea,

                String recommendation,
                String jobTitle,
                String resumeBio) {
}
