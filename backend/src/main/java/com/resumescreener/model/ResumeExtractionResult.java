package com.resumescreener.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.gson.annotations.JsonAdapter;
import com.resumescreener.util.ResumeExtractionResultDeserializer;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@JsonAdapter(ResumeExtractionResultDeserializer.class)
public class ResumeExtractionResult {

    private List<String> skills;

    @JsonProperty("experience_years")
    private int experienceYears;

    private String education;

    private List<String> achievements;

    private List<String> strengths;

    @JsonProperty("missing_requirements")
    private List<String> missingRequirements;

    @JsonProperty("tech_stack")
    private List<String> techStack;

    @JsonProperty("match_score")
    private int matchScore;

    private double confidence;

    private String summary;
}
