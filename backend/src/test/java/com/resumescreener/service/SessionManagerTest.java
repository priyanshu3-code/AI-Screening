package com.resumescreener.service;

import com.resumescreener.model.Session;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("SessionManager Tests")
class SessionManagerTest {

    private SessionManager sessionManager;

    @BeforeEach
    void setUp() {
        sessionManager = new SessionManager();
    }

    @Test
    @DisplayName("Should create a new session with unique ID")
    void testCreateSession() {
        // Arrange
        String resumeFileName = "resume.pdf";
        String resumeText = "Sample resume text";
        String jobDescription = "Sample job description";

        // Act
        Session session = sessionManager.createSession(resumeFileName, resumeText, jobDescription);

        // Assert
        assertNotNull(session);
        assertNotNull(session.getId());
        assertEquals(resumeFileName, session.getResumeFileName());
        assertEquals(resumeText, session.getResumeText());
        assertEquals(jobDescription, session.getJobDescription());
        assertNotNull(session.getCreatedAt());
        assertNotNull(session.getExpiresAt());
    }

    @Test
    @DisplayName("Should retrieve existing session by ID")
    void testGetSession() {
        // Arrange
        Session created = sessionManager.createSession("resume.pdf", "text", "job desc");

        // Act
        Session retrieved = sessionManager.getSession(created.getId());

        // Assert
        assertNotNull(retrieved);
        assertEquals(created.getId(), retrieved.getId());
    }

    @Test
    @DisplayName("Should throw exception for non-existent session")
    void testGetSession_NotFound() {
        // Act & Assert
        assertThrows(RuntimeException.class, () -> sessionManager.getSession("non-existent-id"));
    }

    @Test
    @DisplayName("Should update existing session")
    void testUpdateSession() {
        // Arrange
        Session session = sessionManager.createSession("resume.pdf", "text", "job");
        session.setTotalProcessingTimeMs(5000);

        // Act
        sessionManager.updateSession(session);
        Session updated = sessionManager.getSession(session.getId());

        // Assert
        assertEquals(5000, updated.getTotalProcessingTimeMs());
    }

    @Test
    @DisplayName("Should delete session")
    void testDeleteSession() {
        // Arrange
        Session session = sessionManager.createSession("resume.pdf", "text", "job");
        String sessionId = session.getId();

        // Act
        sessionManager.deleteSession(sessionId);

        // Assert
        assertThrows(RuntimeException.class, () -> sessionManager.getSession(sessionId));
    }

    @Test
    @DisplayName("Should maintain session count")
    void testGetActiveSessionCount() {
        // Arrange
        sessionManager.createSession("resume1.pdf", "text1", "job1");
        sessionManager.createSession("resume2.pdf", "text2", "job2");

        // Act
        int count = sessionManager.getActiveSessionCount();

        // Assert
        assertEquals(2, count);
    }

    @Test
    @DisplayName("Session should not be expired immediately after creation")
    void testSessionNotExpired() {
        // Arrange
        Session session = sessionManager.createSession("resume.pdf", "text", "job");

        // Act
        boolean isExpired = session.isExpired();

        // Assert
        assertFalse(isExpired);
    }
}
