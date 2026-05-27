package com.resumescreener.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class Session {

    private String id;
    private LocalDateTime createdAt;
    private LocalDateTime expiresAt;

    private String resumeFileName;
    private String resumeText;

    private String jobDescription;

    private ResumeExtractionResult extractionResult;

    private List<InterviewQuestion> interviewQuestions;

    private RejectionGuidance rejectionGuidance;

    private RecruiterSummary recruiterSummary;

    private MatchScore matchScoreBreakdown;

    private long totalProcessingTimeMs;

    public Session(String resumeFileName, String resumeText, String jobDescription) {
        this.id = UUID.randomUUID().toString();
        this.createdAt = LocalDateTime.now();
        this.expiresAt = LocalDateTime.now().plusHours(24);
        this.resumeFileName = resumeFileName;
        this.resumeText = resumeText;
        this.jobDescription = jobDescription;
    }

    public boolean isExpired() {
        return LocalDateTime.now().isAfter(expiresAt);
    }
}
