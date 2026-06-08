package com.logAnalyzer.parser.ai.model;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class SummaryResponse{
    String summary;
    boolean cached;
    String provider;
}