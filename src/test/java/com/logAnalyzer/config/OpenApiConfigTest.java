package com.logAnalyzer.config;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.security.SecurityScheme;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class OpenApiConfigTest {

    @Test
    void documentsApiMetadataAndJwtSecurityScheme() {
        OpenAPIDefinition definition = OpenApiConfig.class.getAnnotation(OpenAPIDefinition.class);
        SecurityScheme securityScheme = OpenApiConfig.class.getAnnotation(SecurityScheme.class);

        assertThat(definition).isNotNull();
        assertThat(definition.info().title()).isEqualTo("Log Analyzer API");
        assertThat(definition.info().version()).isEqualTo("v1");
        assertThat(securityScheme).isNotNull();
        assertThat(securityScheme.name()).isEqualTo("bearerAuth");
        assertThat(securityScheme.bearerFormat()).isEqualTo("JWT");
        assertThat(securityScheme.scheme()).isEqualTo("bearer");
    }
}
