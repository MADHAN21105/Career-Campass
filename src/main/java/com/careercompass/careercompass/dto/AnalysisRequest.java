package com.careercompass.careercompass.dto;

public class AnalysisRequest {

    // 1) variables to store incoming data
    private String jobDescription;
    private String resumeText;

    // 2) empty constructor (needed by Spring)
    public AnalysisRequest() {
    }

    // 3) getters and setters

    public String getJobDescription() {
        return jobDescription;
    }

    public void setJobDescription(String jobDescription) {
        this.jobDescription = jobDescription;
    }

    public String getResumeText() {
        return resumeText;
    }

    public void setResumeText(String resumeText) {
        this.resumeText = resumeText;
    }
}
