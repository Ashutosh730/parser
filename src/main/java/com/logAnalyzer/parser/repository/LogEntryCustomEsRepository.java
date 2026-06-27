package com.logAnalyzer.parser.repository;

import co.elastic.clients.elasticsearch._types.FieldValue;
import co.elastic.clients.elasticsearch._types.SortOrder;
import co.elastic.clients.elasticsearch._types.aggregations.Aggregation;
import co.elastic.clients.elasticsearch._types.aggregations.CalendarInterval;
import co.elastic.clients.elasticsearch._types.aggregations.StringTermsBucket;
import co.elastic.clients.util.NamedValue;
import com.logAnalyzer.parser.entity.LogEntryDocument;
import com.logAnalyzer.parser.model.TimelineResult;
import com.logAnalyzer.parser.model.TopErrorResult;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.elasticsearch.client.elc.ElasticsearchAggregations;
import org.springframework.data.elasticsearch.client.elc.NativeQuery;
import org.springframework.data.elasticsearch.core.ElasticsearchOperations;
import org.springframework.data.elasticsearch.core.SearchHits;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@Repository
@RequiredArgsConstructor
public class LogEntryCustomEsRepository {

    private final ElasticsearchOperations elasticsearchOperations;

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

    public List<TopErrorResult> getTopErrors(String sessionId, int size) {

        NativeQuery query = NativeQuery.builder()
                .withQuery(q -> q
                        .bool(b -> b
                                .must(m -> m
                                        .term(t -> t
                                                .field("sessionId.keyword")
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
                .withAggregation("top_errors",
                        Aggregation.of(a -> a
                                .terms(t -> t
                                        .field("message.keyword")  // keyword field for exact aggregation
                                        .size(size)
                                        .order(List.of(NamedValue.of("_count", SortOrder.Desc)))
                                )
                        )
                )
                .withMaxResults(0)
                .build();

        SearchHits<LogEntryDocument> hits = elasticsearchOperations.search(query, LogEntryDocument.class);

        ElasticsearchAggregations aggregations = (ElasticsearchAggregations) hits.getAggregations();

        return aggregations
                .get("top_errors")
                .aggregation()
                .getAggregate()
                .sterms()
                .buckets()
                .array()
                .stream()
                .map(bucket -> TopErrorResult.builder()
                        .message(bucket.key().stringValue())
                        .count(bucket.docCount())
                        .build()
                )
                .toList();

        // Returns: [
        //   { message: "NullPointerException at UserService", count: 23 },
        //   { message: "Connection refused to database", count: 15 },
        // ]
    }

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
}
