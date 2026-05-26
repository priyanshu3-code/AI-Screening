package com.resumescreener.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class RecruiterSummary {

    @JsonProperty("executive_summary")
    private String executiveSummary;

    private List<String> strengths;

    private List<String> concerns;

    private String recommendation;

    @JsonProperty("next_steps")
    private List<String> nextSteps;

    @JsonProperty("interview_readiness")
    private String interviewReadiness;
}
