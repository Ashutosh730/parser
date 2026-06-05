package com.logAnalyzer.parser.repository;

import com.logAnalyzer.parser.entity.LogSession;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface LogSessionRepository extends JpaRepository<LogSession, String> {
}
