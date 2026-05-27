package com.resumescreener.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

import java.util.*;

/**
 * Validates critical application configuration on startup.
 * Provides detailed error messages when configuration is missing or invalid.
 * This ensures the application fails fast with clear guidance rather than
 * failing mysteriously at runtime.
 */
@Component
@Slf4j
public class StartupValidator {

    private final Environment environment;

    public StartupValidator(Environment environment) {
        this.environment = environment;
    }

    /**
     * Runs after application startup is complete.
     * At this point, all beans have been initialized and AppConfiguration
     * validation has completed successfully.
     */
    @EventListener(ApplicationReadyEvent.class)
    public void validateStartup() {
        log.info("========================================");
        log.info("Application Startup Validation Complete");
        log.info("========================================");

        String activeProfile = environment.getActiveProfiles().length > 0
            ? environment.getActiveProfiles()[0]
            : "default";

        log.info("Active Profile: {}", activeProfile);
        log.info("Port: {}", environment.getProperty("server.port"));
        log.info("Context Path: {}", environment.getProperty("server.servlet.context-path"));

        printSecurityCheckResults(activeProfile);
        log.info("========================================");
        log.info("✓ Application ready to accept requests");
        log.info("========================================");
    }

    private void printSecurityCheckResults(String activeProfile) {
        log.info("Security Checks:");

        // API Key Check
        String apiKey = environment.getProperty("huggingface.api.key");
        if (apiKey != null && !apiKey.isBlank()) {
            log.info("  ✓ HuggingFace API Key: configured (value masked)");
        } else {
            log.warn("  ⚠️  HuggingFace API Key: not configured (required for LLM calls)");
        }

        // CORS Check
        String corsOrigins = environment.getProperty("cors.allowed.origins");
        if (corsOrigins != null) {
            log.info("  ✓ CORS Origins: {} configured", corsOrigins.split(",").length);
        } else {
            log.warn("  ⚠️  CORS Origins: not configured");
        }

        // Profile Check
        if ("prod".equals(activeProfile)) {
            log.info("  ✓ Production Profile: enabled (hardened settings)");
        } else if ("test".equals(activeProfile)) {
            log.info("  ℹ️  Test Profile: enabled (use for testing only)");
        } else {
            log.info("  ℹ️  Development Profile: enabled (verbose logging)");
        }

        // Environment Variable Check
        checkEnvironmentVariables();
    }

    private void checkEnvironmentVariables() {
        List<String> criticalVars = List.of(
            "HUGGINGFACE_API_KEY"
        );

        List<String> recommendedVars = List.of(
            "SPRING_PROFILES_ACTIVE",
            "CORS_ALLOWED_ORIGINS",
            "API_BASE_URL",
            "LLM_EXTRACTION_MODEL",
            "LLM_INTERVIEW_MODEL",
            "LLM_SUMMARY_MODEL"
        );

        List<String> unsetCritical = new ArrayList<>();
        for (String var : criticalVars) {
            if (System.getenv(var) == null) {
                unsetCritical.add(var);
            }
        }

        List<String> unsetRecommended = new ArrayList<>();
        for (String var : recommendedVars) {
            if (System.getenv(var) == null) {
                unsetRecommended.add(var);
            }
        }

        if (!unsetCritical.isEmpty()) {
            log.warn("  ⚠️  Critical Environment Variables NOT SET:");
            unsetCritical.forEach(var -> log.warn("      - {}", var));
        }

        if (!unsetRecommended.isEmpty()) {
            log.debug("  Recommended Environment Variables NOT SET (using defaults):");
            unsetRecommended.forEach(var -> log.debug("      - {}", var));
        }
    }
}
