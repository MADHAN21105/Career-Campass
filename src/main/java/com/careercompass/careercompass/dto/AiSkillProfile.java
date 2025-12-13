package com.careercompass.careercompass.dto;

import java.util.ArrayList;
import java.util.List;

public class AiSkillProfile {
    // Skills extracted from Job Description
    private List<String> jdRequiredSkills = new ArrayList<>();

    // Strong hands-on skills found in resume
    private List<String> strongSkills = new ArrayList<>();

    // Weak / learning / certification-based skills
    private List<String> weakSkills = new ArrayList<>();

    // Detected role focus: backend / frontend / data / etc.
    private String roleFocus;

    // True if resume is considered generally related to JD role
    private boolean generallyRelated;

    // -------- GETTERS --------

    public List<String> getJdRequiredSkills() {
        return jdRequiredSkills;
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
}
