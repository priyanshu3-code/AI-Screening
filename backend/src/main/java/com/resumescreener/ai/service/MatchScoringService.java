package com.resumescreener.ai.service;

import com.resumescreener.ai.inference.HuggingFaceInferenceClient;
import com.resumescreener.ai.inference.HuggingFaceInferenceConfig;
import com.resumescreener.model.MatchScore;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.regex.Pattern;

/**
 * Service for calculating semantic similarity between resume and job description.
 * Uses sentence-transformers for deep semantic matching.
 *
 * Provides detailed match score breakdown across skills, experience, tech stack, and education.
 * Complements the LLM-based match score with objective metrics.
 */
@Slf4j
@Service
public class MatchScoringService {

    private final HuggingFaceInferenceClient inferenceClient;
    private final HuggingFaceInferenceConfig config;
    private final SkillExtractionService skillExtractionService;

    @Autowired
    public MatchScoringService(HuggingFaceInferenceClient inferenceClient,
                              HuggingFaceInferenceConfig config,
                              SkillExtractionService skillExtractionService) {
        this.inferenceClient = inferenceClient;
        this.config = config;
        this.skillExtractionService = skillExtractionService;
    }

    /**
     * Calculate comprehensive match score between resume and job description.
     * @param resumeText Resume text
     * @param jobDescription Job description text
     * @param sessionId Session ID for logging
     * @return MatchScore with breakdown across multiple dimensions
     */
    public MatchScore calculateMatchScore(String resumeText, String jobDescription, String sessionId) {
        long startTime = System.currentTimeMillis();

        if (!config.getMatchScoring().isEnabled()) {
            log.debug("[{}] Match scoring is disabled in config", sessionId);
            return createDefaultMatchScore(true);
        }

        if (resumeText == null || jobDescription == null ||
            resumeText.trim().isEmpty() || jobDescription.trim().isEmpty()) {
            log.warn("[{}] Resume or job description is empty", sessionId);
            return createDefaultMatchScore(true);
        }

        try {
            String modelName = config.getMatchScoring().getModel();
            log.debug("[{}] Starting match score calculation using model: {}", sessionId, modelName);

            // Calculate semantic similarity
            double semanticSimilarity = calculateSemanticSimilarity(resumeText, jobDescription, modelName);

            // Extract skills from both texts
            var resumeSkills = skillExtractionService.extractSkills(resumeText, sessionId);
            var jdSkills = extractSkillsFromJD(jobDescription);

            // Calculate component scores
            int skillsMatch = calculateSkillsMatch(resumeSkills, jdSkills);
            int experienceMatch = calculateExperienceMatch(resumeText, jobDescription);
            int techStackMatch = calculateTechStackMatch(resumeSkills, jdSkills);
            int educationMatch = calculateEducationMatch(resumeText, jobDescription);

            // Calculate weighted overall score
            int overallScore = calculateWeightedScore(skillsMatch, experienceMatch, techStackMatch, educationMatch);

            MatchScore result = new MatchScore();
            result.setOverallMatchPercentage(overallScore);
            result.setSkillsMatchPercentage(skillsMatch);
            result.setExperienceMatchPercentage(experienceMatch);
            result.setTechStackMatchPercentage(techStackMatch);
            result.setEducationMatchPercentage(educationMatch);
            result.setSemanticSimilarity(semanticSimilarity);
            result.setMissingRequiredSkills(findMissingSkills(resumeSkills, jdSkills));
            result.setAdditionalSkills(findAdditionalSkills(resumeSkills, jdSkills));
            result.setExperienceGapYears(extractExperienceGap(resumeText, jobDescription));
            result.setScoringMethod("semantic_similarity_hybrid");
            result.setModelName(modelName);
            result.setWasFallback(false);
            result.setConfidence(0.85);

            long duration = System.currentTimeMillis() - startTime;
            log.info("[{}] Match score calculation completed in {}ms (overall: {}%)",
                sessionId, duration, overallScore);

            return result;

        } catch (Exception e) {
            log.error("[{}] Error during match score calculation: {}", sessionId, e.getMessage(), e);
            return createDefaultMatchScore(true);
        }
    }

    /**
     * Calculate semantic similarity between two texts using embeddings.
     */
    private double calculateSemanticSimilarity(String text1, String text2, String modelName) {
        Map<String, Object> result = inferenceClient.calculateSimilarity(text1, text2, modelName);

        if (result == null || (boolean) result.getOrDefault("fallback", false)) {
            // Fallback to word overlap
            return calculateWordOverlapSimilarity(text1, text2);
        }

        return ((Number) result.getOrDefault("similarity", 0.0)).doubleValue();
    }

    /**
     * Calculate skills match percentage.
     */
    private int calculateSkillsMatch(com.resumescreener.model.ExtractedSkills resumeSkills,
                                     Set<String> jdSkills) {
        if (jdSkills.isEmpty()) return 100;

        var resumeSkillNames = new HashSet<String>();
        if (resumeSkills.getTechnicalSkills() != null) {
            resumeSkills.getTechnicalSkills().forEach(s -> resumeSkillNames.add(s.getName().toLowerCase()));
        }
        if (resumeSkills.getSoftSkills() != null) {
            resumeSkills.getSoftSkills().forEach(s -> resumeSkillNames.add(s.getName().toLowerCase()));
        }

        long matches = jdSkills.stream()
            .filter(skill -> resumeSkillNames.stream()
                .anyMatch(rSkill -> rSkill.contains(skill.toLowerCase()) || skill.toLowerCase().contains(rSkill)))
            .count();

        return (int) ((matches * 100) / jdSkills.size());
    }

    /**
     * Calculate experience match based on years.
     */
    private int calculateExperienceMatch(String resumeText, String jobDescription) {
        int resumeYears = extractYearsOfExperience(resumeText);
        int requiredYears = extractRequiredYears(jobDescription);

        if (requiredYears == 0) return 100;
        if (resumeYears >= requiredYears) return 100;

        return Math.max(0, (resumeYears * 100) / requiredYears);
    }

    /**
     * Calculate tech stack match percentage.
     */
    private int calculateTechStackMatch(com.resumescreener.model.ExtractedSkills resumeSkills,
                                       Set<String> jdSkills) {
        var resumeTechSkills = resumeSkills.getTechnicalSkills();
        if (resumeTechSkills == null || resumeTechSkills.isEmpty() || jdSkills.isEmpty()) {
            return 50;
        }

        long matches = jdSkills.stream()
            .filter(skill -> resumeTechSkills.stream()
                .anyMatch(rSkill -> rSkill.getName().toLowerCase().contains(skill.toLowerCase())))
            .count();

        return (int) ((matches * 100) / jdSkills.size());
    }

    /**
     * Calculate education match.
     */
    private int calculateEducationMatch(String resumeText, String jobDescription) {
        Set<String> resumeDegrees = extractDegrees(resumeText);
        Set<String> requiredDegrees = extractDegrees(jobDescription);

        if (requiredDegrees.isEmpty()) return 100;
        if (resumeDegrees.isEmpty()) return 30;

        long matches = requiredDegrees.stream()
            .filter(degree -> resumeDegrees.stream()
                .anyMatch(rDegree -> matchesDegreeLevel(rDegree, degree)))
            .count();

        return (int) ((matches * 100) / requiredDegrees.size());
    }

    /**
     * Calculate weighted overall score (40/30/20/10).
     */
    private int calculateWeightedScore(int skillsMatch, int experienceMatch,
                                      int techMatch, int educationMatch) {
        return (skillsMatch * 40 + experienceMatch * 30 + techMatch * 20 + educationMatch * 10) / 100;
    }

    /**
     * Find skills required but not in resume.
     */
    private List<String> findMissingSkills(com.resumescreener.model.ExtractedSkills resumeSkills,
                                           Set<String> jdSkills) {
        var resumeSkillNames = new HashSet<String>();
        if (resumeSkills.getTechnicalSkills() != null) {
            resumeSkills.getTechnicalSkills().forEach(s -> resumeSkillNames.add(s.getName().toLowerCase()));
        }

        return jdSkills.stream()
            .filter(skill -> resumeSkillNames.stream().noneMatch(rSkill -> rSkill.contains(skill.toLowerCase())))
            .limit(10)
            .toList();
    }

    /**
     * Find skills in resume but not required by JD.
     */
    private List<String> findAdditionalSkills(com.resumescreener.model.ExtractedSkills resumeSkills,
                                              Set<String> jdSkills) {
        var resumeSkillNames = new HashSet<String>();
        if (resumeSkills.getTechnicalSkills() != null) {
            resumeSkills.getTechnicalSkills().forEach(s -> resumeSkillNames.add(s.getName().toLowerCase()));
        }

        return resumeSkillNames.stream()
            .filter(skill -> jdSkills.stream().noneMatch(jdSkill -> jdSkill.toLowerCase().contains(skill)))
            .limit(5)
            .toList();
    }

    /**
     * Calculate simple word overlap similarity (fallback).
     */
    private double calculateWordOverlapSimilarity(String text1, String text2) {
        String[] words1 = text1.toLowerCase().split("\\W+");
        String[] words2 = text2.toLowerCase().split("\\W+");

        Set<String> set1 = new HashSet<>(Arrays.asList(words1));
        Set<String> set2 = new HashSet<>(Arrays.asList(words2));

        int intersection = 0;
        for (String word : set1) {
            if (set2.contains(word) && word.length() > 4) {  // Ignore short words
                intersection++;
            }
        }

        int union = set1.size() + set2.size() - intersection;
        return union > 0 ? (double) intersection / union : 0.0;
    }

    /**
     * Extract years of experience from resume.
     */
    private int extractYearsOfExperience(String text) {
        Pattern pattern = Pattern.compile("(\\d+)\\s*(?:years?|yrs?)\\s*(?:of\\s*)?(?:experience|exp)");
        var matcher = pattern.matcher(text.toLowerCase());

        if (matcher.find()) {
            return Integer.parseInt(matcher.group(1));
        }

        // Fallback: count years in dates
        Pattern datePattern = Pattern.compile("(20\\d{2})\\s*[-–]\\s*(20\\d{2}|present)");
        var dateMatcher = datePattern.matcher(text);
        int totalYears = 0;

        while (dateMatcher.find()) {
            int startYear = Integer.parseInt(dateMatcher.group(1));
            int endYear = dateMatcher.group(2).equalsIgnoreCase("present") ?
                Calendar.getInstance().get(Calendar.YEAR) :
                Integer.parseInt(dateMatcher.group(2));
            totalYears += (endYear - startYear);
        }

        return totalYears;
    }

    /**
     * Extract required years from job description.
     */
    private int extractRequiredYears(String text) {
        Pattern pattern = Pattern.compile("(\\d+)\\+?\\s*(?:years?|yrs?)\\s*(?:of\\s*)?(?:experience|exp)");
        var matcher = pattern.matcher(text.toLowerCase());

        if (matcher.find()) {
            return Integer.parseInt(matcher.group(1));
        }
        return 0;
    }

    /**
     * Extract degree requirements from text.
     */
    private Set<String> extractDegrees(String text) {
        Set<String> degrees = new HashSet<>();
        String textLower = text.toLowerCase();

        if (textLower.contains("phd") || textLower.contains("doctorate")) degrees.add("phd");
        if (textLower.contains("master") || textLower.contains("m.s") || textLower.contains("m.a")) degrees.add("master");
        if (textLower.contains("bachelor") || textLower.contains("b.s") || textLower.contains("b.a")) degrees.add("bachelor");
        if (textLower.contains("associate") || textLower.contains("a.s")) degrees.add("associate");

        return degrees;
    }

    /**
     * Check if degree level matches requirement.
     */
    private boolean matchesDegreeLevel(String resumeDegree, String requiredDegree) {
        // Map degree hierarchy: bachelor < master < phd
        Map<String, Integer> hierarchy = Map.of(
            "associate", 1,
            "bachelor", 2,
            "master", 3,
            "phd", 4
        );

        Integer resumeLevel = hierarchy.getOrDefault(resumeDegree, 0);
        Integer requiredLevel = hierarchy.getOrDefault(requiredDegree, 0);

        return resumeLevel >= requiredLevel;
    }

    /**
     * Extract skills from job description.
     */
    private Set<String> extractSkillsFromJD(String jobDescription) {
        // Parse "Required skills:", "Must have:", etc.
        String[] sections = jobDescription.split("(?i)(requirements|skills|must have|required|qualifications)");

        Set<String> skills = new HashSet<>();
        String textLower = jobDescription.toLowerCase();

        // Look for technical keywords in job description
        String[] commonSkills = {
            "java", "python", "javascript", "spring boot", "react", "angular",
            "docker", "kubernetes", "aws", "azure", "gcp", "postgresql", "mongodb",
            "rest api", "microservices", "git", "ci/cd", "agile", "scrum"
        };

        for (String skill : commonSkills) {
            if (textLower.contains(skill)) {
                skills.add(skill);
            }
        }

        return skills;
    }

    /**
     * Extract experience gap in years (negative if overqualified).
     */
    private double extractExperienceGap(String resumeText, String jobDescription) {
        int resumeYears = extractYearsOfExperience(resumeText);
        int requiredYears = extractRequiredYears(jobDescription);
        return resumeYears - requiredYears;
    }

    /**
     * Create default match score when scoring fails.
     */
    private MatchScore createDefaultMatchScore(boolean isFallback) {
        MatchScore result = new MatchScore();
        result.setOverallMatchPercentage(0);
        result.setSkillsMatchPercentage(0);
        result.setExperienceMatchPercentage(0);
        result.setTechStackMatchPercentage(0);
        result.setEducationMatchPercentage(0);
        result.setSemanticSimilarity(0.0);
        result.setMissingRequiredSkills(new ArrayList<>());
        result.setAdditionalSkills(new ArrayList<>());
        result.setScoringMethod("error_fallback");
        result.setModelName("none");
        result.setWasFallback(isFallback);
        result.setConfidence(0.0);
        return result;
    }

    /**
     * Check if match scoring service is available.
     */
    public boolean isAvailable() {
        return config.getMatchScoring().isEnabled();
    }
}
