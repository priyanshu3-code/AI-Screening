package com.resumescreener.ai.service;

import com.resumescreener.ai.inference.HuggingFaceInferenceClient;
import com.resumescreener.ai.inference.HuggingFaceInferenceConfig;
import com.resumescreener.model.ToxicityReport;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.regex.Pattern;

/**
 * Optional service for detecting toxicity, inappropriate language, and discriminatory content in resumes.
 * Can be disabled in configuration if not needed.
 *
 * Detects:
 * - Inappropriate language
 * - Discriminatory content
 * - Aggressive language
 * - Spam indicators
 */
@Service
@Slf4j
public class ToxicityDetectionService {

    private final HuggingFaceInferenceClient inferenceClient;
    private final HuggingFaceInferenceConfig config;

    // Flagged keywords and patterns
    private static final Set<String> INAPPROPRIATE_KEYWORDS = Set.of(
        "hate", "discriminate", "racist", "sexist", "ageist",
        "abusive", "harassment", "bully", "threat"
    );

    private static final Set<String> DISCRIMINATORY_KEYWORDS = Set.of(
        "men only", "women only", "no disabled", "young only", "old only",
        "christian only", "muslim only", "jewish only", "no foreigners"
    );

    private static final Set<String> AGGRESSIVE_KEYWORDS = Set.of(
        "kill", "destroy", "attack", "violent", "aggressive",
        "force", "coerce", "intimidate", "dominate"
    );

    @Autowired
    public ToxicityDetectionService(HuggingFaceInferenceClient inferenceClient,
                                   HuggingFaceInferenceConfig config) {
        this.inferenceClient = inferenceClient;
        this.config = config;
    }

    /**
     * Detect toxicity in resume text.
     * @param resumeText Resume text to check
     * @param sessionId Session ID for logging
     * @return ToxicityReport with detected issues and recommendations
     */
    public ToxicityReport detectToxicity(String resumeText, String sessionId) {
        long startTime = System.currentTimeMillis();

        if (!config.getToxicityDetection().isEnabled()) {
            log.debug("[{}] Toxicity detection is disabled in config", sessionId);
            return new ToxicityReport();
        }

        if (resumeText == null || resumeText.trim().isEmpty()) {
            log.debug("[{}] Resume text is empty, skipping toxicity check", sessionId);
            return new ToxicityReport();
        }

        try {
            String modelName = config.getToxicityDetection().getModel();
            log.debug("[{}] Starting toxicity detection using model: {}", sessionId, modelName);

            // Try model inference first
            Map<String, Object> result = inferenceClient.detectToxicity(resumeText, modelName);

            if (result != null && !(boolean) result.getOrDefault("fallback", false)) {
                boolean isToxic = (boolean) result.getOrDefault("toxic", false);
                double score = ((Number) result.getOrDefault("score", 0.0)).doubleValue();

                ToxicityReport report = new ToxicityReport();
                report.setToxic(isToxic);
                report.setToxicityScore(score);
                report.setSeverityLevel(calculateSeverity(score));
                @SuppressWarnings("unchecked")
                List<String> issues = (List<String>) result.getOrDefault("detected_issues", new ArrayList<>());
                report.setFlaggedPhrases(issues);
                report.setDetectedIssues(new ArrayList<>());
                report.setModelName(modelName);
                report.setWasFallback(false);
                report.setTimestamp(System.currentTimeMillis());

                long duration = System.currentTimeMillis() - startTime;
                log.info("[{}] Toxicity detection completed in {}ms (toxic: {}, score: {})",
                    sessionId, duration, isToxic, score);

                return report;
            }

            // Fallback to pattern matching
            return performPatternBasedDetection(resumeText, sessionId, startTime);

        } catch (Exception e) {
            log.error("[{}] Error during toxicity detection: {}", sessionId, e.getMessage(), e);
            return performPatternBasedDetection(resumeText, sessionId, startTime);
        }
    }

    /**
     * Fallback toxicity detection using pattern matching.
     */
    private ToxicityReport performPatternBasedDetection(String resumeText, String sessionId, long startTime) {
        String textLower = resumeText.toLowerCase();
        ToxicityReport report = new ToxicityReport();
        List<ToxicityReport.ToxicityIssue> issues = new ArrayList<>();
        List<String> flaggedPhrases = new ArrayList<>();

        // Check for inappropriate language
        for (String keyword : INAPPROPRIATE_KEYWORDS) {
            if (textLower.contains(keyword)) {
                flaggedPhrases.add(keyword);
                issues.add(new ToxicityReport.ToxicityIssue(
                    "inappropriate_language",
                    0.8,
                    "Found inappropriate keyword: " + keyword,
                    "Remove this word from your resume"
                ));
            }
        }

        // Check for discriminatory content
        for (String keyword : DISCRIMINATORY_KEYWORDS) {
            if (textLower.contains(keyword)) {
                flaggedPhrases.add(keyword);
                issues.add(new ToxicityReport.ToxicityIssue(
                    "discriminatory",
                    0.95,
                    "Found discriminatory language: " + keyword,
                    "This violates employment law. Remove immediately."
                ));
            }
        }

        // Check for aggressive language
        for (String keyword : AGGRESSIVE_KEYWORDS) {
            if (textLower.contains(keyword)) {
                flaggedPhrases.add(keyword);
                issues.add(new ToxicityReport.ToxicityIssue(
                    "aggressive",
                    0.7,
                    "Found aggressive language: " + keyword,
                    "Use professional, neutral language instead"
                ));
            }
        }

        // Check for spam patterns
        if (containsExcessiveLinks(resumeText)) {
            issues.add(new ToxicityReport.ToxicityIssue(
                "spam",
                0.6,
                "Resume contains excessive links or promotional content",
                "Focus on professional content only"
            ));
        }

        boolean isToxic = !issues.isEmpty();
        double toxicityScore = calculateToxicityScore(issues);

        report.setToxic(isToxic);
        report.setToxicityScore(toxicityScore);
        report.setSeverityLevel(calculateSeverity(toxicityScore));
        report.setDetectedIssues(issues);
        report.setFlaggedPhrases(flaggedPhrases);
        report.setModelName("pattern_matching");
        report.setWasFallback(true);
        report.setTimestamp(System.currentTimeMillis());

        long duration = System.currentTimeMillis() - startTime;
        log.warn("[{}] Using pattern-based toxicity detection (duration: {}ms, issues: {})",
            sessionId, duration, issues.size());

        return report;
    }

    /**
     * Calculate toxicity severity level.
     */
    private String calculateSeverity(double score) {
        if (score < 0.3) return "none";
        if (score < 0.6) return "low";
        if (score < 0.8) return "medium";
        return "high";
    }

    /**
     * Calculate overall toxicity score from issues.
     */
    private double calculateToxicityScore(List<ToxicityReport.ToxicityIssue> issues) {
        if (issues.isEmpty()) return 0.0;

        return issues.stream()
            .mapToDouble(ToxicityReport.ToxicityIssue::getConfidence)
            .average()
            .orElse(0.0);
    }

    /**
     * Check if resume contains excessive links (spam indicator).
     */
    private boolean containsExcessiveLinks(String text) {
        Pattern urlPattern = Pattern.compile("https?://\\S+");
        var matcher = urlPattern.matcher(text);

        int linkCount = 0;
        while (matcher.find()) {
            linkCount++;
        }

        return linkCount > 5;  // More than 5 links is suspicious
    }

    /**
     * Check if toxicity detection is available.
     */
    public boolean isAvailable() {
        return config.getToxicityDetection().isEnabled();
    }

    /**
     * Get toxicity detection configuration.
     */
    public HuggingFaceInferenceConfig.ToxicityDetectionConfig getConfig() {
        return config.getToxicityDetection();
    }
}
