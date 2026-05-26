package com.resumescreener.service;

import com.resumescreener.model.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.mockito.Mockito.doReturn;

@DisplayName("AIOrchestrationService Tests")
class AIOrchestrationServiceTest {

    @Mock
    private HuggingFaceClient huggingFaceClient;

    private AIOrchestrationService aiOrchestrationService;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        aiOrchestrationService = new AIOrchestrationService();
        // Use reflection to set the mock
        try {
            java.lang.reflect.Field field = AIOrchestrationService.class.getDeclaredField("hfClient");
            field.setAccessible(true);
            field.set(aiOrchestrationService, huggingFaceClient);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Test
    @DisplayName("Should analyze resume and return extraction result with valid data")
    void testAnalyzeResume_Success() {
        // Arrange
        String resumeText = "Senior Java Developer with 5 years experience";
        String jobDescription = "Looking for Java Expert";
        String mockResponse = "{\"choices\":[{\"message\":{\"content\":\"{\\\"skills\\\":[\\\"Java\\\"],\\\"experience_years\\\":5,\\\"match_score\\\":75}\"}}]}";

        when(huggingFaceClient.callLLM(anyString(), anyString()))
            .thenReturn(mockResponse);

        // Act
        ResumeExtractionResult result = aiOrchestrationService.analyzeResume(resumeText, jobDescription);

        // Assert
        assertNotNull(result);
        verify(huggingFaceClient, times(1)).callLLM(anyString(), anyString());
    }

    @Test
    @DisplayName("Should return mock data when LLM call fails")
    void testAnalyzeResume_Failure() {
        // Arrange
        String resumeText = "Resume text";
        String jobDescription = "Job description";

        when(huggingFaceClient.callLLM(anyString(), anyString()))
            .thenThrow(new RuntimeException("API Error"));

        // Act
        ResumeExtractionResult result = aiOrchestrationService.analyzeResume(resumeText, jobDescription);

        // Assert
        assertNotNull(result);
        assertNotNull(result.getSkills());
        assertTrue(result.getMatchScore() > 0);
    }

    @Test
    @DisplayName("Should generate interview questions for high match score")
    void testProcessCandidate_HighScore_InterviewQuestions() {
        // Arrange
        Session session = new Session("resume.pdf", "Resume text", "Job description");
        ResumeExtractionResult extraction = new ResumeExtractionResult();
        extraction.setMatchScore(75);
        extraction.setSkills(Arrays.asList("Java", "Spring"));
        session.setExtractionResult(extraction);

        String mockInterviewResponse = "{\"questions\":[{\"id\":1,\"category\":\"technical\",\"question\":\"Test?\",\"difficulty\":\"medium\",\"time_estimate_minutes\":10,\"tip\":\"Look for...\"}]}";
        String mockSummaryResponse = "{\"executive_summary\":\"Good match\",\"strengths\":[\"skill1\"],\"concerns\":[],\"recommendation\":\"YES\",\"next_steps\":[\"interview\"],\"interview_readiness\":\"Ready\"}";

        when(huggingFaceClient.callLLM(anyString(), anyString()))
            .thenReturn(mockInterviewResponse)
            .thenReturn(mockSummaryResponse);

        // Act
        aiOrchestrationService.processCandidate(session);

        // Assert
        assertNotNull(session.getInterviewQuestions());
        assertNull(session.getRejectionGuidance());
        assertNotNull(session.getRecruiterSummary());
        verify(huggingFaceClient, atLeast(2)).callLLM(anyString(), anyString());
    }

    @Test
    @DisplayName("Should generate rejection guidance for low match score")
    void testProcessCandidate_LowScore_RejectionGuidance() {
        // Arrange
        Session session = new Session("resume.pdf", "Resume text", "Job description");
        ResumeExtractionResult extraction = new ResumeExtractionResult();
        extraction.setMatchScore(45);
        extraction.setSkills(Arrays.asList("Python"));
        extraction.setMissingRequirements(Arrays.asList("Java", "Spring"));
        session.setExtractionResult(extraction);

        String mockFeedbackResponse = "{\"rejection_reasons\":[\"Missing Java\"],\"improvements\":[],\"alternative_roles\":[\"role1\"],\"encouragement\":\"Good luck!\"}";
        String mockSummaryResponse = "{\"executive_summary\":\"Not a match\",\"strengths\":[\"Python\"],\"concerns\":[\"Missing Java\"],\"recommendation\":\"NO\",\"next_steps\":[],\"interview_readiness\":\"Not ready\"}";

        when(huggingFaceClient.callLLM(anyString(), anyString()))
            .thenReturn(mockFeedbackResponse)
            .thenReturn(mockSummaryResponse);

        // Act
        aiOrchestrationService.processCandidate(session);

        // Assert
        assertNull(session.getInterviewQuestions());
        assertNotNull(session.getRejectionGuidance());
        assertNotNull(session.getRecruiterSummary());
        verify(huggingFaceClient, atLeast(2)).callLLM(anyString(), anyString());
    }

    @Test
    @DisplayName("Should always generate recruiter summary")
    void testProcessCandidate_AlwaysGeneratesSummary() {
        // Arrange
        Session session = new Session("resume.pdf", "Resume text", "Job description");
        ResumeExtractionResult extraction = new ResumeExtractionResult();
        extraction.setMatchScore(75);
        extraction.setSkills(Arrays.asList("Java"));
        session.setExtractionResult(extraction);

        String mockInterviewResponse = "{\"questions\":[{\"id\":1,\"category\":\"technical\",\"question\":\"Test?\",\"difficulty\":\"medium\",\"time_estimate_minutes\":10,\"tip\":\"Look for...\"}]}";
        String mockSummaryResponse = "{\"executive_summary\":\"Good match\",\"strengths\":[\"skill1\"],\"concerns\":[],\"recommendation\":\"YES\",\"next_steps\":[\"interview\"],\"interview_readiness\":\"Ready\"}";

        when(huggingFaceClient.callLLM(anyString(), anyString()))
            .thenReturn(mockInterviewResponse)
            .thenReturn(mockSummaryResponse);

        // Act
        aiOrchestrationService.processCandidate(session);

        // Assert
        assertNotNull(session.getRecruiterSummary());
    }
}
