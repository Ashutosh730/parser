package com.logAnalyzer.parser.model;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class TopErrorResult {
    private String message;
    private long count;
}