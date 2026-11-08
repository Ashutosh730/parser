package com.logAnalyzer.ai.repository;

import com.logAnalyzer.ai.entity.AiPromptConfig;
import com.logAnalyzer.ai.enums.AiPromptKey;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface AiPromptConfigRepository extends JpaRepository<AiPromptConfig, Long> {
}
