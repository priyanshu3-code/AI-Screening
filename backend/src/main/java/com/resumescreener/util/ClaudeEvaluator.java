package com.resumescreener.util;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import lombok.extern.slf4j.Slf4j;
import org.apache.hc.client5.http.classic.methods.HttpPost;
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.client5.http.impl.classic.HttpClients;
import org.apache.hc.core5.http.io.entity.StringEntity;

import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

@Slf4j
public class ClaudeEvaluator {

    private static final String CLAUDE_API_URL = "https://api.anthropic.com/v1/messages";
    private static final String CLAUDE_MODEL = "claude-opus-4-7";
    private static final Gson gson = new Gson();
    private static String apiKey;

    static {
        apiKey = System.getenv("ANTHROPIC_API_KEY");
        if (apiKey == null || apiKey.isEmpty()) {
            log.warn("ANTHROPIC_API_KEY not set. Claude evaluation will be skipped.");
        }
    }

    public static class EvaluationResult {
        public String evaluation;              // Claude's judgment
        public String quality;                 // EXCELLENT/GOOD/ACCEPTABLE/POOR
        public String strengths;               // What was done well
        public String weaknesses;              // What could improve
        public int score;                      // 0-100 numerical score
        public Map<String, String> issues;     // Issues found

        public EvaluationResult() {
            this.issues = new HashMap<>();
        }

        @Override
        public String toString() {
            return String.format(
                "Claude Evaluation [%s - Score: %d/100] %s",
                quality, score, evaluation
            );
        }
    }

    // Evaluate extraction result with Claude
    public static EvaluationResult evaluateExtractionOutput(String jsonResponse, String resumeText, String jobDescription) {
        EvaluationResult result = new EvaluationResult();

        if (apiKey == null || apiKey.isEmpty()) {
            log.debug("Claude API key not available. Skipping evaluation.");
            result.evaluation = "SKIPPED - No API key";
            result.quality = "UNKNOWN";
            result.score = 0;
            return result;
        }

        try {
            String prompt = buildExtractionEvaluationPrompt(jsonResponse, resumeText, jobDescription);
            String claudeEvaluation = callClaudeAPI(prompt);

            parseEvaluation(claudeEvaluation, result);
            log.debug("Claude evaluation received for extraction output");
        } catch (Exception e) {
            log.error("Claude evaluation failed", e);
            result.evaluation = "Evaluation failed: " + e.getMessage();
            result.quality = "ERROR";
            result.score = 0;
            result.issues.put("error", e.getMessage());
        }

        return result;
    }

    // Evaluate interview questions with Claude
    public static EvaluationResult evaluateInterviewQuestions(String jsonResponse, String jobDescription) {
        EvaluationResult result = new EvaluationResult();

        if (apiKey == null || apiKey.isEmpty()) {
            log.debug("Claude API key not available. Skipping evaluation.");
            result.evaluation = "SKIPPED - No API key";
            result.quality = "UNKNOWN";
            result.score = 0;
            return result;
        }

        try {
            String prompt = buildInterviewQuestionsEvaluationPrompt(jsonResponse, jobDescription);
            String claudeEvaluation = callClaudeAPI(prompt);

            parseEvaluation(claudeEvaluation, result);
            log.debug("Claude evaluation received for interview questions");
        } catch (Exception e) {
            log.error("Claude evaluation failed", e);
            result.evaluation = "Evaluation failed: " + e.getMessage();
            result.quality = "ERROR";
            result.score = 0;
            result.issues.put("error", e.getMessage());
        }

        return result;
    }

    // Evaluate rejection guidance with Claude
    public static EvaluationResult evaluateRejectionGuidance(String jsonResponse) {
        EvaluationResult result = new EvaluationResult();

        if (apiKey == null || apiKey.isEmpty()) {
            log.debug("Claude API key not available. Skipping evaluation.");
            result.evaluation = "SKIPPED - No API key";
            result.quality = "UNKNOWN";
            result.score = 0;
            return result;
        }

        try {
            String prompt = buildRejectionGuidanceEvaluationPrompt(jsonResponse);
            String claudeEvaluation = callClaudeAPI(prompt);

            parseEvaluation(claudeEvaluation, result);
            log.debug("Claude evaluation received for rejection guidance");
        } catch (Exception e) {
            log.error("Claude evaluation failed", e);
            result.evaluation = "Evaluation failed: " + e.getMessage();
            result.quality = "ERROR";
            result.score = 0;
            result.issues.put("error", e.getMessage());
        }

        return result;
    }

    // Evaluate recruiter summary with Claude
    public static EvaluationResult evaluateRecruiterSummary(String jsonResponse) {
        EvaluationResult result = new EvaluationResult();

        if (apiKey == null || apiKey.isEmpty()) {
            log.debug("Claude API key not available. Skipping evaluation.");
            result.evaluation = "SKIPPED - No API key";
            result.quality = "UNKNOWN";
            result.score = 0;
            return result;
        }

        try {
            String prompt = buildRecruiterSummaryEvaluationPrompt(jsonResponse);
            String claudeEvaluation = callClaudeAPI(prompt);

            parseEvaluation(claudeEvaluation, result);
            log.debug("Claude evaluation received for recruiter summary");
        } catch (Exception e) {
            log.error("Claude evaluation failed", e);
            result.evaluation = "Evaluation failed: " + e.getMessage();
            result.quality = "ERROR";
            result.score = 0;
            result.issues.put("error", e.getMessage());
        }

        return result;
    }

    private static String buildExtractionEvaluationPrompt(String jsonResponse, String resumeText, String jobDescription) {
        return "You are an expert evaluator of LLM outputs. Evaluate this resume extraction output from Mistral 7B.\n\n" +
            "RESUME:\n" + truncate(resumeText, 1000) + "\n\n" +
            "JOB DESCRIPTION:\n" + truncate(jobDescription, 500) + "\n\n" +
            "MISTRAL'S EXTRACTION OUTPUT:\n" + jsonResponse + "\n\n" +
            "Evaluate the extraction on:\n" +
            "1. Are skills correctly extracted and relevant to the job?\n" +
            "2. Is the experience level accurately captured?\n" +
            "3. Are achievements meaningful and well-summarized?\n" +
            "4. Is the match score reasonable (0-100)?\n" +
            "5. Is the overall summary accurate?\n\n" +
            "Respond ONLY in this format:\n" +
            "QUALITY: [EXCELLENT|GOOD|ACCEPTABLE|POOR]\n" +
            "SCORE: [0-100]\n" +
            "EVALUATION: [your assessment in 2-3 sentences]\n" +
            "STRENGTHS: [what was done well]\n" +
            "WEAKNESSES: [what could improve]\n" +
            "ISSUES: [specific problems, comma-separated, or NONE]";
    }

    private static String buildInterviewQuestionsEvaluationPrompt(String jsonResponse, String jobDescription) {
        return "You are an expert evaluator of LLM outputs. Evaluate these interview questions generated by Mistral 7B.\n\n" +
            "JOB DESCRIPTION:\n" + truncate(jobDescription, 500) + "\n\n" +
            "MISTRAL'S INTERVIEW QUESTIONS:\n" + jsonResponse + "\n\n" +
            "Evaluate the questions on:\n" +
            "1. Are there 8-10 questions as required?\n" +
            "2. Are questions relevant to the job role?\n" +
            "3. Do questions cover technical AND behavioral aspects?\n" +
            "4. Are difficulty levels appropriately varied?\n" +
            "5. Are questions clear and well-structured?\n\n" +
            "Respond ONLY in this format:\n" +
            "QUALITY: [EXCELLENT|GOOD|ACCEPTABLE|POOR]\n" +
            "SCORE: [0-100]\n" +
            "EVALUATION: [your assessment in 2-3 sentences]\n" +
            "STRENGTHS: [what was done well]\n" +
            "WEAKNESSES: [what could improve]\n" +
            "ISSUES: [specific problems, comma-separated, or NONE]";
    }

    private static String buildRejectionGuidanceEvaluationPrompt(String jsonResponse) {
        return "You are an expert evaluator of LLM outputs. Evaluate this rejection guidance generated by Mistral 7B.\n\n" +
            "MISTRAL'S REJECTION GUIDANCE:\n" + jsonResponse + "\n\n" +
            "Evaluate the guidance on:\n" +
            "1. Are rejection reasons clear and fair?\n" +
            "2. Are improvement suggestions actionable and realistic?\n" +
            "3. Are alternative roles appropriate?\n" +
            "4. Is the tone encouraging and constructive?\n" +
            "5. Would this help a candidate improve?\n\n" +
            "Respond ONLY in this format:\n" +
            "QUALITY: [EXCELLENT|GOOD|ACCEPTABLE|POOR]\n" +
            "SCORE: [0-100]\n" +
            "EVALUATION: [your assessment in 2-3 sentences]\n" +
            "STRENGTHS: [what was done well]\n" +
            "WEAKNESSES: [what could improve]\n" +
            "ISSUES: [specific problems, comma-separated, or NONE]";
    }

    private static String buildRecruiterSummaryEvaluationPrompt(String jsonResponse) {
        return "You are an expert evaluator of LLM outputs. Evaluate this recruiter summary generated by Meta Llama 3.1.\n\n" +
            "META LLAMA'S RECRUITER SUMMARY:\n" + jsonResponse + "\n\n" +
            "Evaluate the summary on:\n" +
            "1. Is the executive summary professional and comprehensive?\n" +
            "2. Are strengths clearly identified and well-articulated?\n" +
            "3. Are concerns legitimate and relevant?\n" +
            "4. Is the recommendation reasonable based on the data?\n" +
            "5. Are next steps clear and actionable?\n\n" +
            "Respond ONLY in this format:\n" +
            "QUALITY: [EXCELLENT|GOOD|ACCEPTABLE|POOR]\n" +
            "SCORE: [0-100]\n" +
            "EVALUATION: [your assessment in 2-3 sentences]\n" +
            "STRENGTHS: [what was done well]\n" +
            "WEAKNESSES: [what could improve]\n" +
            "ISSUES: [specific problems, comma-separated, or NONE]";
    }

    private static String callClaudeAPI(String prompt) throws Exception {
        CloseableHttpClient httpClient = HttpClients.createDefault();

        try {
            HttpPost request = new HttpPost(CLAUDE_API_URL);
            request.setHeader("x-api-key", apiKey);
            request.setHeader("anthropic-version", "2023-06-01");
            request.setHeader("content-type", "application/json");

            JsonObject body = new JsonObject();
            body.addProperty("model", CLAUDE_MODEL);
            body.addProperty("max_tokens", 500);

            com.google.gson.JsonArray messages = new com.google.gson.JsonArray();
            JsonObject message = new JsonObject();
            message.addProperty("role", "user");
            message.addProperty("content", prompt);
            messages.add(message);

            body.add("messages", messages);

            StringEntity entity = new StringEntity(body.toString(), StandardCharsets.UTF_8);
            request.setEntity(entity);

            return httpClient.execute(request, response -> {
                String responseBody = new String(response.getEntity().getContent().readAllBytes(), StandardCharsets.UTF_8);
                JsonObject responseJson = com.google.gson.JsonParser.parseString(responseBody).getAsJsonObject();
                return responseJson.getAsJsonArray("content").get(0).getAsJsonObject().get("text").getAsString();
            });

        } finally {
            httpClient.close();
        }
    }

    private static void parseEvaluation(String claudeResponse, EvaluationResult result) {
        try {
            String[] lines = claudeResponse.split("\n");

            for (String line : lines) {
                if (line.startsWith("QUALITY:")) {
                    result.quality = line.replace("QUALITY:", "").trim();
                } else if (line.startsWith("SCORE:")) {
                    String scoreStr = line.replace("SCORE:", "").trim().split("/")[0].trim();
                    result.score = Integer.parseInt(scoreStr);
                } else if (line.startsWith("EVALUATION:")) {
                    result.evaluation = line.replace("EVALUATION:", "").trim();
                } else if (line.startsWith("STRENGTHS:")) {
                    result.strengths = line.replace("STRENGTHS:", "").trim();
                } else if (line.startsWith("WEAKNESSES:")) {
                    result.weaknesses = line.replace("WEAKNESSES:", "").trim();
                } else if (line.startsWith("ISSUES:")) {
                    String issuesStr = line.replace("ISSUES:", "").trim();
                    if (!issuesStr.equals("NONE")) {
                        String[] issueList = issuesStr.split(",");
                        for (int i = 0; i < issueList.length; i++) {
                            result.issues.put("issue_" + (i + 1), issueList[i].trim());
                        }
                    }
                }
            }
        } catch (Exception e) {
            log.warn("Could not parse Claude evaluation response", e);
            result.evaluation = claudeResponse;
            result.quality = "UNKNOWN";
        }
    }

    private static String truncate(String text, int maxLength) {
        if (text == null || text.length() <= maxLength) {
            return text;
        }
        return text.substring(0, maxLength) + "... [TRUNCATED]";
    }
}
