package com.resumescreener.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Detailed match score breakdown between resume and job description.
 * Complements the LLM-based match score with semantic similarity metrics.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class MatchScore {

    @JsonProperty("overall_match_percentage")
    private int overallMatchPercentage;  // 0-100

    @JsonProperty("skills_match_percentage")
    private int skillsMatchPercentage;   // 0-100, weight: 40%

    @JsonProperty("experience_match_percentage")
    private int experienceMatchPercentage;  // 0-100, weight: 30%

    @JsonProperty("tech_stack_match_percentage")
    private int techStackMatchPercentage;   // 0-100, weight: 20%

    @JsonProperty("education_match_percentage")
    private int educationMatchPercentage;   // 0-100, weight: 10%

    @JsonProperty("semantic_similarity")
    private double semanticSimilarity;  // 0.0 - 1.0, from sentence-transformers

    @JsonProperty("missing_required_skills")
    private java.util.List<String> missingRequiredSkills;

    @JsonProperty("additional_skills")
    private java.util.List<String> additionalSkills;

    @JsonProperty("experience_gap_years")
    private double experienceGapYears;  // negative if candidate overqualified

    @JsonProperty("years_above_requirement")
    private double yearsAboveRequirement;

    @JsonProperty("scoring_method")
    private String scoringMethod;  // "semantic_similarity", "keyword_matching", "hybrid"

    @JsonProperty("model_name")
    private String modelName;

    @JsonProperty("was_fallback")
    private boolean wasFallback;

    @JsonProperty("confidence")
    private double confidence;  // how confident in this score (0.0 - 1.0)
}
