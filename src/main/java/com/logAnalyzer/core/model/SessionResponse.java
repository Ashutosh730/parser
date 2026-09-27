package com.logAnalyzer.core.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.logAnalyzer.core.enums.DetectedFramework;
import com.logAnalyzer.core.enums.DetectedLanguage;
import com.logAnalyzer.core.enums.LogSessionStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class SessionResponse {
    private String message;
    private String fileName;
    private String sessionId;
    private LocalDateTime uploadedAt;
    private LogSessionStatus status;
    private LocalDateTime completedAt;
    private Integer totalLines = 0;
    private Integer errorCount = 0;
    private Integer warnCount = 0;
    private DetectedLanguage detectedLanguage;
    private DetectedFramework detectedFramework;
}
