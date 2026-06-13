package com.logAnalyzer.parser.ai.entity;

import com.logAnalyzer.parser.ai.enums.AiFeatureEnum;
import com.logAnalyzer.parser.ai.enums.LlmProviderEnum;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "ai_results")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AiResult {

    @Id
    private String id;

    @Column(name = "session_id", nullable = false)
    private String sessionId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 50)
    private AiFeatureEnum feature;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private LlmProviderEnum provider;

    @Column(nullable = false, length = 50)
    private String model;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String result;

    @Column(name = "tokens_used")
    private Integer tokensUsed;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @PrePersist
    public void prePersist() {
        if (id == null) {
            id = UUID.randomUUID().toString();
        }

        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }
    }
}