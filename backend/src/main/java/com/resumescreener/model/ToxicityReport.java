package com.resumescreener.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Toxicity detection report for resume content.
 * Detects inappropriate language, discriminatory content, aggression.
 * Optional feature that can be disabled in configuration.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ToxicityReport {

    @JsonProperty("is_toxic")
    private boolean isToxic;

    @JsonProperty("toxicity_score")
    private double toxicityScore;  // 0.0 - 1.0, higher = more toxic

    @JsonProperty("severity_level")
    private String severityLevel;  // "none", "low", "medium", "high"

    @JsonProperty("detected_issues")
    private List<ToxicityIssue> detectedIssues;

    @JsonProperty("flagged_phrases")
    private List<String> flaggedPhrases;

    @JsonProperty("model_name")
    private String modelName;

    @JsonProperty("was_fallback")
    private boolean wasFallback;

    @JsonProperty("timestamp")
    private long timestamp;

    /**
     * Individual toxicity issue detected
     */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ToxicityIssue {
        @JsonProperty("type")
        private String type;  // "inappropriate_language", "discriminatory", "aggressive", "spam"

        @JsonProperty("confidence")
        private double confidence;  // 0.0 - 1.0

        @JsonProperty("description")
        private String description;

        @JsonProperty("recommendation")
        private String recommendation;  // e.g., "Remove this phrase"
    }
}
