package com.logAnalyzer.core.util;

import com.logAnalyzer.core.exception.StorageException;

import java.nio.file.Path;
import java.util.UUID;

public final class StorageKeyUtil {

    private StorageKeyUtil() {}

    public static String buildKey(String prefix, String userId, String sessionId, String originalFileName) {
        originalFileName = sanitizeFileName(originalFileName);
        int lastDot = originalFileName.lastIndexOf('.');

        String baseName = originalFileName.substring(0, lastDot);
        String extension = originalFileName.substring(lastDot);
        return safeSegment(prefix) + "/" + safeSegment(userId) + "/" + baseName + "_" + sessionId + extension;
    }

    public static String safeSegment(String s) {
        if (s == null || s.isBlank()) return "_";
        return s.replaceAll("[^a-zA-Z0-9._-]", "_");
    }

    public static String sanitizeFileName(String name) {
        if (name == null || name.isBlank()) return "upload.bin";
        String base = name.replaceAll(".*[/\\\\]", "");   // strip any path
        String safe = base.replaceAll("[^a-zA-Z0-9._-]", "_");
        return safe.isBlank() ? "upload.bin" : safe;
    }

    public static Path resolve(Path baseDir, String key) {
        Path p = baseDir.resolve(key).normalize();
        if (!p.startsWith(baseDir)) {
            throw new StorageException("Path traversal attempt: " + key);
        }
        return p;
    }

    /**
     * Extracts the file name portion from a storage key.
     *
     * <p>Given "logs/user-123/session-456/app.log" returns "app.log".
     * Given "app.log" returns "app.log".
     * Given null/blank returns "download".
     *
     * @param key the storage key; may be null
     * @return the last path segment, or "download" if it cannot be derived
     */
    public static String fileNameFromKey(String key) {
        if (key == null || key.isBlank()) {
            return "download";
        }

        // Trim trailing slashes so "logs/a/b/" yields "b" not ""
        String trimmed = key;
        int end = trimmed.length();
        while (end > 0 && trimmed.charAt(end - 1) == '/') {
            end--;
        }
        if (end == 0) {
            return "download";
        }
        trimmed = trimmed.substring(0, end);

        int lastSlash = trimmed.lastIndexOf('/');
        String name = (lastSlash < 0) ? trimmed : trimmed.substring(lastSlash + 1);

        return name.isBlank() ? "download" : name;
    }
}