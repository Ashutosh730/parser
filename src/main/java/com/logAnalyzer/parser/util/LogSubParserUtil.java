package com.logAnalyzer.parser.util;

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
}
