package com.resumescreener.util;

import lombok.extern.slf4j.Slf4j;
import java.util.*;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * Multi-Model LLM Response Evaluator
 * Evaluates response quality based on: Accuracy, Coherence, Relevance, Factuality, Completeness
 * Completely replaces Claude evaluation with internal quality metrics
 */
@Slf4j
public class LLMResponseEvaluator {

    public static class EvaluationResult {
        public int accuracy;              // 0-100: Factual correctness
        public int coherence;             // 0-100: Logical structure
        public int relevance;             // 0-100: Answers the query
        public int factuality;            // 0-100: Verifiable facts
        public int completeness;          // 0-100: Response completeness
        public String quality;            // EXCELLENT/GOOD/ACCEPTABLE/POOR
        public int score;                 // Final score 0-100
        public List<String> strengths;
        public List<String> weaknesses;
        public List<String> issues;
        public int responseLength;
        public int estimatedTokens;
        public long executionTimeMs;
        public String model;

        public EvaluationResult() {
            this.strengths = new ArrayList<>();
            this.weaknesses = new ArrayList<>();
            this.issues = new ArrayList<>();
        }

        @Override
        public String toString() {
            return String.format(
                "LLM Evaluation [%s - Score: %d/100 | Model: %s | Time: %dms]",
                quality, score, model, executionTimeMs
            );
        }
    }

    /**
     * Evaluate LLM response quality with comprehensive metrics
     */
    public static EvaluationResult evaluateResponse(
            String response,
            String prompt,
            String model,
            long executionTimeMs) {

        EvaluationResult result = new EvaluationResult();
        result.model = model;
        result.executionTimeMs = executionTimeMs;
        result.responseLength = response.length();
        result.estimatedTokens = estimateTokens(response);

        logEvaluationStart(model, response.length(), executionTimeMs);

        // 1. ACCURACY EVALUATION
        result.accuracy = evaluateAccuracy(response, prompt);
        log.info("✓ Accuracy: {}/100", result.accuracy);

        if (result.accuracy >= 85) {
            result.strengths.add("Highly accurate factual content");
        } else if (result.accuracy < 60) {
            result.weaknesses.add("Contains potentially inaccurate information");
            result.issues.add("Low accuracy score (" + result.accuracy + "/100)");
        }

        // 2. COHERENCE EVALUATION
        result.coherence = evaluateCoherence(response);
        log.info("✓ Coherence: {}/100", result.coherence);

        if (result.coherence >= 80) {
            result.strengths.add("Well-structured and logically organized");
        } else if (result.coherence < 60) {
            result.weaknesses.add("Lacks clear logical structure");
            result.issues.add("Poor coherence");
        }

        // 3. RELEVANCE EVALUATION
        result.relevance = evaluateRelevance(response, prompt);
        log.info("✓ Relevance: {}/100", result.relevance);

        if (result.relevance >= 85) {
            result.strengths.add("Highly relevant to the query");
        } else if (result.relevance < 60) {
            result.weaknesses.add("Response does not adequately address query");
            result.issues.add("Low relevance");
        }

        // 4. FACTUALITY EVALUATION
        result.factuality = evaluateFactuality(response);
        log.info("✓ Factuality: {}/100", result.factuality);

        if (result.factuality >= 80) {
            result.strengths.add("Based on verifiable facts and evidence");
        } else if (result.factuality < 60) {
            result.weaknesses.add("Contains unverified claims or speculation");
            result.issues.add("Low factuality");
        }

        // 5. COMPLETENESS EVALUATION
        result.completeness = evaluateCompleteness(response, prompt);
        log.info("✓ Completeness: {}/100", result.completeness);

        if (result.completeness >= 80) {
            result.strengths.add("Comprehensive and complete response");
        } else if (result.completeness < 60) {
            result.weaknesses.add("Response feels incomplete or truncated");
            result.issues.add("Low completeness");
        }

        // CALCULATE FINAL SCORE
        result.score = (result.accuracy + result.coherence + result.relevance
                        + result.factuality + result.completeness) / 5;

        // DETERMINE QUALITY RATING
        if (result.score >= 85) {
            result.quality = "EXCELLENT";
        } else if (result.score >= 70) {
            result.quality = "GOOD";
        } else if (result.score >= 50) {
            result.quality = "ACCEPTABLE";
        } else {
            result.quality = "POOR";
        }

        logEvaluationResults(result);

        return result;
    }

    /**
     * ACCURACY: Check for factual correctness
     */
    private static int evaluateAccuracy(String response, String prompt) {
        int score = 70; // Baseline

        // Check for evidence-based language
        if (Pattern.compile("according to|research shows|studies indicate|evidence suggests|data shows",
                Pattern.CASE_INSENSITIVE).matcher(response).find()) {
            score += 15;
        }

        // Check for uncertainty indicators
        if (Pattern.compile("i'm not sure|i don't know|i cannot verify|unverified claim",
                Pattern.CASE_INSENSITIVE).matcher(response).find()) {
            score -= 10;
        }

        // Length check - too short might be incomplete
        if (response.length() < 100) {
            score -= 5;
        }

        // Check for citations/sources
        if (Pattern.compile("\\(source:|sources:|references:|citation",
                Pattern.CASE_INSENSITIVE).matcher(response).find()) {
            score += 10;
        }

        return Math.min(100, Math.max(0, score));
    }

    /**
     * COHERENCE: Check logical flow and structure
     */
    private static int evaluateCoherence(String response) {
        int score = 70; // Baseline

        // Check for paragraph structure
        String[] paragraphs = response.split("\n\n+");
        long nonEmptyParagraphs = Arrays.stream(paragraphs)
                .filter(p -> !p.trim().isEmpty())
                .count();
        if (nonEmptyParagraphs >= 2) {
            score += 10;
        }

        // Check for transition words
        long transitionCount = Arrays.asList(
                "therefore", "however", "furthermore", "moreover",
                "in addition", "as a result", "consequently"
        ).stream()
            .filter(word -> Pattern.compile(word, Pattern.CASE_INSENSITIVE)
                    .matcher(response).find())
            .count();

        if (transitionCount >= 2) {
            score += 10;
        }

        // Check for lists
        if (Pattern.compile("^[\\s]*[-•*\\d.]+\\s+", Pattern.MULTILINE)
                .matcher(response).find()) {
            score += 5;
        }

        // Check for sufficient sentences
        String[] sentences = response.split("[.!?]+");
        if (sentences.length >= 5) {
            score += 5;
        }

        // Check logical flow
        if (hasLogicalFlow(response)) {
            score += 5;
        }

        return Math.min(100, Math.max(0, score));
    }

    /**
     * RELEVANCE: Check if response answers the question
     */
    private static int evaluateRelevance(String response, String prompt) {
        int score = 70; // Baseline

        // Extract keywords from prompt
        List<String> keywords = extractKeywords(prompt);
        String responseLower = response.toLowerCase();

        // Count keyword matches
        long matches = keywords.stream()
                .filter(kw -> responseLower.contains(kw.toLowerCase()))
                .count();

        double ratio = keywords.isEmpty() ? 0 : (double) matches / keywords.size();

        if (ratio >= 0.7) {
            score += 20;
        } else if (ratio >= 0.5) {
            score += 10;
        } else if (ratio >= 0.3) {
            score += 5;
        } else {
            score -= 10;
        }

        // Check for direct answer
        if (Pattern.compile("^(yes|no|true|false|correct|incorrect)",
                Pattern.CASE_INSENSITIVE | Pattern.MULTILINE)
                .matcher(response).find()) {
            score += 5;
        }

        // Check for off-topic content
        if (isOffTopic(response, prompt)) {
            score -= 15;
        }

        return Math.min(100, Math.max(0, score));
    }

    /**
     * FACTUALITY: Check for verifiable facts vs speculation
     */
    private static int evaluateFactuality(String response) {
        int score = 70; // Baseline

        // Check for hedging language (speculation)
        long hedgeCount = Arrays.asList(
                "might", "may", "could", "possibly", "perhaps",
                "seems", "appears", "suggests", "may indicate"
        ).stream()
            .filter(word -> Pattern.compile(word, Pattern.CASE_INSENSITIVE)
                    .matcher(response).find())
            .count();

        if (hedgeCount > 5) {
            score -= 15;
        } else if (hedgeCount > 2) {
            score -= 5;
        }

        // Check for specific numbers/dates
        if (Pattern.compile("\\d{4}|\\d{1,2}[/-]\\d{1,2}[/-]\\d{2,4}|\\d+\\s*(million|billion|thousand|percent|%)",
                Pattern.CASE_INSENSITIVE).matcher(response).find()) {
            score += 10;
        }

        // Check for qualified statements
        if (Pattern.compile("according to|research indicates|studies show|evidence suggests",
                Pattern.CASE_INSENSITIVE).matcher(response).find()) {
            score += 5;
        }

        // Check for misconceptions
        if (containsMisconceptions(response)) {
            score -= 20;
        }

        return Math.min(100, Math.max(0, score));
    }

    /**
     * COMPLETENESS: Check if response fully addresses the topic
     */
    private static int evaluateCompleteness(String response, String prompt) {
        int score = 70; // Baseline

        // Check minimum length
        if (response.length() < 100) {
            score -= 20;
        } else if (response.length() < 500) {
            score -= 5;
        } else if (response.length() >= 1000) {
            score += 10;
        }

        // Check for multiple aspects covered
        String[] sentences = response.split("[.!?]+");
        if (sentences.length >= 10) {
            score += 10;
        }

        // Check for conclusion
        if (Pattern.compile("in conclusion|to summarize|in summary|finally",
                Pattern.CASE_INSENSITIVE).matcher(response).find()) {
            score += 5;
        }

        // Check if truncated
        if (response.endsWith("...") || response.endsWith("[truncated]") ||
                response.endsWith("cont")) {
            score -= 15;
        }

        // Check for examples
        if (Pattern.compile("for example|for instance|such as|like",
                Pattern.CASE_INSENSITIVE).matcher(response).find()) {
            score += 5;
        }

        return Math.min(100, Math.max(0, score));
    }

    /**
     * Helper: Extract keywords from text
     */
    private static List<String> extractKeywords(String text) {
        return Arrays.stream(text.toLowerCase().split("\\s+"))
                .filter(word -> word.length() > 4)
                .limit(10)
                .collect(Collectors.toList());
    }

    /**
     * Helper: Check if response has logical flow
     */
    private static boolean hasLogicalFlow(String text) {
        String[] sentences = text.split("[.!?]+");
        if (sentences.length < 2) {
            return false;
        }

        for (int i = 1; i < sentences.length; i++) {
            String current = sentences[i].toLowerCase();
            String previous = sentences[i - 1].toLowerCase();

            if (current.contains("this") || current.contains("that") ||
                    current.contains("therefore") ||
                    previous.contains(current.split(" ")[0])) {
                return true;
            }
        }
        return false;
    }

    /**
     * Helper: Check if response is off-topic
     */
    private static boolean isOffTopic(String response, String prompt) {
        List<String> keywords = extractKeywords(prompt);
        String responseLower = response.toLowerCase();

        long matches = keywords.stream()
                .filter(kw -> responseLower.contains(kw.toLowerCase()))
                .count();

        return matches < keywords.size() * 0.2;
    }

    /**
     * Helper: Check for misconceptions
     */
    private static boolean containsMisconceptions(String text) {
        String[] misconceptions = {
                "the earth is flat",
                "vaccines cause autism",
                "climate change is a hoax"
        };

        String textLower = text.toLowerCase();
        return Arrays.stream(misconceptions)
                .anyMatch(m -> textLower.contains(m));
    }

    /**
     * Helper: Estimate token count (rough approximation)
     */
    private static int estimateTokens(String text) {
        return Math.round(text.split("\\s+").length * 1.3f);
    }

    /**
     * Logging: Evaluation start
     */
    private static void logEvaluationStart(String model, int responseLength, long executionTime) {
        log.info("\n" + "=".repeat(80));
        log.info("📊 RESPONSE EVALUATION STARTED");
        log.info("=".repeat(80));
        log.info("Model: {}", model);
        log.info("Response Length: {} characters", responseLength);
        log.info("Execution Time: {}ms", executionTime);
        log.info("=".repeat(80));
    }

    /**
     * Logging: Final evaluation results
     */
    private static void logEvaluationResults(EvaluationResult result) {
        log.info("\n" + "=".repeat(80));
        log.info("🎯 FINAL EVALUATION RESULTS");
        log.info("=".repeat(80));
        log.info("Overall Score: {}/100", result.score);
        log.info("Quality Rating: {}", result.quality);
        log.info("Model: {} | Tokens: ~{} | Time: {}ms",
                result.model, result.estimatedTokens, result.executionTimeMs);
        log.info("");
        log.info("Metric Breakdown:");
        log.info("  • Accuracy:     {}/100", result.accuracy);
        log.info("  • Coherence:    {}/100", result.coherence);
        log.info("  • Relevance:    {}/100", result.relevance);
        log.info("  • Factuality:   {}/100", result.factuality);
        log.info("  • Completeness: {}/100", result.completeness);

        if (!result.strengths.isEmpty()) {
            log.info("\n💪 STRENGTHS:");
            result.strengths.forEach(s -> log.info("  ✓ {}", s));
        }

        if (!result.weaknesses.isEmpty()) {
            log.info("\n⚠️  WEAKNESSES:");
            result.weaknesses.forEach(w -> log.info("  ✗ {}", w));
        }

        if (!result.issues.isEmpty()) {
            log.info("\n🔴 ISSUES FOUND:");
            result.issues.forEach(i -> log.info("  ⚠ {}", i));
        }

        log.info("=".repeat(80));
    }

    /**
     * Evaluate JSON extraction output quality
     */
    public static EvaluationResult evaluateExtractionOutput(
            String jsonResponse,
            String resumeText,
            String jobDescription,
            String model,
            long executionTime) {

        String evaluationPrompt = String.format(
                "Evaluate this resume extraction:\nJSON: %s\nResume: %s\nJob: %s",
                jsonResponse, truncate(resumeText, 500), truncate(jobDescription, 300)
        );

        EvaluationResult result = evaluateResponse(
                jsonResponse, evaluationPrompt, model, executionTime
        );

        log.info("LLM Call Evaluation (Extraction) - Score: {}/100 | Quality: {} | Model: {}",
                result.score, result.quality, model);

        return result;
    }

    /**
     * Evaluate interview questions quality
     */
    public static EvaluationResult evaluateInterviewQuestions(
            String jsonResponse,
            String jobDescription,
            String model,
            long executionTime) {

        String evaluationPrompt = String.format(
                "Evaluate these interview questions for job: %s\nQuestions JSON: %s",
                truncate(jobDescription, 300), jsonResponse
        );

        EvaluationResult result = evaluateResponse(
                jsonResponse, evaluationPrompt, model, executionTime
        );

        // Additional checks for interview questions format
        if (jsonResponse.contains("\"questions\"") &&
                Pattern.compile("\"question\"\\s*:\\s*\"[^\"]{20,}\"").matcher(jsonResponse).find()) {
            result.strengths.add("Valid interview questions format");
            result.score = Math.min(100, result.score + 5);
        }

        log.info("LLM Call Evaluation (Interview Questions) - Score: {}/100 | Quality: {} | Model: {}",
                result.score, result.quality, model);

        return result;
    }

    /**
     * Evaluate rejection guidance quality
     */
    public static EvaluationResult evaluateRejectionGuidance(
            String jsonResponse,
            String model,
            long executionTime) {

        String evaluationPrompt = String.format(
                "Evaluate this rejection guidance:\n%s", jsonResponse
        );

        EvaluationResult result = evaluateResponse(
                jsonResponse, evaluationPrompt, model, executionTime
        );

        // Check for constructive tone
        if (Pattern.compile("improve|opportunity|potential|strength|learn",
                Pattern.CASE_INSENSITIVE).matcher(jsonResponse).find()) {
            result.strengths.add("Constructive and encouraging tone");
        }

        log.info("LLM Call Evaluation (Rejection Guidance) - Score: {}/100 | Quality: {} | Model: {}",
                result.score, result.quality, model);

        return result;
    }

    /**
     * Evaluate recruiter summary quality
     */
    public static EvaluationResult evaluateRecruiterSummary(
            String jsonResponse,
            String model,
            long executionTime) {

        String evaluationPrompt = String.format(
                "Evaluate this recruiter summary:\n%s", jsonResponse
        );

        EvaluationResult result = evaluateResponse(
                jsonResponse, evaluationPrompt, model, executionTime
        );

        // Check for professional summary
        if (Pattern.compile("executive|summary|professional|recommendation",
                Pattern.CASE_INSENSITIVE).matcher(jsonResponse).find()) {
            result.strengths.add("Professional summary format");
        }

        log.info("LLM Call Evaluation (Recruiter Summary) - Score: {}/100 | Quality: {} | Model: {}",
                result.score, result.quality, model);

        return result;
    }

    /**
     * Helper: Truncate text for readability
     */
    private static String truncate(String text, int maxLength) {
        if (text == null || text.length() <= maxLength) {
            return text;
        }
        return text.substring(0, maxLength) + "... [TRUNCATED]";
    }
}
