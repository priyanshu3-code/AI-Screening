package com.resumescreener.service;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.resumescreener.model.*;
import com.resumescreener.util.ClaudeEvaluator;
import com.resumescreener.util.ResumeExtractionResultDeserializer;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

@Service
@Slf4j
public class AIOrchestrationService {

    @Autowired
    private HuggingFaceClient hfClient;

    private final Gson gson = new GsonBuilder()
        .registerTypeAdapter(ResumeExtractionResult.class, new ResumeExtractionResultDeserializer())
        .create();

    private static final String MODEL_EXTRACTION = "mistralai/Mistral-7B-Instruct-v0.2:featherless-ai";
    private static final String MODEL_INTERVIEW = "mistralai/Mistral-7B-Instruct-v0.2:featherless-ai";
    private static final String MODEL_SUMMARY = "meta-llama/Llama-3.1-8B-Instruct:novita";

    public ResumeExtractionResult analyzeResume(String resumeText, String jobDescription) {
        log.info("Starting LLM Call 1: Resume Extraction (Model: Mistral)");
        long startTime = System.currentTimeMillis();

        try {
            String prompt = buildExtractionPrompt(resumeText, jobDescription);
            String response = hfClient.callLLM(prompt, MODEL_EXTRACTION);
            String jsonContent = extractJsonContent(response);

            log.info("Raw JSON response (first 500 chars): {}", jsonContent.substring(0, Math.min(500, jsonContent.length())));

            try {
                // Try to parse with Gson first
                ResumeExtractionResult result = gson.fromJson(jsonContent, ResumeExtractionResult.class);
                log.info("✓ Successfully parsed LLM extraction response");

                if (result.getMatchScore() == 0) {
                    log.warn("Match score is 0, calculating from extracted data");
                    result.setMatchScore(calculateMatchScore(result, jobDescription));
                }

                if (result.getExperienceYears() == 0 && !isEmpty(result.getAchievements())) {
                    log.warn("Experience years is 0, inferring from achievements");
                    result.setExperienceYears(inferExperienceYears(result));
                }

                // Evaluate the LLM output quality using Claude
                ClaudeEvaluator.EvaluationResult evaluation = ClaudeEvaluator.evaluateExtractionOutput(jsonContent, resumeText, jobDescription);
                log.info("LLM Call 1 Claude Evaluation: {}", evaluation);
                if (evaluation.strengths != null) {
                    log.debug("  Strengths: {}", evaluation.strengths);
                }
                if (evaluation.weaknesses != null) {
                    log.debug("  Weaknesses: {}", evaluation.weaknesses);
                }
                if (!evaluation.issues.isEmpty()) {
                    log.warn("  Issues Found: {}", evaluation.issues);
                }

                long duration = System.currentTimeMillis() - startTime;
                log.info("LLM Call 1 completed in {}ms | Match Score: {} | Claude Score: {}/100 | Quality: {} | Model: Mistral | SOURCE: LLM",
                    duration, result.getMatchScore(), evaluation.score, evaluation.quality);
                return result;

            } catch (com.google.gson.JsonSyntaxException jsonError) {
                log.error("Failed to parse JSON from LLM response", jsonError);
                log.error("JSON parse error details: {}", jsonError.getMessage());
                throw jsonError;
            }
        } catch (Exception e) {
            log.error("Resume analysis failed - LLM error: {}", e.getMessage(), e);
            log.warn("⚠ FALLING BACK TO MOCK DATA - This means LLM is not working properly");
            return createMockExtractionResult(jobDescription);
        }
    }

    public void processCandidate(Session session) {
        log.info("Processing candidate for session: {}", session.getId());

        ResumeExtractionResult extraction = session.getExtractionResult();
        int matchScore = extraction.getMatchScore();

        log.info("Match score: {} for session: {}", matchScore, session.getId());

        if (matchScore >= 70) {
            log.info("Score {} >= 70%, generating interview questions (LLM Call 2A)", matchScore);
            List<InterviewQuestion> questions = generateInterviewQuestions(extraction, session.getJobDescription());
            if (questions != null && !questions.isEmpty()) {
                session.setInterviewQuestions(questions);
                log.info("Interview questions generated: {} questions", questions.size());
            } else {
                log.warn("No interview questions generated, using mock questions");
                session.setInterviewQuestions(createMockInterviewQuestions());
            }
        } else {
            log.info("Score {} < 70%, generating rejection guidance (LLM Call 2B)", matchScore);
            RejectionGuidance guidance = generateRejectionGuidance(extraction, session.getJobDescription());
            session.setRejectionGuidance(guidance);
        }

        log.info("Generating recruiter summary (LLM Call 3)");
        RecruiterSummary summary = generateRecruiterSummary(extraction, session.getJobDescription());
        session.setRecruiterSummary(summary);

        log.info("Candidate processing complete for session: {}", session.getId());
    }

    private List<InterviewQuestion> generateInterviewQuestions(ResumeExtractionResult resume, String jobDescription) {
        log.info("Starting LLM Call 2A: Interview Questions Generation (Model: Mistral)");
        long startTime = System.currentTimeMillis();

        try {
            String prompt = buildInterviewPrompt(resume, jobDescription);
            String response = hfClient.callLLM(prompt, MODEL_INTERVIEW);
            String jsonContent = extractJsonContent(response);

            log.info("Raw JSON response (first 500 chars): {}", jsonContent.substring(0, Math.min(500, jsonContent.length())));

            try {
                InterviewQuestionsWrapper wrapper = gson.fromJson(jsonContent, InterviewQuestionsWrapper.class);
                log.info("✓ Successfully parsed {} interview questions from LLM", (wrapper.questions != null ? wrapper.questions.size() : 0));

                // Evaluate the LLM output quality using Claude
                ClaudeEvaluator.EvaluationResult evaluation = ClaudeEvaluator.evaluateInterviewQuestions(jsonContent, jobDescription);
                log.info("LLM Call 2A Claude Evaluation: {}", evaluation);
                if (evaluation.strengths != null) {
                    log.debug("  Strengths: {}", evaluation.strengths);
                }
                if (evaluation.weaknesses != null) {
                    log.debug("  Weaknesses: {}", evaluation.weaknesses);
                }
                if (!evaluation.issues.isEmpty()) {
                    log.warn("  Issues Found: {}", evaluation.issues);
                }

                long duration = System.currentTimeMillis() - startTime;
                log.info("LLM Call 2A completed in {}ms | Questions: {} | Claude Score: {}/100 | Quality: {} | Model: Mistral | SOURCE: LLM",
                    duration, (wrapper.questions != null ? wrapper.questions.size() : 0), evaluation.score, evaluation.quality);

                return wrapper.questions;
            } catch (com.google.gson.JsonSyntaxException jsonError) {
                log.error("Failed to parse interview questions JSON from LLM response", jsonError);
                log.error("JSON parse error: {}", jsonError.getMessage());
                throw jsonError;
            }
        } catch (Exception e) {
            log.error("Interview question generation failed - LLM error: {}", e.getMessage(), e);
            log.warn("⚠ FALLING BACK TO MOCK DATA - LLM not returning valid interview questions");
            return createMockInterviewQuestions();
        }
    }

    private RejectionGuidance generateRejectionGuidance(ResumeExtractionResult resume, String jobDescription) {
        log.info("Starting LLM Call 2B: Rejection Guidance Generation (Model: Mistral)");
        long startTime = System.currentTimeMillis();

        try {
            String prompt = buildFeedbackPrompt(resume, jobDescription);
            String response = hfClient.callLLM(prompt, MODEL_INTERVIEW);
            String jsonContent = extractJsonContent(response);

            log.info("Raw JSON response (first 500 chars): {}", jsonContent.substring(0, Math.min(500, jsonContent.length())));

            try {
                RejectionGuidance guidance = gson.fromJson(jsonContent, RejectionGuidance.class);
                log.info("✓ Successfully parsed rejection guidance from LLM");

                // Evaluate the LLM output quality using Claude
                ClaudeEvaluator.EvaluationResult evaluation = ClaudeEvaluator.evaluateRejectionGuidance(jsonContent);
                log.info("LLM Call 2B Claude Evaluation: {}", evaluation);
                if (evaluation.strengths != null) {
                    log.debug("  Strengths: {}", evaluation.strengths);
                }
                if (evaluation.weaknesses != null) {
                    log.debug("  Weaknesses: {}", evaluation.weaknesses);
                }
                if (!evaluation.issues.isEmpty()) {
                    log.warn("  Issues Found: {}", evaluation.issues);
                }

                long duration = System.currentTimeMillis() - startTime;
                log.info("LLM Call 2B completed in {}ms | Claude Score: {}/100 | Quality: {} | Model: Mistral | SOURCE: LLM",
                    duration, evaluation.score, evaluation.quality);

                return guidance;
            } catch (com.google.gson.JsonSyntaxException jsonError) {
                log.error("Failed to parse rejection guidance JSON from LLM response", jsonError);
                log.error("JSON parse error: {}", jsonError.getMessage());
                throw jsonError;
            }
        } catch (Exception e) {
            log.error("Rejection guidance generation failed - LLM error: {}", e.getMessage(), e);
            log.warn("⚠ FALLING BACK TO MOCK DATA - LLM not returning valid rejection guidance");
            return createMockRejectionGuidance();
        }
    }

    private RecruiterSummary generateRecruiterSummary(ResumeExtractionResult resume, String jobDescription) {
        log.info("Starting LLM Call 3: Recruiter Summary Generation (Model: Meta Llama)");
        long startTime = System.currentTimeMillis();

        try {
            String prompt = buildSummaryPrompt(resume, jobDescription);
            String response = hfClient.callLLM(prompt, MODEL_SUMMARY);
            String jsonContent = extractJsonContent(response);

            log.info("Raw JSON response (first 500 chars): {}", jsonContent.substring(0, Math.min(500, jsonContent.length())));

            try {
                RecruiterSummary summary = gson.fromJson(jsonContent, RecruiterSummary.class);
                log.info("✓ Successfully parsed recruiter summary from LLM");

                // Evaluate the LLM output quality using Claude
                ClaudeEvaluator.EvaluationResult evaluation = ClaudeEvaluator.evaluateRecruiterSummary(jsonContent);
                log.info("LLM Call 3 Claude Evaluation: {}", evaluation);
                if (evaluation.strengths != null) {
                    log.debug("  Strengths: {}", evaluation.strengths);
                }
                if (evaluation.weaknesses != null) {
                    log.debug("  Weaknesses: {}", evaluation.weaknesses);
                }
                if (!evaluation.issues.isEmpty()) {
                    log.warn("  Issues Found: {}", evaluation.issues);
                }

                long duration = System.currentTimeMillis() - startTime;
                log.info("LLM Call 3 completed in {}ms | Claude Score: {}/100 | Quality: {} | Model: Meta Llama | SOURCE: LLM",
                    duration, evaluation.score, evaluation.quality);

                return summary;
            } catch (com.google.gson.JsonSyntaxException jsonError) {
                log.error("Failed to parse recruiter summary JSON from LLM response", jsonError);
                log.error("JSON parse error: {}", jsonError.getMessage());
                throw jsonError;
            }
        } catch (Exception e) {
            log.error("Recruiter summary generation failed - LLM error: {}", e.getMessage(), e);
            log.warn("⚠ FALLING BACK TO MOCK DATA - LLM not returning valid recruiter summary");
            return createMockRecruiterSummary();
        }
    }

    private String buildExtractionPrompt(String resumeText, String jobDescription) {
        return "You are an expert HR recruiter. Analyze this resume against the job description and calculate a match score.\n" +
            "Extract and structure the candidate's information as JSON.\n\n" +
            "Resume:\n" + resumeText + "\n\n" +
            "Job Description:\n" + jobDescription + "\n\n" +
            "IMPORTANT: Calculate match_score as a number 0-100 based on:\n" +
            "- How many required skills the candidate has (40%)\n" +
            "- Years of experience vs. job requirement (30%)\n" +
            "- Relevant tech stack match (20%)\n" +
            "- Education and achievements (10%)\n\n" +
            "Return ONLY a valid JSON object (no markdown, no extra text) with these fields:\n" +
            "{\n" +
            "  \"skills\": [\"skill1\", \"skill2\"],\n" +
            "  \"experience_years\": 5,\n" +
            "  \"education\": \"Bachelor's in Computer Science\",\n" +
            "  \"achievements\": [\"achievement1\", \"achievement2\"],\n" +
            "  \"strengths\": [\"strength1\", \"strength2\"],\n" +
            "  \"missing_requirements\": [\"requirement1\"],\n" +
            "  \"tech_stack\": [\"tech1\", \"tech2\"],\n" +
            "  \"match_score\": 75,\n" +
            "  \"confidence\": 0.85,\n" +
            "  \"summary\": \"Brief summary\"\n" +
            "}";
    }

    private String buildInterviewPrompt(ResumeExtractionResult resume, String jobDescription) {
        String skills = (resume.getSkills() != null && !resume.getSkills().isEmpty())
            ? String.join(", ", resume.getSkills())
            : "Not specified";

        String strengths = (resume.getStrengths() != null && !resume.getStrengths().isEmpty())
            ? String.join(", ", resume.getStrengths())
            : "Not specified";

        return "You are a senior technical hiring manager. This candidate scored above 70% match.\n" +
            "Generate 8-10 interview questions tailored to this strong candidate and job role.\n\n" +
            "Candidate Skills: " + skills + "\n" +
            "Experience Level: " + resume.getExperienceYears() + " years\n" +
            "Strengths: " + strengths + "\n\n" +
            "Job Requirements:\n" + jobDescription + "\n\n" +
            "Return ONLY valid JSON (no markdown, no extra text):\n" +
            "{\n" +
            "  \"questions\": [\n" +
            "    {\n" +
            "      \"id\": 1,\n" +
            "      \"category\": \"technical\",\n" +
            "      \"question\": \"Describe your experience with...\",\n" +
            "      \"difficulty\": \"medium\",\n" +
            "      \"time_estimate_minutes\": 10,\n" +
            "      \"tip\": \"Look for...\"\n" +
            "    }\n" +
            "  ]\n" +
            "}";
    }

    private String buildFeedbackPrompt(ResumeExtractionResult resume, String jobDescription) {
        String missingReqs = (resume.getMissingRequirements() != null && !resume.getMissingRequirements().isEmpty())
            ? String.join(", ", resume.getMissingRequirements())
            : "No specific missing requirements identified";

        String strengths = (resume.getStrengths() != null && !resume.getStrengths().isEmpty())
            ? String.join(", ", resume.getStrengths())
            : "Not specified";

        return "You are a compassionate career coach. This candidate didn't meet the 70% threshold.\n" +
            "Provide constructive feedback and improvement suggestions based on the job requirements.\n\n" +
            "Match Score: " + resume.getMatchScore() + "\n" +
            "Missing Requirements: " + missingReqs + "\n" +
            "Candidate Strengths: " + strengths + "\n\n" +
            "Return ONLY valid JSON (no markdown, no extra text):\n" +
            "{\n" +
            "  \"rejection_reasons\": [\"reason1\", \"reason2\"],\n" +
            "  \"improvements\": [\n" +
            "    {\n" +
            "      \"skill\": \"Skill Name\",\n" +
            "      \"current_level\": \"beginner\",\n" +
            "      \"recommended_resources\": [\"resource1\", \"resource2\"],\n" +
            "      \"estimated_months\": 6\n" +
            "    }\n" +
            "  ],\n" +
            "  \"alternative_roles\": [\"role1\", \"role2\"],\n" +
            "  \"encouragement\": \"Encouraging message\"\n" +
            "}";
    }

    private String buildSummaryPrompt(ResumeExtractionResult resume, String jobDescription) {
        String skills = (resume.getSkills() != null && !resume.getSkills().isEmpty())
            ? String.join(", ", resume.getSkills())
            : "Not specified";

        String summary = (resume.getSummary() != null) ? resume.getSummary() : "Not provided";

        return "You are a professional recruiter writing a hiring summary for a hiring manager.\n\n" +
            "Candidate Match Score: " + resume.getMatchScore() + "\n" +
            "Skills: " + skills + "\n" +
            "Experience: " + resume.getExperienceYears() + " years\n" +
            "Summary: " + summary + "\n\n" +
            "Return ONLY valid JSON (no markdown, no extra text):\n" +
            "{\n" +
            "  \"executive_summary\": \"150-200 word professional summary\",\n" +
            "  \"strengths\": [\"strength1\", \"strength2\", \"strength3\"],\n" +
            "  \"concerns\": [\"concern1\"],\n" +
            "  \"recommendation\": \"YES\",\n" +
            "  \"next_steps\": [\"step1\", \"step2\"],\n" +
            "  \"interview_readiness\": \"Ready for technical interview\"\n" +
            "}";
    }

    private String extractJsonContent(String response) {
        try {
            String cleaned = hfClient.extractJsonFromResponse(response);

            int jsonStart = cleaned.indexOf('{');
            int jsonEnd = cleaned.lastIndexOf('}');

            if (jsonStart != -1 && jsonEnd != -1 && jsonEnd > jsonStart) {
                String extracted = cleaned.substring(jsonStart, jsonEnd + 1);

                // If JSON is incomplete (missing closing brackets), fix it
                extracted = fixIncompleteJson(extracted);

                return extracted;
            }

            return cleaned;
        } catch (Exception e) {
            log.warn("Could not extract JSON from response", e);
            return response;
        }
    }

    private String fixIncompleteJson(String json) {
        int openBraces = 0;
        int openBrackets = 0;

        for (char c : json.toCharArray()) {
            if (c == '{') openBraces++;
            else if (c == '}') openBraces--;
            else if (c == '[') openBrackets++;
            else if (c == ']') openBrackets--;
        }

        StringBuilder fixed = new StringBuilder(json);

        while (openBrackets > 0) {
            fixed.append("]");
            openBrackets--;
        }

        while (openBraces > 0) {
            fixed.append("}");
            openBraces--;
        }

        return fixed.toString();
    }

    private ResumeExtractionResult createMockExtractionResult(String jobDescription) {
        ResumeExtractionResult result = new ResumeExtractionResult();
        result.setSkills(Arrays.asList("Java", "Spring Boot", "SQL", "Docker"));
        result.setExperienceYears(5);
        result.setEducation("Bachelor's in Computer Science");
        result.setAchievements(Arrays.asList("Led 3 projects", "Improved system performance"));
        result.setStrengths(Arrays.asList("Problem solving", "Team collaboration"));
        result.setMissingRequirements(Arrays.asList("Kubernetes"));
        result.setTechStack(Arrays.asList("Java", "Spring", "PostgreSQL"));
        result.setMatchScore(75);
        result.setConfidence(0.8);
        result.setSummary("Good match with minor gaps");
        return result;
    }

    private List<InterviewQuestion> createMockInterviewQuestions() {
        List<InterviewQuestion> questions = new ArrayList<>();
        questions.add(new InterviewQuestion(1, "technical", "Describe your experience with microservices", "hard", 10, "Look for architectural understanding"));
        questions.add(new InterviewQuestion(2, "behavioral", "Tell about a time you led a team", "medium", 8, "Look for leadership skills"));
        return questions;
    }

    private RejectionGuidance createMockRejectionGuidance() {
        RejectionGuidance guidance = new RejectionGuidance();
        guidance.setRejectionReasons(Arrays.asList("Missing experience with required framework"));
        guidance.setAlternativeRoles(Arrays.asList("Junior Developer", "Backend Engineer"));
        guidance.setEncouragement("Keep learning! You have strong fundamentals.");
        return guidance;
    }

    private RecruiterSummary createMockRecruiterSummary() {
        RecruiterSummary summary = new RecruiterSummary();
        summary.setExecutiveSummary("Candidate shows promise with good technical foundation");
        summary.setStrengths(Arrays.asList("Problem solving", "Communication"));
        summary.setConcerns(Arrays.asList("Limited experience with specific tech"));
        summary.setRecommendation("MAYBE");
        summary.setNextSteps(Arrays.asList("Technical interview", "Reference check"));
        summary.setInterviewReadiness("Ready");
        return summary;
    }

    private int calculateMatchScore(ResumeExtractionResult result, String jobDescription) {
        int score = 0;

        if (!isEmpty(result.getSkills())) {
            score += 40;
            log.debug("Skills match: +40 points");
        }

        if (result.getExperienceYears() > 3) {
            score += 30;
            log.debug("Experience match ({}+ years): +30 points", result.getExperienceYears());
        } else if (result.getExperienceYears() > 0) {
            score += 15;
            log.debug("Some experience ({} years): +15 points", result.getExperienceYears());
        }

        if (!isEmpty(result.getTechStack())) {
            score += 20;
            log.debug("Tech stack match: +20 points");
        }

        if (result.getEducation() != null && !isEmpty(result.getAchievements())) {
            score += 10;
            log.debug("Education and achievements: +10 points");
        }

        if (score == 0 && !isEmpty(result.getSkills())) {
            score = 75;
            log.warn("Calculated score is 0 but skills are present, defaulting to 75");
        }

        return Math.min(score, 100);
    }

    private int inferExperienceYears(ResumeExtractionResult result) {
        if (isEmpty(result.getAchievements())) {
            return 0;
        }

        String achievementText = String.join(" ", result.getAchievements()).toLowerCase();

        if (achievementText.contains("senior") || achievementText.contains("lead") || achievementText.contains("architect")) {
            return 8;
        } else if (achievementText.contains("mid") || achievementText.contains("manage") || achievementText.contains("design")) {
            return 5;
        } else if (achievementText.contains("junior") || achievementText.contains("develop")) {
            return 2;
        }

        return 3;
    }

    private boolean isEmpty(List<String> list) {
        return list == null || list.isEmpty();
    }

    private static class InterviewQuestionsWrapper {
        List<InterviewQuestion> questions;
    }
}
