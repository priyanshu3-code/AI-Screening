package com.resumescreener.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class RejectionGuidance {

    @JsonProperty("rejection_reasons")
    private List<String> rejectionReasons;

    private List<Improvement> improvements;

    @JsonProperty("alternative_roles")
    private List<String> alternativeRoles;

    private String encouragement;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Improvement {

        private String skill;

        @JsonProperty("current_level")
        private String currentLevel;

        @JsonProperty("recommended_resources")
        private List<String> recommendedResources;

        @JsonProperty("estimated_months")
        private int estimatedMonths;
    }
}
