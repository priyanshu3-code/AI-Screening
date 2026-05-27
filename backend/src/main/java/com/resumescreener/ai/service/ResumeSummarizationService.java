package com.resumescreener.ai.service;

import com.resumescreener.ai.inference.HuggingFaceInferenceClient;
import com.resumescreener.ai.inference.HuggingFaceInferenceConfig;
import com.resumescreener.model.SummarizedResume;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Map;

/**
 * Service for abstractive summarization of resume content.
 * Uses lightweight models like facebook/bart-large-cnn.
 *
 * Falls back gracefully if model is unavailable or fails.
 */
@Service
@Slf4j
public class ResumeSummarizationService {

    private final HuggingFaceInferenceClient inferenceClient;
    private final HuggingFaceInferenceConfig config;

    @Autowired
    public ResumeSummarizationService(HuggingFaceInferenceClient inferenceClient,
                                     HuggingFaceInferenceConfig config) {
        this.inferenceClient = inferenceClient;
        this.config = config;
    }

    /**
     * Summarize a resume to key points.
     * @param resumeText Raw resume text
     * @param sessionId Session ID for logging
     * @return SummarizedResume object with summary text and metadata
     */
    public SummarizedResume summarizeResume(String resumeText, String sessionId) {
        long startTime = System.currentTimeMillis();

        if (!config.getSummarization().isEnabled()) {
            log.debug("[{}] Resume summarization is disabled in config", sessionId);
            return createFallbackSummary(resumeText, "disabled", startTime);
        }

        if (resumeText == null || resumeText.trim().isEmpty()) {
            log.warn("[{}] Resume text is empty, cannot summarize", sessionId);
            return new SummarizedResume();
        }

        try {
            String modelName = config.getSummarization().getModel();
            log.debug("[{}] Starting resume summarization using model: {}", sessionId, modelName);

            Map<String, Object> result = inferenceClient.summarizeText(resumeText, modelName);

            if (result == null) {
                return createFallbackSummary(resumeText, "inference_disabled", startTime);
            }

            String summaryText = (String) result.getOrDefault("summary", "");
            boolean isFallback = (boolean) result.getOrDefault("fallback", false);
            double confidence = ((Number) result.getOrDefault("confidence", 0.0)).doubleValue();
            long inferenceTime = ((Number) result.getOrDefault("duration_ms", 0L)).longValue();

            SummarizedResume summary = new SummarizedResume();
            summary.setSummaryText(summaryText);
            summary.setConfidenceScore(confidence);
            summary.setWordCount(summaryText.split("\\s+").length);
            summary.setCompressionRatio((double) resumeText.length() / Math.max(summaryText.length(), 1));
            summary.setGeneratedAtMs(System.currentTimeMillis());
            summary.setModelName(modelName);
            summary.setWasFallback(isFallback);

            long totalTime = System.currentTimeMillis() - startTime;
            log.info("[{}] Resume summarization completed in {}ms (inference: {}ms, fallback: {})",
                sessionId, totalTime, inferenceTime, isFallback);

            return summary;

        } catch (Exception e) {
            log.error("[{}] Error during resume summarization: {}", sessionId, e.getMessage(), e);
            return createFallbackSummary(resumeText, e.getClass().getSimpleName(), startTime);
        }
    }

    /**
     * Create fallback summary when inference fails.
     * Uses extractive approach: first sentences + ellipsis.
     */
    private SummarizedResume createFallbackSummary(String resumeText, String reason, long startTime) {
        String summary;

        if (resumeText == null || resumeText.trim().isEmpty()) {
            summary = "";
        } else if (resumeText.length() > 500) {
            // Extract first 500 characters
            summary = resumeText.substring(0, 500).trim();
            if (!summary.endsWith(".")) {
                int lastPeriod = summary.lastIndexOf('.');
                if (lastPeriod > 0) {
                    summary = summary.substring(0, lastPeriod + 1);
                }
            }
            summary += "...";
        } else {
            summary = resumeText;
        }

        SummarizedResume fallback = new SummarizedResume();
        fallback.setSummaryText(summary);
        fallback.setConfidenceScore(0.5);  // Lower confidence for fallback
        fallback.setWordCount(summary.split("\\s+").length);
        fallback.setCompressionRatio(resumeText != null ? (double) resumeText.length() / Math.max(summary.length(), 1) : 1.0);
        fallback.setGeneratedAtMs(System.currentTimeMillis());
        fallback.setModelName("fallback_extractive");
        fallback.setWasFallback(true);

        long duration = System.currentTimeMillis() - startTime;
        log.warn("Using fallback summarization (reason: {}, duration: {}ms)", reason, duration);

        return fallback;
    }

    /**
     * Check if summarization service is available.
     */
    public boolean isAvailable() {
        return config.getSummarization().isEnabled();
    }
}
