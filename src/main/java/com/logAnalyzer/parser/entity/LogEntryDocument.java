package com.logAnalyzer.parser.entity;

import com.logAnalyzer.parser.enums.LogLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import org.springframework.data.annotation.Id;
import org.springframework.data.elasticsearch.annotations.Document;

import java.time.LocalDateTime;
import java.util.UUID;

@Document(indexName = "log_entries")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LogEntryDocument {

    @Id
    private String id;

    private String sessionId;
    private LocalDateTime logTimestamp;

    private LogLevel level;

    private String thread;
    private String className;

    private String message;

    private String pid;

    private String rawLog;

    private LocalDateTime createdAt;

    public void initialize() {
        if (id == null) {
            id = UUID.randomUUID().toString();
        }
        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }
    }
}

