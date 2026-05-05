package com.logAnalyzer.parser.model.parsed;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class ParsedLog {

    private String timestamp;
    private String level;
    private String thread;
    private String className;
    private String message;
    private String pid;

    private String rawLog;   // if not parsed, store the raw log for reference
}