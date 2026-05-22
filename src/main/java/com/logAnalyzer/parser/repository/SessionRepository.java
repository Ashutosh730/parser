package com.logAnalyzer.parser.repository;

import com.logAnalyzer.parser.model.LogSession;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface SessionRepository extends JpaRepository<LogSession, Long> {
}
