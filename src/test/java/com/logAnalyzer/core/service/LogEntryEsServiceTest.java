package com.logAnalyzer.core.service;

import com.logAnalyzer.core.entity.LogEntryDocument;
import org.junit.jupiter.api.Test;
import org.springframework.data.elasticsearch.core.ElasticsearchOperations;
import org.springframework.data.elasticsearch.core.SearchHits;
import org.springframework.data.elasticsearch.core.query.CriteriaQuery;

import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class LogEntryEsServiceTest {

    @Test
    void searchProductsByPriceRange_shouldDelegateRangeQueryToElasticsearch() {
        ElasticsearchOperations operations = mock(ElasticsearchOperations.class);
        SearchHits<LogEntryDocument> expected = mock(SearchHits.class);
        when(operations.search(any(CriteriaQuery.class), eq(LogEntryDocument.class))).thenReturn(expected);

        LogEntryEsService service = new LogEntryEsService(operations);

        SearchHits<LogEntryDocument> actual = service.searchProductsByPriceRange(10.5, 25.75);

        assertSame(expected, actual);
        verify(operations).search(any(CriteriaQuery.class), eq(LogEntryDocument.class));
    }
}