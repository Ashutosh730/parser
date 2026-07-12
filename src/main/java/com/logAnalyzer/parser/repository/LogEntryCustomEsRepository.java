package com.logAnalyzer.parser.repository;

import co.elastic.clients.elasticsearch._types.FieldValue;
import co.elastic.clients.elasticsearch._types.aggregations.Aggregate;
import co.elastic.clients.elasticsearch._types.aggregations.Aggregation;
import co.elastic.clients.elasticsearch._types.aggregations.CalendarInterval;
import co.elastic.clients.elasticsearch._types.aggregations.StringTermsBucket;
import com.logAnalyzer.parser.ai.model.InterpretedFilter;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.elasticsearch.client.elc.ElasticsearchAggregations;
import com.logAnalyzer.parser.entity.LogEntryDocument;
import com.logAnalyzer.parser.model.TimelineResult;
import com.logAnalyzer.parser.model.TopErrorResult;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.elasticsearch.client.elc.NativeQuery;
import org.springframework.data.elasticsearch.core.ElasticsearchOperations;
import org.springframework.data.elasticsearch.core.SearchHit;
import org.springframework.data.elasticsearch.core.SearchHits;
import org.springframework.data.elasticsearch.core.query.Criteria;
import org.springframework.data.elasticsearch.core.query.CriteriaQuery;
import org.springframework.data.elasticsearch.core.query.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Slf4j
@Repository
@RequiredArgsConstructor
public class LogEntryCustomEsRepository {

    private final ElasticsearchOperations elasticsearchOperations;

    public SearchHits<LogEntryDocument> getSearchResult(String sessionId, InterpretedFilter filter) {

            Criteria criteria = Criteria.where("sessionId").is(sessionId);

            // Level filter — validate against enum first, ignore if invalid
            if (filter.getLevels() != null && !filter.getLevels().isEmpty()) {
                List<String> validLevels = filter.getLevels().stream()
                        .filter(this::isValidLevel)
                        .toList();

                if (!validLevels.isEmpty()) {
                    criteria = criteria.and("level").in(validLevels);  // ES "terms" query
                }
            }

            // Keyword search — OR across message field
            if (filter.getKeywords() != null && !filter.getKeywords().isEmpty()) {
                Criteria keywordCriteria = null;
                for (String keyword : filter.getKeywords()) {
                    Criteria c = Criteria.where("message").contains(keyword);
                    keywordCriteria = (keywordCriteria == null) ? c : keywordCriteria.or(c);
                }
                criteria = criteria.and(keywordCriteria);
            }

            // Time range
            if (filter.getTimeFrom() != null && filter.getTimeTo() != null) {
                criteria = criteria.and("logTimestamp")
                        .greaterThanEqual(filter.getTimeFrom())
                        .lessThanEqual(filter.getTimeTo());
            }

            // Class/service name
            if (filter.getClassName() != null) {
                criteria = criteria.and("className").contains(filter.getClassName());
            }

        Query esQuery = new CriteriaQuery(criteria).setPageable(PageRequest.of(0, 100));
        return elasticsearchOperations.search(esQuery, LogEntryDocument.class);
    }

    private boolean isValidLevel(String level) {
        return Set.of("ERROR", "WARN", "INFO", "DEBUG").contains(level.toUpperCase());
    }


    public Map<String, Long> getLevelDistribution(String sessionId, List<String> levels) {

        NativeQuery query = NativeQuery.builder()
                .withQuery(q -> q
                        .bool(b -> {
                            // always filter by session
                            b.must(m -> m
                                    .term(t -> t
                                            .field("sessionId.keyword")
                                            .value(sessionId)
                                    )
                            );

                            // filter by levels only if specified
                            if (levels != null && !levels.isEmpty()) {
                                b.must(m -> m
                                        .terms(t -> t
                                                .field("level.keyword")
                                                .terms(tv -> tv
                                                        .value(levels.stream()
                                                                .map(FieldValue::of)
                                                                .toList()
                                                        )
                                                )
                                        )
                                );
                            }

                            return b;
                        })
                )
                .withAggregation("level_distribution",
                        Aggregation.of(a -> a
                                .terms(t -> t.field("level.keyword").size(10))
                        )
                )
                .withMaxResults(0)
                .build();

        SearchHits<LogEntryDocument> hits = elasticsearchOperations.search(query, LogEntryDocument.class);

        // Extract aggregation result
        ElasticsearchAggregations aggregations = (ElasticsearchAggregations) hits.getAggregations();

        return aggregations
                .get("level_distribution")
                .aggregation()
                .getAggregate()
                .sterms()
                .buckets()
                .array()
                .stream()
                .collect(Collectors.toMap(
                        bucket -> bucket.key().stringValue(),
                        StringTermsBucket::docCount
                ));

        // Returns: { "ERROR": 47, "WARN": 203, "INFO": 5821, "DEBUG": 120 }
    }

//    will implement later
//    public List<TopErrorResult> getTopErrors(String sessionId, int size) {
//
//        NativeQuery query = NativeQuery.builder()
//                .withQuery(q -> q.bool(b -> b
//                        .filter(f -> f
//                                .term(t -> t
//                                        .field("sessionId.keyword")
//                                        .value(sessionId)
//                                )
//                        )
//                        .filter(f -> f
//                                .term(t -> t
//                                        .field("level.keyword")
//                                        .value("ERROR")
//                                )
//                        )
//                ))
//                .withAggregation(
//                        "top_errors",
//                        Aggregation.of(a -> a
//                                .terms(t -> t
//                                        .field("message.keyword")
//                                        .size(size)
//                                )
//                        )
//                )
////                .withMaxResults(0)
//                .build();
//
//
//        SearchHits<LogEntryDocument> hits =
//                elasticsearchOperations.search(query, LogEntryDocument.class);
//
//
//        ElasticsearchAggregations aggs =
//                (ElasticsearchAggregations) hits.getAggregations();
//
//
//        if (aggs == null) {
//            return List.of();
//        }
//
//
//        Aggregate aggregate = aggs.aggregations()
//                .stream()
//                .filter(a -> a.aggregation().getName().equals("top_errors"))
//                .findFirst()
//                .orElseThrow()
//                .aggregation()
//                .getAggregate();
//
//
//        return aggregate.sterms()
//                .buckets()
//                .array()
//                .stream()
//                .map(bucket ->
//                        TopErrorResult.builder()
//                                .message(bucket.key().stringValue())
//                                .count(bucket.docCount())
//                                .build()
//                )
//                .toList();
//    }

    public List<TimelineResult> getErrorTimeline(String sessionId) {

        NativeQuery query = NativeQuery.builder()
                .withQuery(q -> q
                        .bool(b -> b
                                .must(m -> m
                                        .term(t -> t
                                                .field("sessionId")
                                                .value(sessionId)
                                        )
                                )
                                .must(m -> m
                                        .term(t -> t
                                                .field("level.keyword")
                                                .value("ERROR")
                                        )
                                )
                        )
                )
                .withAggregation("error_timeline",
                        Aggregation.of(a -> a
                                .dateHistogram(d -> d
                                        .field("timestamp")
                                        .calendarInterval(CalendarInterval.Hour)  // group by hour
                                        .format("yyyy-MM-dd'T'HH:mm:ss")
                                )
                        )
                )
                .withMaxResults(0)
                .build();

        SearchHits<LogEntryDocument> hits = elasticsearchOperations.search(query, LogEntryDocument.class);

        ElasticsearchAggregations aggregations = (ElasticsearchAggregations) hits.getAggregations();

        return aggregations
                .get("error_timeline")
                .aggregation()
                .getAggregate()
                .dateHistogram()
                .buckets()
                .array()
                .stream()
                .map(bucket -> TimelineResult.builder()
                        .timestamp(bucket.keyAsString())
                        .count(bucket.docCount())
                        .build()
                )
                .toList();

        // Returns: [
        //   { timestamp: "2024-01-15T10:00:00", count: 5 },
        //   { timestamp: "2024-01-15T11:00:00", count: 23 },
        //   { timestamp: "2024-01-15T12:00:00", count: 2 },
        // ]
    }
    public List<LogEntryDocument> findBySessionIdAndKeywords(String sessionId, List<String> keywords, PageRequest pageRequest) {
        NativeQuery query = NativeQuery.builder()
                .withQuery(q -> q
                        .bool(b -> {
                            // session filter
                            b.must(m -> m
                                    .term(t -> t
                                            .field("sessionId.keyword")
                                            .value(sessionId)
                                    )
                            );

                            // keyword filter — match any keyword in message
                            if (keywords != null && !keywords.isEmpty()) {
                                b.must(m -> m
                                        .bool(kb -> {
                                            keywords.forEach(keyword ->
                                                    kb.should(s -> s
                                                            .match(mt -> mt
                                                                    .field("message")
                                                                    .query(keyword)
                                                            )
                                                    )
                                            );
                                            kb.minimumShouldMatch("1");
                                            return kb;
                                        })
                                );
                            }

                            return b;
                        })
                )
                .withPageable(pageRequest)
                .build();

        return elasticsearchOperations
                .search(query, LogEntryDocument.class)
                .stream()
                .map(SearchHit::getContent)
                .toList();
    }
}
