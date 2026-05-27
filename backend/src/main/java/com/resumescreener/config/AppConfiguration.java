package com.resumescreener.config;

import jakarta.annotation.PostConstruct;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties
@ConfigurationProperties(prefix = "app")
@Getter
@Slf4j
public class AppConfiguration {

    // ============================================
    // Server Configuration
    // ============================================
    @Value("${server.port:8080}")
    private int serverPort;

    @Value("${server.servlet.context-path:/}")
    private String contextPath;

    // ============================================
    // API Configuration
    // ============================================
    @Value("${api.base-url:http://localhost:8080}")
    private String apiBaseUrl;

    @Value("${api.timeout-seconds:30}")
    private int apiTimeoutSeconds;

    // ============================================
    // HuggingFace Configuration
    // ============================================
    @Value("${huggingface.api.key:#{null}}")
    private String huggingFaceApiKey;

    @Value("${huggingface.api.url:https://router.huggingface.co/v1}")
    private String huggingFaceApiUrl;

    // ============================================
    // LLM Configuration
    // ============================================
    @Value("${llm.extraction.model:mistralai/Mistral-7B-Instruct-v0.2}")
    private String extractionModel;

    @Value("${llm.extraction.timeout-ms:30000}")
    private int extractionTimeoutMs;

    @Value("${llm.extraction.max-tokens:1024}")
    private int extractionMaxTokens;

    @Value("${llm.extraction.temperature:0.3}")
    private double extractionTemperature;

    @Value("${llm.interview.model:mistralai/Mistral-7B-Instruct-v0.2}")
    private String interviewModel;

    @Value("${llm.interview.timeout-ms:45000}")
    private int interviewTimeoutMs;

    @Value("${llm.interview.max-tokens:2048}")
    private int interviewMaxTokens;

    @Value("${llm.interview.temperature:0.5}")
    private double interviewTemperature;

    @Value("${llm.summary.model:meta-llama/Llama-3.1-8B-Instruct}")
    private String summaryModel;

    @Value("${llm.summary.timeout-ms:15000}")
    private int summaryTimeoutMs;

    @Value("${llm.summary.max-tokens:512}")
    private int summaryMaxTokens;

    @Value("${llm.summary.temperature:0.3}")
    private double summaryTemperature;

    @Value("${llm.retry.max-attempts:3}")
    private int llmRetryMaxAttempts;

    @Value("${llm.retry.initial-backoff-ms:1000}")
    private long llmRetryInitialBackoffMs;

    @Value("${llm.retry.backoff-multiplier:2}")
    private long llmRetryBackoffMultiplier;

    // ============================================
    // File Upload Configuration
    // ============================================
    @Value("${file.max-size-mb:10}")
    private int maxFileSizeMb;

    @Value("${file.allowed-types:pdf,txt,doc,docx}")
    private String allowedFileTypes;

    @Value("${file.min-resume-chars:100}")
    private int minResumeChars;

    // ============================================
    // Job Description Configuration
    // ============================================
    @Value("${job-description.min-chars:50}")
    private int minJobDescriptionChars;

    @Value("${job-description.max-chars:50000}")
    private int maxJobDescriptionChars;

    // ============================================
    // Session Configuration
    // ============================================
    @Value("${session.timeout-minutes:60}")
    private int sessionTimeoutMinutes;

    @Value("${session.cleanup-interval-minutes:60}")
    private int sessionCleanupIntervalMinutes;

    @Value("${session.max-concurrent:100}")
    private int maxConcurrentSessions;

    // ============================================
    // Match Score Configuration
    // ============================================
    @Value("${scoring.interview-threshold:70}")
    private int interviewThreshold;

    @Value("${scoring.weight-skills:0.40}")
    private double weightSkills;

    @Value("${scoring.weight-experience:0.30}")
    private double weightExperience;

    @Value("${scoring.weight-tech-stack:0.20}")
    private double weightTechStack;

    @Value("${scoring.weight-education:0.10}")
    private double weightEducation;

    // ============================================
    // CORS Configuration
    // ============================================
    @Value("${cors.allowed-origins:http://localhost:4200}")
    private String corsAllowedOrigins;

    // ============================================
    // Logging Configuration
    // ============================================
    @Value("${logging.level.root:INFO}")
    private String logLevelRoot;

    @Value("${logging.level.app:DEBUG}")
    private String logLevelApp;

    @Value("${spring.profiles.active:dev}")
    private String activeProfile;

    /**
     * Validates all critical configuration on application startup.
     * Fails fast with clear error messages if required configuration is missing or invalid.
     */
    @PostConstruct
    public void validateConfiguration() {
        log.info("========================================");
        log.info("Validating Application Configuration");
        log.info("========================================");
        log.info("Active Profile: {}", activeProfile);

        validateRequiredConfigurations();
        validatePortConfiguration();
        validateHuggingFaceConfiguration();
        validateLLMConfiguration();
        validateFileUploadConfiguration();
        validateJobDescriptionConfiguration();
        validateSessionConfiguration();
        validateScoringConfiguration();
        validateCorsConfiguration();

        log.info("========================================");
        log.info("✓ All configurations validated successfully");
        log.info("========================================");
    }

    private void validateRequiredConfigurations() {
        log.debug("Validating required configurations...");

        if (huggingFaceApiKey == null || huggingFaceApiKey.isBlank()) {
            String message = """
                ❌ CRITICAL: HuggingFace API Key is not configured!

                To fix this issue:
                1. Ensure HUGGINGFACE_API_KEY environment variable is set
                2. Or add huggingface.api.key to application-{profile}.yml

                Get your API key from: https://huggingface.co/settings/tokens

                The application cannot start without a valid HuggingFace API key.
                """;
            log.error(message);
            throw new IllegalStateException("HuggingFace API key is required but not configured");
        }

        if ("your_hf_token_here".equals(huggingFaceApiKey) ||
            "your_api_key_here".equals(huggingFaceApiKey) ||
            huggingFaceApiKey.startsWith("hf_")) {
            log.warn("⚠️  Using default/example API key. For production, set HUGGINGFACE_API_KEY environment variable.");
        } else {
            log.info("✓ HuggingFace API Key is configured (value masked)");
        }
    }

    private void validatePortConfiguration() {
        log.debug("Validating port configuration...");

        if (serverPort <= 0 || serverPort > 65535) {
            throw new IllegalStateException(
                "Invalid server port: " + serverPort + ". Port must be between 1 and 65535."
            );
        }

        log.info("✓ Server port configured: {}", serverPort);
    }

    private void validateHuggingFaceConfiguration() {
        log.debug("Validating HuggingFace configuration...");

        if (huggingFaceApiUrl == null || huggingFaceApiUrl.isBlank()) {
            throw new IllegalStateException("HuggingFace API URL is required");
        }

        if (!huggingFaceApiUrl.startsWith("http://") && !huggingFaceApiUrl.startsWith("https://")) {
            throw new IllegalStateException(
                "HuggingFace API URL must start with http:// or https://: " + huggingFaceApiUrl
            );
        }

        log.info("✓ HuggingFace API URL: {}", huggingFaceApiUrl);
    }

    private void validateLLMConfiguration() {
        log.debug("Validating LLM configuration...");

        validateModelConfig("Extraction", extractionModel, extractionTimeoutMs, extractionMaxTokens, extractionTemperature);
        validateModelConfig("Interview", interviewModel, interviewTimeoutMs, interviewMaxTokens, interviewTemperature);
        validateModelConfig("Summary", summaryModel, summaryTimeoutMs, summaryMaxTokens, summaryTemperature);

        if (llmRetryMaxAttempts < 1 || llmRetryMaxAttempts > 10) {
            throw new IllegalStateException(
                "LLM retry max attempts must be between 1 and 10, got: " + llmRetryMaxAttempts
            );
        }

        if (llmRetryInitialBackoffMs < 100 || llmRetryInitialBackoffMs > 30000) {
            throw new IllegalStateException(
                "LLM retry initial backoff must be between 100ms and 30000ms, got: " + llmRetryInitialBackoffMs
            );
        }

        log.info("✓ LLM configuration validated");
    }

    private void validateModelConfig(String modelName, String model, int timeout, int maxTokens, double temperature) {
        if (model == null || model.isBlank()) {
            throw new IllegalStateException(modelName + " model is required");
        }

        if (timeout < 5000 || timeout > 120000) {
            throw new IllegalStateException(
                modelName + " timeout must be between 5000ms and 120000ms, got: " + timeout
            );
        }

        if (maxTokens < 128 || maxTokens > 4096) {
            throw new IllegalStateException(
                modelName + " max tokens must be between 128 and 4096, got: " + maxTokens
            );
        }

        if (temperature < 0.0 || temperature > 2.0) {
            throw new IllegalStateException(
                modelName + " temperature must be between 0.0 and 2.0, got: " + temperature
            );
        }

        log.info("  ✓ {} Model: {}", modelName, model);
    }

    private void validateFileUploadConfiguration() {
        log.debug("Validating file upload configuration...");

        if (maxFileSizeMb < 1 || maxFileSizeMb > 100) {
            throw new IllegalStateException(
                "Max file size must be between 1MB and 100MB, got: " + maxFileSizeMb + "MB"
            );
        }

        if (allowedFileTypes == null || allowedFileTypes.isBlank()) {
            throw new IllegalStateException("Allowed file types must be configured");
        }

        if (minResumeChars < 10 || minResumeChars > 10000) {
            throw new IllegalStateException(
                "Min resume chars must be between 10 and 10000, got: " + minResumeChars
            );
        }

        log.info("✓ File upload configuration validated (max size: {}MB)", maxFileSizeMb);
    }

    private void validateJobDescriptionConfiguration() {
        log.debug("Validating job description configuration...");

        if (minJobDescriptionChars < 10 || minJobDescriptionChars > 1000) {
            throw new IllegalStateException(
                "Min job description chars must be between 10 and 1000, got: " + minJobDescriptionChars
            );
        }

        if (maxJobDescriptionChars < 1000 || maxJobDescriptionChars > 100000) {
            throw new IllegalStateException(
                "Max job description chars must be between 1000 and 100000, got: " + maxJobDescriptionChars
            );
        }

        if (minJobDescriptionChars >= maxJobDescriptionChars) {
            throw new IllegalStateException(
                "Min job description chars (" + minJobDescriptionChars + ") must be less than max (" + maxJobDescriptionChars + ")"
            );
        }

        log.info("✓ Job description configuration validated");
    }

    private void validateSessionConfiguration() {
        log.debug("Validating session configuration...");

        if (sessionTimeoutMinutes < 5 || sessionTimeoutMinutes > 1440) {
            throw new IllegalStateException(
                "Session timeout must be between 5 and 1440 minutes, got: " + sessionTimeoutMinutes
            );
        }

        if (sessionCleanupIntervalMinutes < 1 || sessionCleanupIntervalMinutes > 60) {
            throw new IllegalStateException(
                "Session cleanup interval must be between 1 and 60 minutes, got: " + sessionCleanupIntervalMinutes
            );
        }

        if (maxConcurrentSessions < 1 || maxConcurrentSessions > 10000) {
            throw new IllegalStateException(
                "Max concurrent sessions must be between 1 and 10000, got: " + maxConcurrentSessions
            );
        }

        log.info("✓ Session configuration validated (timeout: {} minutes)", sessionTimeoutMinutes);
    }

    private void validateScoringConfiguration() {
        log.debug("Validating scoring configuration...");

        if (interviewThreshold < 0 || interviewThreshold > 100) {
            throw new IllegalStateException(
                "Interview threshold must be between 0 and 100, got: " + interviewThreshold
            );
        }

        double totalWeight = weightSkills + weightExperience + weightTechStack + weightEducation;
        if (Math.abs(totalWeight - 1.0) > 0.01) {
            throw new IllegalStateException(
                "Scoring weights must sum to 1.0, got: " + totalWeight +
                " (skills: " + weightSkills + ", exp: " + weightExperience +
                ", tech: " + weightTechStack + ", edu: " + weightEducation + ")"
            );
        }

        log.info("✓ Scoring configuration validated (threshold: {}%)", interviewThreshold);
    }

    private void validateCorsConfiguration() {
        log.debug("Validating CORS configuration...");

        if (corsAllowedOrigins == null || corsAllowedOrigins.isBlank()) {
            throw new IllegalStateException("CORS allowed origins must be configured");
        }

        String[] origins = corsAllowedOrigins.split(",");
        for (String origin : origins) {
            String trimmedOrigin = origin.trim();
            if (!trimmedOrigin.startsWith("http://") && !trimmedOrigin.startsWith("https://")) {
                throw new IllegalStateException(
                    "CORS origin must start with http:// or https://: " + trimmedOrigin
                );
            }
        }

        log.info("✓ CORS configuration validated");
    }
}
