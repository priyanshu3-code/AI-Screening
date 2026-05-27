package com.resumescreener.controller;

import com.resumescreener.dto.CandidateInsights;
import com.resumescreener.dto.ErrorResponse;
import com.resumescreener.model.Session;
import com.resumescreener.service.CandidateInsightsService;
import com.resumescreener.service.SessionManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * REST API for Candidate Insights Dashboard.
 * Provides actionable recruiter insights synthesized from resume analysis.
 */
@RestController
@RequestMapping("/api/v1/insights")
@CrossOrigin(origins = "${cors.allowed.origins:http://localhost:4200}")
public class InsightsController {

    private static final Logger log = LoggerFactory.getLogger(InsightsController.class);

    @Autowired
    private CandidateInsightsService insightsService;

    @Autowired
    private SessionManager sessionManager;

    /**
     * Get candidate insights for a completed analysis.
     *
     * @param sessionId Session ID from completed analysis
     * @return Candidate insights dashboard data
     */
    @GetMapping("/{sessionId}")
    public ResponseEntity<?> getCandidateInsights(@PathVariable String sessionId) {
        if (sessionId == null || sessionId.isBlank()) {
            return ResponseEntity.badRequest().body(new ErrorResponse(
                "Session ID is required",
                400
            ));
        }

        try {
            Session session = sessionManager.getSession(sessionId);
            if (session == null) {
                return ResponseEntity.status(404).body(new ErrorResponse(
                    "Session not found",
                    404
                ));
            }

            if (session.getExtractionResult() == null) {
                return ResponseEntity.status(400).body(new ErrorResponse(
                    "Analysis not completed for this session",
                    400
                ));
            }

            CandidateInsights insights = insightsService.generateInsights(
                sessionId,
                session.getResumeText(),
                session.getJobDescription(),
                session.getExtractionResult()
            );

            return ResponseEntity.ok(insights);

        } catch (Exception e) {
            log.error("Failed to generate insights for session: {}", sessionId, e);
            return ResponseEntity.status(500).body(new ErrorResponse(
                "Failed to generate insights: " + e.getMessage(),
                500
            ));
        }
    }

    /**
     * Get recommendations summary (quick view).
     * Lighter payload for dashboard widgets.
     *
     * @param sessionId Session ID
     * @return Just the hiring recommendation
     */
    @GetMapping("/{sessionId}/recommendation")
    public ResponseEntity<?> getRecommendation(@PathVariable String sessionId) {
        try {
            Session session = sessionManager.getSession(sessionId);
            if (session == null || session.getExtractionResult() == null) {
                return ResponseEntity.status(404).body(new ErrorResponse(
                    "Session or analysis not found",
                    404
                ));
            }

            CandidateInsights insights = insightsService.generateInsights(
                sessionId,
                session.getResumeText(),
                session.getJobDescription(),
                session.getExtractionResult()
            );

            return ResponseEntity.ok(insights.getRecommendation());

        } catch (Exception e) {
            log.error("Failed to get recommendation for session: {}", sessionId, e);
            return ResponseEntity.status(500).body(new ErrorResponse(
                "Failed to get recommendation",
                500
            ));
        }
    }

    /**
     * Get skill fit analysis.
     *
     * @param sessionId Session ID
     * @return Skill matching breakdown
     */
    @GetMapping("/{sessionId}/skills")
    public ResponseEntity<?> getSkillAnalysis(@PathVariable String sessionId) {
        try {
            Session session = sessionManager.getSession(sessionId);
            if (session == null || session.getExtractionResult() == null) {
                return ResponseEntity.status(404).body(new ErrorResponse(
                    "Session or analysis not found",
                    404
                ));
            }

            CandidateInsights insights = insightsService.generateInsights(
                sessionId,
                session.getResumeText(),
                session.getJobDescription(),
                session.getExtractionResult()
            );

            return ResponseEntity.ok(insights.getSkillFit());

        } catch (Exception e) {
            log.error("Failed to get skill analysis for session: {}", sessionId, e);
            return ResponseEntity.status(500).body(new ErrorResponse(
                "Failed to get skill analysis",
                500
            ));
        }
    }

    /**
     * Get risk assessment.
     *
     * @param sessionId Session ID
     * @return Risk flags and mitigation strategies
     */
    @GetMapping("/{sessionId}/risks")
    public ResponseEntity<?> getRiskAssessment(@PathVariable String sessionId) {
        try {
            Session session = sessionManager.getSession(sessionId);
            if (session == null || session.getExtractionResult() == null) {
                return ResponseEntity.status(404).body(new ErrorResponse(
                    "Session or analysis not found",
                    404
                ));
            }

            CandidateInsights insights = insightsService.generateInsights(
                sessionId,
                session.getResumeText(),
                session.getJobDescription(),
                session.getExtractionResult()
            );

            return ResponseEntity.ok(insights.getRiskFlags());

        } catch (Exception e) {
            log.error("Failed to get risk assessment for session: {}", sessionId, e);
            return ResponseEntity.status(500).body(new ErrorResponse(
                "Failed to get risk assessment",
                500
            ));
        }
    }
}
