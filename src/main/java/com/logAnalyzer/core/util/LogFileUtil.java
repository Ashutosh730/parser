package com.logAnalyzer.core.util;

import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Locale;
import java.util.UUID;

public class LogFileUtil {

    private static final List<String> ALLOWED_EXTENSIONS = List.of(".log", ".txt");

    public static String validateFile(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            return "File is empty";
        }

        return validateFileName(file.getOriginalFilename());
    }

    public static String validateFileName(String originalFileName) {
        if (originalFileName == null || originalFileName.isBlank()) {
            return "Invalid file";
        }

        String normalizedName = stripUrlArtifacts(originalFileName.trim());
        if (normalizedName.isEmpty()) {
            return "Invalid file";
        }

        String filenameOnly = extractFileName(normalizedName);
        if (filenameOnly.isBlank() || filenameOnly.contains(":")) {
            return "Invalid file";
        }

        int lastDot = filenameOnly.lastIndexOf('.');
        if (lastDot <= 0 || lastDot == filenameOnly.length() - 1) {
            return "Invalid file";
        }

        String extension = filenameOnly.substring(lastDot).toLowerCase(Locale.ROOT);
        if (!ALLOWED_EXTENSIONS.contains(extension)) {
            return "Only .log and .txt files are allowed";
        }
        return null;
    }

    public static String generateUniqueFileName(String originalFileName) {
        String sanitizedName = stripUrlArtifacts(originalFileName);
        String filenameOnly = extractFileName(sanitizedName);
        String uploadSessionId = UUID.randomUUID().toString();

        int lastDot = filenameOnly.lastIndexOf('.');
        if (lastDot <= 0) {
            return filenameOnly + "_" + uploadSessionId;
        }

        String baseName = filenameOnly.substring(0, lastDot);
        String extension = filenameOnly.substring(lastDot);
        return baseName + "_" + uploadSessionId + extension;
    }

    private static String stripUrlArtifacts(String value) {
        String withoutQuery = value.replace("\\", "/");
        withoutQuery = withoutQuery.split("[?#]", 2)[0];
        return withoutQuery.trim();
    }

    private static String extractFileName(String value) {
        String normalized = stripUrlArtifacts(value);
        int lastSlash = Math.max(normalized.lastIndexOf('/'), normalized.lastIndexOf('\\'));
        return lastSlash >= 0 ? normalized.substring(lastSlash + 1) : normalized;
    }
}
