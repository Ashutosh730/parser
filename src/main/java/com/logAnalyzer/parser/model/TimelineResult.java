package com.logAnalyzer.parser.model;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class TimelineResult {
    private String timestamp;
    private long count;
}