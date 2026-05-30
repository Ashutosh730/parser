package com.logAnalyzer.parser.mapper;

import com.logAnalyzer.parser.model.parsed.LogEvent;
import com.logAnalyzer.parser.model.parsed.ParsedLog;

public final class ParsedLogMapper {

    private ParsedLogMapper() {}

    public static LogEvent toEntity(ParsedLog dto, String sessionId) {
        if (dto == null) return null;
        return LogEvent.builder()
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
    }
}

