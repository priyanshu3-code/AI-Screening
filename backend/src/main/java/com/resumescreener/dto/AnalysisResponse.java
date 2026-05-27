package com.resumescreener.dto;

import com.resumescreener.model.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class AnalysisResponse {

    // Original fields (from LLM analysis)
    private String sessionId;
    private ResumeExtractionResult extractedData;
    private List<InterviewQuestion> interviewQuestions;
    private RejectionGuidance rejectionGuidance;
    private RecruiterSummary recruiterSummary;
    private long processingTimeMs;

    // New Hugging Face feature fields (from local inference)
    private SummarizedResume resumeSummary;
    private ExtractedSkills extractedSkills;
    private MatchScore matchScoreBreakdown;
    private ToxicityReport toxicityFlags;

    public AnalysisResponse(Session session) {
        this.sessionId = session.getId();
        this.extractedData = session.getExtractionResult();
        this.interviewQuestions = session.getInterviewQuestions();
        this.rejectionGuidance = session.getRejectionGuidance();
        this.recruiterSummary = session.getRecruiterSummary();
        this.processingTimeMs = session.getTotalProcessingTimeMs();
        this.matchScoreBreakdown = session.getMatchScoreBreakdown();
    }
}
