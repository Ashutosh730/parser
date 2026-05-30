package com.logAnalyzer.parser.model.parsed;

import com.logAnalyzer.parser.enums.DetectedFramework;
import com.logAnalyzer.parser.enums.LogLevel;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class ParsedLog {

    private String timestamp;
    private LogLevel level;
    private String thread;
    private String className;
    private String message;
    private String pid;
    private DetectedFramework framework;

    private String rawLog;   // if not parsed, store the raw log for reference
}