package com.logAnalyzer.core.service;

public interface SessionService {
    String create(String originalFilename, String storagePath, String userId);
    void delete(String sessionId, String userId);
}
