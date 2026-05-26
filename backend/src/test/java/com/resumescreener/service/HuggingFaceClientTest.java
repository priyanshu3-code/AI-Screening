package com.resumescreener.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.web.client.RestTemplate;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpStatus;
import org.springframework.web.client.HttpClientErrorException;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@DisplayName("HuggingFaceClient Tests")
class HuggingFaceClientTest {

    @Mock
    private RestTemplate restTemplate;

    private HuggingFaceClient huggingFaceClient;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        huggingFaceClient = new HuggingFaceClient(restTemplate);
    }

    @Test
    @DisplayName("Should successfully call LLM with valid prompt and model")
    void testCallLLMSuccess() {
        // Arrange
        String prompt = "Test prompt";
        String model = "mistralai/Mistral-7B-Instruct-v0.2:featherless-ai";
        String mockResponse = "{\"choices\":[{\"message\":{\"content\":\"Test response\"}}]}";

        when(restTemplate.postForObject(anyString(), any(HttpEntity.class), eq(String.class)))
            .thenReturn(mockResponse);

        // Act
        String result = huggingFaceClient.callLLM(prompt, model);

        // Assert
        assertNotNull(result);
        assertTrue(result.contains("choices"));
        verify(restTemplate, times(1)).postForObject(anyString(), any(HttpEntity.class), eq(String.class));
    }

    @Test
    @DisplayName("Should throw exception when API call fails")
    void testCallLLMFailure() {
        // Arrange
        String prompt = "Test prompt";
        String model = "mistralai/Mistral-7B-Instruct-v0.2:featherless-ai";

        when(restTemplate.postForObject(anyString(), any(HttpEntity.class), eq(String.class)))
            .thenThrow(new HttpClientErrorException(HttpStatus.UNAUTHORIZED));

        // Act & Assert
        assertThrows(RuntimeException.class, () -> huggingFaceClient.callLLM(prompt, model));
    }

    @Test
    @DisplayName("Should extract content from OpenAI format response")
    void testExtractJsonFromResponse_OpenAIFormat() {
        // Arrange
        String response = "{\"choices\":[{\"message\":{\"content\":\"Extracted content\"}}]}";

        // Act
        String result = huggingFaceClient.extractJsonFromResponse(response);

        // Assert
        assertEquals("Extracted content", result);
    }

    @Test
    @DisplayName("Should extract generated_text from alternative format")
    void testExtractJsonFromResponse_GeneratedTextFormat() {
        // Arrange
        String response = "{\"generated_text\":\"Alternative content\"}";

        // Act
        String result = huggingFaceClient.extractJsonFromResponse(response);

        // Assert
        assertEquals("Alternative content", result);
    }

    @Test
    @DisplayName("Should return original response when parsing fails")
    void testExtractJsonFromResponse_InvalidJSON() {
        // Arrange
        String response = "Invalid JSON response";

        // Act
        String result = huggingFaceClient.extractJsonFromResponse(response);

        // Assert
        assertEquals(response, result);
    }
}
