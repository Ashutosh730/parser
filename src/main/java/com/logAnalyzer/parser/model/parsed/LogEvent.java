package com.logAnalyzer.parser.model.parsed;

import com.logAnalyzer.parser.enums.LogLevel;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "log_events", indexes = {
        @Index(name = "idx_log_events_session", columnList = "session_id"),
        @Index(name = "idx_log_events_level", columnList = "level")
})
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LogEvent {

    @Id
    @Column(name = "id", length = 36, nullable = false)
    private String id;

    @Column(name = "session_id", length = 36, nullable = false)
    private String sessionId;

    @Column(name = "log_timestamp")
    private String logTimestamp;

    @Enumerated(EnumType.STRING)
    @Column(name = "level", length = 20)
    private LogLevel level;

    @Column(name = "thread", length = 200)
    private String thread;

    @Column(name = "class_name", length = 500)
    private String className;

    @Lob
    @Column(name = "message")
    private String message;

    @Column(name = "pid", length = 50)
    private String pid;

    @Lob
    @Column(name = "raw_log")
    private String rawLog;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @PrePersist
    public void prePersist() {
        if (id == null) id = UUID.randomUUID().toString();
        if (createdAt == null) createdAt = LocalDateTime.now();
    }
}

