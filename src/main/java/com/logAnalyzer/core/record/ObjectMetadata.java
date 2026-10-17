package com.logAnalyzer.core.record;

import java.time.Instant;

public record ObjectMetadata(
        String key,
        long sizeBytes,
        String contentType,
        Instant lastModified,
        String provider
) {}