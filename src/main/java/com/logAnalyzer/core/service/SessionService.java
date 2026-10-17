package com.logAnalyzer.core.service;

import com.logAnalyzer.core.model.PagedResponse;
import com.logAnalyzer.core.model.SessionResponse;
import org.springframework.data.domain.Pageable;

public interface SessionService {
    String create(String originalFilename, String storagePath, String userId, String sessionId);
    void delete(String sessionId, String userId);
    PagedResponse<SessionResponse> getSessions(String userId, Pageable pageable);
}
