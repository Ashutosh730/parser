package com.logAnalyzer.parser.util;

import java.util.UUID;

public class LogFileUtil {
    public static String generateUniqueFileName(String originalFileName) {
        String uploadSessionId = UUID.randomUUID().toString();
        String[] parts = originalFileName.split("\\.(?=[^.]+$)");
        return parts[0] + "_" + uploadSessionId + "." + parts[1];
    }
}
