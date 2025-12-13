package com.careercompass.careercompass.dto;

public class ResumeExtractResponse {
    private String text;

    // Default constructor (required by Spring/Jackson)
    public ResumeExtractResponse() {
    }

    // Constructor to quickly create the object
    public ResumeExtractResponse(String text) {
        this.text = text;
    }

    // Getter
    public String getText() {
        return text;
    }

    // Setter
    public void setText(String text) {
        this.text = text;
    }
}
