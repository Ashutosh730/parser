package com.logAnalyzer.parser.ai.repository;

import com.logAnalyzer.parser.ai.entity.AiResult;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface AiResultRepository extends JpaRepository<AiResult, String> {
    List<AiResult> findBySessionId(String id);
}
