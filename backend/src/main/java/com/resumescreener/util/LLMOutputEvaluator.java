package com.resumescreener.util;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import lombok.extern.slf4j.Slf4j;
import java.util.HashMap;
import java.util.Map;

@Slf4j
public class LLMOutputEvaluator {

    public static class EvaluationResult {
        public int completenessScore;           // 0-100: Are required fields present?
        public int formatScore;                 // 0-100: Is JSON valid and well-formed?
        public int dataQualityScore;            // 0-100: Are values meaningful (not empty/null)?
        public int relevanceScore;              // 0-100: Are values relevant to the task?
        public double overallScore;             // 0-100: Weighted average
        public String evaluation;               // Detailed evaluation message
        public Map<String, String> issues;      // Issues found during evaluation

        public EvaluationResult() {
            this.issues = new HashMap<>();
        }

        @Override
        public String toString() {
            return String.format(
                "LLM Output Evaluation [Overall: %.1f/100] Completeness: %d, Format: %d, Quality: %d, Relevance: %d | %s",
                overallScore, completenessScore, formatScore, dataQualityScore, relevanceScore, evaluation
            );
        }
    }

    // Evaluate extraction result (Resume Analysis - LLM Call 1)
    public static EvaluationResult evaluateExtractionOutput(String jsonResponse) {
        EvaluationResult result = new EvaluationResult();

        try {
            JsonObject json = JsonParser.parseString(jsonResponse).getAsJsonObject();

            // 1. Completeness Check - Required fields for extraction
            boolean hasSkills = json.has("skills") && !json.getAsJsonArray("skills").isEmpty();
            boolean hasEducation = json.has("education") && !json.get("education").isJsonNull();
            boolean hasMatchScore = json.has("match_score");
            boolean hasConfidence = json.has("confidence");
            boolean hasAchievements = json.has("achievements") && !json.getAsJsonArray("achievements").isEmpty();

            int completenessCount = 0;
            if (hasSkills) completenessCount++;
            if (hasEducation) completenessCount++;
            if (hasMatchScore) completenessCount++;
            if (hasConfidence) completenessCount++;
            if (hasAchievements) completenessCount++;

            result.completenessScore = (completenessCount * 20);

            if (!hasSkills) result.issues.put("missing_skills", "Skills array is empty or missing");
            if (!hasEducation) result.issues.put("missing_education", "Education field is empty or missing");
            if (!hasMatchScore) result.issues.put("missing_match_score", "Match score field is missing");
            if (!hasAchievements) result.issues.put("missing_achievements", "Achievements array is empty or missing");

            // 2. Format Score
            result.formatScore = 100; // If we got here, JSON is valid

            // 3. Data Quality Check
            int qualityCount = 0;

            if (hasSkills) {
                int skillCount = json.getAsJsonArray("skills").size();
                if (skillCount >= 4) {
                    qualityCount++;
                } else {
                    result.issues.put("low_skill_count", "Only " + skillCount + " skills extracted (expected 4+)");
                }
            }

            if (hasMatchScore) {
                int score = json.get("match_score").getAsInt();
                if (score > 0 && score <= 100) {
                    qualityCount++;
                } else {
                    result.issues.put("invalid_match_score", "Match score out of range: " + score);
                }
            }

            if (hasConfidence) {
                double confidence = json.get("confidence").getAsDouble();
                if (confidence >= 0.5 && confidence <= 1.0) {
                    qualityCount++;
                } else {
                    result.issues.put("low_confidence", "Confidence score too low: " + confidence);
                }
            }

            if (hasEducation) {
                String edu = json.get("education").getAsString();
                if (edu.length() > 10) {
                    qualityCount++;
                } else {
                    result.issues.put("vague_education", "Education description too short");
                }
            }

            result.dataQualityScore = (qualityCount * 25);

            // 4. Relevance Score
            int relevanceCount = 0;

            if (hasSkills) {
                String skillsText = json.getAsJsonArray("skills").toString().toLowerCase();
                if (skillsText.contains("java") || skillsText.contains("spring") ||
                    skillsText.contains("python") || skillsText.contains("backend")) {
                    relevanceCount++;
                }
            }

            if (hasAchievements) {
                String achievementsText = json.getAsJsonArray("achievements").toString().toLowerCase();
                if (achievementsText.contains("design") || achievementsText.contains("develop") ||
                    achievementsText.contains("implement") || achievementsText.contains("architect")) {
                    relevanceCount++;
                }
            }

            if (hasMatchScore) {
                int score = json.get("match_score").getAsInt();
                if (score >= 50) {
                    relevanceCount++;
                }
            }

            result.relevanceScore = (relevanceCount * 33);

            // Calculate overall score (weighted)
            result.overallScore = (result.completenessScore * 0.3) +
                                 (result.formatScore * 0.2) +
                                 (result.dataQualityScore * 0.3) +
                                 (result.relevanceScore * 0.2);

            // Build evaluation message
            if (result.overallScore >= 85) {
                result.evaluation = "EXCELLENT - Output is complete, well-formatted, and relevant";
            } else if (result.overallScore >= 70) {
                result.evaluation = "GOOD - Output is mostly complete with minor issues";
            } else if (result.overallScore >= 50) {
                result.evaluation = "ACCEPTABLE - Output has some quality issues but is usable";
            } else {
                result.evaluation = "POOR - Output has significant quality issues";
            }

        } catch (Exception e) {
            result.formatScore = 0;
            result.completenessScore = 0;
            result.dataQualityScore = 0;
            result.relevanceScore = 0;
            result.overallScore = 0;
            result.evaluation = "PARSING FAILED - " + e.getMessage();
            result.issues.put("parse_error", e.getMessage());
        }

        return result;
    }

    // Evaluate interview questions output (LLM Call 2A)
    public static EvaluationResult evaluateInterviewQuestionsOutput(String jsonResponse) {
        EvaluationResult result = new EvaluationResult();

        try {
            JsonObject json = JsonParser.parseString(jsonResponse).getAsJsonObject();

            // 1. Completeness Check
            boolean hasQuestions = json.has("questions") && !json.getAsJsonArray("questions").isEmpty();
            int questionCount = hasQuestions ? json.getAsJsonArray("questions").size() : 0;

            result.completenessScore = hasQuestions ? 100 : 0;

            if (!hasQuestions) {
                result.issues.put("no_questions", "Questions array is empty or missing");
            } else if (questionCount < 8) {
                result.issues.put("low_question_count", "Only " + questionCount + " questions (expected 8-10)");
                result.completenessScore = 60 + (questionCount * 5);
            } else if (questionCount >= 8 && questionCount <= 10) {
                result.completenessScore = 100;
            } else if (questionCount > 10) {
                result.completenessScore = 85;
                result.issues.put("excess_questions", questionCount + " questions (expected 8-10)");
            }

            // 2. Format Score
            result.formatScore = 100;

            // 3. Data Quality - Check question structure
            int qualityQuestions = 0;
            if (hasQuestions) {
                for (int i = 0; i < Math.min(questionCount, 10); i++) {
                    JsonObject q = json.getAsJsonArray("questions").get(i).getAsJsonObject();
                    boolean hasId = q.has("id");
                    boolean hasCategory = q.has("category") && !q.get("category").getAsString().isEmpty();
                    boolean hasQuestion = q.has("question") && q.get("question").getAsString().length() > 20;
                    boolean hasDifficulty = q.has("difficulty");

                    if (hasId && hasCategory && hasQuestion && hasDifficulty) {
                        qualityQuestions++;
                    }
                }
            }

            result.dataQualityScore = questionCount > 0 ? (qualityQuestions * 100 / questionCount) : 0;

            // 4. Relevance Score
            int relevantQuestions = 0;
            if (hasQuestions) {
                for (int i = 0; i < Math.min(questionCount, 10); i++) {
                    JsonObject q = json.getAsJsonArray("questions").get(i).getAsJsonObject();
                    String questionText = q.has("question") ? q.get("question").getAsString().toLowerCase() : "";

                    if (questionText.contains("experience") || questionText.contains("skill") ||
                        questionText.contains("design") || questionText.contains("architecture") ||
                        questionText.contains("java") || questionText.contains("spring")) {
                        relevantQuestions++;
                    }
                }
            }

            result.relevanceScore = questionCount > 0 ? (relevantQuestions * 100 / questionCount) : 0;

            // Overall score
            result.overallScore = (result.completenessScore * 0.4) +
                                 (result.formatScore * 0.15) +
                                 (result.dataQualityScore * 0.25) +
                                 (result.relevanceScore * 0.2);

            if (result.overallScore >= 85) {
                result.evaluation = "EXCELLENT - Questions are well-structured and relevant (" + questionCount + " questions)";
            } else if (result.overallScore >= 70) {
                result.evaluation = "GOOD - Questions are mostly well-structured (" + questionCount + " questions)";
            } else if (result.overallScore >= 50) {
                result.evaluation = "ACCEPTABLE - Questions exist but need improvement (" + questionCount + " questions)";
            } else {
                result.evaluation = "POOR - Questions are incomplete or low quality (" + questionCount + " questions)";
            }

        } catch (Exception e) {
            result.formatScore = 0;
            result.completenessScore = 0;
            result.dataQualityScore = 0;
            result.relevanceScore = 0;
            result.overallScore = 0;
            result.evaluation = "PARSING FAILED - " + e.getMessage();
            result.issues.put("parse_error", e.getMessage());
        }

        return result;
    }

    // Generic evaluation for any JSON response
    public static EvaluationResult evaluateGenericOutput(String jsonResponse, String outputType) {
        EvaluationResult result = new EvaluationResult();

        try {
            JsonObject json = JsonParser.parseString(jsonResponse).getAsJsonObject();

            // Basic format check
            result.formatScore = 100;

            // Field count as completeness
            result.completenessScore = Math.min(100, json.size() * 15);

            // Non-null values as quality
            long nonNullCount = json.entrySet().stream()
                .filter(e -> !e.getValue().isJsonNull())
                .count();

            result.dataQualityScore = json.size() > 0 ? (int)(nonNullCount * 100 / json.size()) : 0;

            // Summary length as relevance
            String summary = json.has("summary") ? json.get("summary").getAsString() : "";
            result.relevanceScore = summary.length() > 50 ? 100 : (summary.length() / 50) * 100;

            result.overallScore = (result.completenessScore * 0.25) +
                                 (result.formatScore * 0.25) +
                                 (result.dataQualityScore * 0.25) +
                                 (result.relevanceScore * 0.25);

            if (result.overallScore >= 85) {
                result.evaluation = "EXCELLENT - " + outputType + " output is complete and well-formed";
            } else if (result.overallScore >= 70) {
                result.evaluation = "GOOD - " + outputType + " output is mostly complete";
            } else if (result.overallScore >= 50) {
                result.evaluation = "ACCEPTABLE - " + outputType + " output is usable with minor issues";
            } else {
                result.evaluation = "POOR - " + outputType + " output needs review";
            }

        } catch (Exception e) {
            result.formatScore = 0;
            result.overallScore = 0;
            result.evaluation = "PARSING FAILED";
            result.issues.put("error", e.getMessage());
        }

        return result;
    }
}
