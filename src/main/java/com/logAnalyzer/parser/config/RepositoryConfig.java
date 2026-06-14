package com.logAnalyzer.parser.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.data.elasticsearch.repository.config.EnableElasticsearchRepositories;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.FilterType;

@Configuration
@EnableJpaRepositories(
    basePackages = {"com.logAnalyzer.parser.repository", "com.logAnalyzer.parser.ai.repository"},
    excludeFilters = {
        @ComponentScan.Filter(
            type = FilterType.REGEX,
            pattern = ".*ElasticsearchRepository"
        ),
        @ComponentScan.Filter(
            type = FilterType.REGEX,
            pattern = ".*EsRepository"
        )
    }
)
@EnableElasticsearchRepositories(
    basePackages = "com.logAnalyzer.parser.repository",
    includeFilters = {
        @ComponentScan.Filter(
            type = FilterType.REGEX,
            pattern = ".*ElasticsearchRepository"
        ),
        @ComponentScan.Filter(
            type = FilterType.REGEX,
            pattern = ".*EsRepository"
        )
    }
)
public class RepositoryConfig {
}

