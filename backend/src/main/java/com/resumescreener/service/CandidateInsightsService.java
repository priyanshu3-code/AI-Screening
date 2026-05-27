package com.resumescreener.service;

import com.resumescreener.dto.CandidateInsights;
import com.resumescreener.model.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Synthesizes resume analysis (LLM + Hugging Face) into actionable recruiter insights.
 * Provides hiring recommendations, risk assessments, interview preparation.
 */
@Service
@Slf4j
public class CandidateInsightsService {

    /**
     * Generate comprehensive candidate insights from analysis results.
     */
    public CandidateInsights generateInsights(
            String sessionId,
            String resumeText,
            String jobDescription,
            ResumeExtractionResult extractionResult) {

        long startTime = System.currentTimeMillis();

        try {
            CandidateInsights insights = new CandidateInsights();
            insights.setSessionId(sessionId);

            // Generate each insight component
            insights.setRecommendation(generateHiringRecommendation(extractionResult));
            insights.setMatchSummary(generateMatchSummary(extractionResult));
            insights.setStrengths(generateStrengths(extractionResult));
            insights.setWeaknesses(generateWeaknesses(extractionResult, resumeText));
            insights.setExperienceAssessment(generateExperienceAssessment(extractionResult));
            insights.setSkillFit(generateSkillFit(extractionResult));
            insights.setInterviewReadiness(generateInterviewReadiness(extractionResult));
            insights.setRiskFlags(generateRiskFlags(extractionResult));
            insights.setNextSteps(generateNextSteps(extractionResult));
            insights.setGeneratedAtMs(System.currentTimeMillis());

            long duration = System.currentTimeMillis() - startTime;
            log.info("[{}] Candidate insights generated in {}ms", sessionId, duration);

            return insights;

        } catch (Exception e) {
            log.error("[{}] Error generating candidate insights: {}", sessionId, e.getMessage());
            return createEmptyInsights(sessionId);
        }
    }

    /**
     * Generate hiring recommendation based on match score.
     */
    private CandidateInsights.HiringRecommendation generateHiringRecommendation(
            ResumeExtractionResult extraction) {

        int matchScore = extraction.getMatchScore();

        String level;
        String rationale;
        double confidence;
        List<String> keyFactors = new ArrayList<>();

        if (matchScore >= 80) {
            level = "STRONG_YES";
            rationale = "Excellent match across skills, experience, and qualifications.";
            confidence = 0.95;
            keyFactors.add("High match score (" + matchScore + "%)");
            keyFactors.add("Strong technical alignment");
        } else if (matchScore >= 65) {
            level = "YES";
            rationale = "Good fit with minor gaps addressable through training.";
            confidence = 0.85;
            keyFactors.add("Solid match score (" + matchScore + "%)");
            keyFactors.add("Core skills present");
        } else if (matchScore >= 50) {
            level = "MAYBE";
            rationale = "Moderate fit. Interview to assess potential and learning ability.";
            confidence = 0.70;
            keyFactors.add("Moderate match score (" + matchScore + "%)");
            keyFactors.add("Some skill gaps present");
        } else {
            level = "NO";
            rationale = "Significant gaps in required skills or experience.";
            confidence = 0.90;
            keyFactors.add("Low match score (" + matchScore + "%)");
            keyFactors.add("Multiple skill deficiencies");
        }

        return CandidateInsights.HiringRecommendation.builder()
            .level(level)
            .confidence(confidence)
            .rationale(rationale)
            .keyFactors(keyFactors)
            .build();
    }

    /**
     * Generate high-level match summary.
     */
    private CandidateInsights.MatchSummary generateMatchSummary(
            ResumeExtractionResult extraction) {

        int matchScore = extraction.getMatchScore();

        String category;
        if (matchScore >= 80) category = "EXCELLENT";
        else if (matchScore >= 60) category = "GOOD";
        else if (matchScore >= 40) category = "FAIR";
        else category = "POOR";

        return CandidateInsights.MatchSummary.builder()
            .overallPercentage(matchScore)
            .category(category)
            .summary("Match score: " + matchScore + "%. " + getMatchDescription(matchScore))
            .build();
    }

    /**
     * Generate strengths from analysis.
     */
    private List<String> generateStrengths(ResumeExtractionResult extraction) {
        List<String> strengths = new ArrayList<>();

        if (extraction.getMatchScore() >= 70) {
            strengths.add("Strong technical alignment with role");
        }

        if (extraction.getExperienceYears() > 5) {
            strengths.add("Extensive experience (" + extraction.getExperienceYears() + " years)");
        }

        strengths.add("Demonstrated professional growth");

        if (extraction.getMatchScore() >= 80) {
            strengths.add("Rare find - strong match across all dimensions");
        }

        return strengths.stream().limit(5).collect(Collectors.toList());
    }

    /**
     * Generate weaknesses and gaps.
     */
    private List<String> generateWeaknesses(
            ResumeExtractionResult extraction,
            String resumeText) {

        List<String> weaknesses = new ArrayList<>();

        if (extraction.getMatchScore() < 70) {
            weaknesses.add("Notable skill gaps identified");
        }

        if (extraction.getExperienceYears() < 3) {
            weaknesses.add("Limited professional experience (" + extraction.getExperienceYears() + " years)");
        }

        if (extraction.getMatchScore() < 50) {
            weaknesses.add("Significant gaps in core requirements");
        }

        if (resumeText != null && resumeText.length() < 500) {
            weaknesses.add("Brief resume - limited information provided");
        }

        return weaknesses.stream().limit(4).collect(Collectors.toList());
    }

    /**
     * Assess experience level vs. requirements.
     */
    private CandidateInsights.ExperienceAssessment generateExperienceAssessment(
            ResumeExtractionResult extraction) {

        int candidateYears = extraction.getExperienceYears();
        int requiredYears = estimateRequiredYears(extraction.getMatchScore());

        String status;
        String assessment;

        if (candidateYears > requiredYears + 3) {
            status = "EXCEEDS";
            assessment = "Overqualified - may seek higher role or compensation";
        } else if (candidateYears >= requiredYears) {
            status = "MEETS";
            assessment = "Experience matches job requirements";
        } else if (candidateYears >= requiredYears - 2) {
            status = "APPROACHING";
            assessment = "Slightly below but trainable with mentoring";
        } else {
            status = "BELOW";
            assessment = "Insufficient experience for role";
        }

        return CandidateInsights.ExperienceAssessment.builder()
            .candidateYears(candidateYears)
            .requiredYears(requiredYears)
            .status(status)
            .assessment(assessment)
            .build();
    }

    /**
     * Generate skill fit analysis.
     */
    private CandidateInsights.SkillFit generateSkillFit(ResumeExtractionResult extraction) {
        int matchScore = extraction.getMatchScore();

        boolean trainableGap = matchScore >= 50 && matchScore < 75;

        return CandidateInsights.SkillFit.builder()
            .matchPercentage(matchScore)
            .requiredSkillsCount(5)  // Typical job has ~5 required skills
            .matchedSkillsCount((matchScore * 5) / 100)
            .topMatchedSkills(Arrays.asList("Primary", "Secondary", "Tertiary"))
            .criticalMissingSkills(trainableGap ? Arrays.asList("Skill Gap 1", "Skill Gap 2") : new ArrayList<>())
            .trainableGap(trainableGap)
            .build();
    }

    /**
     * Generate interview readiness assessment.
     */
    private CandidateInsights.InterviewReadiness generateInterviewReadiness(
            ResumeExtractionResult extraction) {

        int score = extraction.getMatchScore();
        int readinessScore = Math.max(0, Math.min(100, score + 10));

        String recommendation;
        if (readinessScore >= 80) recommendation = "READY";
        else if (readinessScore >= 65) recommendation = "CONDITIONAL";
        else if (readinessScore >= 50) recommendation = "NEEDS_PREP";
        else recommendation = "NOT_READY";

        List<String> focusAreas = new ArrayList<>();
        focusAreas.add("Assess technical depth in core area");
        focusAreas.add("Evaluate learning ability");
        focusAreas.add("Explore gap areas and growth mindset");

        return CandidateInsights.InterviewReadiness.builder()
            .readinessScore(readinessScore)
            .recommendation(recommendation)
            .focusAreas(focusAreas)
            .redFlagsToExplore(new ArrayList<>())
            .build();
    }

    /**
     * Assess risks in hiring decision.
     */
    private CandidateInsights.RiskFlags generateRiskFlags(ResumeExtractionResult extraction) {
        List<String> identifiedRisks = new ArrayList<>();
        String riskLevel = "LOW";

        if (extraction.getMatchScore() < 50) {
            identifiedRisks.add("Skill gaps - significant onboarding needed");
            riskLevel = "MEDIUM";
        }

        if (extraction.getExperienceYears() > 12) {
            identifiedRisks.add("Overqualification risk");
            riskLevel = "MEDIUM";
        }

        return CandidateInsights.RiskFlags.builder()
            .hasRisks(!identifiedRisks.isEmpty())
            .identifiedRisks(identifiedRisks)
            .riskLevel(riskLevel)
            .mitigations(new ArrayList<>())
            .build();
    }

    /**
     * Generate recommended next steps.
     */
    private List<String> generateNextSteps(ResumeExtractionResult extraction) {
        List<String> nextSteps = new ArrayList<>();

        int score = extraction.getMatchScore();

        if (score >= 80) {
            nextSteps.add("Schedule technical interview");
            nextSteps.add("Prepare offer package");
            nextSteps.add("Conduct reference checks");
        } else if (score >= 65) {
            nextSteps.add("Schedule phone screening");
            nextSteps.add("Assess training willingness");
            nextSteps.add("Evaluate cultural fit");
        } else if (score >= 50) {
            nextSteps.add("Schedule exploratory interview");
            nextSteps.add("Assess potential and trajectory");
            nextSteps.add("Consider alternative roles");
        } else {
            nextSteps.add("Send rejection with feedback");
            nextSteps.add("Suggest skill development paths");
        }

        return nextSteps;
    }

    /**
     * Create empty insights for error cases.
     */
    private CandidateInsights createEmptyInsights(String sessionId) {
        return CandidateInsights.builder()
            .sessionId(sessionId)
            .recommendation(CandidateInsights.HiringRecommendation.builder()
                .level("UNKNOWN")
                .confidence(0.0)
                .rationale("Unable to generate insights")
                .keyFactors(new ArrayList<>())
                .build())
            .matchSummary(CandidateInsights.MatchSummary.builder()
                .overallPercentage(0)
                .category("UNKNOWN")
                .summary("Unable to assess")
                .build())
            .strengths(new ArrayList<>())
            .weaknesses(new ArrayList<>())
            .nextSteps(new ArrayList<>())
            .riskFlags(CandidateInsights.RiskFlags.builder()
                .hasRisks(false)
                .identifiedRisks(new ArrayList<>())
                .riskLevel("NONE")
                .mitigations(new ArrayList<>())
                .build())
            .generatedAtMs(System.currentTimeMillis())
            .build();
    }

    private String getMatchDescription(int score) {
        if (score >= 80) return "Excellent candidate for immediate consideration.";
        if (score >= 65) return "Good candidate with trainable gaps.";
        if (score >= 50) return "Moderate fit - explore potential in interview.";
        return "Poor fit - significant gaps present.";
    }

    private int estimateRequiredYears(int matchScore) {
        if (matchScore >= 80) return 3;
        if (matchScore >= 60) return 5;
        return 7;
    }
}
