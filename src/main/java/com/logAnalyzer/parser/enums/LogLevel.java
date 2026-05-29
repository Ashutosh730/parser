package com.logAnalyzer.parser.enums;

//TRACE -> DEBUG -> INFO -> WARN -> ERROR -> FATAL
public enum LogLevel {
    TRACE,
    DEBUG,
    INFO,
    WARN,
    ERROR,
    FATAL,
    OFF,
    ALL;

    public LogLevel toLogLevel(String level) {
        if (level == null) {
            return LogLevel.INFO; // or whatever default you want
        }

        try {
            return LogLevel.valueOf(level.toUpperCase());
        } catch (IllegalArgumentException e) {
            return LogLevel.INFO; // fallback for unknown values
        }
    }
}