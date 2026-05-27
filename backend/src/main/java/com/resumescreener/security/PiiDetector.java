package com.resumescreener.security;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * Detects and masks Personally Identifiable Information (PII).
 * Identifies emails, phone numbers, credit cards, API keys, Aadhaar, etc.
 */
@Component
@Slf4j
public class PiiDetector {

    // PII Detection Patterns
    private static final Map<String, Pattern> PII_PATTERNS = new LinkedHashMap<>();

    static {
        // Email addresses
        PII_PATTERNS.put("EMAIL", Pattern.compile(
            "\\b[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Z|a-z]{2,}\\b"
        ));

        // Phone numbers (various formats)
        PII_PATTERNS.put("PHONE", Pattern.compile(
            "(?:\\+?91[-.]?)?\\(?\\d{3}\\)?[-.]?\\d{3}[-.]?\\d{4}\\b|" +  // Indian format
            "\\b(?:\\+?1[-.]?)?(?:\\(\\d{3}\\)|\\d{3})[-.]?\\d{3}[-.]?\\d{4}\\b"  // US format
        ));

        // Credit card numbers (16 digits, Visa/Mastercard/Amex patterns)
        PII_PATTERNS.put("CREDIT_CARD", Pattern.compile(
            "\\b(?:\\d{4}[-\\s]?){3}\\d{4}\\b|\\b\\d{15}\\b|\\b\\d{16}\\b"
        ));

        // Aadhaar numbers (12 digits, Indian ID)
        PII_PATTERNS.put("AADHAAR", Pattern.compile(
            "\\b\\d{4}\\s\\d{4}\\s\\d{4}\\b|\\b\\d{12}\\b"
        ));

        // API Keys and tokens (common patterns)
        PII_PATTERNS.put("API_KEY", Pattern.compile(
            "(?:api[_-]?key|apikey|access[_-]?token|bearer|token)[\\s]*[=:][\\s]*['\"]?([a-zA-Z0-9_\\-]{20,})['\"]?",
            Pattern.CASE_INSENSITIVE
        ));

        // AWS Keys
        PII_PATTERNS.put("AWS_KEY", Pattern.compile(
            "(?:AKIA|AIDA)[0-9A-Z]{16}"
        ));

        // JWT Tokens
        PII_PATTERNS.put("JWT_TOKEN", Pattern.compile(
            "eyJ[A-Za-z0-9_-]+\\.eyJ[A-Za-z0-9_-]+\\.[A-Za-z0-9_-]+"
        ));

        // Social Security Numbers (SSN format)
        PII_PATTERNS.put("SSN", Pattern.compile(
            "\\b(?!000|666|9\\d{2})\\d{3}[-]?(?!00)\\d{2}[-]?(?!0000)\\d{4}\\b"
        ));

        // PAN (Indian tax ID)
        PII_PATTERNS.put("PAN", Pattern.compile(
            "\\b[A-Z]{5}[0-9]{4}[A-Z]{1}\\b"
        ));

        // URLs with credentials
        PII_PATTERNS.put("URL_WITH_CREDS", Pattern.compile(
            "https?://[^:]+:[^@]+@[^/]+"
        ));

        // GitHub tokens
        PII_PATTERNS.put("GITHUB_TOKEN", Pattern.compile(
            "ghp_[0-9a-zA-Z]{36}|gho_[0-9a-zA-Z]{36}|ghu_[0-9a-zA-Z]{36}"
        ));

        // Database connection strings
        PII_PATTERNS.put("DB_CONNECTION", Pattern.compile(
            "(?:mongodb|mysql|postgresql|sql)://[^:]+:[^@]+@[^/]+"
        ));
    }

    /**
     * Analyzes text for PII presence.
     */
    public PiiDetectionReport analyzeForPii(String text) {
        if (text == null || text.isBlank()) {
            return PiiDetectionReport.safe();
        }

        PiiDetectionReport report = new PiiDetectionReport();

        for (Map.Entry<String, Pattern> entry : PII_PATTERNS.entrySet()) {
            String piiType = entry.getKey();
            Pattern pattern = entry.getValue();
            Matcher matcher = pattern.matcher(text);

            List<String> matches = new ArrayList<>();
            while (matcher.find()) {
                // Extract match, limit length to prevent logging huge strings
                String match = matcher.group();
                String truncated = match.length() > 50 ? match.substring(0, 47) + "..." : match;
                matches.add(truncated);
            }

            if (!matches.isEmpty()) {
                report.addDetection(piiType, matches.size(), matches);
            }
        }

        return report;
    }

    /**
     * Masks all detected PII in the text.
     */
    public String maskPii(String text) {
        if (text == null) return null;

        String masked = text;

        for (Map.Entry<String, Pattern> entry : PII_PATTERNS.entrySet()) {
            String piiType = entry.getKey();
            Pattern pattern = entry.getValue();
            masked = maskByType(masked, pattern, piiType);
        }

        return masked;
    }

    /**
     * Masks PII by type.
     */
    private String maskByType(String text, Pattern pattern, String piiType) {
        return pattern.matcher(text).replaceAll(matcher -> {
            String match = matcher.group();
            return generateMask(piiType, match.length());
        });
    }

    /**
     * Generates appropriate mask for PII type.
     */
    private String generateMask(String piiType, int originalLength) {
        return switch (piiType) {
            case "EMAIL" -> maskEmail();
            case "PHONE" -> "[PHONE]";
            case "CREDIT_CARD" -> maskCreditCard();
            case "AADHAAR" -> "[AADHAAR]";
            case "SSN" -> "[SSN]";
            case "PAN" -> "[PAN]";
            case "API_KEY", "AWS_KEY", "JWT_TOKEN" -> "[SECRET_KEY]";
            case "GITHUB_TOKEN" -> "[GITHUB_TOKEN]";
            case "URL_WITH_CREDS" -> "[URL_WITH_CREDENTIALS]";
            case "DB_CONNECTION" -> "[DB_CONNECTION_STRING]";
            default -> "[REDACTED]";
        };
    }

    private String maskEmail() {
        return "[EMAIL]";
    }

    private String maskCreditCard() {
        // Show last 4 digits
        return "[CARD_****]";
    }

    /**
     * Masks sensitive parts but keeps some info for validation.
     */
    public String maskSensitivePartial(String text) {
        if (text == null) return null;

        // More conservative masking - only hide most sensitive info
        String masked = text;

        // Mask API keys and tokens
        masked = masked.replaceAll("(?i)(api[_-]?key|token|password)[\\s]*[=:][\\s]*['\"]?[a-zA-Z0-9_\\-]{20,}['\"]?",
            "$1=[SECRET]");

        // Mask credit cards
        masked = masked.replaceAll("\\b(?:\\d{4}[-\\s]?){3}\\d{4}\\b", "[CARD_****]");

        // Mask email domains (show local part)
        masked = masked.replaceAll("([a-zA-Z0-9._%+-]+)@[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,}",
            "$1@[DOMAIN]");

        return masked;
    }

    /**
     * PII Detection Report
     */
    public static class PiiDetectionReport {
        private boolean containsPii = false;
        private Map<String, Integer> piiTypeCounts = new LinkedHashMap<>();
        private Map<String, List<String>> piiSamples = new LinkedHashMap<>();

        public void addDetection(String piiType, int count, List<String> samples) {
            this.containsPii = true;
            this.piiTypeCounts.put(piiType, count);
            this.piiSamples.put(piiType, samples);
        }

        public static PiiDetectionReport safe() {
            return new PiiDetectionReport();
        }

        public boolean containsPii() { return containsPii; }
        public Map<String, Integer> getPiiTypeCounts() { return piiTypeCounts; }
        public String getSummary() {
            if (!containsPii) return "No PII detected";
            return String.format("Detected %d PII types: %s",
                piiTypeCounts.size(),
                piiTypeCounts.keySet().stream()
                    .map(type -> type + "(" + piiTypeCounts.get(type) + ")")
                    .collect(Collectors.joining(", ")));
        }

        public int getTotalPiiCount() {
            return piiTypeCounts.values().stream().mapToInt(Integer::intValue).sum();
        }
    }
}
