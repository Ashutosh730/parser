package com.logAnalyzer.ai.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "log_embeddings")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LogEmbedding {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;

    @Column(name = "session_id", nullable = false)
    private String sessionId;

    @Column(name = "log_document_id", nullable = false)
    private String logDocumentId;    // links to Elasticsearch doc id

    @Column(name = "embedding_text", columnDefinition = "TEXT")
    private String embeddingText;    // what was embedded

    @Column(name = "level", nullable = false)
    private String level;            // ERROR or WARN

    @Column(name = "embedding", columnDefinition = "vector(1536)")
    private float[] embedding;       // the actual vector

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @PrePersist
    public void prePersist() {
        createdAt = LocalDateTime.now();
    }
}