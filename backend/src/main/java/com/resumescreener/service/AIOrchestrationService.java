package com.resumescreener.service;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.resumescreener.model.*;
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

    private final Gson gson = new Gson();

    private static final String MODEL_EXTRACTION = "mistralai/Mistral-7B-Instruct-v0.2:featherless-ai";
    private static final String MODEL_INTERVIEW = "mistralai/Mistral-7B-Instruct-v0.2:featherless-ai";
    private static final String MODEL_SUMMARY = "meta-llama/Llama-3.1-8B-Instruct:novita";

    public ResumeExtractionResult analyzeResume(String resumeText, String jobDescription) {
        log.info("Starting LLM Call 1: Resume Extraction");
        long startTime = System.currentTimeMillis();

        try {
            String prompt = buildExtractionPrompt(resumeText, jobDescription);
            String response = hfClient.callLLM(prompt, MODEL_EXTRACTION);
            String jsonContent = extractJsonContent(response);

            ResumeExtractionResult result = gson.fromJson(jsonContent, ResumeExtractionResult.class);

            log.info("LLM Call 1 completed in {}ms", System.currentTimeMillis() - startTime);
            return result;
        } catch (Exception e) {
            log.error("Resume analysis failed, using mock data", e);
            return createMockExtractionResult(jobDescription);
        }
    }

    public void processCandidate(Session session) {
        log.info("Processing candidate for session: {}", session.getId());

        ResumeExtractionResult extraction = session.getExtractionResult();
        int matchScore = extraction.getMatchScore();

        if (matchScore >= 70) {
            log.info("Score {} >= 70%, generating interview questions (LLM Call 2A)", matchScore);
            List<InterviewQuestion> questions = generateInterviewQuestions(extraction, session.getJobDescription());
            session.setInterviewQuestions(questions);
        } else {
            log.info("Score {} < 70%, generating rejection guidance (LLM Call 2B)", matchScore);
            RejectionGuidance guidance = generateRejectionGuidance(extraction, session.getJobDescription());
            session.setRejectionGuidance(guidance);
        }

        log.info("Generating recruiter summary (LLM Call 3)");
        RecruiterSummary summary = generateRecruiterSummary(extraction, session.getJobDescription());
        session.setRecruiterSummary(summary);

        log.info("Candidate processing complete");
    }

    private List<InterviewQuestion> generateInterviewQuestions(ResumeExtractionResult resume, String jobDescription) {
        try {
            String prompt = buildInterviewPrompt(resume, jobDescription);
            String response = hfClient.callLLM(prompt, MODEL_INTERVIEW);
            String jsonContent = extractJsonContent(response);

            InterviewQuestionsWrapper wrapper = gson.fromJson(jsonContent, InterviewQuestionsWrapper.class);
            return wrapper.questions;
        } catch (Exception e) {
            log.error("Interview question generation failed, using mock data", e);
            return createMockInterviewQuestions();
        }
    }

    private RejectionGuidance generateRejectionGuidance(ResumeExtractionResult resume, String jobDescription) {
        try {
            String prompt = buildFeedbackPrompt(resume, jobDescription);
            String response = hfClient.callLLM(prompt, MODEL_INTERVIEW);
            String jsonContent = extractJsonContent(response);

            return gson.fromJson(jsonContent, RejectionGuidance.class);
        } catch (Exception e) {
            log.error("Rejection guidance generation failed, using mock data", e);
            return createMockRejectionGuidance();
        }
    }

    private RecruiterSummary generateRecruiterSummary(ResumeExtractionResult resume, String jobDescription) {
        try {
            String prompt = buildSummaryPrompt(resume, jobDescription);
            String response = hfClient.callLLM(prompt, MODEL_SUMMARY);
            String jsonContent = extractJsonContent(response);

            return gson.fromJson(jsonContent, RecruiterSummary.class);
        } catch (Exception e) {
            log.error("Recruiter summary generation failed, using mock data", e);
            return createMockRecruiterSummary();
        }
    }

    private String buildExtractionPrompt(String resumeText, String jobDescription) {
        return """
            You are an expert HR recruiter. Analyze this resume against the job description.
            Extract and structure the candidate's information as JSON.

            Resume:
            """ + resumeText + """

            Job Description:
            """ + jobDescription + """

            Return ONLY a valid JSON object (no markdown, no extra text) with these fields:
            {
              "skills": ["skill1", "skill2"],
              "experience_years": 5,
              "education": "Bachelor's in Computer Science",
              "achievements": ["achievement1", "achievement2"],
              "strengths": ["strength1", "strength2"],
              "missing_requirements": ["requirement1"],
              "tech_stack": ["tech1", "tech2"],
              "match_score": 75,
              "confidence": 0.85,
              "summary": "Brief summary"
            }
            """;
    }

    private String buildInterviewPrompt(ResumeExtractionResult resume, String jobDescription) {
        return """
            You are a senior technical hiring manager. Generate 8-10 interview questions
            tailored to this candidate and job role.

            Candidate Skills: """ + String.join(", ", resume.getSkills()) + """

            Job Requirements: """ + jobDescription + """

            Return ONLY valid JSON (no markdown, no extra text):
            {
              "questions": [
                {
                  "id": 1,
                  "category": "technical",
                  "question": "Describe your experience with...",
                  "difficulty": "medium",
                  "time_estimate_minutes": 10,
                  "tip": "Look for..."
                }
              ]
            }
            """;
    }

    private String buildFeedbackPrompt(ResumeExtractionResult resume, String jobDescription) {
        return """
            You are a compassionate career coach. This candidate didn't meet requirements.
            Provide constructive feedback and improvement suggestions.

            Match Score: """ + resume.getMatchScore() + """
            Missing Requirements: """ + String.join(", ", resume.getMissingRequirements()) + """

            Return ONLY valid JSON (no markdown, no extra text):
            {
              "rejection_reasons": ["reason1", "reason2"],
              "improvements": [
                {
                  "skill": "Skill Name",
                  "current_level": "beginner",
                  "recommended_resources": ["resource1", "resource2"],
                  "estimated_months": 6
                }
              ],
              "alternative_roles": ["role1", "role2"],
              "encouragement": "Encouraging message"
            }
            """;
    }

    private String buildSummaryPrompt(ResumeExtractionResult resume, String jobDescription) {
        return """
            You are a professional recruiter writing a hiring summary for a hiring manager.

            Candidate Match Score: """ + resume.getMatchScore() + """
            Skills: """ + String.join(", ", resume.getSkills()) + """

            Return ONLY valid JSON (no markdown, no extra text):
            {
              "executive_summary": "150-200 word professional summary",
              "strengths": ["strength1", "strength2", "strength3"],
              "concerns": ["concern1"],
              "recommendation": "YES",
              "next_steps": ["step1", "step2"],
              "interview_readiness": "Ready for technical interview"
            }
            """;
    }

    private String extractJsonContent(String response) {
        try {
            String cleaned = hfClient.extractJsonFromResponse(response);

            int jsonStart = cleaned.indexOf('{');
            int jsonEnd = cleaned.lastIndexOf('}');

            if (jsonStart != -1 && jsonEnd != -1 && jsonEnd > jsonStart) {
                return cleaned.substring(jsonStart, jsonEnd + 1);
            }

            return cleaned;
        } catch (Exception e) {
            log.warn("Could not extract JSON from response", e);
            return response;
        }
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

    private static class InterviewQuestionsWrapper {
        List<InterviewQuestion> questions;
    }
}
