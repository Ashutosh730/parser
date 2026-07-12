package com.logAnalyzer.parser.ai.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class DiagnosisResponse {
    private String sessionId;
    private String timeline;
    private String provider;
    private String model;
    private boolean cached;
    private String impactSummary;
    private List<RootCause> rootCauses;
    private List<String> preventiveActions;

    @Data
    @Builder
    public static class RootCause {
        private String cause;
        private double confidence;
        private List<String> affectedComponents;
        private String firstOccurrence;
        private int frequency;
        private List<Fix> fixes;
    }

    @Data
    @Builder
    public static class Fix {
        private String suggestion;
        private String category;
        private String priority;
    }
}