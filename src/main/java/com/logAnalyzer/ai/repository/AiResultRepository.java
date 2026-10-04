package com.logAnalyzer.ai.repository;

import com.logAnalyzer.ai.entity.AiResult;
import com.logAnalyzer.ai.enums.AiFeatureEnum;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface AiResultRepository extends JpaRepository<AiResult, String> {
    List<AiResult> findBySessionIdAndFeature(String id, AiFeatureEnum feature);
    void deleteBySessionId(String sessionId);
}
