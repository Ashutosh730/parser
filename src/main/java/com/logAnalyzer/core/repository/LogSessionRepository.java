package com.logAnalyzer.core.repository;

import com.logAnalyzer.core.entity.LogSession;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Page;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Repository
public interface LogSessionRepository extends JpaRepository<LogSession, String> {
    List<LogSession> findByUserId(String userId);
    Optional<LogSession> findByIdAndUserId(String id, String userId);
    @Transactional
    Page<LogSession> findByUserIdAndDeletedAtIsNull(String userId, Pageable pageable);
    Optional<LogSession> findByIdAndUserIdAndDeletedAtIsNull(String id, String userId);
}
