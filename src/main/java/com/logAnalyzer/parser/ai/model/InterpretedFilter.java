package com.logAnalyzer.parser.ai.model;

import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class InterpretedFilter {
    private List<String> keywords;
    private List<String> level;
    private String timeFrom;
    private String timeTo;
    private String className;
    private String explanation;
}