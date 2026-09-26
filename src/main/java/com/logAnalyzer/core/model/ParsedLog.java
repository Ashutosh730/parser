package com.logAnalyzer.core.model;

import com.logAnalyzer.core.enums.DetectedFramework;
import com.logAnalyzer.core.enums.LogLevel;
import lombok.Builder;
import lombok.Data;

import java.time.Instant;

@Data
@Builder
public class ParsedLog {

    private Instant timestamp;
    private LogLevel level;
    private String thread;
    private String className;
    private String message;
    private String pid;
    private DetectedFramework framework;

    private String rawLog;   // if not parsed, store the raw log for reference
}