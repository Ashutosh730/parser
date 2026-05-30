package com.logAnalyzer.parser.util;

import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.UUID;

public class LogFileUtil {

    private static final List<String> ALLOWED_EXTENSIONS = List.of(".log", ".txt");

    public static String validateFile(MultipartFile file) {
        if (file.isEmpty()) {
            return "File is empty";
        }

        String name = file.getOriginalFilename();
        if (name == null || !name.contains(".")) {
            return "Invalid file";
        }

        String extension = name.substring(name.lastIndexOf(".")).toLowerCase();
        if (!ALLOWED_EXTENSIONS.contains(extension)) {
            return "Only .log and .txt files are allowed";
        }
        return null;
    }

    public static String generateUniqueFileName(String originalFileName) {
        String uploadSessionId = UUID.randomUUID().toString();
        String[] parts = originalFileName.split("\\.(?=[^.]+$)");
        return parts[0] + "_" + uploadSessionId + "." + parts[1];
    }
}
