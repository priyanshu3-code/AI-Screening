package com.resumescreener.config;

import com.resumescreener.ai.inference.HuggingFaceInferenceClient;
import com.resumescreener.ai.inference.HuggingFaceInferenceConfig;
import com.resumescreener.ai.service.*;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Spring configuration for Hugging Face local inference.
 * Registers all AI services with dependency injection.
 */
@Configuration
@Slf4j
public class HuggingFaceConfiguration {

    /**
     * Create HuggingFace inference configuration from application.yml.
     */
    @Bean
    @ConfigurationProperties(prefix = "huggingface.local-inference")
    public HuggingFaceInferenceConfig huggingFaceInferenceConfig() {
        return new HuggingFaceInferenceConfig();
    }

    /**
     * Create HuggingFace inference client (handles model loading and inference).
     */
    @Bean
    public HuggingFaceInferenceClient huggingFaceInferenceClient(HuggingFaceInferenceConfig config) {
        return new HuggingFaceInferenceClient(config);
    }

    /**
     * Resume summarization service.
     */
    @Bean
    public ResumeSummarizationService resumeSummarizationService(
            HuggingFaceInferenceClient inferenceClient,
            HuggingFaceInferenceConfig config) {
        return new ResumeSummarizationService(inferenceClient, config);
    }

    /**
     * Skill extraction service.
     */
    @Bean
    public SkillExtractionService skillExtractionService(
            HuggingFaceInferenceClient inferenceClient,
            HuggingFaceInferenceConfig config) {
        return new SkillExtractionService(inferenceClient, config);
    }

    /**
     * Match scoring service (semantic similarity + skill matching).
     */
    @Bean
    public MatchScoringService matchScoringService(
            HuggingFaceInferenceClient inferenceClient,
            HuggingFaceInferenceConfig config,
            SkillExtractionService skillExtractionService) {
        return new MatchScoringService(inferenceClient, config, skillExtractionService);
    }

    /**
     * Toxicity detection service (optional).
     */
    @Bean
    public ToxicityDetectionService toxicityDetectionService(
            HuggingFaceInferenceClient inferenceClient,
            HuggingFaceInferenceConfig config) {
        return new ToxicityDetectionService(inferenceClient, config);
    }

    /**
     * Validate Hugging Face configuration on startup.
     */
    @PostConstruct
    public void validateHuggingFaceLocalInferenceConfig() {
        HuggingFaceInferenceConfig config = huggingFaceInferenceConfig();

        log.info("========================================");
        log.info("Hugging Face Local Inference Configuration");
        log.info("========================================");

        if (!config.isEnabled()) {
            log.warn("⚠️  Hugging Face local inference is DISABLED");
            return;
        }

        log.info("✓ Hugging Face local inference: ENABLED");
        log.info("  Model cache size: {}", config.getModelCacheSize());

        validateServiceConfig("Resume Summarization", config.getSummarization().isEnabled(),
                config.getSummarization().getModel());
        validateServiceConfig("Skill Extraction", config.getSkillExtraction().isEnabled(),
                config.getSkillExtraction().getModel());
        validateServiceConfig("Match Scoring", config.getMatchScoring().isEnabled(),
                config.getMatchScoring().getModel());
        validateServiceConfig("Toxicity Detection", config.getToxicityDetection().isEnabled(),
                config.getToxicityDetection().getModel());

        log.info("========================================");
    }

    private void validateServiceConfig(String serviceName, boolean enabled, String modelName) {
        if (!enabled) {
            log.info("  ⚪ {}: DISABLED", serviceName);
        } else {
            log.info("  ✓ {}: ENABLED (model: {})", serviceName, modelName);
        }
    }
}
