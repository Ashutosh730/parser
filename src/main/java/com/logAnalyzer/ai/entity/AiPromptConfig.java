package com.logAnalyzer.ai.entity;

import com.logAnalyzer.ai.enums.AiFeatureEnum;
import com.logAnalyzer.ai.enums.AiPromptKey;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.OffsetDateTime;

@Entity
@Table(
        name = "ai_prompt_config",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_ai_prompt_config_prompt_key",
                columnNames = "prompt_key"
        )
)
@Getter
@Setter
@NoArgsConstructor
public class AiPromptConfig {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 50)
    private AiFeatureEnum feature;

    @Enumerated(EnumType.STRING)
    @Column(name = "prompt_key", nullable = false, length = 80)
    private AiPromptKey promptKey;

    @Column(name = "system_prompt", nullable = false, columnDefinition = "TEXT")
    private String systemPrompt;

    @Column(
            name = "created_at", nullable = false,
            columnDefinition = "TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP"
    )
    private OffsetDateTime createdAt;

    @Column(name = "created_by", length = 100)
    private String createdBy;

    @Column(
            name = "updated_at", nullable = false,
            columnDefinition = "TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP"
    )
    private OffsetDateTime updatedAt;

    @Column(name = "updated_by", length = 100)
    private String updatedBy;

    @PrePersist
    void onCreate() {
        OffsetDateTime now = OffsetDateTime.now();
        createdAt = now;
        updatedAt = now;
    }

    @PreUpdate
    void onUpdate() {
        updatedAt = OffsetDateTime.now();
    }
}
