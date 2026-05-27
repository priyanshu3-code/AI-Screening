package com.resumescreener.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Synthesized candidate insights dashboard data.
 * Combines LLM analysis, Hugging Face local inference, and heuristics into
 * actionable recruiter recommendations.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CandidateInsights {

    @JsonProperty("session_id")
    private String sessionId;

    @JsonProperty("recommendation")
    private HiringRecommendation recommendation;

    @JsonProperty("match_summary")
    private MatchSummary matchSummary;

    @JsonProperty("strengths")
    private List<String> strengths;

    @JsonProperty("weaknesses")
    private List<String> weaknesses;

    @JsonProperty("experience_assessment")
    private ExperienceAssessment experienceAssessment;

    @JsonProperty("skill_fit")
    private SkillFit skillFit;

    @JsonProperty("interview_readiness")
    private InterviewReadiness interviewReadiness;

    @JsonProperty("next_steps")
    private List<String> nextSteps;

    @JsonProperty("risk_flags")
    private RiskFlags riskFlags;

    @JsonProperty("generated_at_ms")
    private long generatedAtMs;

    /**
     * Hiring recommendation (STRONG_YES, YES, MAYBE, NO).
     * Based on match score, experience, skills, and qualitative factors.
     */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class HiringRecommendation {
        @JsonProperty("level")
        private String level;  // STRONG_YES, YES, MAYBE, NO

        @JsonProperty("confidence")
        private double confidence;  // 0.0-1.0

        @JsonProperty("rationale")
        private String rationale;  // Why this recommendation?

        @JsonProperty("key_factors")
        private List<String> keyFactors;  // Factors supporting the recommendation
    }

    /**
     * High-level match summary.
     */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class MatchSummary {
        @JsonProperty("overall_percentage")
        private int overallPercentage;  // 0-100

        @JsonProperty("category")
        private String category;  // EXCELLENT (80+), GOOD (60-79), FAIR (40-59), POOR (<40)

        @JsonProperty("summary")
        private String summary;  // "Strong technical fit with experience gaps in X"
    }

    /**
     * Experience assessment relative to job requirements.
     */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class ExperienceAssessment {
        @JsonProperty("candidate_years")
        private int candidateYears;

        @JsonProperty("required_years")
        private int requiredYears;

        @JsonProperty("status")
        private String status;  // EXCEEDS, MEETS, APPROACHING, BELOW

        @JsonProperty("assessment")
        private String assessment;  // "Overqualified - may seek higher-level role"
    }

    /**
     * Skill match breakdown.
     */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class SkillFit {
        @JsonProperty("match_percentage")
        private int matchPercentage;

        @JsonProperty("required_skills_count")
        private int requiredSkillsCount;

        @JsonProperty("matched_skills_count")
        private int matchedSkillsCount;

        @JsonProperty("top_matched_skills")
        private List<String> topMatchedSkills;

        @JsonProperty("critical_missing_skills")
        private List<String> criticalMissingSkills;

        @JsonProperty("trainable_gap")
        private boolean trainableGap;  // true if gaps can be addressed with training
    }

    /**
     * Interview readiness assessment.
     */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class InterviewReadiness {
        @JsonProperty("readiness_score")
        private int readinessScore;  // 0-100

        @JsonProperty("recommendation")
        private String recommendation;  // READY, CONDITIONAL, NEEDS_PREP, NOT_READY

        @JsonProperty("focus_areas")
        private List<String> focusAreas;  // "Ask about Docker experience", "Assess Spring Boot depth"

        @JsonProperty("red_flags_to_explore")
        private List<String> redFlagsToExplore;
    }

    /**
     * Risk assessment for hiring decision.
     */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class RiskFlags {
        @JsonProperty("has_risks")
        private boolean hasRisks;

        @JsonProperty("identified_risks")
        private List<String> identifiedRisks;

        @JsonProperty("risk_level")
        private String riskLevel;  // HIGH, MEDIUM, LOW, NONE

        @JsonProperty("mitigations")
        private List<String> mitigations;  // How to mitigate risks
    }
}
