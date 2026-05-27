package com.resumescreener.ai.inference;

import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Configuration for Hugging Face local inference.
 * Loaded from application.yml under huggingface.local-inference section.
 */
@Data
@NoArgsConstructor
public class HuggingFaceInferenceConfig {

    private boolean enabled = true;
    private int modelCacheSize = 5;

    // Summarization config
    private SummarizationConfig summarization = new SummarizationConfig();

    // Skill extraction config
    private SkillExtractionConfig skillExtraction = new SkillExtractionConfig();

    // Match scoring config
    private MatchScoringConfig matchScoring = new MatchScoringConfig();

    // Toxicity detection config
    private ToxicityDetectionConfig toxicityDetection = new ToxicityDetectionConfig();

    @Data
    @NoArgsConstructor
    public static class SummarizationConfig {
        private boolean enabled = true;
        private String model = "facebook/bart-large-cnn";
        private int maxLength = 200;
        private int minLength = 50;
        private double confidence = 0.8;
    }

    @Data
    @NoArgsConstructor
    public static class SkillExtractionConfig {
        private boolean enabled = true;
        private String model = "bert-base-uncased";
        private double confidenceThreshold = 0.7;
        private int maxSkills = 50;
    }

    @Data
    @NoArgsConstructor
    public static class MatchScoringConfig {
        private boolean enabled = true;
        private String model = "sentence-transformers/all-MiniLM-L6-v2";
        private boolean useSemanticSimilarity = true;
        private double similarityThreshold = 0.5;
    }

    @Data
    @NoArgsConstructor
    public static class ToxicityDetectionConfig {
        private boolean enabled = false;  // Optional, disabled by default
        private String model = "distilbert-base-uncased-finetuned-sst-2-english";
        private double severityThreshold = 0.8;
    }
}
