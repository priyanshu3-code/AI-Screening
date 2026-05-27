package com.resumescreener.ai.service;

import com.resumescreener.ai.inference.HuggingFaceInferenceClient;
import com.resumescreener.ai.inference.HuggingFaceInferenceConfig;
import com.resumescreener.model.ExtractedSkills;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Service for extracting technical and soft skills from resume text.
 * Uses token classification or keyword matching with confidence scores.
 *
 * Falls back gracefully to keyword matching if model inference fails.
 */
@Service
@Slf4j
public class SkillExtractionService {

    private final HuggingFaceInferenceClient inferenceClient;
    private final HuggingFaceInferenceConfig config;

    // Common technical skills database
    private static final Map<String, String> TECHNICAL_SKILLS = Map.ofEntries(
        // Programming Languages
        Map.entry("java", "programming_language"),
        Map.entry("python", "programming_language"),
        Map.entry("javascript", "programming_language"),
        Map.entry("typescript", "programming_language"),
        Map.entry("go", "programming_language"),
        Map.entry("rust", "programming_language"),
        Map.entry("c++", "programming_language"),
        Map.entry("c#", "programming_language"),
        Map.entry("kotlin", "programming_language"),
        Map.entry("scala", "programming_language"),

        // Frameworks & Libraries
        Map.entry("spring boot", "framework"),
        Map.entry("spring", "framework"),
        Map.entry("react", "framework"),
        Map.entry("angular", "framework"),
        Map.entry("vue", "framework"),
        Map.entry("django", "framework"),
        Map.entry("flask", "framework"),
        Map.entry("fastapi", "framework"),
        Map.entry("express", "framework"),
        Map.entry("nest.js", "framework"),

        // Databases
        Map.entry("postgresql", "database"),
        Map.entry("mysql", "database"),
        Map.entry("mongodb", "database"),
        Map.entry("redis", "database"),
        Map.entry("cassandra", "database"),
        Map.entry("elasticsearch", "database"),
        Map.entry("dynamodb", "database"),

        // Infrastructure & Cloud
        Map.entry("docker", "infrastructure"),
        Map.entry("kubernetes", "infrastructure"),
        Map.entry("aws", "cloud"),
        Map.entry("azure", "cloud"),
        Map.entry("gcp", "cloud"),
        Map.entry("terraform", "infrastructure"),
        Map.entry("jenkins", "cicd"),
        Map.entry("gitlab-ci", "cicd"),
        Map.entry("github-actions", "cicd"),

        // Data & ML
        Map.entry("tensorflow", "ml"),
        Map.entry("pytorch", "ml"),
        Map.entry("pandas", "data_science"),
        Map.entry("numpy", "data_science"),
        Map.entry("scikit-learn", "ml"),
        Map.entry("spark", "big_data"),
        Map.entry("hadoop", "big_data")
    );

    // Common soft skills
    private static final Set<String> SOFT_SKILLS = Set.of(
        "leadership", "communication", "teamwork", "problem solving", "analytical thinking",
        "project management", "agile", "scrum", "negotiation", "time management",
        "critical thinking", "creativity", "adaptability", "collaboration", "mentoring"
    );

    @Autowired
    public SkillExtractionService(HuggingFaceInferenceClient inferenceClient,
                                 HuggingFaceInferenceConfig config) {
        this.inferenceClient = inferenceClient;
        this.config = config;
    }

    /**
     * Extract skills from resume text.
     * @param resumeText Raw resume text
     * @param sessionId Session ID for logging
     * @return ExtractedSkills object with technical, soft skills, and certifications
     */
    public ExtractedSkills extractSkills(String resumeText, String sessionId) {
        long startTime = System.currentTimeMillis();

        if (!config.getSkillExtraction().isEnabled()) {
            log.debug("[{}] Skill extraction is disabled in config", sessionId);
            return new ExtractedSkills();
        }

        if (resumeText == null || resumeText.trim().isEmpty()) {
            log.warn("[{}] Resume text is empty, cannot extract skills", sessionId);
            return new ExtractedSkills();
        }

        try {
            String modelName = config.getSkillExtraction().getModel();
            log.debug("[{}] Starting skill extraction using model: {}", sessionId, modelName);

            Map<String, Object> result = inferenceClient.extractSkills(resumeText, modelName);

            if (result == null) {
                return performKeywordMatching(resumeText, sessionId, startTime, "inference_disabled");
            }

            boolean isFallback = (boolean) result.getOrDefault("fallback", false);

            if (isFallback) {
                log.debug("[{}] Inference returned fallback, using keyword matching", sessionId);
                return performKeywordMatching(resumeText, sessionId, startTime, "inference_failed");
            }

            @SuppressWarnings("unchecked")
            List<Map<String, Object>> skillsList = (List<Map<String, Object>>) result.get("skills");
            ExtractedSkills skills = buildExtractedSkills(skillsList, modelName, false);

            long totalTime = System.currentTimeMillis() - startTime;
            log.info("[{}] Skill extraction completed in {}ms, found {} total skills",
                sessionId, totalTime, skills.getTotalSkillCount());

            return skills;

        } catch (Exception e) {
            log.error("[{}] Error during skill extraction: {}", sessionId, e.getMessage(), e);
            return performKeywordMatching(resumeText, sessionId, startTime, e.getClass().getSimpleName());
        }
    }

    /**
     * Fallback to keyword matching when inference fails.
     */
    private ExtractedSkills performKeywordMatching(String resumeText, String sessionId,
                                                  long startTime, String reason) {
        String textLower = resumeText.toLowerCase();
        List<ExtractedSkills.Skill> technicalSkills = new ArrayList<>();
        List<ExtractedSkills.Skill> softSkills = new ArrayList<>();
        Map<String, Integer> skillFrequency = new HashMap<>();

        // Count skill mentions
        for (String skill : TECHNICAL_SKILLS.keySet()) {
            int count = countOccurrences(textLower, skill);
            if (count > 0) {
                skillFrequency.put(skill, count);
                technicalSkills.add(new ExtractedSkills.Skill(
                    skill,
                    0.8,  // confidence
                    TECHNICAL_SKILLS.get(skill),
                    count,
                    inferExperienceLevel(count)
                ));
            }
        }

        for (String skill : SOFT_SKILLS) {
            int count = countOccurrences(textLower, skill);
            if (count > 0) {
                skillFrequency.put(skill, count);
                softSkills.add(new ExtractedSkills.Skill(
                    skill,
                    0.7,
                    "soft_skill",
                    count,
                    inferExperienceLevel(count)
                ));
            }
        }

        // Sort by frequency
        technicalSkills.sort((a, b) -> Integer.compare(b.getFrequency(), a.getFrequency()));
        softSkills.sort((a, b) -> Integer.compare(b.getFrequency(), a.getFrequency()));

        ExtractedSkills result = new ExtractedSkills();
        result.setTechnicalSkills(technicalSkills);
        result.setSoftSkills(softSkills);
        result.setCertifications(new ArrayList<>());
        result.setLanguages(extractLanguages(textLower));
        result.setTotalSkillCount(technicalSkills.size() + softSkills.size());
        result.setAverageConfidence(0.75);
        result.setExtractionMethod("keyword_matching");
        result.setModelName("keyword_matching");
        result.setWasFallback(true);

        long duration = System.currentTimeMillis() - startTime;
        log.warn("[{}] Using keyword matching for skill extraction (reason: {}, duration: {}ms, skills: {})",
            sessionId, reason, duration, result.getTotalSkillCount());

        return result;
    }

    /**
     * Build ExtractedSkills from inference results.
     */
    private ExtractedSkills buildExtractedSkills(List<Map<String, Object>> skillsList,
                                                 String modelName, boolean wasFallback) {
        List<ExtractedSkills.Skill> technicalSkills = new ArrayList<>();
        List<ExtractedSkills.Skill> softSkills = new ArrayList<>();

        double totalConfidence = 0;
        int count = 0;

        for (Map<String, Object> skill : skillsList) {
            String name = (String) skill.get("name");
            String category = (String) skill.getOrDefault("category", "other");
            double confidence = ((Number) skill.getOrDefault("confidence", 0.8)).doubleValue();

            ExtractedSkills.Skill skillObj = new ExtractedSkills.Skill(
                name,
                confidence,
                category,
                1,
                "intermediate"
            );

            if ("soft_skill".equals(category)) {
                softSkills.add(skillObj);
            } else {
                technicalSkills.add(skillObj);
            }

            totalConfidence += confidence;
            count++;
        }

        ExtractedSkills result = new ExtractedSkills();
        result.setTechnicalSkills(technicalSkills);
        result.setSoftSkills(softSkills);
        result.setCertifications(new ArrayList<>());
        result.setLanguages(new ArrayList<>());
        result.setTotalSkillCount(count);
        result.setAverageConfidence(count > 0 ? totalConfidence / count : 0);
        result.setExtractionMethod("token_classification");
        result.setModelName(modelName);
        result.setWasFallback(wasFallback);

        return result;
    }

    /**
     * Extract language skills from resume.
     */
    private List<ExtractedSkills.Skill> extractLanguages(String textLower) {
        Map<String, String> languages = Map.ofEntries(
            Map.entry("english", "language"),
            Map.entry("spanish", "language"),
            Map.entry("french", "language"),
            Map.entry("german", "language"),
            Map.entry("chinese", "language"),
            Map.entry("japanese", "language"),
            Map.entry("portuguese", "language"),
            Map.entry("russian", "language")
        );

        List<ExtractedSkills.Skill> result = new ArrayList<>();
        for (String lang : languages.keySet()) {
            if (textLower.contains(lang)) {
                result.add(new ExtractedSkills.Skill(lang, 0.8, "language", 1, "intermediate"));
            }
        }
        return result;
    }

    /**
     * Count occurrences of a substring (case-insensitive).
     */
    private int countOccurrences(String text, String substring) {
        int count = 0;
        int index = 0;
        while ((index = text.indexOf(substring, index)) != -1) {
            count++;
            index += substring.length();
        }
        return count;
    }

    /**
     * Infer experience level based on frequency.
     */
    private String inferExperienceLevel(int frequency) {
        if (frequency >= 5) return "expert";
        if (frequency >= 3) return "advanced";
        if (frequency >= 2) return "intermediate";
        return "beginner";
    }

    /**
     * Check if skill extraction service is available.
     */
    public boolean isAvailable() {
        return config.getSkillExtraction().isEnabled();
    }
}
