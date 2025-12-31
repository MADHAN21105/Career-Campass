package com.careercompass.careercompass.dto;

public record SkillEvidence(
        String skill,
        boolean found,
        int frequency,
        String strength,
        boolean groupRepresentative) {
    // Canonical constructor is auto-generated

    // Helper for creating a "not found" evidence
    public static SkillEvidence notFound(String skill) {
        return new SkillEvidence(skill, false, 0, "weak", false);
    }

    // Helper to create with modified properties (wither pattern)
    public SkillEvidence withGroupRepresentative(boolean groupRepresentative) {
        return new SkillEvidence(skill, found, frequency, strength, groupRepresentative);
    }
}
