package com.logAnalyzer.parser.repository;

import com.logAnalyzer.parser.entity.LogEntryDocument;
import com.logAnalyzer.parser.enums.LogLevel;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.elasticsearch.repository.ElasticsearchRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface LogEntryEsRepository extends ElasticsearchRepository<LogEntryDocument, String> {
    List<LogEntryDocument> findBySessionIdAndLevelIn(String sessionId, List<LogLevel> levels, PageRequest pageRequest);
}