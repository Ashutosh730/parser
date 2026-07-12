package com.logAnalyzer.parser.entity;

import com.logAnalyzer.parser.enums.LogLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import org.springframework.data.annotation.Id;
import org.springframework.data.elasticsearch.annotations.*;

import java.time.Instant;
import java.util.UUID;

@Document(indexName = "log_entries")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LogEntryDocument {

    @Id
    private String id;
    @Field(type = FieldType.Keyword)
    private String sessionId;
    @Field(type = FieldType.Date)
    private Instant logTimestamp;
    @Field(type = FieldType.Keyword)
    private LogLevel level;
    private String thread;
    private String className;
    @MultiField(
            mainField = @Field(type = FieldType.Text),
            otherFields = {
                    @InnerField(
                            suffix = "keyword",
                            type = FieldType.Keyword
                    )
            }
    )
    private String message;
    private String pid;
    private String rawLog;
    @Field(type = FieldType.Date)
    private Instant createdAt;

    public void initialize() {
        if (id == null) {
            id = UUID.randomUUID().toString();
        }
        if (createdAt == null) {
            createdAt = Instant.now();
        }
    }
}

