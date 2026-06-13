package com.logAnalyzer.parser.ai.model;

import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class RootCauseResponse {
    private List<RootCause> rootCauses;
    private String timeline;
    private String impactSummary;
    private String provider;
    private String model;
    private boolean cached;

    @Data
    @Builder
    public static class RootCause {
        private String cause;
        private double confidence;
        private List<String> affectedComponents;
        private String firstOccurrence;
        private int frequency;
    }
}