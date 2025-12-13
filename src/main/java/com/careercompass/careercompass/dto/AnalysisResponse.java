package com.careercompass.careercompass.dto;

import java.util.List;

public class AnalysisResponse {

    private double score;
    private String matchLevel;

    private List<String> jdSkills;
    private List<String> resumeSkills;
    private List<String> matchedSkills;
    private List<String> missingSkills;

    private List<String> resumeImprovementTips;
    private List<String> skillImprovementTips;

    private String tip;

    public AnalysisResponse() {
    }

    public double getScore() {
        return score;
    }

    public void setScore(double score) {
        this.score = score;
    }

    public String getMatchLevel() {
        return matchLevel;
    }

    public void setMatchLevel(String matchLevel) {
        this.matchLevel = matchLevel;
    }

    public List<String> getJdSkills() {
        return jdSkills;
    }

    public void setJdSkills(List<String> jdSkills) {
        this.jdSkills = jdSkills;
    }

    public List<String> getResumeSkills() {
        return resumeSkills;
    }

    public void setResumeSkills(List<String> resumeSkills) {
        this.resumeSkills = resumeSkills;
    }

    public List<String> getMatchedSkills() {
        return matchedSkills;
    }

    public void setMatchedSkills(List<String> matchedSkills) {
        this.matchedSkills = matchedSkills;
    }

    public List<String> getMissingSkills() {
        return missingSkills;
    }

    public void setMissingSkills(List<String> missingSkills) {
        this.missingSkills = missingSkills;
    }

    public List<String> getResumeImprovementTips() {
        return resumeImprovementTips;
    }

    public void setResumeImprovementTips(List<String> resumeImprovementTips) {
        this.resumeImprovementTips = resumeImprovementTips;
    }

    public List<String> getSkillImprovementTips() {
        return skillImprovementTips;
    }

    public void setSkillImprovementTips(List<String> skillImprovementTips) {
        this.skillImprovementTips = skillImprovementTips;
    }

    public String getTip() {
        return tip;
    }

    public void setTip(String tip) {
        this.tip = tip;
    }

    // New granular fields for Pro Tips
    private String summary;
    private String strength;
    private String improvementArea;
    private String recommendation;

    public String getSummary() {
        return summary;
    }

    public void setSummary(String summary) {
        this.summary = summary;
    }

    public String getStrength() {
        return strength;
    }

    public void setStrength(String strength) {
        this.strength = strength;
    }

    public String getImprovementArea() {
        return improvementArea;
    }

    public void setImprovementArea(String improvementArea) {
        this.improvementArea = improvementArea;
    }

    public String getRecommendation() {
        return recommendation;
    }

    public void setRecommendation(String recommendation) {
        this.recommendation = recommendation;
    }

    // Fields for enhanced insights
    private String areasToImprove;
    private String nextSteps;

    public String getAreasToImprove() {
        return areasToImprove;
    }

    public void setAreasToImprove(String areasToImprove) {
        this.areasToImprove = areasToImprove;
    }

    public String getNextSteps() {
        return nextSteps;
    }

    public void setNextSteps(String nextSteps) {
        this.nextSteps = nextSteps;
    }
}
