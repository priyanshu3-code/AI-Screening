package com.resumescreener.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class InterviewQuestion {

    private int id;

    private String category;

    private String question;

    private String difficulty;

    @JsonProperty("time_estimate_minutes")
    private int timeEstimateMinutes;

    private String tip;
}
