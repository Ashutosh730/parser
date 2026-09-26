package com.logAnalyzer.core.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.data.elasticsearch.repository.config.EnableElasticsearchRepositories;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.FilterType;

@Configuration
@EnableJpaRepositories(
    basePackages = {"com.logAnalyzer.core.repository", "com.logAnalyzer.ai.repository", "com.logAnalyzer.auth.repository"},
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
    basePackages = "com.logAnalyzer.core.repository",
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

