package com.logAnalyzer.parser.repository;

import com.logAnalyzer.parser.entity.LogSession;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface LogSessionRepository extends JpaRepository<LogSession, String> {
    List<LogSession> findByUserId(String userId);
    Optional<LogSession> findByIdAndUserId(String id, String userId);
}
