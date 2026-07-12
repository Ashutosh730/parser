package com.logAnalyzer.parser.ai.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class SummaryResponse{
    String sessionId;
    String summary;
    boolean cached;
    String provider;
    String model;
}