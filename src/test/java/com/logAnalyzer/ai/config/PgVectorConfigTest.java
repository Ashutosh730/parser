package com.logAnalyzer.ai.config;

import org.junit.jupiter.api.Test;
import org.springframework.jdbc.datasource.init.DataSourceInitializer;

import javax.sql.DataSource;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.mock;

class PgVectorConfigTest {

    @Test
    void dataSourceInitializer_shouldBeConfiguredWithDataSource() {
        DataSource dataSource = mock(DataSource.class);

        DataSourceInitializer initializer =
                new PgVectorConfig().dataSourceInitializer(dataSource);

        assertNotNull(initializer);
    }
}
