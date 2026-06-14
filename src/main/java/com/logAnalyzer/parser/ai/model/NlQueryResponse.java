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
    private long totalHits;
    private String provider;
    private String model;
    private InterpretedFilter interpretedFilter;
    private List<LogEntryDocument> results;
}