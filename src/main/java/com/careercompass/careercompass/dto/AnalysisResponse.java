package com.careercompass.careercompass.dto;

import java.util.List;

@SuppressWarnings("all")
public class AnalysisResponse {

    private double score;
    private String matchLevel;

    private List<String> jdSkills;
    private List<String> resumeSkills;
    private List<String> matchedSkills;
    private List<String> missingSkills;

    private List<String> resumeImprovementTips;
    private List<String> skillImprovementTips;
    private List<String> mandatorySkills;
    private List<String> preferredSkills;
    private List<String> careerGrowthSkills;
    private List<String> proTips;

    private String tip;

    // ✅ Improvement 5: Score explanation for transparency
    private String scoreExplanation;

    // Granular scoring breakdown for UI
    private int mandatoryMatchedCount;
    private int mandatoryTotalCount;
    private int preferredMatchedCount;
    private int preferredTotalCount;
    private int overallMatchedCount;
    private int overallTotalCount;

    private double hardSkillsScore;
    private double educationScore;
    private double titleScore;
    private double semanticScore;

    public AnalysisResponse() {
        this.jdSkills = new java.util.ArrayList<>();
        this.resumeSkills = new java.util.ArrayList<>();
        this.matchedSkills = new java.util.ArrayList<>();
        this.missingSkills = new java.util.ArrayList<>();
        this.resumeImprovementTips = new java.util.ArrayList<>();
        this.skillImprovementTips = new java.util.ArrayList<>();
        this.mandatorySkills = new java.util.ArrayList<>();
        this.preferredSkills = new java.util.ArrayList<>();
        this.preferredSkills = new java.util.ArrayList<>();
        this.careerGrowthSkills = new java.util.ArrayList<>();
        this.proTips = new java.util.ArrayList<>();
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

    public List<String> getMandatorySkills() {
        return mandatorySkills;
    }

    public void setMandatorySkills(List<String> mandatorySkills) {
        this.mandatorySkills = mandatorySkills;
    }

    public List<String> getPreferredSkills() {
        return preferredSkills;
    }

    public void setPreferredSkills(List<String> preferredSkills) {
        this.preferredSkills = preferredSkills;
    }

    // ATS Scoring Fields
    private String jobTitle;
    private String resumeTitle;
    private String educationRequirement;

    public String getJobTitle() {
        return jobTitle;
    }

    public void setJobTitle(String jobTitle) {
        this.jobTitle = jobTitle;
    }

    public String getResumeTitle() {
        return resumeTitle;
    }

    public void setResumeTitle(String resumeTitle) {
        this.resumeTitle = resumeTitle;
    }

    public String getEducationRequirement() {
        return educationRequirement;
    }

    public void setEducationRequirement(String educationRequirement) {
        this.educationRequirement = educationRequirement;
    }

    public String getScoreExplanation() {
        return scoreExplanation;
    }

    public void setScoreExplanation(String scoreExplanation) {
        this.scoreExplanation = scoreExplanation;
    }

    public List<String> getCareerGrowthSkills() {
        return careerGrowthSkills;
    }

    public void setCareerGrowthSkills(List<String> careerGrowthSkills) {
        this.careerGrowthSkills = careerGrowthSkills;
    }

    public List<String> getProTips() {
        return proTips;
    }

    public void setProTips(List<String> proTips) {
        this.proTips = proTips;
    }

    public int getMandatoryMatchedCount() {
        return mandatoryMatchedCount;
    }

    public void setMandatoryMatchedCount(int mandatoryMatchedCount) {
        this.mandatoryMatchedCount = mandatoryMatchedCount;
    }

    public int getMandatoryTotalCount() {
        return mandatoryTotalCount;
    }

    public void setMandatoryTotalCount(int mandatoryTotalCount) {
        this.mandatoryTotalCount = mandatoryTotalCount;
    }

    public int getPreferredMatchedCount() {
        return preferredMatchedCount;
    }

    public void setPreferredMatchedCount(int preferredMatchedCount) {
        this.preferredMatchedCount = preferredMatchedCount;
    }

    public int getPreferredTotalCount() {
        return preferredTotalCount;
    }

    public void setPreferredTotalCount(int preferredTotalCount) {
        this.preferredTotalCount = preferredTotalCount;
    }

    public int getOverallMatchedCount() {
        return overallMatchedCount;
    }

    public void setOverallMatchedCount(int overallMatchedCount) {
        this.overallMatchedCount = overallMatchedCount;
    }

    public int getOverallTotalCount() {
        return overallTotalCount;
    }

    public void setOverallTotalCount(int overallTotalCount) {
        this.overallTotalCount = overallTotalCount;
    }

    public double getHardSkillsScore() {
        return hardSkillsScore;
    }

    public void setHardSkillsScore(double hardSkillsScore) {
        this.hardSkillsScore = hardSkillsScore;
    }

    public double getEducationScore() {
        return educationScore;
    }

    public void setEducationScore(double educationScore) {
        this.educationScore = educationScore;
    }

    public double getTitleScore() {
        return titleScore;
    }

    public void setTitleScore(double titleScore) {
        this.titleScore = titleScore;
    }

    public double getSemanticScore() {
        return semanticScore;
    }

    public void setSemanticScore(double semanticScore) {
        this.semanticScore = semanticScore;
    }
}
