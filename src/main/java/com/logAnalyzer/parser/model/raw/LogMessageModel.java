package com.logAnalyzer.parser.model.raw;

import lombok.Data;
import org.springframework.stereotype.Component;

@Data
@Component
public class LogMessageModel {
    private String logLine;
    private String fileName;
    private Long timestamp;
    private String uploadSessionId;
}
