package com.resumescreener.controller;

import com.resumescreener.dto.AnalysisRequest;
import com.resumescreener.model.*;
import com.resumescreener.service.AIOrchestrationService;
import com.resumescreener.service.SessionManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@DisplayName("AnalysisController Tests")
class AnalysisControllerTest {

    @Mock
    private AIOrchestrationService aiOrchestrationService;

    @Mock
    private SessionManager sessionManager;

    private AnalysisController analysisController;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        analysisController = new AnalysisController();

        // Use reflection to set mocks
        try {
            java.lang.reflect.Field aiField = AnalysisController.class.getDeclaredField("aiService");
            aiField.setAccessible(true);
            aiField.set(analysisController, aiOrchestrationService);

            java.lang.reflect.Field sessionField = AnalysisController.class.getDeclaredField("sessionManager");
            sessionField.setAccessible(true);
            sessionField.set(analysisController, sessionManager);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Test
    @DisplayName("Should analyze resume and return analysis response")
    void testAnalyzeResume_Success() {
        // Arrange
        String sessionId = "test-session-id";
        AnalysisRequest request = new AnalysisRequest(
            sessionId,
            "Senior Java Developer with 5 years experience",
            "Looking for Java Expert"
        );

        Session session = new Session("resume.pdf", request.getResumeText(), request.getJobDescription());
        session.setId(sessionId);

        ResumeExtractionResult extraction = new ResumeExtractionResult();
        extraction.setMatchScore(75);
        extraction.setSkills(Arrays.asList("Java", "Spring"));

        when(sessionManager.getSession(sessionId)).thenReturn(session);
        when(aiOrchestrationService.analyzeResume(anyString(), anyString())).thenReturn(extraction);

        // Act
        ResponseEntity<?> response = analysisController.analyzeResume(request);

        // Assert
        assertNotNull(response);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        verify(sessionManager, times(1)).getSession(sessionId);
        verify(aiOrchestrationService, times(1)).analyzeResume(anyString(), anyString());
    }

    @Test
    @DisplayName("Should handle session not found error")
    void testAnalyzeResume_SessionNotFound() {
        // Arrange
        AnalysisRequest request = new AnalysisRequest(
            "invalid-id",
            "Resume text",
            "Job description"
        );

        when(sessionManager.getSession("invalid-id"))
            .thenThrow(new RuntimeException("Session not found: invalid-id"));

        // Act
        ResponseEntity<?> response = analysisController.analyzeResume(request);

        // Assert
        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
    }

    @Test
    @DisplayName("Should get analysis results for existing session")
    void testGetResults_Success() {
        // Arrange
        String sessionId = "test-session-id";
        Session session = new Session("resume.pdf", "Resume text", "Job description");
        session.setId(sessionId);

        ResumeExtractionResult extraction = new ResumeExtractionResult();
        extraction.setMatchScore(75);
        session.setExtractionResult(extraction);

        when(sessionManager.getSession(sessionId)).thenReturn(session);

        // Act
        ResponseEntity<?> response = analysisController.getResults(sessionId);

        // Assert
        assertNotNull(response);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        verify(sessionManager, times(1)).getSession(sessionId);
    }

    @Test
    @DisplayName("Should return 404 when session not found for results")
    void testGetResults_NotFound() {
        // Arrange
        when(sessionManager.getSession("invalid-id"))
            .thenThrow(new RuntimeException("Session not found: invalid-id"));

        // Act
        ResponseEntity<?> response = analysisController.getResults("invalid-id");

        // Assert
        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
    }
}
