package com.logAnalyzer.core.repository;

import co.elastic.clients.elasticsearch._types.FieldValue;
import com.logAnalyzer.ai.model.InterpretedFilter;
import com.logAnalyzer.core.entity.LogEntryDocument;
import org.springframework.data.elasticsearch.client.elc.ElasticsearchAggregations;
import org.springframework.data.elasticsearch.client.elc.NativeQuery;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.elasticsearch.core.SearchHit;
import org.springframework.data.elasticsearch.core.SearchHits;
import org.springframework.data.elasticsearch.core.ElasticsearchOperations;
import org.springframework.data.elasticsearch.core.query.Query;

import java.util.List;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.RETURNS_DEEP_STUBS;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class LogEntryCustomEsRepositoryTest {

    @Mock
    private ElasticsearchOperations operations;

    @Mock
    private SearchHits<LogEntryDocument> searchHits;

    @Mock
    private SearchHit<LogEntryDocument> searchHit;

    private LogEntryCustomEsRepository repository;

    @BeforeEach
    void setUp() {
        repository = new LogEntryCustomEsRepository(operations);
    }

    @Test
    void getSearchResult_shouldBuildQueryAndDelegateToElasticsearch() {
        InterpretedFilter filter = InterpretedFilter.builder()
                .levels(List.of("ERROR", "invalid"))
                .keywords(List.of("timeout", "database"))
                .className("ExampleService")
                .timeFrom("2026-09-27T00:00:00Z")
                .timeTo("2026-09-27T23:59:59Z")
                .build();
        when(operations.search(any(Query.class), eq(LogEntryDocument.class)))
                .thenReturn(searchHits);

        SearchHits<LogEntryDocument> result =
                repository.getSearchResult("session-1", filter);

        assertSame(searchHits, result);
        ArgumentCaptor<Query> query = ArgumentCaptor.forClass(Query.class);
        verify(operations).search(query.capture(), eq(LogEntryDocument.class));
        assertEquals(0, query.getValue().getPageable().getPageNumber());
        assertEquals(100, query.getValue().getPageable().getPageSize());
    }

    @Test
    void getSearchResult_shouldSupportEmptyOptionalFilters() {
        when(operations.search(any(Query.class), eq(LogEntryDocument.class)))
                .thenReturn(searchHits);

        assertSame(searchHits, repository.getSearchResult(
                "session-1", InterpretedFilter.builder().build()));
        verify(operations).search(any(Query.class), eq(LogEntryDocument.class));
    }

    @Test
    void getErrorTimeline_shouldUseMappedFieldsInGeneratedQuery() {
        ElasticsearchAggregations aggregations = mock(ElasticsearchAggregations.class, RETURNS_DEEP_STUBS);
        doReturn(aggregations).when(searchHits).getAggregations();
        when(aggregations.get("error_timeline").aggregation().getAggregate()
                .dateHistogram().buckets().array()).thenReturn(List.of());
        when(operations.search(any(Query.class), eq(LogEntryDocument.class)))
                .thenReturn(searchHits);

        repository.getErrorTimeline("session-1");

        ArgumentCaptor<NativeQuery> query = ArgumentCaptor.forClass(NativeQuery.class);
        verify(operations).search(query.capture(), eq(LogEntryDocument.class));
        assertEquals("sessionId", query.getValue().getQuery().bool().must().get(0).term().field());
        assertEquals("level", query.getValue().getQuery().bool().must().get(1).term().field());
        assertEquals("logTimestamp", query.getValue().getAggregations()
                .get("error_timeline").dateHistogram().field());
    }

    @Test
    void getLevelDistribution_shouldMapAggregationBucketsToCounts() {
        ElasticsearchAggregations aggregations =
                mock(ElasticsearchAggregations.class, RETURNS_DEEP_STUBS);
        co.elastic.clients.elasticsearch._types.aggregations.StringTermsBucket errorBucket =
                mock(co.elastic.clients.elasticsearch._types.aggregations.StringTermsBucket.class);
        co.elastic.clients.elasticsearch._types.aggregations.StringTermsBucket warnBucket =
                mock(co.elastic.clients.elasticsearch._types.aggregations.StringTermsBucket.class);

        when(errorBucket.key()).thenReturn(FieldValue.of("ERROR"));
        when(errorBucket.docCount()).thenReturn(7L);
        when(warnBucket.key()).thenReturn(FieldValue.of("WARN"));
        when(warnBucket.docCount()).thenReturn(3L);
        when(aggregations.get("level_distribution")
                .aggregation().getAggregate().sterms().buckets().array())
                .thenReturn(List.of(errorBucket, warnBucket));
        doReturn(aggregations).when(searchHits).getAggregations();
        when(operations.search(any(Query.class), eq(LogEntryDocument.class)))
                .thenReturn(searchHits);

        java.util.Map<String, Long> result = repository.getLevelDistribution(
                "session-1", List.of("ERROR", "WARN"));

        assertEquals(java.util.Map.of("ERROR", 7L, "WARN", 3L), result);
        ArgumentCaptor<NativeQuery> query = ArgumentCaptor.forClass(NativeQuery.class);
        verify(operations).search(query.capture(), eq(LogEntryDocument.class));
        assertEquals("sessionId", query.getValue().getQuery().bool().must().get(0).term().field());
        assertEquals("level", query.getValue().getQuery().bool().must().get(1).terms().field());
        assertEquals("level", query.getValue().getAggregations()
                .get("level_distribution").terms().field());
    }

    @Test
    void findBySessionIdAndKeywords_shouldMapSearchHitsToDocuments() {
        LogEntryDocument first = LogEntryDocument.builder().id("one").build();
        LogEntryDocument second = LogEntryDocument.builder().id("two").build();
        when(searchHit.getContent()).thenReturn(first, second);
        when(searchHits.stream()).thenReturn(Stream.of(searchHit, searchHit));
        when(operations.search(any(Query.class), eq(LogEntryDocument.class)))
                .thenReturn(searchHits);

        List<LogEntryDocument> result = repository.findBySessionIdAndKeywords(
                "session-1", List.of("timeout", "failure"), PageRequest.of(1, 10));

        assertEquals(List.of(first, second), result);
        ArgumentCaptor<NativeQuery> query = ArgumentCaptor.forClass(NativeQuery.class);
        verify(operations).search(query.capture(), eq(LogEntryDocument.class));
        assertEquals("sessionId", query.getValue().getQuery().bool().must().get(0).term().field());
    }

    @Test
    void findBySessionIdAndKeywords_shouldSupportNullKeywords() {
        when(searchHits.stream()).thenReturn(Stream.empty());
        when(operations.search(any(Query.class), eq(LogEntryDocument.class)))
                .thenReturn(searchHits);

        List<LogEntryDocument> result = repository.findBySessionIdAndKeywords(
                "session-1", null, PageRequest.of(0, 5));

        assertEquals(List.of(), result);
    }
}
