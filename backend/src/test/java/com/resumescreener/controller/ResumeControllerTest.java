package com.resumescreener.controller;

import com.resumescreener.model.Session;
import com.resumescreener.service.SessionManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.multipart.MultipartFile;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@DisplayName("ResumeController Tests")
class ResumeControllerTest {

    @Mock
    private SessionManager sessionManager;

    private ResumeController resumeController;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        resumeController = new ResumeController();

        // Use reflection to set mock
        try {
            java.lang.reflect.Field field = ResumeController.class.getDeclaredField("sessionManager");
            field.setAccessible(true);
            field.set(resumeController, sessionManager);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Test
    @DisplayName("Should upload resume successfully")
    void testUploadResume_Success() {
        // Arrange
        MultipartFile file = new MockMultipartFile(
            "file",
            "resume.txt",
            "text/plain",
            "Senior Java Developer with 5 years experience".getBytes()
        );
        String jobDescription = "Looking for Java Expert";

        Session mockSession = new Session("resume.txt", "Senior Java Developer with 5 years experience", jobDescription);
        when(sessionManager.createSession(anyString(), anyString(), anyString())).thenReturn(mockSession);

        // Act
        ResponseEntity<?> response = resumeController.uploadResume(file, jobDescription);

        // Assert
        assertNotNull(response);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        verify(sessionManager, times(1)).createSession(anyString(), anyString(), anyString());
    }

    @Test
    @DisplayName("Should reject empty file")
    void testUploadResume_EmptyFile() {
        // Arrange
        MultipartFile emptyFile = new MockMultipartFile(
            "file",
            "resume.txt",
            "text/plain",
            new byte[0]
        );
        String jobDescription = "Job description";

        // Act
        ResponseEntity<?> response = resumeController.uploadResume(emptyFile, jobDescription);

        // Assert
        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
    }

    @Test
    @DisplayName("Should reject file larger than 10MB")
    void testUploadResume_FileTooLarge() {
        // Arrange
        byte[] largeContent = new byte[11 * 1024 * 1024]; // 11MB
        MultipartFile largeFile = new MockMultipartFile(
            "file",
            "resume.txt",
            "text/plain",
            largeContent
        );
        String jobDescription = "Job description";

        // Act
        ResponseEntity<?> response = resumeController.uploadResume(largeFile, jobDescription);

        // Assert
        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
    }

    @Test
    @DisplayName("Should get resume preview")
    void testGetPreview_Success() {
        // Arrange
        String sessionId = "test-session-id";
        Session session = new Session("resume.txt", "Resume content", "Job description");
        session.setId(sessionId);

        when(sessionManager.getSession(sessionId)).thenReturn(session);

        // Act
        ResponseEntity<?> response = resumeController.getPreview(sessionId);

        // Assert
        assertNotNull(response);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        verify(sessionManager, times(1)).getSession(sessionId);
    }

    @Test
    @DisplayName("Should return 404 when session not found for preview")
    void testGetPreview_NotFound() {
        // Arrange
        when(sessionManager.getSession("invalid-id"))
            .thenThrow(new RuntimeException("Session not found: invalid-id"));

        // Act
        ResponseEntity<?> response = resumeController.getPreview("invalid-id");

        // Assert
        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
    }
}
