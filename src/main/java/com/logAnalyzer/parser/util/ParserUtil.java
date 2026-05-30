package com.logAnalyzer.parser.util;

import com.logAnalyzer.parser.enums.DetectedLanguage;

public class ParserUtil {
    public static DetectedLanguage getDetectedLanguage(String className) {
        if(className.toLowerCase().contains("java")) {
            return DetectedLanguage.JAVA;
        } else if(className.toLowerCase().contains("python")) {
            return DetectedLanguage.PYTHON;
        } else if(className.toLowerCase().contains("javascript")) {
            return DetectedLanguage.JAVA_SCRIPT;
        }
        return DetectedLanguage.UNKNOWN;
    }
}
