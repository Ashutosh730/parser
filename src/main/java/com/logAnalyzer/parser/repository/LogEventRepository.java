package com.logAnalyzer.parser.repository;

import com.logAnalyzer.parser.model.parsed.LogEvent;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface LogEventRepository extends JpaRepository<LogEvent, String> {
}

