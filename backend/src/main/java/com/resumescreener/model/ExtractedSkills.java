package com.resumescreener.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Represents skills extracted from a resume.
 * Includes technical skills, soft skills, certifications with confidence scores.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ExtractedSkills {

    @JsonProperty("technical_skills")
    private List<Skill> technicalSkills;

    @JsonProperty("soft_skills")
    private List<Skill> softSkills;

    @JsonProperty("certifications")
    private List<Skill> certifications;

    @JsonProperty("languages")
    private List<Skill> languages;

    @JsonProperty("total_skill_count")
    private int totalSkillCount;

    @JsonProperty("average_confidence")
    private double averageConfidence;  // 0.0 - 1.0

    @JsonProperty("extraction_method")
    private String extractionMethod;  // "token_classification", "keyword_matching", "hybrid"

    @JsonProperty("model_name")
    private String modelName;

    @JsonProperty("was_fallback")
    private boolean wasFallback;

    /**
     * Individual skill with confidence score and category
     */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Skill {
        @JsonProperty("name")
        private String name;

        @JsonProperty("confidence")
        private double confidence;  // 0.0 - 1.0

        @JsonProperty("category")
        private String category;  // "technical", "soft", "certification", "language"

        @JsonProperty("frequency")
        private int frequency;  // how many times mentioned in resume

        @JsonProperty("level")
        private String level;  // "beginner", "intermediate", "advanced", "expert"
    }
}
