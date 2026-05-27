package com.resumescreener.security;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.*;

/**
 * Comprehensive safety validation service that orchestrates all security checks.
 * Validates resume and job description before AI processing.
 */
@Service
@Slf4j
public class SafetyValidationService {

    @Autowired
    private PromptInjectionDetector injectionDetector;

    @Autowired
    private PiiDetector piiDetector;

    @Autowired
    private SecurityEventLogger eventLogger;

    // Configuration for safety thresholds
    private static final String BLOCK_INJECTION_SEVERITY = "CRITICAL"; // Block CRITICAL level
    private static final String FLAG_INJECTION_SEVERITY = "HIGH";      // Flag HIGH level
    private static final boolean BLOCK_ON_PII = false;                 // Don't block, just warn
    private static final int MAX_INJECTION_DETECTIONS = 3;             // Block if 3+ injections detected

    /**
     * Validates user input (resume text) for safety.
     */
    public SafetyValidationResult validateResume(String resumeText, String sessionId) {
        SafetyValidationResult result = new SafetyValidationResult();

        if (resumeText == null || resumeText.isBlank()) {
            result.addWarning("Resume text is empty");
            return result;
        }

        // Check for prompt injection in resume (rare but possible)
        PromptInjectionDetector.PromptInjectionReport injectionReport =
            injectionDetector.analyzeInput(resumeText);

        if (!injectionReport.isSafe()) {
            result.addInjectionDetection(injectionReport);

            if ("CRITICAL".equals(injectionReport.getSeverity())) {
                result.block("Resume contains suspicious content that cannot be processed safely");
                eventLogger.logSecurityEvent(
                    sessionId,
                    "RESUME_INJECTION_DETECTED",
                    injectionReport.getSeverity(),
                    injectionReport.getSummary(),
                    resumeText
                );
                return result;
            } else if ("HIGH".equals(injectionReport.getSeverity())) {
                result.addWarning("Resume contains suspicious patterns. Processing with caution.");
                eventLogger.logSecurityEvent(
                    sessionId,
                    "RESUME_INJECTION_FLAGGED",
                    "HIGH",
                    injectionReport.getSummary(),
                    resumeText
                );
            }
        }

        // Check for PII (should be in resume, but log if present)
        PiiDetector.PiiDetectionReport piiReport = piiDetector.analyzeForPii(resumeText);
        if (piiReport.containsPii()) {
            result.addPiiDetection(piiReport);
            eventLogger.logSecurityEvent(
                sessionId,
                "RESUME_PII_DETECTED",
                "INFO",
                piiReport.getSummary(),
                resumeText
            );
        }

        // Check resume length
        if (resumeText.length() > 100000) {
            result.block("Resume exceeds maximum length (100,000 characters)");
            return result;
        }

        if (resumeText.length() < 100) {
            result.addWarning("Resume is very short - may not contain sufficient information for analysis");
        }

        return result;
    }

    /**
     * Validates job description for safety.
     * Job description should NOT contain PII or injections.
     */
    public SafetyValidationResult validateJobDescription(String jobDescription, String sessionId) {
        SafetyValidationResult result = new SafetyValidationResult();

        if (jobDescription == null || jobDescription.isBlank()) {
            result.block("Job description is required");
            return result;
        }

        // Check for prompt injection (job description shouldn't have injections)
        PromptInjectionDetector.PromptInjectionReport injectionReport =
            injectionDetector.analyzeInput(jobDescription);

        if (!injectionReport.isSafe()) {
            result.addInjectionDetection(injectionReport);

            // More strict for job description
            if (injectionReport.getDetectionTypes().size() >= 2) {
                result.block("Job description contains suspicious content. Please provide clean job description.");
                eventLogger.logSecurityEvent(
                    sessionId,
                    "JD_INJECTION_DETECTED",
                    injectionReport.getSeverity(),
                    injectionReport.getSummary(),
                    jobDescription
                );
                return result;
            } else {
                result.addWarning("Job description contains some suspicious patterns.");
                eventLogger.logSecurityEvent(
                    sessionId,
                    "JD_INJECTION_FLAGGED",
                    injectionReport.getSeverity(),
                    injectionReport.getSummary(),
                    jobDescription
                );
            }
        }

        // Check for PII (job description shouldn't have PII)
        PiiDetector.PiiDetectionReport piiReport = piiDetector.analyzeForPii(jobDescription);
        if (piiReport.containsPii()) {
            result.addPiiDetection(piiReport);
            result.addWarning("Job description contains personally identifiable information. Please review for sensitive data.");
            eventLogger.logSecurityEvent(
                sessionId,
                "JD_PII_DETECTED",
                "WARNING",
                piiReport.getSummary(),
                jobDescription
            );
        }

        // Check job description length
        if (jobDescription.length() < 50) {
            result.block("Job description is too short (minimum 50 characters)");
            return result;
        }

        if (jobDescription.length() > 50000) {
            result.block("Job description exceeds maximum length (50,000 characters)");
            return result;
        }

        return result;
    }

    /**
     * Validates combined input before AI processing.
     */
    public SafetyValidationResult validateForAiProcessing(String resume, String jobDescription, String sessionId) {
        SafetyValidationResult result = new SafetyValidationResult();

        // Validate both inputs
        SafetyValidationResult resumeResult = validateResume(resume, sessionId);
        SafetyValidationResult jdResult = validateJobDescription(jobDescription, sessionId);

        // Merge results
        result.merge(resumeResult);
        result.merge(jdResult);

        // Additional combined validation
        String combined = resume + " " + jobDescription;

        // Check for pattern-based attacks across both inputs
        PromptInjectionDetector.PromptInjectionReport combinedReport =
            injectionDetector.analyzeInput(combined);

        if (combinedReport.getDetectionTypes().size() > MAX_INJECTION_DETECTIONS) {
            result.block("Combined input contains too many suspicious patterns");
            eventLogger.logSecurityEvent(
                sessionId,
                "COMBINED_INJECTION_DETECTED",
                "CRITICAL",
                "Multiple injection attempts detected across inputs",
                combined
            );
        }

        return result;
    }

    /**
     * Gets safe version of text for logging (with PII masked).
     */
    public String getSafeTextForLogging(String text) {
        if (text == null) return null;
        return piiDetector.maskPii(text);
    }

    /**
     * Safety Validation Result class
     */
    public static class SafetyValidationResult {
        private boolean blocked = false;
        private String blockReason = null;
        private List<String> warnings = new ArrayList<>();
        private PromptInjectionDetector.PromptInjectionReport injectionReport;
        private PiiDetector.PiiDetectionReport piiReport;

        public void block(String reason) {
            this.blocked = true;
            this.blockReason = reason;
        }

        public void addWarning(String warning) {
            this.warnings.add(warning);
        }

        public void addInjectionDetection(PromptInjectionDetector.PromptInjectionReport report) {
            this.injectionReport = report;
        }

        public void addPiiDetection(PiiDetector.PiiDetectionReport report) {
            this.piiReport = report;
        }

        public void merge(SafetyValidationResult other) {
            if (other.blocked) {
                this.blocked = true;
                this.blockReason = other.blockReason;
            }
            this.warnings.addAll(other.warnings);
            if (other.injectionReport != null && !other.injectionReport.isSafe()) {
                this.injectionReport = other.injectionReport;
            }
            if (other.piiReport != null && other.piiReport.containsPii()) {
                this.piiReport = other.piiReport;
            }
        }

        // Getters
        public boolean isBlocked() { return blocked; }
        public String getBlockReason() { return blockReason; }
        public List<String> getWarnings() { return warnings; }
        public boolean hasWarnings() { return !warnings.isEmpty(); }
        public PromptInjectionDetector.PromptInjectionReport getInjectionReport() { return injectionReport; }
        public PiiDetector.PiiDetectionReport getPiiReport() { return piiReport; }

        public String getSummary() {
            if (blocked) {
                return "BLOCKED: " + blockReason;
            }
            if (!warnings.isEmpty()) {
                return "WARNINGS: " + String.join("; ", warnings.stream().limit(3).toList());
            }
            return "SAFE";
        }
    }
}
