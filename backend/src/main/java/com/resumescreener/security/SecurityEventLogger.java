package com.resumescreener.security;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

/**
 * Logs security events safely without exposing sensitive data.
 * Masks PII before logging and maintains an audit trail.
 */
@Component
@Slf4j
public class SecurityEventLogger {

    @Autowired(required = false)
    private PiiDetector piiDetector;

    private static final DateTimeFormatter TIMESTAMP_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    // In-memory audit log (in production, use database)
    private final Queue<SecurityEvent> auditLog = new LinkedList<>();
    private static final int MAX_AUDIT_LOG_SIZE = 10000;

    /**
     * Logs a security event safely.
     * Automatically masks PII in the text parameter.
     */
    public void logSecurityEvent(String sessionId, String eventType, String severity,
                                 String description, String rawText) {
        try {
            // Mask PII in raw text for logging
            String safeText = maskPiiForLogging(rawText);

            SecurityEvent event = SecurityEvent.builder()
                .timestamp(LocalDateTime.now())
                .sessionId(sessionId)
                .eventType(eventType)
                .severity(severity)
                .description(description)
                .safeTextPreview(truncateForLog(safeText, 200))
                .build();

            addToAuditLog(event);

            // Log based on severity
            logBySeverity(event);

        } catch (Exception e) {
            log.error("Error logging security event: {}", e.getMessage());
        }
    }

    /**
     * Logs an attack attempt.
     */
    public void logAttackAttempt(String sessionId, String attackType, String details) {
        logSecurityEvent(sessionId, "ATTACK_DETECTED", "CRITICAL", attackType + ": " + details, "");
    }

    /**
     * Logs a validation warning.
     */
    public void logValidationWarning(String sessionId, String warningType, String details) {
        logSecurityEvent(sessionId, "VALIDATION_WARNING", "MEDIUM", warningType + ": " + details, "");
    }

    /**
     * Gets audit log for a specific session.
     */
    public List<SecurityEvent> getSessionAuditLog(String sessionId) {
        return auditLog.stream()
            .filter(event -> sessionId.equals(event.getSessionId()))
            .toList();
    }

    /**
     * Gets recent security events (for monitoring dashboard).
     */
    public List<SecurityEvent> getRecentSecurityEvents(int limit) {
        return auditLog.stream()
            .sorted((a, b) -> b.getTimestamp().compareTo(a.getTimestamp()))
            .limit(limit)
            .toList();
    }

    /**
     * Gets security statistics.
     */
    public SecurityStatistics getSecurityStatistics() {
        SecurityStatistics stats = new SecurityStatistics();

        auditLog.forEach(event -> {
            stats.incrementEventCount(event.getEventType());
            stats.incrementSeverityCount(event.getSeverity());
        });

        return stats;
    }

    // ========== Private Helper Methods ==========

    private String maskPiiForLogging(String text) {
        if (text == null || text.isBlank()) {
            return "";
        }

        if (piiDetector != null) {
            return piiDetector.maskPii(text);
        }

        // Fallback masking if PiiDetector not available
        return text.replaceAll("\\b[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Z|a-z]{2,}\\b", "[EMAIL]")
                   .replaceAll("\\b(?:\\d{4}[-\\s]?){3}\\d{4}\\b", "[CARD]")
                   .replaceAll("\\b\\d{4}\\s\\d{4}\\s\\d{4}\\b", "[AADHAAR]");
    }

    private String truncateForLog(String text, int maxLength) {
        if (text == null) return "";
        if (text.length() <= maxLength) return text;
        return text.substring(0, maxLength) + "...";
    }

    private void addToAuditLog(SecurityEvent event) {
        auditLog.offer(event);

        // Maintain max size
        while (auditLog.size() > MAX_AUDIT_LOG_SIZE) {
            auditLog.poll();
        }
    }

    private void logBySeverity(SecurityEvent event) {
        switch (event.getSeverity().toUpperCase()) {
            case "CRITICAL" -> log.error(
                "[SECURITY-CRITICAL] {} | Session: {} | Event: {} | {}",
                event.getTimestamp().format(TIMESTAMP_FORMATTER),
                event.getSessionId(),
                event.getEventType(),
                event.getDescription()
            );

            case "HIGH" -> log.warn(
                "[SECURITY-HIGH] {} | Session: {} | Event: {} | {}",
                event.getTimestamp().format(TIMESTAMP_FORMATTER),
                event.getSessionId(),
                event.getEventType(),
                event.getDescription()
            );

            case "MEDIUM" -> log.warn(
                "[SECURITY-MEDIUM] {} | Session: {} | Event: {}",
                event.getTimestamp().format(TIMESTAMP_FORMATTER),
                event.getSessionId(),
                event.getEventType()
            );

            case "INFO" -> log.info(
                "[SECURITY-INFO] {} | Session: {} | Event: {}",
                event.getTimestamp().format(TIMESTAMP_FORMATTER),
                event.getSessionId(),
                event.getEventType()
            );

            default -> log.debug("[SECURITY-{}] {}", event.getSeverity(), event.getDescription());
        }
    }

    /**
     * Security Event DTO
     */
    public static class SecurityEvent {
        private LocalDateTime timestamp;
        private String sessionId;
        private String eventType;
        private String severity; // CRITICAL, HIGH, MEDIUM, INFO
        private String description;
        private String safeTextPreview;

        // Constructor
        public SecurityEvent(LocalDateTime timestamp, String sessionId, String eventType,
                            String severity, String description, String safeTextPreview) {
            this.timestamp = timestamp;
            this.sessionId = sessionId;
            this.eventType = eventType;
            this.severity = severity;
            this.description = description;
            this.safeTextPreview = safeTextPreview;
        }

        // Builder pattern
        public static SecurityEventBuilder builder() {
            return new SecurityEventBuilder();
        }

        public static class SecurityEventBuilder {
            private LocalDateTime timestamp;
            private String sessionId;
            private String eventType;
            private String severity;
            private String description;
            private String safeTextPreview;

            public SecurityEventBuilder timestamp(LocalDateTime timestamp) {
                this.timestamp = timestamp;
                return this;
            }

            public SecurityEventBuilder sessionId(String sessionId) {
                this.sessionId = sessionId;
                return this;
            }

            public SecurityEventBuilder eventType(String eventType) {
                this.eventType = eventType;
                return this;
            }

            public SecurityEventBuilder severity(String severity) {
                this.severity = severity;
                return this;
            }

            public SecurityEventBuilder description(String description) {
                this.description = description;
                return this;
            }

            public SecurityEventBuilder safeTextPreview(String safeTextPreview) {
                this.safeTextPreview = safeTextPreview;
                return this;
            }

            public SecurityEvent build() {
                return new SecurityEvent(timestamp, sessionId, eventType, severity, description, safeTextPreview);
            }
        }

        // Getters
        public LocalDateTime getTimestamp() { return timestamp; }
        public String getSessionId() { return sessionId; }
        public String getEventType() { return eventType; }
        public String getSeverity() { return severity; }
        public String getDescription() { return description; }
        public String getSafeTextPreview() { return safeTextPreview; }
    }

    /**
     * Security statistics for monitoring
     */
    public static class SecurityStatistics {
        private final Map<String, Integer> eventTypeCounts = new HashMap<>();
        private final Map<String, Integer> severityCounts = new HashMap<>();

        public void incrementEventCount(String eventType) {
            eventTypeCounts.put(eventType, eventTypeCounts.getOrDefault(eventType, 0) + 1);
        }

        public void incrementSeverityCount(String severity) {
            severityCounts.put(severity, severityCounts.getOrDefault(severity, 0) + 1);
        }

        public Map<String, Integer> getEventTypeCounts() { return eventTypeCounts; }
        public Map<String, Integer> getSeverityCounts() { return severityCounts; }

        public int getTotalCriticalEvents() {
            return severityCounts.getOrDefault("CRITICAL", 0);
        }

        public int getTotalHighEvents() {
            return severityCounts.getOrDefault("HIGH", 0);
        }

        @Override
        public String toString() {
            return "Critical: " + getTotalCriticalEvents() + ", High: " + getTotalHighEvents() +
                   ", Event Types: " + eventTypeCounts.size();
        }
    }
}
