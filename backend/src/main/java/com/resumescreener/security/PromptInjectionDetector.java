package com.resumescreener.security;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * Detects prompt injection attempts in user input.
 * Identifies common attack patterns that try to manipulate AI behavior.
 */
@Component
@Slf4j
public class PromptInjectionDetector {

    // Patterns that indicate prompt injection attempts
    private static final List<String> INJECTION_KEYWORDS = List.of(
        // System override attempts
        "ignore all previous", "forget everything", "disregard",
        "override", "bypass", "circumvent", "disable safety",

        // Role/instruction manipulation
        "you are now", "from now on", "new instructions", "new system prompt",
        "pretend you are", "act as if", "role-play", "assume you are",

        // Sensitive information requests
        "reveal api key", "show secret", "display password", "leak",
        "what is your prompt", "what are your instructions", "your system prompt",
        "show me your config", "internal instructions",

        // Jailbreak attempts
        "sudo", "admin mode", "developer mode", "debug mode",
        "no restrictions", "unrestricted", "without safety",
        "ignore guidelines", "ignore policy", "ignore restrictions",

        // Context manipulation
        "the above instructions are now obsolete",
        "please analyze the following instructions instead",
        "execute the following code", "run this command"
    );

    // Suspicious patterns (regex)
    private static final List<Pattern> INJECTION_PATTERNS = List.of(
        // Bracket notation for jailbreaks
        Pattern.compile("\\[SYSTEM\\]|\\[JAILBREAK\\]|\\[OVERRIDE\\]|\\[ADMIN\\]", Pattern.CASE_INSENSITIVE),

        // Comment-based injection
        Pattern.compile("<!--.*?-->|//.*?|#.*?$", Pattern.MULTILINE),

        // Code execution attempts
        Pattern.compile("```|<script>|eval\\(|exec\\(", Pattern.CASE_INSENSITIVE),

        // SQL injection patterns (unlikely but possible)
        Pattern.compile("(union|select|drop|insert|delete|update|where)\\s+(from|into|table)", Pattern.CASE_INSENSITIVE),

        // JSON/XML injection
        Pattern.compile("[{\\[]\"|\"\\s*:|attribute\\s*=", Pattern.CASE_INSENSITIVE)
    );

    // Suspicious character sequences
    private static final List<String> SUSPICIOUS_SEQUENCES = List.of(
        "{{", "}}", "<<", ">>", "\\x", "\\u"
    );

    // Suspicious prompt structure patterns
    private static final Pattern MULTI_INSTRUCTION_PATTERN =
        Pattern.compile("\\n\\n[A-Z][A-Z\\s]{2,}:\\s*", Pattern.MULTILINE);

    /**
     * Analyzes input for prompt injection attempts.
     * Returns a report of detected risks.
     */
    public PromptInjectionReport analyzeInput(String input) {
        if (input == null || input.isBlank()) {
            return PromptInjectionReport.safe();
        }

        PromptInjectionReport report = new PromptInjectionReport();
        String lowerInput = input.toLowerCase();

        // Check for injection keywords
        List<String> detectedKeywords = INJECTION_KEYWORDS.stream()
            .filter(keyword -> containsWordBoundary(lowerInput, keyword))
            .collect(Collectors.toList());

        if (!detectedKeywords.isEmpty()) {
            report.addDetection("KEYWORD_INJECTION",
                "Detected " + detectedKeywords.size() + " suspicious keywords: " +
                String.join(", ", detectedKeywords.stream().limit(5).collect(Collectors.toList())));
        }

        // Check for pattern matches
        List<String> detectedPatterns = INJECTION_PATTERNS.stream()
            .filter(pattern -> pattern.matcher(input).find())
            .map(Pattern::pattern)
            .limit(5)
            .collect(Collectors.toList());

        if (!detectedPatterns.isEmpty()) {
            report.addDetection("PATTERN_INJECTION",
                "Detected suspicious patterns in content");
        }

        // Check for suspicious character sequences
        List<String> detectedSequences = SUSPICIOUS_SEQUENCES.stream()
            .filter(input::contains)
            .collect(Collectors.toList());

        if (!detectedSequences.isEmpty()) {
            report.addDetection("SUSPICIOUS_CHARACTERS",
                "Detected " + detectedSequences.size() + " suspicious character sequences");
        }

        // Check for multi-instruction structure
        if (MULTI_INSTRUCTION_PATTERN.matcher(input).find()) {
            report.addDetection("MULTI_INSTRUCTION",
                "Detected potential multiple instructions structure");
        }

        // Check for excessive special characters (possible encoding attack)
        long specialCharCount = input.chars()
            .filter(c -> !Character.isLetterOrDigit(c) && !Character.isWhitespace(c))
            .count();

        if (specialCharCount > input.length() * 0.3) {
            report.addDetection("HIGH_SPECIAL_CHARS",
                "High proportion of special characters detected");
        }

        // Check input length (extremely long inputs could be attacks)
        if (input.length() > 50000) {
            report.addDetection("EXCESSIVE_LENGTH",
                "Input exceeds recommended length (50000 chars)");
        }

        // Set severity based on number of detections
        report.calculateSeverity();

        return report;
    }

    /**
     * Check if a word exists with word boundaries (not as substring).
     */
    private boolean containsWordBoundary(String text, String word) {
        String pattern = "(?:^|\\s|[^a-z])" + Pattern.quote(word) + "(?:$|\\s|[^a-z])";
        return Pattern.compile(pattern, Pattern.CASE_INSENSITIVE).matcher(text).find();
    }

    /**
     * Report class for prompt injection analysis results.
     */
    public static class PromptInjectionReport {
        private boolean safe = true;
        private String severity = "NONE"; // NONE, LOW, MEDIUM, HIGH, CRITICAL
        private List<String> detectionTypes = new ArrayList<>();
        private List<String> details = new ArrayList<>();

        public void addDetection(String type, String detail) {
            this.safe = false;
            this.detectionTypes.add(type);
            this.details.add(detail);
        }

        public void calculateSeverity() {
            if (safe) {
                this.severity = "NONE";
                return;
            }

            // Determine severity based on detection types
            boolean hasCritical = detectionTypes.contains("MULTI_INSTRUCTION") ||
                                 detectionTypes.contains("PATTERN_INJECTION");
            boolean hasHigh = detectionTypes.contains("KEYWORD_INJECTION") ||
                             detectionTypes.contains("EXCESSIVE_LENGTH");
            boolean hasMedium = detectionTypes.contains("SUSPICIOUS_CHARACTERS") ||
                               detectionTypes.contains("HIGH_SPECIAL_CHARS");

            if (hasCritical) {
                this.severity = "CRITICAL";
            } else if (hasHigh) {
                this.severity = "HIGH";
            } else if (hasMedium) {
                this.severity = "MEDIUM";
            } else {
                this.severity = "LOW";
            }
        }

        public static PromptInjectionReport safe() {
            return new PromptInjectionReport();
        }

        // Getters
        public boolean isSafe() { return safe; }
        public String getSeverity() { return severity; }
        public List<String> getDetectionTypes() { return detectionTypes; }
        public List<String> getDetails() { return details; }
        public String getSummary() {
            return String.format("Severity: %s, Detections: %d", severity, detectionTypes.size());
        }
    }
}
