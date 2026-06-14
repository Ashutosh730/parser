package com.logAnalyzer.parser.mapper;

import com.logAnalyzer.parser.entity.LogEntryDocument;
import com.logAnalyzer.parser.model.ParsedLog;

public final class ParsedLogMapper {

    private ParsedLogMapper() {}

    public static LogEntryDocument toEntity(ParsedLog dto, String sessionId) {
        if (dto == null) return null;
        LogEntryDocument logEntryDocument = LogEntryDocument.builder()
                .id(null)
                .sessionId(sessionId)
                .logTimestamp(dto.getTimestamp())
                .level(dto.getLevel())
                .thread(dto.getThread())
                .className(dto.getClassName())
                .message(dto.getMessage())
                .pid(dto.getPid())
                .rawLog(dto.getRawLog())
                .build();
        logEntryDocument.initialize();
        return logEntryDocument;
    }
}

