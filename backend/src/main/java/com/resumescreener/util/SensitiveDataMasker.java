package com.resumescreener.util;

import java.util.regex.Pattern;

public class SensitiveDataMasker {

    private static final Pattern EMAIL_PATTERN =
        Pattern.compile("\\b[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Z|a-z]{2,}\\b");

    private static final Pattern PHONE_PATTERN =
        Pattern.compile("(?:\\+?1[-.]?)?\\(?([0-9]{3})\\)?[-.]?([0-9]{3})[-.]?([0-9]{4})\\b");

    private static final Pattern LINKEDIN_PATTERN =
        Pattern.compile("(?:https?://)?(?:www\\.)?linkedin\\.com/in/[A-Za-z0-9-]+");

    private static final Pattern GITHUB_PATTERN =
        Pattern.compile("(?:https?://)?(?:www\\.)?github\\.com/[A-Za-z0-9-]+");

    private static final Pattern PERSONAL_WEBSITE_PATTERN =
        Pattern.compile("(?:https?://)?(?:www\\.)?[A-Za-z0-9]+-?[A-Za-z0-9]*\\.[A-Za-z]{2,}");

    public static String maskSensitiveData(String text) {
        if (text == null || text.isEmpty()) {
            return text;
        }

        String masked = text;

        masked = maskEmails(masked);
        masked = maskPhoneNumbers(masked);
        masked = maskLinkedIn(masked);
        masked = maskGitHub(masked);

        return masked;
    }

    private static String maskEmails(String text) {
        return EMAIL_PATTERN.matcher(text).replaceAll("[EMAIL_REDACTED]");
    }

    private static String maskPhoneNumbers(String text) {
        return PHONE_PATTERN.matcher(text).replaceAll("[PHONE_REDACTED]");
    }

    private static String maskLinkedIn(String text) {
        return LINKEDIN_PATTERN.matcher(text).replaceAll("[LINKEDIN_REDACTED]");
    }

    private static String maskGitHub(String text) {
        return GITHUB_PATTERN.matcher(text).replaceAll("[GITHUB_REDACTED]");
    }

    public static String maskResumeName(String resumeName) {
        if (resumeName == null || resumeName.isEmpty()) {
            return "[NAME_REDACTED]";
        }

        int lastDotIndex = resumeName.lastIndexOf('.');
        if (lastDotIndex > 0) {
            String extension = resumeName.substring(lastDotIndex);
            return "[RESUME_REDACTED]" + extension;
        }
        return "[RESUME_REDACTED]";
    }
}
