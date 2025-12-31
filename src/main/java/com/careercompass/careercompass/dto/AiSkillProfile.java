package com.careercompass.careercompass.dto;

import java.util.ArrayList;
import java.util.List;

@SuppressWarnings("all")
public class AiSkillProfile {
    // Skills extracted from Job Description
    private List<String> jdRequiredSkills = new ArrayList<>();

    // Strong hands-on skills found in resume
    private List<String> strongSkills = new ArrayList<>();

    // Weak / learning / certification-based skills
    private List<String> weakSkills = new ArrayList<>();

    // Explicitly tracked missing skills from LLM
    private List<String> missingSkills = new ArrayList<>();

    // Detected role focus: backend / frontend / data / etc.
    private String roleFocus;

    // Detected role title from JD
    private String jdRole;

    // Extracted bio or summary from resume
    private String resumeBio;

    // Skills from JD that were actually found (matched)
    private List<String> matchedSkills = new ArrayList<>();

    // True if resume is considered generally related to JD role
    private boolean generallyRelated;

    // The calculated ATS score (if available)
    private double atsScore;

    // -------- NEW FIELDS FOR REPORT UNIFICATION --------
    private String summary;
    private String strength;
    private String improvementArea;
    private String recommendation;
    private List<String> resumeImprovementTips = new ArrayList<>();
    private List<String> skillImprovementTips = new ArrayList<>();
    private List<String> mandatorySkills = new ArrayList<>();
    private List<String> preferredSkills = new ArrayList<>();
    private String resumeTitle;
    private String educationRequirement;
    private List<String> careerGrowthSkills = new ArrayList<>();
    private List<String> proTips = new ArrayList<>();

    // -------- GETTERS --------

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

    public List<String> getResumeImprovementTips() {
        return resumeImprovementTips != null ? resumeImprovementTips : new ArrayList<>();
    }

    public void setResumeImprovementTips(List<String> resumeImprovementTips) {
        this.resumeImprovementTips = resumeImprovementTips;
    }

    public List<String> getSkillImprovementTips() {
        return skillImprovementTips != null ? skillImprovementTips : new ArrayList<>();
    }

    public void setSkillImprovementTips(List<String> skillImprovementTips) {
        this.skillImprovementTips = skillImprovementTips;
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

    public List<String> getCareerGrowthSkills() {
        return careerGrowthSkills != null ? careerGrowthSkills : new ArrayList<>();
    }

    public void setCareerGrowthSkills(List<String> careerGrowthSkills) {
        this.careerGrowthSkills = careerGrowthSkills;
    }

    public double getAtsScore() {
        return atsScore;
    }

    public void setAtsScore(double atsScore) {
        this.atsScore = atsScore;
    }

    public List<String> getMissingSkills() {
        if (missingSkills != null && !missingSkills.isEmpty()) {
            return missingSkills;
        }
        if (jdRequiredSkills == null)
            return new ArrayList<>();
        List<String> missing = new ArrayList<>(jdRequiredSkills);
        if (matchedSkills != null) {
            missing.removeAll(matchedSkills);
        }
        return missing;
    }

    public void setMissingSkills(List<String> missingSkills) {
        this.missingSkills = missingSkills;
    }

    public String getResumeBio() {
        return resumeBio != null ? resumeBio : "";
    }

    public String getJdRole() {
        return jdRole != null ? jdRole : roleFocus;
    }

    public void setResumeBio(String resumeBio) {
        this.resumeBio = resumeBio;
    }

    public void setJdRole(String jdRole) {
        this.jdRole = jdRole;
    }

    public List<String> getMatchedSkills() {
        return matchedSkills;
    }

    public void setMatchedSkills(List<String> matchedSkills) {
        this.matchedSkills = matchedSkills;
    }

    public List<String> getJdRequiredSkills() {
        return jdRequiredSkills != null ? jdRequiredSkills : new ArrayList<>();
    }

    public List<String> getStrongSkills() {
        return strongSkills;
    }

    public List<String> getWeakSkills() {
        return weakSkills;
    }

    public String getRoleFocus() {
        return roleFocus;
    }

    public boolean isGenerallyRelated() {
        return generallyRelated;
    }

    // -------- SETTERS --------

    public void setJdRequiredSkills(List<String> jdRequiredSkills) {
        this.jdRequiredSkills = jdRequiredSkills;
    }

    public void setStrongSkills(List<String> strongSkills) {
        this.strongSkills = strongSkills;
    }

    public void setWeakSkills(List<String> weakSkills) {
        this.weakSkills = weakSkills;
    }

    public void setRoleFocus(String roleFocus) {
        this.roleFocus = roleFocus;
    }

    public void setGenerallyRelated(boolean generallyRelated) {
        this.generallyRelated = generallyRelated;
    }

    public List<String> getProTips() {
        return proTips;
    }

    public void setProTips(List<String> proTips) {
        this.proTips = proTips;
    }
}
