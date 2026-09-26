package com.logAnalyzer.ai.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.datasource.init.DataSourceInitializer;

import javax.sql.DataSource;

@Configuration
public class PgVectorConfig {

    @Bean
    public DataSourceInitializer dataSourceInitializer(DataSource dataSource) {
        DataSourceInitializer initializer = new DataSourceInitializer();
        initializer.setDataSource(dataSource);
        initializer.setDatabasePopulator(connection -> {
            // Enable pgvector extension
            connection.prepareStatement("CREATE EXTENSION IF NOT EXISTS vector")
                    .execute();
        });
        return initializer;
    }
}