package com.logAnalyzer.core.repository;

import com.logAnalyzer.core.entity.LogParserConfig;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface LogParserConfigRepository extends JpaRepository<LogParserConfig, Long> {
}
