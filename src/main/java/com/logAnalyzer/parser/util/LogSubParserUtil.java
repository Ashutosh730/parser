package com.logAnalyzer.parser.util;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;

public class LogSubParserUtil {
    public static String extractHeaderLine(String logLine) {
        if (logLine == null) return "";
        int newlineIndex = logLine.indexOf('\n');
        return newlineIndex < 0 ? logLine : logLine.substring(0, newlineIndex);
    }

    public static String extractContinuationLines(String logLine) {
        if (logLine == null) return "";
        int newlineIndex = logLine.indexOf('\n');
        if (newlineIndex < 0 || newlineIndex >= logLine.length() - 1) return "";
        return logLine.substring(newlineIndex + 1);
    }

    public static Instant parseTimestamp(String timestamp, String pattern) {
        if (timestamp == null) return null;
        if (pattern == null || pattern.isEmpty()) {
            return Instant.parse(timestamp);
        }
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern(pattern);
        return LocalDateTime.parse(timestamp, formatter)
                .atZone(ZoneId.systemDefault())
                .toInstant();
    }
}
