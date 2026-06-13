package com.logAnalyzer.parser.entity;

import com.logAnalyzer.parser.enums.DetectedFramework;
import com.logAnalyzer.parser.enums.DetectedLanguage;
import com.logAnalyzer.parser.enums.LogSessionStatus;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "log_sessions")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LogSession {

    @Id
    @Column(name = "id", length = 36, nullable = false)
    private String id;

    @Column(name = "file_name")
    private String fileName;

    @Column(name = "storage_path", length = 500)
    private String storagePath;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", length = 20)
    private LogSessionStatus status;

    @Column(name = "uploaded_at")
    private LocalDateTime uploadedAt;

    @Column(name = "completed_at")
    private LocalDateTime completedAt;

    @Column(name = "total_lines")
    private Integer totalLines = 0;

    @Column(name = "error_count")
    private Integer errorCount = 0;

    @Column(name = "warn_count")
    private Integer warnCount = 0;

    @Enumerated(EnumType.STRING)
    @Column(name = "detected_language", length = 50)
    private DetectedLanguage detectedLanguage;

    @Enumerated(EnumType.STRING)
    @Column(name = "detected_framework", length = 50)
    private DetectedFramework detectedFramework;

    @Column(name = "failure_reason", length = 500)
    private String failureReason;
}