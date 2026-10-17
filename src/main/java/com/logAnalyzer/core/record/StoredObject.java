package com.logAnalyzer.core.record;

/**
 * Opaque key returned from store().
 * The value is provider-specific but callers must treat it as a black box.
 */
public record StoredObject(
        String key,
        long sizeBytes,
        String contentType,
        String provider
) {}