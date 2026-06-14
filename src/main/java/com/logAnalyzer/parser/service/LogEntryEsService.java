package com.logAnalyzer.parser.service;

import com.logAnalyzer.parser.entity.LogEntryDocument;
import lombok.RequiredArgsConstructor;
import org.springframework.data.elasticsearch.core.ElasticsearchOperations;
import org.springframework.data.elasticsearch.core.SearchHits;
import org.springframework.data.elasticsearch.core.query.Criteria;
import org.springframework.data.elasticsearch.core.query.CriteriaQuery;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class LogEntryEsService {
    private final ElasticsearchOperations elasticsearchOperations;

    public SearchHits<LogEntryDocument> searchProductsByPriceRange(double minPrice, double maxPrice) {
        Criteria criteria = new Criteria("price")
                .greaterThanEqual(minPrice)
                .lessThanEqual(maxPrice);

        CriteriaQuery query = new CriteriaQuery(criteria);
        return elasticsearchOperations.search(query, LogEntryDocument.class);
    }
}
