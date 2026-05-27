package com.resumescreener.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

/**
 * Provides debug information about configuration loaded from all sources.
 * Useful for troubleshooting configuration issues in development.
 * Only logs in DEBUG mode to avoid verbosity in production.
 */
@Component
@Slf4j
public class ConfigurationDebugger {

    private final Environment environment;

    public ConfigurationDebugger(Environment environment) {
        this.environment = environment;
    }

    @EventListener(ApplicationReadyEvent.class)
    public void debugConfiguration() {
        if (!log.isDebugEnabled()) {
            return;
        }

        log.debug("========================================");
        log.debug("Configuration Sources Debug Info");
        log.debug("========================================");

        debugHuggingFaceConfig();
        debugLLMConfig();
        debugServerConfig();
        debugSessionConfig();
        debugScoringConfig();
        debugEnvironmentVariables();

        log.debug("========================================");
    }

    private void debugHuggingFaceConfig() {
        log.debug("HuggingFace Configuration:");
        log.debug("  URL: {}", environment.getProperty("huggingface.api.url"));
        String apiKey = environment.getProperty("huggingface.api.key");
        if (apiKey != null && !apiKey.isBlank()) {
            log.debug("  API Key: configured (length: {})", apiKey.length());
        } else {
            log.debug("  API Key: NOT CONFIGURED");
        }
    }

    private void debugLLMConfig() {
        log.debug("LLM Configuration:");
        log.debug("  Extraction Model: {}", environment.getProperty("llm.extraction.model"));
        log.debug("  Extraction Timeout: {}ms", environment.getProperty("llm.extraction.timeout-ms"));
        log.debug("  Interview Model: {}", environment.getProperty("llm.interview.model"));
        log.debug("  Interview Timeout: {}ms", environment.getProperty("llm.interview.timeout-ms"));
        log.debug("  Summary Model: {}", environment.getProperty("llm.summary.model"));
        log.debug("  Summary Timeout: {}ms", environment.getProperty("llm.summary.timeout-ms"));
        log.debug("  Retry Attempts: {}", environment.getProperty("llm.retry.max-attempts"));
        log.debug("  Backoff Multiplier: {}", environment.getProperty("llm.retry.backoff-multiplier"));
    }

    private void debugServerConfig() {
        log.debug("Server Configuration:");
        log.debug("  Port: {}", environment.getProperty("server.port"));
        log.debug("  Context Path: {}", environment.getProperty("server.servlet.context-path"));
        log.debug("  API Base URL: {}", environment.getProperty("api.base-url"));
        log.debug("  API Timeout: {}s", environment.getProperty("api.timeout-seconds"));
    }

    private void debugSessionConfig() {
        log.debug("Session Configuration:");
        log.debug("  Timeout: {} minutes", environment.getProperty("session.timeout-minutes"));
        log.debug("  Cleanup Interval: {} minutes", environment.getProperty("session.cleanup-interval-minutes"));
        log.debug("  Max Concurrent: {}", environment.getProperty("session.max-concurrent"));
    }

    private void debugScoringConfig() {
        log.debug("Scoring Configuration:");
        log.debug("  Interview Threshold: {}%", environment.getProperty("scoring.interview-threshold"));
        log.debug("  Skills Weight: {}", environment.getProperty("scoring.weight-skills"));
        log.debug("  Experience Weight: {}", environment.getProperty("scoring.weight-experience"));
        log.debug("  Tech Stack Weight: {}", environment.getProperty("scoring.weight-tech-stack"));
        log.debug("  Education Weight: {}", environment.getProperty("scoring.weight-education"));
    }

    private void debugEnvironmentVariables() {
        log.debug("Environment Variables Status:");

        String[] criticalVars = {
            "HUGGINGFACE_API_KEY",
            "SPRING_PROFILES_ACTIVE"
        };

        for (String var : criticalVars) {
            String value = System.getenv(var);
            if (value != null) {
                if ("HUGGINGFACE_API_KEY".equals(var)) {
                    log.debug("  {} = <MASKED> (length: {})", var, value.length());
                } else {
                    log.debug("  {} = {}", var, value);
                }
            } else {
                log.debug("  {} = NOT SET (using default)", var);
            }
        }
    }

    /**
     * Can be called programmatically to print current configuration.
     * Useful when debugging issues.
     */
    public void printConfigurationDebug() {
        log.info("Current Configuration Debug:");
        String[] activeProfiles = environment.getActiveProfiles();
        if (activeProfiles.length == 0) {
            activeProfiles = new String[]{"default"};
        }
        log.info("  Active Profiles: {}", String.join(", ", activeProfiles));
        log.info("  Server Port: {}", environment.getProperty("server.port"));
        log.info("  HuggingFace URL: {}", environment.getProperty("huggingface.api.url"));
    }
}
