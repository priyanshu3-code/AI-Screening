package com.resumescreener.controller;

import com.resumescreener.dto.AnalysisRequest;
import com.resumescreener.dto.AnalysisResponse;
import com.resumescreener.dto.ErrorResponse;
import com.resumescreener.model.Session;
import com.resumescreener.service.AIOrchestrationService;
import com.resumescreener.service.SessionManager;
import com.resumescreener.security.SafetyValidationService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/analysis")
@CrossOrigin(origins = "${cors.allowed.origins:http://localhost:4200}")
public class AnalysisController {

    private static final Logger log = LoggerFactory.getLogger(AnalysisController.class);

    @Autowired
    private AIOrchestrationService aiService;

    @Autowired
    private SessionManager sessionManager;

    @Autowired
    private SafetyValidationService safetyValidationService;

    @PostMapping("/screen")
    public ResponseEntity<?> analyzeResume(@RequestBody AnalysisRequest request) {
        if (request == null || request.getSessionId() == null || request.getSessionId().isBlank()) {
            log.warn("Analysis request received with missing sessionId");
            return ResponseEntity.badRequest().body(new ErrorResponse(
                "Session ID is required",
                400
            ));
        }

        if (request.getResumeText() == null || request.getResumeText().isBlank()) {
            log.warn("Analysis request received with missing resume text for session: {}", request.getSessionId());
            return ResponseEntity.badRequest().body(new ErrorResponse(
                "Resume text is required",
                400
            ));
        }

        log.info("Analysis request received for session: {}", request.getSessionId());
        long startTime = System.currentTimeMillis();

        try {
            Session session = sessionManager.getSession(request.getSessionId());
            if (session == null) {
                log.warn("Session not found: {}", request.getSessionId());
                return ResponseEntity.status(404).body(new ErrorResponse(
                    "Session not found",
                    404
                ));
            }

            // SAFETY VALIDATION: Check for prompt injection and PII before processing
            SafetyValidationService.SafetyValidationResult safetyResult =
                safetyValidationService.validateForAiProcessing(
                    request.getResumeText(),
                    request.getJobDescription(),
                    request.getSessionId()
                );

            // If blocked, return error to user
            if (safetyResult.isBlocked()) {
                log.warn("Analysis blocked for session {} - Reason: {}",
                    request.getSessionId(), safetyResult.getBlockReason());
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(new ErrorResponse(
                    safetyResult.getBlockReason(),
                    400
                ));
            }

            // If there are warnings, log them
            if (safetyResult.hasWarnings()) {
                log.warn("Safety warnings for session {}: {}",
                    request.getSessionId(), safetyResult.getWarnings());
            }

            // LLM Call 1: Resume Extraction
            var extractionResult = aiService.analyzeResume(
                request.getResumeText(),
                request.getJobDescription()
            );
            session.setExtractionResult(extractionResult);

            // Conditional LLM Calls 2A/2B + 3
            aiService.processCandidate(session);

            session.setTotalProcessingTimeMs(System.currentTimeMillis() - startTime);
            sessionManager.updateSession(session);

            log.info("Analysis completed in {}ms", session.getTotalProcessingTimeMs());
            return ResponseEntity.ok(buildAnalysisResponse(session));

        } catch (IllegalArgumentException e) {
            log.error("Invalid argument in analysis: {}", e.getMessage());
            return ResponseEntity.badRequest().body(new ErrorResponse(
                "Invalid request: " + e.getMessage(),
                400
            ));
        } catch (Exception e) {
            log.error("Analysis failed", e);
            return ResponseEntity.status(500).body(new ErrorResponse(
                "Analysis failed: " + e.getMessage(),
                500
            ));
        }
    }

    @GetMapping("/{sessionId}/results")
    public ResponseEntity<?> getResults(@PathVariable String sessionId) {
        if (sessionId == null || sessionId.isBlank()) {
            log.warn("Results request received with missing sessionId");
            return ResponseEntity.badRequest().body(new ErrorResponse(
                "Session ID is required",
                400
            ));
        }

        try {
            Session session = sessionManager.getSession(sessionId);
            if (session == null) {
                log.warn("Session not found for results: {}", sessionId);
                return ResponseEntity.status(404).body(new ErrorResponse(
                    "Session not found",
                    404
                ));
            }
            return ResponseEntity.ok(buildAnalysisResponse(session));
        } catch (Exception e) {
            log.error("Failed to retrieve results for session: {}", sessionId, e);
            return ResponseEntity.status(500).body(new ErrorResponse(
                "Failed to retrieve results: " + e.getMessage(),
                500
            ));
        }
    }

    private AnalysisResponse buildAnalysisResponse(Session session) {
        AnalysisResponse response = new AnalysisResponse(session);
        return response;
    }
}
