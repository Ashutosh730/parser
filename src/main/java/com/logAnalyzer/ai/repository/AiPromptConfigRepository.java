package com.logAnalyzer.ai.repository;

import com.logAnalyzer.ai.entity.AiPromptConfig;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AiPromptConfigRepository extends JpaRepository<AiPromptConfig, Long> {
}
