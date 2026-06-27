package com.logAnalyzer.parser.ai.model;

import com.logAnalyzer.parser.entity.LogEntryDocument;
import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class NlQueryResponse {
    private String sessionId;
    private String originalQuery;
    private String intent;                  // "SEARCH" or "AGGREGATION"
    private long totalHits;
    private String provider;
    private String model;
    private Object aggregationResult;        // populated for AGGREGATION
    private InterpretedFilter interpretedFilter;
    private List<LogEntryDocument> results;
}