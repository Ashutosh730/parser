package com.logAnalyzer.core.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.OffsetDateTime;

@Entity
@Table(
        name = "log_parser_config",
        uniqueConstraints = @UniqueConstraint(name = "uk_log_parser_language_framework", columnNames = {"language", "framework"})
)
@Getter
@Setter
@NoArgsConstructor
public class LogParserConfig {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 50)
    private String language;

    @Column(length = 100)
    private String framework;

    @Column(name = "language_log_regex", nullable = false, columnDefinition = "text[]")
    @JdbcTypeCode(SqlTypes.ARRAY)
    private String[] languageLogRegex;

    @Column(name = "framework_log_regex", columnDefinition = "text")
    private String frameworkLogRegex;

    @Column(name = "created_at", nullable = false)
    private OffsetDateTime createdAt;

    @Column(name = "created_by", length = 100)
    private String createdBy;

    @Column(name = "updated_at", nullable = false)
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
