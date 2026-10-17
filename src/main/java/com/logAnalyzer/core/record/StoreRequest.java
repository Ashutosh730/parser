package com.logAnalyzer.core.record;

import java.io.InputStream;
import java.util.Map;

public record StoreRequest(
        InputStream inputStream,
        long contentLength,
        String contentType,
        String userId,
        String sessionId,
        String originalFileName,
        Map<String, String> metadata
) {}